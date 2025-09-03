package net.caffeinemc.mods.lithium.mixin.ai.non_poi_block_search;

import net.caffeinemc.mods.lithium.common.ai.non_poi_block_search.LithiumMoveToBlockGoal;
import net.caffeinemc.mods.lithium.common.util.collections.FixedChunkSectionBuffer;
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

    /**
     * - MoveToBlockGoal search is quite laggy if a lot of mobs are trying to start it - e.g. Portal Gold Farms
     * This is because the searched blocks are not POIs and the search range can be massive - 47x7x47 for zombies.
     * During this search both getChunk and getBlockState contribute a large portion of the lag.
     * - The current implementation optimizes it by caching the ChunkAccesses and by checking whether the ChunkSection
     * has the target block using ChunkSection::maybeHas.
     * - If no ChunkSection in the search range has any target blocks, this check returns early.
     * Otherwise, the search will proceed normally but will only run getBlockState if the chunkSection has the
     * target block. While not as fast as returning early, this still reduces a substantial portion of the lag.
     * - Note: If ChunkSections in the search range have A LOT of blockStates and ChunkSections all have turtle eggs but
     * out of range of the search there may not be much of a benefit or even possible regression.
     */
    public boolean lithium$findNearestBlock(Predicate<BlockState> requiredBlock, BiPredicate<ChunkAccess, BlockPos> lithium$isValidTarget) {
        int i = this.searchRange;
        int j = this.verticalSearchRange;
        BlockPos blockPos = this.mob.blockPosition();
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        //Center of the search starts 1 block below the mob position
        BlockPos center = blockPos.offset(0,-1,0);

        //Range is +-(searchRange - 1), +-verticalSearchRange, +-(searchRange - 1)
        BlockPos corner0 = center.offset(-this.searchRange+1, -this.verticalSearchRange, -this.searchRange+1);
        BlockPos corner1 = center.offset(this.searchRange-1, this.verticalSearchRange, this.searchRange-1);

        //Cache ChunkAccesses (this is surprisingly expensive) and track minimum distance for the ChunkSection in a layer
        //Always use Y:0 for chunkAccesses
        FixedChunkSectionBuffer<ChunkAccess> chunkAccesses = new FixedChunkSectionBuffer<>(null,
                corner0.offset(0, -center.getY(), 0), corner1.offset(0, -center.getY(), 0)
        );
        FixedChunkSectionBuffer<Integer> chunkSectionsMinimumDistance = new FixedChunkSectionBuffer<>(-1, corner0, corner1);
        LevelReader levelReader = this.mob.level();

        ArrayList<Long> chunksToInterate = this.initializeChunkSections(center, levelReader, requiredBlock,
                chunkAccesses, chunkSectionsMinimumDistance);
        if(chunksToInterate.size() == 0){
            return false; //No sections with target block - return early
        }

        for (int k = this.verticalSearchStart; k <= j; k = k > 0 ? -k : 1 - k) {
            for (int l = 0; l < i; l++) {
                /*
                int currentClosest = Integer.MAX_VALUE;
                for (int m = 0; m <= l; m = m > 0 ? -m : 1 - m) {
                    for (int n = m < l && m > -l ? l : 0; n <= l; n = n > 0 ? -n : 1 - n) {
                        mutableBlockPos.setWithOffset(blockPos, m, k - 1, n);
                        int minDistance = chunkSectionsMinimumDistance.get(mutableBlockPos);
                        if(minDistance < currentClosest){
                            ChunkAccess chunkAccess = chunkAccesses.get(mutableBlockPos.offset(0,-mutableBlockPos.getY(),0));
                            if (this.mob.isWithinHome(mutableBlockPos) &&
                                    requiredBlock.test(chunkAccess.getBlockState(mutableBlockPos))
                                    && lithium$isValidTarget.test(chunkAccess, mutableBlockPos)) {
                                this.blockPos = mutableBlockPos;
                                return true;
                            }
                        }
                    }
                }*/
                int y = blockPos.getY() + k - 1;
                int foundClosest = Integer.MAX_VALUE;
                int ringMax = this.searchRange-1;
                for(long chunk: chunksToInterate){
                    int chunkX = SectionPos.x(chunk);
                    int chunkZ = SectionPos.z(chunk);
                    ChunkAccess chunkAccess = chunkAccesses.get(chunkX, 0, chunkZ);
                    //If ChunkSection may have close enough targets, iterate layer in Paletted Container (xz) order
                    if(foundClosest > chunkSectionsMinimumDistance.get(chunkX, SectionPos.blockToSectionCoord(y), chunkZ)){
                        int xMin = SectionPos.sectionToBlockCoord(chunkX);
                        int zMin = SectionPos.sectionToBlockCoord(chunkZ);
                        LevelChunkSection levelChunkSection = chunkAccess.getSections()[chunkAccess.getSectionIndex(y)];
                        for(int z = Math.max(center.getZ()-ringMax, zMin); z < Math.min(center.getZ()+ringMax, zMin+15)+1; z++){
                            for(int x = Math.max(center.getX()-ringMax, xMin); x < Math.min(center.getX()+ringMax, xMin+15)+1; x++){
                                int dX = x - center.getX();
                                int dZ = z - center.getZ();
                                int ring = this.getRing(dX, dZ);
                                int ringIndex = this.getRingIndex(ring);
                                int currentDistance = ringIndex + this.getWithinRingIndex(ring, dX, dZ);
                                if (currentDistance < foundClosest
                                        && this.mob.isWithinHome(new BlockPos(x, y, z))
                                        && requiredBlock.test(levelChunkSection.getBlockState(x & 15, y & 15, z & 15))
                                        && lithium$isValidTarget.test(chunkAccess, mutableBlockPos)) {
                                    mutableBlockPos.set(x, y, z);
                                    foundClosest = currentDistance;
                                     ringMax = ring;
                                }
                            }
                        }
                    }
                }
                if(foundClosest < Integer.MAX_VALUE){
                    this.blockPos = mutableBlockPos;
                    return true;
                }
            }
        }

        return false;
    }

    @Unique
    private ArrayList<Long> initializeChunkSections(BlockPos center, LevelReader levelReader, Predicate<BlockState> requiredBlock,
                                        FixedChunkSectionBuffer<ChunkAccess> chunkAccesses,
                                        FixedChunkSectionBuffer<Integer> chunkSectionsMinimumDistance){
        ArrayList<Long> chunksToInterate = new ArrayList<>(chunkAccesses.length);
        for(int x=chunkSectionsMinimumDistance.xMin; x<chunkSectionsMinimumDistance.xMin+chunkSectionsMinimumDistance.xLength; x++){
            for(int z=chunkSectionsMinimumDistance.zMin; z<chunkSectionsMinimumDistance.zMin+chunkSectionsMinimumDistance.zLength; z++){
                // This is originally made to match the chunk-loading behavior in RemoveBlockGoal
                // However other goals can chunk-load if their search range is large enough
                ChunkAccess chunkAccess = levelReader.getChunk(
                        x, z, ChunkStatus.FULL, false
                );
                int yMax = chunkSectionsMinimumDistance.yMin+chunkSectionsMinimumDistance.yLength;
                if(chunkAccess != null){ //RemoveBlockGoal::isValidTarget will also return false if null
                    boolean hasSubchunks = false;
                    chunkAccesses.set(x, 0, z, chunkAccess);
                    for(int y=chunkSectionsMinimumDistance.yMin; y<yMax; y++){
                        int chunkSectionYIndex = chunkAccess.getSectionIndexFromSectionY(y);
                        int chunkSectionMinDistance;
                        if (chunkSectionYIndex >= 0 && chunkSectionYIndex < chunkAccess.getSections().length) {
                            LevelChunkSection levelChunkSection = chunkAccess.getSections()[chunkSectionYIndex];
                            if(levelChunkSection.maybeHas(requiredBlock)){
                                chunkSectionMinDistance = this.getMinimumDistanceOfChunk(
                                        center.getX(), center.getZ(), x, z);
                                hasSubchunks = true;
                            }else{
                                chunkSectionMinDistance = Integer.MAX_VALUE;
                            }
                        } else {
                            chunkSectionMinDistance = Integer.MAX_VALUE;
                        }
                        chunkSectionsMinimumDistance.set(x,y,z, chunkSectionMinDistance);
                    }
                    if(hasSubchunks){
                        chunksToInterate.add(SectionPos.asLong(x, 0, z));
                    }
                }else{
                    for(int y=chunkSectionsMinimumDistance.yMin; y<yMax; y++){
                        chunkSectionsMinimumDistance.set(x, y, z, Integer.MAX_VALUE);
                    }
                }

            }
        }

        //Sort chunks by closest possible ring
        chunksToInterate.sort( (chunkLong0, chunkLong1) -> {
             int x0 = SectionPos.x(chunkLong0);
             int z0 = SectionPos.z(chunkLong0);
             int x1 = SectionPos.x(chunkLong1);
             int z1 = SectionPos.z(chunkLong1);

             return getMinimumDistanceOfChunk(center.getX(), center.getZ(), x0, z0) -
                     getMinimumDistanceOfChunk(center.getX(), center.getZ(), x1, z1);
        });

        return chunksToInterate;
    }

    @Unique
    private int getMinimumDistanceOfChunk(int centerX, int centerZ, int chunkX, int chunkZ){
        int minX = SectionPos.sectionToBlockCoord(chunkX);
        int minZ = SectionPos.sectionToBlockCoord(chunkZ);
        int closestX = Mth.clamp(centerX, minX, minX+15);
        int closestZ = Mth.clamp(centerZ, minZ, minZ+15);

        int dX = closestX - centerX;
        int dZ = closestZ - centerZ;

        return this.getRingIndex(this.getRing(dX, dZ));
    }

    @Unique
    private int getRing(int dX, int dZ){
        return Math.max(Math.abs(dX), Math.abs(dZ));
    }

    @Unique
    private int getRingIndex(int ring){
        return (2*ring-1)*(2*ring-1)-(ring==0?1:0);
    }

    @Unique
    private int getWithinRingIndex(int ring, int dX, int dZ){
        return Math.abs(dX) < ring ?
                (Math.abs(dX)*2-(dX>0?1:0))*2 + (dZ<0?1:0)
                : Math.max(ring*2-1,0)*2 + (ring*2+1)*(dX<0?1:0) + Math.abs(dZ)*2-(dZ>0?1:0);
    }

}
