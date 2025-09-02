package net.caffeinemc.mods.lithium.mixin.ai.non_poi_block_search;

import net.caffeinemc.mods.lithium.common.ai.non_poi_block_search.LithiumMoveToBlockGoal;
import net.caffeinemc.mods.lithium.common.util.collections.FixedChunkSectionBuffer;
import net.minecraft.core.BlockPos;
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

        int possibleSections = this.initializeChunkSections(levelReader, requiredBlock, chunkAccesses, chunkSectionsMinimumDistance);
        if(possibleSections == 0){
            return false; //No sections with target block - return early
        }

        for (int k = this.verticalSearchStart; k <= j; k = k > 0 ? -k : 1 - k) {
            for (int l = 0; l < i; l++) {
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
                }
            }
        }

        return false;
    }

    @Unique
    private int initializeChunkSections(LevelReader levelReader, Predicate<BlockState> requiredBlock,
                                        FixedChunkSectionBuffer<ChunkAccess> chunkAccesses,
                                        FixedChunkSectionBuffer<Integer> chunkSectionsMinimumDistance){
        int possibleSections = 0;
        for(int x=chunkSectionsMinimumDistance.xMin; x<chunkSectionsMinimumDistance.xMin+chunkSectionsMinimumDistance.xLength; x++){
            for(int z=chunkSectionsMinimumDistance.zMin; z<chunkSectionsMinimumDistance.zMin+chunkSectionsMinimumDistance.zLength; z++){
                // This is originally made to match the chunk-loading behavior in RemoveBlockGoal
                // However other goals can chunk-load if their search range is large enough
                ChunkAccess chunkAccess = levelReader.getChunk(
                        x, z, ChunkStatus.FULL, false
                );
                int yMax = chunkSectionsMinimumDistance.yMin+chunkSectionsMinimumDistance.yLength;
                if(chunkAccess != null){ //RemoveBlockGoal::isValidTarget will also return false if null
                    chunkAccesses.set(x, 0, z, chunkAccess);
                    for(int y=chunkSectionsMinimumDistance.yMin; y<yMax; y++){
                        int chunkSectionYIndex = chunkAccess.getSectionIndexFromSectionY(y);
                        int chunkSectionMinDistance;
                        if (chunkSectionYIndex >= 0 && chunkSectionYIndex < chunkAccess.getSections().length) {
                            LevelChunkSection levelChunkSection = chunkAccess.getSections()[chunkSectionYIndex];
                            if(levelChunkSection.maybeHas(requiredBlock)){
                                chunkSectionMinDistance = 0;
                                possibleSections++;
                            }else{
                                chunkSectionMinDistance = Integer.MAX_VALUE;
                            }
                        } else {
                            chunkSectionMinDistance = Integer.MAX_VALUE;
                        }
                        chunkSectionsMinimumDistance.set(x,y,z, chunkSectionMinDistance);
                    }
                }else{
                    for(int y=chunkSectionsMinimumDistance.yMin; y<yMax; y++){
                        chunkSectionsMinimumDistance.set(x, y, z, Integer.MAX_VALUE);
                    }
                }

            }
        }
        return possibleSections;
    }
}
