package net.caffeinemc.mods.lithium.mixin.minimal_nonvanilla.world.poi_unloading;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.caffeinemc.mods.lithium.common.world.interests.PoiUnloading;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PoiManager.class)
public abstract class PoiManagerMixin implements PoiUnloading {
    @Shadow
    @Final
    private LongSet loadedChunks = new LongOpenHashSet();

    @Override
    public boolean lithium$shouldUnloadChunkPOIs(ChunkPos chunkPos) {
        return !loadedChunks.contains(chunkPos.toLong());
    }
}
