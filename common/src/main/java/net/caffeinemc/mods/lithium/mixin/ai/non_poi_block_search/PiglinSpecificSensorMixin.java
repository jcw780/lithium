package net.caffeinemc.mods.lithium.mixin.ai.non_poi_block_search;

import net.caffeinemc.mods.lithium.common.ai.non_poi_block_search.CheckAndCacheBlockChecker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.PiglinSpecificSensor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(PiglinSpecificSensor.class)
public abstract class PiglinSpecificSensorMixin {
    @Redirect(method = "doTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/sensing/PiglinSpecificSensor;findNearestRepellent(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)Ljava/util/Optional;"))
    public Optional<BlockPos> lithium$findNearestRepellent(ServerLevel serverLevel, LivingEntity livingEntity){
        BlockPos mobPos = livingEntity.blockPosition();
        final int horizontalRange = 8, verticalRange = 4;
        CheckAndCacheBlockChecker checker = new CheckAndCacheBlockChecker(
                mobPos.offset(-horizontalRange,-verticalRange,-horizontalRange), mobPos.offset(horizontalRange,verticalRange,horizontalRange),
                serverLevel,
                (blockState) -> {
                    boolean bl = blockState.is(BlockTags.PIGLIN_REPELLENTS);
                    return bl && blockState.is(Blocks.SOUL_CAMPFIRE) ? CampfireBlock.isLitCampfire(blockState) : bl;
                    },
                true);
        if(checker.shouldStop()) return Optional.empty();
        return BlockPos.findClosestMatch(mobPos, horizontalRange, verticalRange, checker::checkPosition);
    }
}
