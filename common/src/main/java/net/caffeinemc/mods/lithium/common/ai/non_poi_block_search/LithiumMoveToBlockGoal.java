package net.caffeinemc.mods.lithium.common.ai.non_poi_block_search;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

public interface LithiumMoveToBlockGoal {
    boolean lithium$findNearestBlock(Predicate<BlockState> requiredBlock, BiPredicate<ChunkAccess, BlockPos> lithium$isValidTarget,
                                     final boolean shouldChunkLoad);
}
