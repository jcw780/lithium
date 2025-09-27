package net.caffeinemc.mods.lithium.mixin.ai.non_poi_block_search;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.caffeinemc.mods.lithium.common.ai.non_poi_block_search.CheckAndCacheBlockChecker;
import net.caffeinemc.mods.lithium.common.ai.non_poi_block_search.LithiumMoveToBlockGoal;
import net.caffeinemc.mods.lithium.common.ai.non_poi_block_search.NonPOISearchDistances.MoveToBlockGoalDistances;
import net.caffeinemc.mods.lithium.common.util.Pos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * [Vanilla Copy] - Search order is different, but should result in the same position
 * MoveToBlockGoal search is quite laggy if a lot of mobs are trying to start it - e.g. Portal Gold Farms
 * This is because the searched blocks are not POIs and the search range can be massive - 47x7x47 for zombies.
 * During this search both getChunk and getBlockState contribute a large portion of the lag.
 * The current implementation optimizes it by caching the ChunkAccesses and by checking whether the ChunkSection
 * has the target block using ChunkSection::maybeHas.
 * <p>
 * Basic Logic:
 * <p>
 * - Prescan chunks and ChunkSections - cache the ChunkAccess and if a ChunkSection has the target block(s) then flag it.
 * <p>
 * - If no ChunkSection in the search range has any target block(s), then return early.
 * <p>
 * - Otherwise, the search will proceed on a layer by layer [same as vanilla] then ChunkSection basis if the
 * ChunkSection has the target block - "empty" ChunkSections will not be iterated through.
 * <p>
 * Note: If ChunkSections in the search range have A LOT of different blockStates and all ChunkSections have *had*
 * turtle eggs but the eggs are not in the search there may not be much of a benefit or even possible regression.
 * <p>
 * Additional Note: Please correctly specify whether the search may chunk-load to avoid observably altering behavior
 * in unusual situations. Default getBlockState will chunk-load.
 * @author jcw780
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

    @Override
    public boolean lithium$findNearestBlock(Predicate<BlockState> requiredBlock, BiPredicate<ChunkAccess,
            BlockPos.MutableBlockPos> lithium$isValidTarget, final boolean shouldChunkLoad) {
        //Center of the search starts 1 block below the mob's block position
        BlockPos center = this.mob.blockPosition().offset(0,-1,0);

        //Range is +-(searchRange - 1), +-verticalSearchRange, +-(searchRange - 1)
        //Cache ChunkAccesses - getting them is surprisingly expensive - and track whether subchunks have the block
        final LevelReader levelReader = this.mob.level();
        CheckAndCacheBlockChecker checker = new CheckAndCacheBlockChecker(center,
                this.searchRange-1, this.verticalSearchRange,
                levelReader, requiredBlock, shouldChunkLoad);
        LongArrayList chunksToIterate = new LongArrayList(checker.getChunkSize());
        checker.initializeChunks(chunksToIterate::addLast);

        if(checker.shouldStop()) return false; //No chunks with the target block - return early

        final int minY = Pos.BlockCoord.getMinY(levelReader);
        final int maxY = Pos.BlockCoord.getMaxYInclusive(levelReader);

        // Prefer chunk aware search because it also cuts iterations inside "empty" chunk sections
        if(!checker.hasUnloadedPossibleChunks()){
            return this.lithium$chunkAwareSearch(center, lithium$isValidTarget, checker, chunksToIterate, minY, maxY);
        }

        // Use vanilla search because unordered search may observably alter chunk-loading behavior
        return this.lithium$vanillaOrderSearch(center, lithium$isValidTarget, checker, minY, maxY);
    }

    @Unique
    private boolean lithium$vanillaOrderSearch(BlockPos center,
                                               BiPredicate<ChunkAccess, BlockPos.MutableBlockPos> lithium$isValidTarget,
                                               CheckAndCacheBlockChecker checker, final int minY, final int maxY){
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        final int centerY = center.getY();

        for (int k = this.verticalSearchStart; k <= this.verticalSearchRange; k = k > 0 ? -k : 1 - k) {
            final int y = centerY + k;

            if (y < minY || y > maxY) {
                continue;
            }

            for (int l = 0; l < this.searchRange; l++) {
                for (int m = 0; m <= l; m = m > 0 ? -m : 1 - m) {
                    for (int n = m < l && m > -l ? l : 0; n <= l; n = n > 0 ? -n : 1 - n) {
                        mutableBlockPos.setWithOffset(center, m, k, n);
                        if (this.mob.isWithinHome(mutableBlockPos) && checker.checkPosition(mutableBlockPos)) {
                            // ChunkAccess is always loaded at this point
                            ChunkAccess chunkAccess = checker.getCachedChunkAccess(mutableBlockPos);
                            if (lithium$isValidTarget.test(chunkAccess, mutableBlockPos)){
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
    private boolean lithium$chunkAwareSearch(BlockPos center,
                                             BiPredicate<ChunkAccess, BlockPos.MutableBlockPos> lithium$isValidTarget,
                                             CheckAndCacheBlockChecker checker, LongArrayList chunksToIterate,
                                             final int minY, final int maxY){
        // Sort chunks by closest possible relative distance
        // Note: In this search order, the closest point normally is also the closest point in the search
        chunksToIterate.sort((chunkLong0, chunkLong1) ->
                MoveToBlockGoalDistances.getMinimumSortOrderOfChunk(center, chunkLong0)
                        - MoveToBlockGoalDistances.getMinimumSortOrderOfChunk(center, chunkLong1)
        );

        Predicate<BlockState> requiredBlock = checker.blockStatePredicate;
        final int minSectionY = checker.minSectionY;

        BlockPos.MutableBlockPos foundPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos currentPos = new BlockPos.MutableBlockPos();

        // Same layer order as vanilla - saves iterations if targets are found in the first layer
        for (int k = this.verticalSearchStart; k <= this.verticalSearchRange; k = k > 0 ? -k : 1 - k) {
            final int y = center.getY() + k;

            if (y < minY || y > maxY) {
                continue;
            }

            final int chunkY = SectionPos.blockToSectionCoord(y);
            final int ySectionIndex = chunkY - minSectionY;

            int closestFound = Integer.MAX_VALUE;
            int ringMax = this.searchRange-1;

            // Iterate through slices of chunks that may have the target blockState
            for(long chunkPos: chunksToIterate){
                final int chunkX = ChunkPos.getX(chunkPos);
                final int chunkZ = ChunkPos.getZ(chunkPos);
                //No subsequent chunks can be closer since it's sorted
                if(closestFound < MoveToBlockGoalDistances.getMinimumSortOrderOfChunk(center, chunkX, chunkZ)){
                    break;
                }

                //Current subchunk does not have the block
                if(!checker.checkCachedSection(chunkX, chunkY, chunkZ)){
                    continue;
                }

                ChunkAccess chunkAccess = checker.getCachedChunkAccess(chunkPos);
                //If ChunkSection may have close enough targets, iterate layer in Paletted Container (x then z) order
                final int chunkBlockX = SectionPos.sectionToBlockCoord(chunkX);
                int xMin = Math.max(center.getX()-ringMax, chunkBlockX);
                int xMax = Math.min(center.getX()+ringMax, chunkBlockX+15);
                final int chunkBlockZ = SectionPos.sectionToBlockCoord(chunkZ);
                int zMin = Math.max(center.getZ()-ringMax, chunkBlockZ);
                int zMax = Math.min(center.getZ()+ringMax, chunkBlockZ+15);
                LevelChunkSection levelChunkSection = chunkAccess.getSections()[ySectionIndex];
                for(int z = zMin; z <= zMax; z++){
                    for(int x = xMin; x <= xMax; x++){
                        int dX = x - center.getX();
                        int dZ = z - center.getZ();
                        int ring = MoveToBlockGoalDistances.getRing(dX, dZ);
                        int currentDistance = MoveToBlockGoalDistances.getVanillaSortOrderInt(ring, dX, dZ);
                        if (currentDistance < closestFound
                                && this.mob.isWithinHome(currentPos.set(x, y, z))
                                && requiredBlock.test(levelChunkSection.getBlockState(x & 15, y & 15, z & 15))
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

             if(closestFound < Integer.MAX_VALUE){
                this.blockPos = foundPos;
                return true;
            }
        }

        return false;
    }
}
