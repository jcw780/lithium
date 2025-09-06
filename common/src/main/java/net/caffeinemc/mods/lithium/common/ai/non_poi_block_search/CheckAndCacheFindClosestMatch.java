package net.caffeinemc.mods.lithium.common.ai.non_poi_block_search;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.function.Predicate;

public interface CheckAndCacheFindClosestMatch {
    default Optional<BlockPos> cachedFindClosestMatch(LevelReader levelReader, LivingEntity livingEntity,
                                                      int horizontalRange, int verticalRange,
                                                      Predicate<BlockState> blockStatePredicate,
                                                      boolean shouldChunkLoad){
        BlockPos mobPos = livingEntity.blockPosition();
        CheckAndCacheBlockChecker checker = new CheckAndCacheBlockChecker(
                mobPos.offset(-horizontalRange,-verticalRange,-horizontalRange),
                mobPos.offset(horizontalRange,verticalRange,horizontalRange),
                levelReader, blockStatePredicate, shouldChunkLoad);
        if(checker.shouldStop()) return Optional.empty();
        return BlockPos.findClosestMatch(mobPos, horizontalRange, verticalRange, checker::checkPosition);
    }
}
