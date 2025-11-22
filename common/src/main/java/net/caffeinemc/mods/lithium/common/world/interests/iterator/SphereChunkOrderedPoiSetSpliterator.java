package net.caffeinemc.mods.lithium.common.world.interests.iterator;

import net.caffeinemc.mods.lithium.common.util.Distances;
import net.caffeinemc.mods.lithium.common.world.interests.RegionBasedStorageSectionExtended;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.village.poi.PoiSection;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class SphereChunkOrderedPoiSetSpliterator extends Spliterators.AbstractSpliterator<Stream<PoiSection>> {
    private final int limit;
    private final int minChunkX;
    private final BlockPos origin;
    private final double radiusSq;
    private final RegionBasedStorageSectionExtended<PoiSection> storage;
    private final int maxChunkX;
    int chunkX;
    int chunkZ;
    int iterated;

    public SphereChunkOrderedPoiSetSpliterator(int radius, BlockPos origin, RegionBasedStorageSectionExtended<PoiSection> storage) {
        super((long) ((origin.getX() + radius + 1 >> 4) - (origin.getX() - radius - 1 >> 4) + 1) * ((origin.getZ() + radius + 1 >> 4) - (origin.getZ() - radius - 1 >> 4) + 1), Spliterator.ORDERED);
        this.origin = origin;
        this.radiusSq = radius * radius;
        this.storage = storage;

        this.minChunkX = origin.getX() - radius - 1 >> 4;
        this.maxChunkX = origin.getX() + radius + 1 >> 4;
        int minChunkZ = origin.getZ() - radius - 1 >> 4;
        int maxChunkZ = origin.getZ() + radius + 1 >> 4;
        this.limit = (this.maxChunkX - this.minChunkX + 1) * ((maxChunkZ) - (minChunkZ) + 1);

        this.chunkX = this.minChunkX;
        this.chunkZ = minChunkZ;
        this.iterated = 0;
    }

    @Override
    public boolean tryAdvance(Consumer<? super Stream<PoiSection>> action) {
        while (true) {
            if (this.iterated >= this.limit) {
                return false;
            } else {
                this.iterated++;
                boolean progress = false;
                if (Distances.getMinChunkToBlockDistanceL2Sq(this.origin, this.chunkX, this.chunkZ) <= this.radiusSq) {
                    //future work: filter sections with too high distance on the y axis as well
                    action.accept(this.storage.lithium$getWithinChunkColumn(this.chunkX, this.chunkZ));
                    progress = true;
                }

                this.chunkX++;
                if (this.chunkX > maxChunkX) {
                    this.chunkZ++;
                    this.chunkX = minChunkX;
                }

                if (progress) {
                    return true;
                }
            }
        }
    }
}
