package net.caffeinemc.mods.lithium.mixin.world.block_entity_ticking.sleeping.sculk_catalyst;

import net.caffeinemc.mods.lithium.common.block.entity.sleeping_sculk.ListeningSculkSpreader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SculkSpreader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SculkSpreader.class)
public class SculkSpreaderMixin implements ListeningSculkSpreader {
    private Runnable listener;

    @Inject(method = "addCursors", at = @At("RETURN"))
    public void listenToCursorAddition(BlockPos blockPos, int i, CallbackInfo ci) {
        this.listener.run();
    }

    public void lithium$setCurrentVibrationUpdateListener(Runnable listener) {
        this.listener = listener;
    }
}
