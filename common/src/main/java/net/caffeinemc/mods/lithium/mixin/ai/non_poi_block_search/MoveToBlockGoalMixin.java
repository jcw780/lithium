package net.caffeinemc.mods.lithium.mixin.ai.non_poi_block_search;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.caffeinemc.mods.lithium.common.ai.non_poi_block_search.LithiumMoveToBlockGoal;
import net.caffeinemc.mods.lithium.common.util.collections.fixed_contiguous_region.FixedChunkSectionBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**[Vanilla Copy] - Search order is different, but should result in the same position
 * MoveToBlockGoal search is quite laggy if a lot of mobs are trying to start it - e.g. Portal Gold Farms
 * This is because the searched blocks are not POIs and the search range can be massive - 47x7x47 for zombies.
 * During this search both getChunk and getBlockState contribute a large portion of the lag.
 * - The current implementation optimizes it by caching the ChunkAccesses and by checking whether the ChunkSection
 * has the target block using ChunkSection::maybeHas.
 * - If no ChunkSection in the search range has any target blocks, this check returns early.
 * Otherwise, the search will proceed on a layer by layer [same as vanilla] then ChunkSection basis if the
 * ChunkSection has the target block.
 * - Note: If ChunkSections in the search range have A LOT of different blockStates and all ChunkSections have *had*
 * turtle eggs but the eggs are not in the search there may not be much of a benefit or even possible regression.
 * - Also note: The search implemented here will only be vanilla for non-chunk loading searches - e.g. RemoveBlockGoal
 */
@Mixin(MoveToBlockGoal.class)
public abstract class MoveToBlockGoalMixin implements LithiumMoveToBlockGoal {
    @Shadow
    @Final
    protected PathfinderMob mob;
    @Shadow
    @Final
    private int searchRange;
    @Shadow
    @Final
    private int verticalSearchRange;
    @Shadow
    protected int verticalSearchStart;
    @Shadow
    protected BlockPos blockPos;

    public boolean lithium$findNearestBlock(Predicate<BlockState> requiredBlock, BiPredicate<ChunkAccess,
            BlockPos> lithium$isValidTarget) {
        //Center of the search starts 1 block below the mob's block position
        BlockPos center = this.mob.blockPosition().offset(0,-1,0);

        //Range is +-(searchRange - 1), +-verticalSearchRange, +-(searchRange - 1)
        BlockPos corner0 = center.offset(-this.searchRange+1, -this.verticalSearchRange, -this.searchRange+1);
        BlockPos corner1 = center.offset(this.searchRange-1, this.verticalSearchRange, this.searchRange-1);

        //Cache ChunkAccesses - getting them is surprisingly expensive - and track minimum distance for ChunkSections
        //Always use Y:0 for chunkAccesses
        FixedChunkSectionBuffer<ChunkAccess> chunkAccesses = new FixedChunkSectionBuffer<>(null,
                corner0.offset(0, -center.getY(), 0), corner1.offset(0, -center.getY(), 0)
        );
        FixedChunkSectionBuffer<Integer> chunkSectionsMinimumDistance = new FixedChunkSectionBuffer<>(Integer.MAX_VALUE,
                corner0, corner1);

        LongArrayList chunksToIterate = this.initializeChunkSections(center, this.mob.level(), requiredBlock,
                chunkAccesses, chunkSectionsMinimumDistance);

        if(chunksToIterate.isEmpty()) return false; //No chunks with the target block - return early

        BlockPos.MutableBlockPos foundPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos currentPos = new BlockPos.MutableBlockPos();
        for (int k = this.verticalSearchStart; k <= this.verticalSearchRange; k = k > 0 ? -k : 1 - k) {
            int y = center.getY() + k;
            int closestFound = Integer.MAX_VALUE;
            int ringMax = this.searchRange-1;
            for(long chunk: chunksToIterate){
                int chunkX = SectionPos.x(chunk);
                int chunkZ = SectionPos.z(chunk);
                ChunkAccess chunkAccess = chunkAccesses.get(chunkX, 0, chunkZ);
                //If ChunkSection may have close enough targets, iterate layer in Paletted Container (xz) order
                if(closestFound > chunkSectionsMinimumDistance.get(chunkX, SectionPos.blockToSectionCoord(y), chunkZ)){
                    int chunkBlockX = SectionPos.sectionToBlockCoord(chunkX);
                    int xMin = Math.max(center.getX()-ringMax, chunkBlockX);
                    int xMax = Math.min(center.getX()+ringMax, chunkBlockX+15);
                    int chunkBlockZ = SectionPos.sectionToBlockCoord(chunkZ);
                    int zMin = Math.max(center.getZ()-ringMax, chunkBlockZ);
                    int zMax = Math.min(center.getZ()+ringMax, chunkBlockZ+15);
                    LevelChunkSection levelChunkSection = chunkAccess.getSections()[chunkAccess.getSectionIndex(y)];
                    for(int z = zMin; z <= zMax; z++){
                        for(int x = xMin; x <= xMax; x++){
                            int dX = x - center.getX();
                            int dZ = z - center.getZ();
                            int ring = this.getRing(dX, dZ);
                            int currentDistance = this.getRelativeDistance(ring, dX, dZ);
                            if (currentDistance < closestFound
                                    && this.mob.isWithinHome(currentPos.set(x, y, z))
                                    && requiredBlock.test(levelChunkSection.getBlockState(
                                            x & 15, y & 15, z & 15))
                                    && lithium$isValidTarget.test(chunkAccess, currentPos)) {
                                ringMax = ring;
                                xMin = Math.max(center.getX()-ringMax, chunkBlockX);
                                xMax = Math.min(center.getX()+ringMax, chunkBlockX+15);
                                zMax = Math.min(center.getZ()+ringMax, chunkBlockZ+15);
                                foundPos.set(x, y, z);
                                closestFound = currentDistance;
                            }
                        }
                    }
                }
            }

            if(closestFound < Integer.MAX_VALUE){
                this.blockPos = foundPos;
                return true;
            }
        }

        return false;
    }

    @Unique
    private LongArrayList initializeChunkSections(BlockPos center, LevelReader levelReader,
                                                    Predicate<BlockState> requiredBlock,
                                                    FixedChunkSectionBuffer<ChunkAccess> chunkAccesses,
                                                    FixedChunkSectionBuffer<Integer> chunkSectionsMinimumDistance){
        LongArrayList chunksToIterate = new LongArrayList(chunkAccesses.length);
        int xLimit = chunkAccesses.xMin+chunkAccesses.xLength;
        int zLimit = chunkAccesses.zMin+chunkAccesses.zLength;
        for(int x=chunkAccesses.xMin; x<xLimit; x++){
            for(int z=chunkAccesses.zMin; z<zLimit; z++){
                // This is originally made to match the chunk-loading behavior in RemoveBlockGoal
                // However other goals can chunk-load if their search range is [made] large enough
                ChunkAccess chunkAccess = levelReader.getChunk(
                        x, z, ChunkStatus.FULL, false
                );

                //RemoveBlockGoal::isValidTarget will also return false if chunkAccess is null regardless of block type
                if(chunkAccess != null){
                    boolean hasSubchunks = false;
                    chunkAccesses.set(x, 0, z, chunkAccess);
                    int yMax = chunkSectionsMinimumDistance.yMin+chunkSectionsMinimumDistance.yLength;
                    for(int y=chunkSectionsMinimumDistance.yMin; y<yMax; y++){
                        int chunkSectionYIndex = chunkAccess.getSectionIndexFromSectionY(y);
                        LevelChunkSection[] chunkSections = chunkAccess.getSections();
                        if (chunkSectionYIndex >= 0
                                && chunkSectionYIndex < chunkSections.length
                                && chunkSections[chunkSectionYIndex].maybeHas(requiredBlock)) {
                            chunkSectionsMinimumDistance.set(x,y,z, this.getMinimumDistanceOfChunk(
                                    center.getX(), center.getZ(), x, z));
                            hasSubchunks = true;
                        }
                    }

                    if(hasSubchunks){
                        chunksToIterate.add(SectionPos.asLong(x, 0, z));
                    }
                }
            }
        }

        //Sort chunks by closest possible relative distance
        chunksToIterate.sort((chunkLong0, chunkLong1) -> getMinimumDistanceOfChunk(
                center.getX(), center.getZ(), SectionPos.x(chunkLong0), SectionPos.z(chunkLong0))
                - getMinimumDistanceOfChunk(
                        center.getX(), center.getZ(), SectionPos.x(chunkLong1), SectionPos.z(chunkLong1))
        );

        return chunksToIterate;
    }

    @Unique
    private int getMinimumDistanceOfChunk(int centerX, int centerZ, int chunkX, int chunkZ){
        int minX = SectionPos.sectionToBlockCoord(chunkX);
        int minZ = SectionPos.sectionToBlockCoord(chunkZ);
        int closestX = Mth.clamp(centerX, minX, minX+15);
        int closestZ = Mth.clamp(centerZ, minZ, minZ+15);

        int dX = closestX - centerX;
        int dZ = closestZ - centerZ;

        //This will always get the closest one due to the nature of the search
        return this.getRelativeDistance(this.getRing(dX, dZ), dX, dZ);
    }

    @Unique
    private int getRing(int dX, int dZ){
        return Math.max(Math.abs(dX), Math.abs(dZ));
    }

    @Unique
    private int getRelativeDistance(int ring, int dX, int dZ){
        /** This is equivalent to:
         * int ringX = Math.abs(dX) * 2 - Boolean.compare(dX > 0, false);
         * int ringZ = Math.abs(dZ) * 2 - Boolean.compare(dZ > 0, false);
         * return ring << 16 | ringX << 8 | ringZ;
         *
         * This works because the search prioritizes in order of:
         * 1. The distance of y from the center - Not used
         * 2. Whether y is - or + (- is closer) - Not used
         * 3. The square ring that the block is in (outer is further)
         * 4. The distance of x from the center
         * 5. Whether x is - or + (- is closer)
         * 6. The distance of z from the center
         * 7. Whether z is - or + (- is closer)
         *
         * Note: The bit-packing only works for horizontal search ranges of <=128.
         * You can convert to longs if you somehow exceed that, but also seriously consider POIs instead.
         */
        return (((Math.abs(dX) << 9) | (Math.abs(dZ) << 1))
                - ((Boolean.compare(dX > 0, false) << 8) | (Boolean.compare(dZ > 0, false))))
                | (ring << 16);
    }

}
