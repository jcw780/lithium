package net.caffeinemc.mods.lithium.common.util.collections.fixed_contiguous_region;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

public class FixedChunkSectionBitset extends FixedContiguousRegionBitset{
    public FixedChunkSectionBitset(int x0, int x1, int y0, int y1, int z0, int z1) {
        super(x0, x1, y0, y1, z0, z1);
    }

    public FixedChunkSectionBitset(SectionPos start, SectionPos end){
        super(start.getX(), end.getX(), start.getY(), end.getY(), start.getZ(), end.getZ());
    }

    public FixedChunkSectionBitset(BlockPos start, BlockPos end){
        super(SectionPos.blockToSectionCoord(start.getX()),
                SectionPos.blockToSectionCoord(end.getX()),
                SectionPos.blockToSectionCoord(start.getY()),
                SectionPos.blockToSectionCoord(end.getY()),
                SectionPos.blockToSectionCoord(start.getZ()),
                SectionPos.blockToSectionCoord(end.getZ()));
    }

    public boolean get(BlockPos blockPos){
        return this.get(
                SectionPos.blockToSectionCoord(blockPos.getX()),
                SectionPos.blockToSectionCoord(blockPos.getY()),
                SectionPos.blockToSectionCoord(blockPos.getZ())
        );
    }

    public boolean get(SectionPos sectionPos){
        return this.get(sectionPos.getX(), sectionPos.getY(), sectionPos.getZ());
    }

    public void set(BlockPos blockPos, boolean input){
        this.set(
                SectionPos.blockToSectionCoord(blockPos.getX()),
                SectionPos.blockToSectionCoord(blockPos.getY()),
                SectionPos.blockToSectionCoord(blockPos.getZ()),
                input
        );
    }

    public void set(SectionPos sectionPos, boolean input){
        this.set(sectionPos.getX(), sectionPos.getY(), sectionPos.getZ(), input);
    }
}
