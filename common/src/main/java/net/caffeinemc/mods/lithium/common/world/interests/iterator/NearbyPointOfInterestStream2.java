
package net.caffeinemc.mods.lithium.common.world.interests.iterator;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongHeapPriorityQueue;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongPriorityQueue;
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
import org.gradle.internal.impldep.bsh.Primitive;
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
public class NearbyPointOfInterestStream2 extends Spliterators.AbstractSpliterator<PoiRecord> {
    private final RegionBasedStorageSectionExtended<PoiSection> storage;

    private final Predicate<Holder<PoiType>> typeSelector;
    private final PoiManager.Occupancy occupationStatus;

    private final ArrayList<SortedPointOfInterest> points;
    private final Predicate<PoiRecord> afterSortingPredicate;
    private final Consumer<PoiRecord> collector;
    private final BlockPos origin;

    private final long originSubchunk;

    private final int chunkYMin;
    private final double minChunkYDistSq;

    //private final LongPriorityQueue subchunksToCheck;
    private final LongArrayList subchunksToCheck;
    private int subChunksSearched = 0;
    private boolean forciblyDeplete = false;
    private final int forciblyDepleteTrigger;
    private int ring;
    private final int ringMax;
    private final LongIterator ringIterator;
    private final long chunkCornerMax;
    private final long chunkCornerMin;
    private double lowestWaitingDistance;
    private final double distanceLimitL2Sq;
    private int pointIndex;
    private final Comparator<? super SortedPointOfInterest> pointComparator;

    public NearbyPointOfInterestStream2(Predicate<Holder<PoiType>> typeSelector,
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
        this.originSubchunk = SectionPos.blockToSection(origin.asLong());
        this.chunkYMin = this.storage.lithium$getChunkYMin();
        final int chunkYMax = this.storage.lithium$getChunkYMaxInclusive();

        // If the origin is outside of build limit (e.g. 1.18+ overworld to nether portal teleport) in the target dimension,
        // the min Y distance is not zero for a chunk.
        final int minChunkYDist = Math.clamp(
                this.origin.getY(), SectionPos.sectionToBlockCoord(this.chunkYMin),
                SectionPos.sectionToBlockCoord(chunkYMax, 15)) - this.origin.getY();
        this.minChunkYDistSq = minChunkYDist * minChunkYDist;

        //Todo: Remove allocations from offsets
        long sectionCorner = SectionPos.blockToSection(origin.offset(radius, 0, radius).asLong());
        this.chunkCornerMax = ChunkPos.asLong(SectionPos.x(sectionCorner), SectionPos.z(sectionCorner));
        sectionCorner = SectionPos.blockToSection(origin.offset(-radius, 0, -radius).asLong());
        this.chunkCornerMin = ChunkPos.asLong(SectionPos.x(sectionCorner), SectionPos.z(sectionCorner));

        // Chunk searches will expand in a concentric square rings around the origin
        // Subchunks with POIs will be put into the priority queue to be searched when no more possible subchunks are closer
        this.ring = 0;
        int originSubchunkX = SectionPos.x(originSubchunk);
        int originSubchunkZ = SectionPos.z(originSubchunk);
        this.ringMax =
                Math.max(Math.max(ChunkPos.getX(chunkCornerMax) - originSubchunkX, originSubchunkX - ChunkPos.getX(chunkCornerMin)),
                        Math.max(ChunkPos.getZ(chunkCornerMax) - originSubchunkZ, originSubchunkZ - ChunkPos.getZ(chunkCornerMin)));
        this.ringIterator = getRingsOfChunksIterator();
        this.lowestWaitingDistance = Double.MAX_VALUE;
        /*this.subchunksToCheck = new LongHeapPriorityQueue(
                (s0, s1) -> Double.compare(Distances.getMinSubChunkDistanceSq(this.origin, s0),
                        Distances.getMinSubChunkDistanceSq(this.origin, s1)
                ));*/
        final int subchunksPerChunk = chunkYMax - chunkYMin + 1;
        final int listSize = Math.max(16,subchunksPerChunk) * 4;
        this.subchunksToCheck = new LongArrayList(Collections.nCopies(listSize, 0L));
        this.forciblyDepleteTrigger = listSize - subchunksPerChunk;

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

        while (this.ringIterator.hasNext() || !this.isSubchunkListEmpty()){
            this.keepAddingRingsUntilSufficient();

            int previousSize = this.points.size();
            if(!this.isSubchunkListEmpty() && this.lowestWaitingDistance >= this.getMinimumNextPotentialDistance()) {
                long subchunk = subchunksToCheck.getLong(this.subChunksSearched++);
                //double dist = Distances.getMinSubChunkDistanceSq(this.origin, subchunk);
                this.storage.lithium$getElementAt(subchunk)
                        .ifPresent(section -> ((PointOfInterestSetExtended) section)
                                .lithium$collectMatchingPoints(this.typeSelector, this.occupationStatus, this.collector));
                this.forciblyDeplete = (!this.forciblyDeplete || !this.isSubchunkListEmpty()) && this.forciblyDeplete;
            }

            if (this.points.size() == previousSize) {
                // Advance more aggressively - search may have already ensured that the next POI is closer even if it does
                // not add more POIs.
                if(this.lowestWaitingDistance >= this.getMinimumNextPotentialDistance()){
                    continue;
                }
            } else {
                this.points.subList(this.pointIndex, this.points.size()).sort(this.pointComparator);
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

            //Only consume points if we are sure that there are no closer (or same distance) points to be scanned. Otherwise scan more chunks
            if (next.distanceSq() >= this.getMinimumNextPotentialDistance()) {
                this.lowestWaitingDistance = next.distanceSq();
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
     * Keep searching for POI subchunks in concentric square rings around the origin
     * Continue if: There are more rings to scan.
     * and the queue is either empty or the first subchunk is not closer than unchecked possible subchunks
     */
    private void keepAddingRingsUntilSufficient(){
        if (!this.forciblyDeplete && this.ringIterator.hasNext() && (this.isSubchunkListEmpty() ||
                Distances.getMinSubChunkDistanceSq(this.origin, this.subchunksToCheck.getLong(this.subChunksSearched)) >=
                        this.getPotentialRingDistanceSq())
        ){
            this.subchunksToCheck.removeElements(0, this.subChunksSearched);
            this.subChunksSearched = 0;
            postLoop: do {
                final int ringStart = this.ring;
                do {
                    final long chunkPos = this.ringIterator.nextLong();
                    final int currentChunkX = ChunkPos.getX(chunkPos);
                    final int currentChunkZ = ChunkPos.getZ(chunkPos);

                    if (this.distanceLimitL2Sq >= Distances.getMinChunkToBlockDistanceL2Sq(this.origin, currentChunkX, currentChunkZ)) {
                        BitSet poiSections = this.storage.lithium$getNonEmptyPOISections(currentChunkX, currentChunkZ);
                        int nextBit = poiSections.nextSetBit(0);
                        while (nextBit >= 0) {
                            this.subchunksToCheck.add(
                                    SectionPos.asLong(currentChunkX, nextBit + this.chunkYMin, currentChunkZ)
                            );
                            nextBit = poiSections.nextSetBit(nextBit + 1);
                        }
                    }

                    if (this.subchunksToCheck.size() > this.forciblyDepleteTrigger){
                        this.forciblyDeplete = true;
                        break;
                    }

                    if (this.ring > ringStart) {
                        break;
                    }
                } while (this.ringIterator.hasNext());
                subchunksToCheck.subList(this.subChunksSearched, this.subchunksToCheck.size())
                        .sort((s0, s1) ->
                                Double.compare(
                                        Distances.getMinSubChunkDistanceSq(this.origin, s0),
                                        Distances.getMinSubChunkDistanceSq(this.origin, s1)));
                if(this.forciblyDeplete){
                    break;
                }
            }while (this.ringIterator.hasNext() && (this.isSubchunkListEmpty() ||
                    Distances.getMinSubChunkDistanceSq(this.origin, this.subchunksToCheck.getLong(subChunksSearched)) >=
                            this.getPotentialRingDistanceSq()));
        }
    }

    private boolean isSubchunkListEmpty(){
        return this.subchunksToCheck.size() <= this.subChunksSearched;
    }

    // Minimum of the next [closest] subchunk in the queue or the closest potential unchecked chunks [next ring]
    private double getMinimumNextPotentialDistance(){
        return Math.min(this.isSubchunkListEmpty() ?
                        Double.MAX_VALUE : Distances.getMinSubChunkDistanceSq(
                                this.origin, this.subchunksToCheck.getLong(this.subChunksSearched)
                ), this.getPotentialRingDistanceSq());
    }

    private double getPotentialRingDistanceSq(){
        int x = this.origin.getX();
        int z = this.origin.getZ();
        int closestEdgeDistance = Math.min(Math.min((x&15)+1, 16 - x&15),
                Math.min((z&15)+1, 16 - z&15));

        int ringDistance = Math.max(this.ring - 1,0) * 16 + (ring > 0 ? closestEdgeDistance : 0);

        return this.ring > this.ringMax ? Double.MAX_VALUE : ringDistance * ringDistance + this.minChunkYDistSq;
    }

    private LongIterator getRingsOfChunksIterator(){
        return new LongIterator() {
            int x = 0;
            final int cx = SectionPos.x(originSubchunk);
            final int maxX = ChunkPos.getX(chunkCornerMax);
            final int minX = ChunkPos.getX(chunkCornerMin);
            int fx = cx;
            int z = 0;
            final int cz = SectionPos.z(originSubchunk);
            final int maxZ = ChunkPos.getZ(chunkCornerMax);
            final int minZ = ChunkPos.getZ(chunkCornerMin);
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

}