package net.caffeinemc.mods.lithium.mixin.world.block_entity_ticking.sleeping.sculk_sensor;

import net.caffeinemc.mods.lithium.common.block.entity.SetChangedHandlingBlockEntity;
import net.caffeinemc.mods.lithium.common.block.entity.SleepingBlockEntity;
import net.caffeinemc.mods.lithium.common.block.entity.sleeping_sculk_sensor.ListeningVibrationData;
import net.caffeinemc.mods.lithium.mixin.world.block_entity_ticking.sleeping.WrappedBlockEntityTickInvokerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SculkSensorBlockEntity.class)
public abstract class SculkSensorBlockEntityMixin implements SleepingBlockEntity, SetChangedHandlingBlockEntity, VibrationSystem {
    @Shadow
    private VibrationSystem.Data vibrationData;

    @Shadow
    @Final
    private Listener vibrationListener;

    private WrappedBlockEntityTickInvokerAccessor tickWrapper = null;
    private TickingBlockEntity sleepingTicker = null;

    @Override
    public WrappedBlockEntityTickInvokerAccessor lithium$getTickWrapper() {
        return tickWrapper;
    }

    @Override
    public void lithium$setTickWrapper(WrappedBlockEntityTickInvokerAccessor tickWrapper) {
        this.tickWrapper = tickWrapper;
        this.lithium$setSleepingTicker(null);
    }

    @Override
    public TickingBlockEntity lithium$getSleepingTicker() {
        return sleepingTicker;
    }

    @Override
    public void lithium$setSleepingTicker(TickingBlockEntity sleepingTicker) {
        this.sleepingTicker = sleepingTicker;
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/block/entity/BlockEntityType;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("RETURN"))
    private void setVibrationListenerListener(BlockEntityType blockEntityType, BlockPos blockPos, BlockState blockState, CallbackInfo ci) {
        ((ListeningVibrationData) this.vibrationListener).lithium$setCurrentVibrationUpdateListener(this::wakeUpNow);
    }

    @Override
    public void lithium$handleSetChanged() {
        if (this.vibrationData.getCurrentVibration() == null) {
            this.lithium$startSleeping();
        }
    }
}
