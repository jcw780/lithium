package net.caffeinemc.mods.lithium.mixin.ai.non_poi_block_search;

import net.caffeinemc.mods.lithium.common.ai.non_poi_block_search.CheckAndCacheBlockChecker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.sensing.HoglinSpecificSensor;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(HoglinSpecificSensor.class)
public abstract class HoglinSpecificSensorMixin {
    @Redirect(method = "doTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/monster/hoglin/Hoglin;)V",
    at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/sensing/HoglinSpecificSensor;findNearestRepellent(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/monster/hoglin/Hoglin;)Ljava/util/Optional;"))
    private Optional<BlockPos> lithium$findNearestRepellent(HoglinSpecificSensor instance, ServerLevel serverLevel, Hoglin hoglin) {
        BlockPos mobPos = hoglin.blockPosition();
        final int horizontalRange = 8, verticalRange = 4;
        CheckAndCacheBlockChecker checker = new CheckAndCacheBlockChecker(
                mobPos.offset(-horizontalRange,-verticalRange,-horizontalRange), mobPos.offset(horizontalRange,verticalRange,horizontalRange),
                serverLevel,
                blockState -> blockState.is(BlockTags.HOGLIN_REPELLENTS),
                true);
        if(checker.shouldStop()) return Optional.empty();
        return BlockPos.findClosestMatch(mobPos, horizontalRange, verticalRange, checker::checkPosition);
    }
}
