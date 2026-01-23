package net.caffeinemc.mods.lithium.mixin.world.block_entity_ticking.sleeping.sculk_sensor;

import net.caffeinemc.mods.lithium.common.block.entity.SleepingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.BaseEntityBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SculkSensorBlock.class)
public abstract class SculkSensorBlockMixin extends BaseEntityBlock {
    public SculkSensorBlockMixin(Properties properties) {
        super(properties);
    }

    @Inject(method = {"method_32905"}, at = @At(value = "RETURN"))
    private static void checkSleep(Level level, BlockPos blockPos, BlockState blockState, SculkSensorBlockEntity sculkSensorBlockEntity, CallbackInfo ci) {
        if (sculkSensorBlockEntity.getVibrationData().getCurrentVibration() == null) {
            ((SleepingBlockEntity) sculkSensorBlockEntity).lithium$startSleeping();
        }
    }



}
