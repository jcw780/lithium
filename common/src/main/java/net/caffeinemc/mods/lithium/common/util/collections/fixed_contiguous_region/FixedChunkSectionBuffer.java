package net.caffeinemc.mods.lithium.common.util.collections.fixed_contiguous_region;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

public class FixedChunkSectionBuffer<T> extends FixedContiguousRegionBuffer<T> {
    public FixedChunkSectionBuffer(T initialValue, int x0, int x1, int y0, int y1, int z0, int z1) {
        super(initialValue, x0, x1, y0, y1, z0, z1);
    }

    public FixedChunkSectionBuffer(T initialValue, SectionPos start, SectionPos end){
        super(initialValue, start.getX(), end.getX(), start.getY(), end.getY(), start.getZ(), end.getZ());
    }

    public FixedChunkSectionBuffer(T initialValue, BlockPos start, BlockPos end){
        super(initialValue,
                SectionPos.blockToSectionCoord(start.getX()),
                SectionPos.blockToSectionCoord(end.getX()),
                SectionPos.blockToSectionCoord(start.getY()),
                SectionPos.blockToSectionCoord(end.getY()),
                SectionPos.blockToSectionCoord(start.getZ()),
                SectionPos.blockToSectionCoord(end.getZ()));
    }

    public T get(BlockPos blockPos){
        return this.get(
                SectionPos.blockToSectionCoord(blockPos.getX()),
                SectionPos.blockToSectionCoord(blockPos.getY()),
                SectionPos.blockToSectionCoord(blockPos.getZ())
        );
    }

    public T get(SectionPos sectionPos){
        return this.get(sectionPos.getX(), sectionPos.getY(), sectionPos.getZ());
    }

    public void set(BlockPos blockPos, T input){
        this.set(
                SectionPos.blockToSectionCoord(blockPos.getX()),
                SectionPos.blockToSectionCoord(blockPos.getY()),
                SectionPos.blockToSectionCoord(blockPos.getZ()),
                input
        );
    }

    public void set(SectionPos sectionPos, T input){
        this.set(sectionPos.getX(), sectionPos.getY(), sectionPos.getZ(), input);
    }
}
