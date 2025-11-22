
package net.caffeinemc.mods.lithium.common.world.interests.iterator;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.caffeinemc.mods.lithium.common.util.Distances;
import net.caffeinemc.mods.lithium.common.util.tuples.SortedPointOfInterest;
import net.caffeinemc.mods.lithium.common.world.interests.PointOfInterestSetExtended;
import net.caffeinemc.mods.lithium.common.world.interests.RegionBasedStorageSectionExtended;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A specialized spliterator which returns points of interests fom the center of the search radius, outwards. This can
 * provide a huge reduction in time for situations where an entity needs to search for a point of interest around a
 * location in the world. For example, nether portals ordinarily search a huge volume around the "expected" location
 * of a portal, but it is almost always right at or nearby to the search origin.
 */
public class NearbyPointOfInterestStream extends Spliterators.AbstractSpliterator<PoiRecord> {
    private final RegionBasedStorageSectionExtended<PoiSection> storage;

    private final Predicate<Holder<PoiType>> typeSelector;
    private final PoiManager.Occupancy occupationStatus;

    private final ArrayList<SortedPointOfInterest> points;
    private final Predicate<PoiRecord> afterSortingPredicate;
    private final Consumer<PoiRecord> collector;
    private final BlockPos origin;

    private final int clampedOriginChunkY;

    private final int chunkYMin;
    private final double minChunkYDistSq;

    private final ObjectArrayList<QueuedSection> queuedPOISections;
    private int queuedSectionsSearched;
    private boolean forciblyDeplete;
    private final int forciblyDepleteTrigger;
    private int ring;
    private final int ringMax;
    private final LongIterator ringIterator;

    private final int ringClosestEdgeDistance;
    private double closestRingDistanceSq;

    private double nextSectionDistanceSq;
    private double lowestWaitingDistanceSq;
    private final double distanceLimitL2Sq;
    private int pointIndex;
    private final Comparator<? super SortedPointOfInterest> pointComparator;

    public NearbyPointOfInterestStream(Predicate<Holder<PoiType>> typeSelector,
                                       PoiManager.Occupancy status,
                                       boolean useSquareDistanceLimit,
                                       boolean preferNegativeY,
                                       @Nullable Predicate<PoiRecord> afterSortingPredicate,
                                       BlockPos origin, int radius,
                                       RegionBasedStorageSectionExtended<PoiSection> storage) {
        super(Long.MAX_VALUE, Spliterator.ORDERED);

        this.storage = storage;

        this.points = new ArrayList<>();
        this.occupationStatus = status;
        this.typeSelector = typeSelector;

        this.origin = origin;
        this.chunkYMin = this.storage.lithium$getChunkYMin();
        final int chunkYMax = this.storage.lithium$getChunkYMaxInclusive();

        this.clampedOriginChunkY = Math.clamp(SectionPos.blockToSectionCoord(origin.getY()), chunkYMin, chunkYMax);

        // If the origin is outside of build limit (e.g. 1.18+ overworld to nether portal teleport) in the target dimension,
        // the min Y distance is not zero for a chunk.
        final int minChunkYDist = Math.min(Math.max(this.origin.getY(), SectionPos.sectionToBlockCoord(this.chunkYMin)),
                SectionPos.sectionToBlockCoord(chunkYMax, 15)) - this.origin.getY();
        this.minChunkYDistSq = minChunkYDist * minChunkYDist;

        final int originX = this.origin.getX();
        final int originZ = this.origin.getZ();
        final int chunkMaxX = SectionPos.blockToSectionCoord(origin.getX() + radius);
        final int chunkMinX = SectionPos.blockToSectionCoord(origin.getX() - radius);
        final int chunkMaxZ = SectionPos.blockToSectionCoord(origin.getZ() + radius);
        final int chunkMinZ = SectionPos.blockToSectionCoord(origin.getZ() - radius);

        // Chunk searches will expand in a concentric rings around the origin chunk
        this.ring = 0;
        final int originChunkX = SectionPos.blockToSectionCoord(originX);
        final int originChunkZ = SectionPos.blockToSectionCoord(originZ);
        this.ringMax = Math.max(Math.max(chunkMaxX - originChunkX, originChunkX - chunkMinX),
                Math.max(chunkMaxZ - originChunkZ, originChunkZ - chunkMinZ));
        this.ringIterator = getRingsOfChunksIterator(originChunkX, chunkMaxX, chunkMinX, originChunkZ, chunkMaxZ, chunkMinZ);

        // Initialize Ring Distances
        // This is used to decide the minimum distance to possible POI located in the next ring
        this.ringClosestEdgeDistance = Math.min(Math.min((originX & 15) + 1, 16 - originX & 15),
                Math.min((originZ & 15) + 1, 16 - originZ & 15));
        this.closestRingDistanceSq = this.getPotentialRingDistanceSq();

        this.nextSectionDistanceSq = Double.MAX_VALUE;
        this.lowestWaitingDistanceSq = Double.MAX_VALUE;

        // Note: This is much faster than PriorityHeapQueue because dequeues become very expensive
        // Also keep track of square distances because otherwise comparisons become very expensive
        // Todo: If and when value records are thing convert over to it
        final int sectionsPerChunk = chunkYMax - chunkYMin + 1;
        final int listSize = sectionsPerChunk * 9; // Sufficient to store 2 full rings [ring: 0-1]
        this.queuedPOISections = new ObjectArrayList<>(listSize);
        this.queuedSectionsSearched = 0; // Also the index for the next queued section
        this.forciblyDeplete = false;
        this.forciblyDepleteTrigger = listSize - sectionsPerChunk; // Leave 1 chunk's worth left to avoid resizing

        if (useSquareDistanceLimit) {
            this.collector = (point) -> {
                if (Distances.isWithinSquareRadius(this.origin, radius, point.getPos())) {
                    this.points.add(new SortedPointOfInterest(point, this.origin));
                }
            };
        } else {
            double radiusSq = radius * radius;
            this.collector = (point) -> {
                if (Distances.isWithinCircleRadius(this.origin, radiusSq, point.getPos())) {
                    this.points.add(new SortedPointOfInterest(point, this.origin));
                }
            };
        }

        this.distanceLimitL2Sq = useSquareDistanceLimit ? radius * radius * 2 : radius * radius;
        this.afterSortingPredicate = afterSortingPredicate;
        this.pointComparator = preferNegativeY ? (o1, o2) -> {
            // Use the cached values from earlier
            int cmp = Double.compare(o1.distanceSq(), o2.distanceSq());

            if (cmp != 0) {
                return cmp;
            }

            // Sort by the y-coord (bottom-most first) if any points share an identical distance from one another
            int negativeY = Integer.compare(o1.getY(), o2.getY());
            if (negativeY != 0) {
                return negativeY;
            }

            // Sort by the chunk coord
            int cmp3 = Integer.compare(SectionPos.blockToSectionCoord(o1.getX()), SectionPos.blockToSectionCoord(o2.getX()));
            if (cmp3 != 0) {
                return cmp3;
            }
            return Integer.compare(SectionPos.blockToSectionCoord(o1.getZ()), SectionPos.blockToSectionCoord(o2.getZ()));

        } : (o1, o2) -> {
            // Use the cached values from earlier
            int cmp = Double.compare(o1.distanceSq(), o2.distanceSq());

            if (cmp != 0) {
                return cmp;
            }

            // Sort by the chunk coord
            int cmp2 = Integer.compare(SectionPos.blockToSectionCoord(o1.getX()), SectionPos.blockToSectionCoord(o2.getX()));
            if (cmp2 != 0) {
                return cmp2;
            }
            int cmp3 = Integer.compare(SectionPos.blockToSectionCoord(o1.getZ()), SectionPos.blockToSectionCoord(o2.getZ()));
            if (cmp3 != 0) {
                return cmp3;
            }
            return Integer.compare(SectionPos.blockToSectionCoord(o1.getY()), SectionPos.blockToSectionCoord(o2.getY()));

        };
    }

    @Override
    public boolean tryAdvance(Consumer<? super PoiRecord> action) {
        // Accepted POI:

        // Order of the POI:
        // return closest accepted POI (L2 distance). If several exist:
        // return the one with most negative Y. If several exist:
        // return the one with most negative X. If several exist:
        // return the one with most negative Z. If several exist: Be confused about two POIs being in the same location.

        if (this.pointIndex < this.points.size()) {
            if (this.tryAdvancePoint(action)) {
                return true;
            }
        }

        while (this.ringIterator.hasNext() || !this.isSectionListEmpty()) {
            this.keepAddingRingsUntilSufficient();

            final int previousSize = this.points.size();
            while (!this.isSectionListEmpty() && this.lowestWaitingDistanceSq >= this.getMinimumNextPotentialDistanceSq()) {
                final long sectionPos = queuedPOISections.get(this.queuedSectionsSearched++).sectionPos;
                this.nextSectionDistanceSq = this.getNextSectionDistanceSq();
                final Optional<PoiSection> poiSection = this.storage.lithium$getElementAt(sectionPos);
                if (poiSection.isPresent()) {
                    ((PointOfInterestSetExtended) poiSection.get())
                            .lithium$collectMatchingPoints(this.typeSelector, this.occupationStatus, this.collector);
                }

                this.forciblyDeplete = (!this.forciblyDeplete || !this.isSectionListEmpty()) && this.forciblyDeplete;
                if (this.points.size() > previousSize) {
                    this.points.subList(this.pointIndex, this.points.size()).sort(this.pointComparator);
                    break;
                }

                if (!forciblyDeplete && this.nextSectionDistanceSq > this.closestRingDistanceSq) {
                    break;
                }
            }

            // Return the first point in the chunk
            if (this.tryAdvancePoint(action)) {
                return true; //Returns true when progress was made by consuming an element
            }
        }

        return this.tryAdvancePoint(action);
    }

    private boolean tryAdvancePoint(Consumer<? super PoiRecord> action) {
        while (this.pointIndex < this.points.size()) {
            SortedPointOfInterest next = this.points.get(this.pointIndex);

            // Only consume points if we are sure that there are no closer (or same distance) points to be scanned.
            // Otherwise, scan more chunks
            if (next.distanceSq() >= this.getMinimumNextPotentialDistanceSq()) {
                this.lowestWaitingDistanceSq = next.distanceSq();
                return false;
            }
            this.pointIndex++;

            if (this.afterSortingPredicate == null || this.afterSortingPredicate.test(next.poi())) {
                action.accept(next.poi());
                return true; //Progress was made
            }
            //Continue with the other points when condition is not met
        }
        return false; //No more points. Scan more chunks
    }

    /*
     * Keep searching for POI section in concentric rings of chunks around the origin. Continue if:
     * List is empty
     * There are more rings to scan.
     * The closer of:
     * - The next section
     * - The closest POI in the point list
     * is not closer than:
     * - Unchecked possible sections in the list
     * - Chunks in next ring
     */
    private void keepAddingRingsUntilSufficient() {
        if (!this.forciblyDeplete && this.ringIterator.hasNext() &&
                (Math.min(this.lowestWaitingDistanceSq, this.nextSectionDistanceSq) >= this.closestRingDistanceSq)) {
            this.queuedPOISections.removeElements(0, this.queuedSectionsSearched);
            this.queuedSectionsSearched = 0;
            int ringStart = this.ring;
            do {
                final long chunkPos = this.ringIterator.nextLong();
                final int currentChunkX = ChunkPos.getX(chunkPos);
                final int currentChunkZ = ChunkPos.getZ(chunkPos);

                if (this.distanceLimitL2Sq >= Distances.getMinChunkToBlockDistanceL2Sq(this.origin, currentChunkX, currentChunkZ)) {
                    // This iterates the column in order of the closest section
                    // Doing so noticeably reduces sorting duration later on because parts of the list will already be sorted
                    final BitSet poiSections = this.storage.lithium$getNonEmptyPOISections(currentChunkX, currentChunkZ);
                    int upperBit = poiSections.nextSetBit(this.clampedOriginChunkY - this.chunkYMin);
                    int upperDist = this.getYDistanceFromBitIndex(upperBit);
                    int lowerBit = poiSections.previousSetBit(this.clampedOriginChunkY - this.chunkYMin - 1);
                    int lowerDist = this.getYDistanceFromBitIndex(lowerBit);
                    while (upperBit != -1 || lowerBit != -1) {
                        final int currentChunkY;
                        if (lowerDist <= upperDist) {
                            currentChunkY = lowerBit + this.chunkYMin;
                            lowerBit = lowerBit == -1 ? -1 : poiSections.previousSetBit(lowerBit - 1);
                            lowerDist = this.getYDistanceFromBitIndex(lowerBit);
                        } else {
                            currentChunkY = upperBit + this.chunkYMin;
                            upperBit = upperBit == -1 ? -1 : poiSections.nextSetBit(upperBit + 1);
                            upperDist = this.getYDistanceFromBitIndex(upperBit);
                        }
                        this.queuedPOISections.add(
                                new QueuedSection(
                                        SectionPos.asLong(currentChunkX, currentChunkY, currentChunkZ),
                                        Distances.getMinSectionDistanceSq(
                                                this.origin, currentChunkX, currentChunkY, currentChunkZ)
                                )
                        );
                    }

                    this.forciblyDeplete = this.queuedPOISections.size() > this.forciblyDepleteTrigger;
                }

                if (forciblyDeplete || this.ring > ringStart) {
                    this.sortSectionList();
                    this.nextSectionDistanceSq = this.getNextSectionDistanceSq();
                    if (forciblyDeplete
                            || Math.min(this.lowestWaitingDistanceSq, this.nextSectionDistanceSq)
                            < this.closestRingDistanceSq) {
                        break;
                    }
                    ringStart = this.ring;
                }
            } while (this.ringIterator.hasNext());
        }
    }

    private void sortSectionList() {
        // Note: Do not use unstable sort - the fastutils quicksort is quite a bit slower
        queuedPOISections.subList(this.queuedSectionsSearched, this.queuedPOISections.size())
                .sort(Comparator.comparingDouble(qS -> qS.minDistance));
    }

    private boolean isSectionListEmpty() {
        return this.queuedPOISections.size() <= this.queuedSectionsSearched;
    }

    // Minimum of the next [closest] section in the queue or the closest potential unchecked chunks [next ring]
    private double getMinimumNextPotentialDistanceSq() {
        return Math.min(this.nextSectionDistanceSq, this.closestRingDistanceSq);
    }

    private double getNextSectionDistanceSq() {
        return this.isSectionListEmpty() ?
                Double.MAX_VALUE : this.queuedPOISections.get(this.queuedSectionsSearched).minDistance;
    }

    private int getYDistanceFromBitIndex(final int bitIndex) {
        return bitIndex == -1 ?
                Integer.MAX_VALUE :
                Math.abs(Distances.getClosestAlongSectionAxis(
                        this.origin.getY(), bitIndex + this.chunkYMin) - this.origin.getY());
    }

    // Expand chunks to search in concentric square rings
    // Todo: Gap-less circle drawing algorithms may have better performance
    private double getPotentialRingDistanceSq() {
        final int ringDistance = Math.max(this.ring - 1, 0) * 16 + (ring > 0 ? this.ringClosestEdgeDistance : 0);
        return this.ring > this.ringMax ? Double.MAX_VALUE : ringDistance * ringDistance + this.minChunkYDistSq;
    }

    private LongIterator getRingsOfChunksIterator(final int cx, final int maxX, final int minX,
                                                  final int cz, final int maxZ, final int minZ) {
        return new LongIterator() {
            int x = 0;
            int fx = cx;
            int z = 0;
            int fz = cz;

            @Override
            public long nextLong() {
                long res = ChunkPos.asLong(fx, fz);
                do {
                    z = z > 0 ? -z : 1 - z;
                    if (z > ring) {
                        x = x > 0 ? -x : 1 - x;
                        if (x > ring) {
                            x = 0;
                            ring++;
                            closestRingDistanceSq = getPotentialRingDistanceSq();
                        }
                        z = x < ring && x > -ring ? ring : 0;
                    }
                    fx = cx + x;
                    fz = cz + z;
                } while (ring <= ringMax && fx < minX || fx > maxX || fz < minZ || fz > maxZ);
                return res;
            }

            @Override
            public boolean hasNext() {
                return ring <= ringMax;
            }
        };
    }

    private record QueuedSection(long sectionPos, double minDistance) {
    }
}