package net.caffeinemc.mods.lithium.mixin.ai.non_poi_block_search;

import net.caffeinemc.mods.lithium.common.ai.non_poi_block_search.CheckAndCacheFindClosestMatch;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.PiglinSpecificSensor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

/** [Vanilla Copy]
 *  Optimizes Piglin repellent search.
 */
@Mixin(PiglinSpecificSensor.class)
public abstract class PiglinSpecificSensorMixin implements CheckAndCacheFindClosestMatch {
    @Redirect(method = "doTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/sensing/PiglinSpecificSensor;findNearestRepellent(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)Ljava/util/Optional;"))
    public Optional<BlockPos> redirectFindNearestRepellent(ServerLevel serverLevel, LivingEntity livingEntity){
        return cachedFindClosestMatch(serverLevel, livingEntity, 8, 4,
                this::lithium$isValidRepellent, true);
    }

    @Unique
    private boolean lithium$isValidRepellent(BlockState blockState){
        boolean bl = blockState.is(BlockTags.PIGLIN_REPELLENTS);
        return bl && blockState.is(Blocks.SOUL_CAMPFIRE) ? CampfireBlock.isLitCampfire(blockState) : bl;
    }
}
