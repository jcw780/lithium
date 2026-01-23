package net.caffeinemc.mods.lithium.mixin.world.block_entity_ticking.sleeping.sculk_sensor_shrieker;

import net.caffeinemc.mods.lithium.common.block.entity.SleepingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CalibratedSculkSensorBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CalibratedSculkSensorBlock.class)
public abstract class CalibratedSculkSensorBlockMixin {
    @Inject(method = {"method_49813"}, at = @At(value = "RETURN"))
    private static void checkSleep(Level level, BlockPos blockPos, BlockState blockState, CalibratedSculkSensorBlockEntity calibratedSculkSensorBlockEntity, CallbackInfo ci) {
        if (calibratedSculkSensorBlockEntity.getVibrationData().getCurrentVibration() == null) {
            ((SleepingBlockEntity) calibratedSculkSensorBlockEntity).lithium$startSleeping();
        }
    }
}
