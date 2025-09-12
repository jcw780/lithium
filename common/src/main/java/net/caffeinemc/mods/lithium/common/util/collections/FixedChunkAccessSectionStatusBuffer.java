package net.caffeinemc.mods.lithium.common.util.collections;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;

public class FixedChunkAccessSectionStatusBuffer {
    public final int xMin, yMin, zMin;
    public final int xLength, yLength, zLength, length;

    public final BitSet chunkSectionStatus;
    public final ArrayList<ChunkAccess> chunkAccesses;

    public FixedChunkAccessSectionStatusBuffer(int x0, int x1, int y0, int y1, int z0, int z1){
        this.xMin = Math.min(x0, x1);
        this.yMin = Math.min(y0, y1);
        this.zMin = Math.min(z0, z1);

        this.xLength = Math.max(x0, x1) - this.xMin + 1;
        this.yLength = Math.max(y0, y1) - this.yMin + 1;
        this.zLength = Math.max(z0, z1) - this.zMin + 1;

        this.length = yLength*xLength*zLength;

        this.chunkSectionStatus = new BitSet(length);
        this.chunkAccesses = new ArrayList<>(Collections.nCopies(xLength*zLength,null));
    }

    public FixedChunkAccessSectionStatusBuffer(BlockPos start, BlockPos end){
        this(SectionPos.blockToSectionCoord(start.getX()),
                SectionPos.blockToSectionCoord(end.getX()),
                SectionPos.blockToSectionCoord(start.getY()),
                SectionPos.blockToSectionCoord(end.getY()),
                SectionPos.blockToSectionCoord(start.getZ()),
                SectionPos.blockToSectionCoord(end.getZ()));
    }

    public int getSectionIndex(int x, int y, int z){
        int dx = x - this.xMin;
        int dy = y - this.yMin;
        int dz = z - this.zMin;

        return (dx * this.zLength + dz) * this.yLength + dy;
    }

    public int getSectionIndex(long sectionPos){
        return this.getSectionIndex(
                SectionPos.x(sectionPos),
                SectionPos.y(sectionPos),
                SectionPos.z(sectionPos)
        );
    }

    public boolean getChunkSectionStatus(BlockPos blockPos){
        return this.getChunkSectionStatus(SectionPos.blockToSection(blockPos.asLong()));
    }

    public boolean getChunkSectionStatus(long sectionPos){
        return this.chunkSectionStatus.get(this.getSectionIndex(sectionPos));
    }

    public void setChunkSectionStatus(long sectionPos, boolean value){
        this.chunkSectionStatus.set(this.getSectionIndex(sectionPos));
    }

    public int getChunkIndex(int x, int z){
        int dx = x - this.xMin;
        int dz = z - this.zMin;

        return dx * this.zLength + dz;
    }

    public int getChunkIndex(long chunkPos){
        return this.getChunkIndex(ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
    }

    public ChunkAccess getChunkAccess(long chunkPos){
        return this.chunkAccesses.get(this.getChunkIndex(chunkPos));
    }

    public ChunkAccess getChunkAccess(BlockPos blockPos){
        return this.getChunkAccess(ChunkPos.asLong(blockPos));
    }

    public void setChunkAccess(long chunkPos, ChunkAccess chunkAccess){
        this.chunkAccesses.set(this.getChunkIndex(chunkPos), chunkAccess);
    }

    public void setChunkAccess(BlockPos blockPos, ChunkAccess chunkAccess){
        this.setChunkAccess(ChunkPos.asLong(blockPos), chunkAccess);
    }

    public boolean hasTrueChunkSections(){
        return this.chunkSectionStatus.nextSetBit(0) == -1;
    }
}
