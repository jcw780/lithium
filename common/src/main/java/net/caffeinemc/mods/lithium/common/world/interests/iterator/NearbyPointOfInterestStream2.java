
package net.caffeinemc.mods.lithium.common.world.interests.iterator;

import it.unimi.dsi.fastutil.longs.LongHeapPriorityQueue;
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
import org.apache.tools.ant.taskdefs.Javadoc;
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
    private final int adjustedY;
    private final int clampedCenter;

    private LongPriorityQueue subchunksToCheck;
    private int ring;
    private final int ringMax;
    private final long chunkCornerMax;
    private final long chunkCornerMin;
    private double lowestWaitingDistance;
    private double currChunkMinDistanceSq;
    private double distanceLimitL2Sq;
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
        this.adjustedY = this.origin.getY() - SectionPos.sectionToBlockCoord(chunkYMin);
        this.clampedCenter = Math.max(SectionPos.blockToSectionCoord(this.origin.getY()), chunkYMin);

        long sectionCorner = SectionPos.blockToSection(origin.offset(radius, 0, radius).asLong());
        this.chunkCornerMax = ChunkPos.asLong(SectionPos.x(sectionCorner), SectionPos.z(sectionCorner));
        sectionCorner = SectionPos.blockToSection(origin.offset(-radius, 0, -radius).asLong());
        this.chunkCornerMin = ChunkPos.asLong(SectionPos.x(sectionCorner), SectionPos.z(sectionCorner));

        this.ring = 0;
        int originSubchunkX = SectionPos.x(originSubchunk);
        int originSubchunkZ = SectionPos.z(originSubchunk);
        this.ringMax =
                Math.max(Math.max(ChunkPos.getX(chunkCornerMax) - originSubchunkX, originSubchunkX - ChunkPos.getX(chunkCornerMin)),
                        Math.max(ChunkPos.getZ(chunkCornerMax) - originSubchunkZ, originSubchunkZ - ChunkPos.getZ(chunkCornerMin)));
        this.lowestWaitingDistance = Double.MAX_VALUE;
        this.subchunksToCheck = new LongHeapPriorityQueue(
                (s0, s1) -> Double.compare(this.getSubChunkDistanceSq(s0), this.getSubChunkDistanceSq(s1)));

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

        while (this.ring <= this.ringMax || !this.subchunksToCheck.isEmpty()){
            this.keepAddingRingsUntilSufficient();

            int previousSize = this.points.size();
            if(!this.subchunksToCheck.isEmpty()) {
                long currentSection = subchunksToCheck.dequeueLong();
                int x = SectionPos.x(currentSection);
                int y = SectionPos.y(currentSection);
                int z = SectionPos.z(currentSection);

                this.storage.lithium$getElementAt(currentSection)
                        .ifPresent(section -> ((PointOfInterestSetExtended) section)
                                .lithium$collectMatchingPoints(this.typeSelector, this.occupationStatus, this.collector));
            }

            if (this.points.size() == previousSize) {
                if (this.lowestWaitingDistance >= this.getMinimumNextPotentialDistance()) {
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

    private void keepAddingRingsUntilSufficient(){
        while (this.ring <= this.ringMax &&
                (this.subchunksToCheck.isEmpty() || this.getSubChunkDistanceSq(this.subchunksToCheck.firstLong()) >= this.getPotentialRingDistanceSq())){
            for (int x = 0; x <= this.ring + 1; x = x > 0 ? -x : 1 - x) {
                for (int z = x < this.ring + 1 && x > -this.ring + 1 ? this.ring + 1 : 0;
                     z <= this.ring + 1; z = z > 0 ? -z : 1 - z) {
                    long currentChunk = ChunkPos.asLong(SectionPos.x(this.originSubchunk) + x,
                            SectionPos.z(this.originSubchunk) + z);
                    if(this.isWithinBounds(currentChunk)){
                        BitSet poiSections = this.storage.lithium$getNonEmptyPOISections(
                                ChunkPos.getX(currentChunk), ChunkPos.getZ(currentChunk));
                        int nextBit = poiSections.nextSetBit(0);
                        while (nextBit >= 0){
                            long currentSubchunk = SectionPos.asLong(ChunkPos.getX(currentChunk),
                                    nextBit + this.chunkYMin,
                                    ChunkPos.getZ(currentChunk));

                            if(this.distanceLimitL2Sq >= this.getSubChunkDistanceSq(currentSubchunk)) {
                                this.subchunksToCheck.enqueue(currentSubchunk);
                            }
                            nextBit = poiSections.nextSetBit(nextBit+1);
                        }
                    }
                }
            }

            this.ring++;
        }
    }

    private boolean isWithinBounds(long chunkPos){
        int x = ChunkPos.getX(chunkPos);
        int maxX = ChunkPos.getX(this.chunkCornerMax);
        int minX = ChunkPos.getX(this.chunkCornerMin);
        int z = ChunkPos.getZ(chunkPos);
        int maxZ = ChunkPos.getZ(this.chunkCornerMax);
        int minZ = ChunkPos.getZ(this.chunkCornerMin);

        return (Boolean.compare(x <= maxX, false) & Boolean.compare(x >= minX, false) &
                Boolean.compare(z <= maxZ, false) & Boolean.compare(z >= minZ, false)) == 1;
    }

    private double getSubChunkDistanceSq(long sectionPos){
        int x = SectionPos.sectionToBlockCoord(SectionPos.x(sectionPos));
        int y = SectionPos.sectionToBlockCoord(SectionPos.y(sectionPos));
        int z = SectionPos.sectionToBlockCoord(SectionPos.z(sectionPos));

        int distX = getDistanceOnAxis(x, this.origin.getX());
        int distY = getDistanceOnAxis(y, this.origin.getY());
        int distZ = getDistanceOnAxis(z, this.origin.getZ());

        return distX * distX + distY * distY + distZ * distZ;
    }

    private int getDistanceOnAxis(int chunkMin, int origin){
        int closest = Math.clamp(origin, chunkMin, chunkMin+15);
        return closest - origin;
    }

    private double getMinimumNextPotentialDistance(){
        return Math.min(this.subchunksToCheck.isEmpty() ?
                        Double.MAX_VALUE : this.getSubChunkDistanceSq(this.subchunksToCheck.firstLong())
                , this.getPotentialRingDistanceSq());
    }

    private double getPotentialRingDistanceSq(){
        int x = this.origin.getX();
        int z = this.origin.getZ();
        int closestEdgeDistance = Math.min(Math.min((x&15)+1, 16 - x&15),
                Math.min((z&15)+1, 16 - z&15));

        int ringDistance = (this.ring * 16 + closestEdgeDistance);

        return this.ring > this.ringMax ? Double.MAX_VALUE : ringDistance * ringDistance;
    }

}