package net.caffeinemc.mods.lithium.mixin.world.block_entity_ticking.sleeping.sculk_catalyst;

import net.caffeinemc.mods.lithium.common.block.entity.sleeping_sculk.ListeningCatalystListener;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.SculkCatalystBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SculkCatalystBlockEntity.CatalystListener.class)
public abstract class CatalystListenerMixin implements ListeningCatalystListener {
    private Runnable listener;

    @Inject(method = "handleGameEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/SculkSpreader;addCursors(Lnet/minecraft/core/BlockPos;I)V"))
    public void listenToCursorAddition(ServerLevel serverLevel, Holder<GameEvent> holder, GameEvent.Context context, Vec3 vec3, CallbackInfoReturnable<Boolean> cir) {
        this.listener.run();
    }

    public void lithium$setCurrentVibrationUpdateListener(Runnable listener) {
        this.listener = listener;
    }
}
