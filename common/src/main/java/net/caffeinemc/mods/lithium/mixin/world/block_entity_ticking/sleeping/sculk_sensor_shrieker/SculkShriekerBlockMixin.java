package net.caffeinemc.mods.lithium.mixin.world.block_entity_ticking.sleeping.sculk_sensor_shrieker;

import net.caffeinemc.mods.lithium.common.block.entity.SleepingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SculkShriekerBlock.class)
public abstract class SculkShriekerBlockMixin {
    @Inject(method = {"method_42317"}, at = @At(value = "RETURN"))
    private static void checkSleep(Level level, BlockPos blockPos, BlockState blockState, SculkShriekerBlockEntity sculkShriekerBlockEntity, CallbackInfo ci) {
        if (sculkShriekerBlockEntity.getVibrationData().getCurrentVibration() == null) {
            ((SleepingBlockEntity) sculkShriekerBlockEntity).lithium$startSleeping();
        }
    }
}
