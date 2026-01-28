package net.caffeinemc.mods.lithium.mixin.minimal_nonvanilla.world.poi_unloading;

import net.caffeinemc.mods.lithium.common.world.interests.PoiUnloading;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
    @Shadow
    @Final
    private PoiManager poiManager;

    // Inject after the chunkMap save because PoiManager::flush is called
    @Inject(method = "method_60440", at = @At(value = "INVOKE", target = "net/minecraft/server/level/ChunkMap.save (Lnet/minecraft/world/level/chunk/ChunkAccess;)Z", shift = At.Shift.AFTER))
    void unloadPOI(ChunkHolder chunkHolder, CompletableFuture completableFuture, long l, CallbackInfo ci) {
        ((PoiUnloading) this.poiManager).lithium$unloadChunkPOIs(chunkHolder.getPos());
    }
}
