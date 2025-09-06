package net.caffeinemc.mods.lithium.mixin.ai.non_poi_block_search;

import net.caffeinemc.mods.lithium.common.ai.non_poi_block_search.CheckAndCacheFindClosestMatch;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.sensing.HoglinSpecificSensor;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

/** [Vanilla Copy]
 *  Optimizes Hoglin repellent search.
 *  Note: Search may chunk load in unusual circumstances - must maintain behavior
 */
@Mixin(HoglinSpecificSensor.class)
public abstract class HoglinSpecificSensorMixin implements CheckAndCacheFindClosestMatch {
    @Redirect(method = "doTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/monster/hoglin/Hoglin;)V",
    at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/sensing/HoglinSpecificSensor;findNearestRepellent(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/monster/hoglin/Hoglin;)Ljava/util/Optional;"))
    private Optional<BlockPos> lithium$findNearestRepellent(HoglinSpecificSensor instance, ServerLevel serverLevel,
                                                            Hoglin hoglin) {
        return cachedFindClosestMatch(serverLevel, hoglin, 8, 4,
                blockState -> blockState.is(BlockTags.HOGLIN_REPELLENTS), true);
    }
}
