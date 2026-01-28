package net.caffeinemc.mods.lithium.common.world.interests;

import net.minecraft.world.level.ChunkPos;

public interface PoiUnloading {
    boolean lithium$shouldUnloadChunkPOIs(ChunkPos chunkPos);
    void lithium$unloadChunkPOIs(ChunkPos chunkPos);
}
