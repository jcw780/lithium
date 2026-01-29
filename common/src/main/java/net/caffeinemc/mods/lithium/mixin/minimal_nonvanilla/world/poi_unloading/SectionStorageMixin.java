package net.caffeinemc.mods.lithium.mixin.minimal_nonvanilla.world.poi_unloading;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.caffeinemc.mods.lithium.common.util.collections.ListeningLong2ObjectOpenHashMap;
import net.caffeinemc.mods.lithium.common.world.interests.PoiUnloading;
import net.caffeinemc.mods.lithium.common.world.interests.RegionBasedStorageSectionExtended;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.BitSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Mixin(SectionStorage.class)
public abstract class SectionStorageMixin<R> implements PoiUnloading, RegionBasedStorageSectionExtended<R> {
    @Shadow
    @Final
    private Object loadLock;

    @Shadow
    @Final
    private LongSet loadedChunks;

    @Shadow
    @Final
    private Long2ObjectMap<CompletableFuture<Optional<SectionStorage.PackedChunk<?>>>> pendingLoads;

    @Shadow
    @Final
    private Long2ObjectMap<Optional<R>> storage;

    @Override
    public void lithium$unloadChunkPOIs(ChunkPos chunkPos) {
        if (!this.lithium$shouldUnloadChunkPOIs(chunkPos)) {
            return;
        }

        // Note: shouldn't need to write chunks since it's after PoiManager::flush in chunk unload

        // Remove column bitset
        BitSet chunkSections = this.lithium$removeColumn(chunkPos);
        if (chunkSections != null) {
            // This relies on the reduce poi memory optimizations so we only need to remove sections with a POISection
            final int chunkYMin = this.lithium$getChunkYMin();
            int nextSectionY = -1;
            while ((nextSectionY = chunkSections.nextSetBit(nextSectionY + 1)) != -1) {
                // Column is already removed so we do not need to update the column in the columns hashmap
                ((ListeningLong2ObjectOpenHashMap<Optional<R>>)this.storage).removeSilently(
                        SectionPos.asLong(chunkPos.x, chunkYMin + nextSectionY, chunkPos.z)
                );
            }
        }

        synchronized (this.loadLock) {
            this.loadedChunks.remove(chunkPos.toLong());
            this.pendingLoads.remove(chunkPos.toLong());
        }
    }
}
