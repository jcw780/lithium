package net.caffeinemc.mods.lithium.common.ai.non_poi_block_search;

import net.caffeinemc.mods.lithium.common.util.collections.fixed_contiguous_region.FixedChunkSectionBitset;
import net.caffeinemc.mods.lithium.common.util.collections.fixed_contiguous_region.FixedChunkSectionBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.function.Predicate;

public class CheckAndCacheBlockChecker {
    FixedChunkSectionBuffer<ChunkAccess> chunkAccesses;
    FixedChunkSectionBitset possibleChunkSections;
    LevelReader levelReader;
    final boolean shouldChunkLoad;
    Predicate<BlockState> blockStatePredicate;

    public CheckAndCacheBlockChecker(BlockPos start, BlockPos end, LevelReader levelReader, Predicate<BlockState> blockStatePredicate, boolean shouldChunkLoad){
        this.chunkAccesses = new FixedChunkSectionBuffer<>(null, start.offset(0,-start.getY(),0), end.offset(0,-end.getY(),0));
        this.possibleChunkSections = new FixedChunkSectionBitset(start, end);
        this.levelReader = levelReader;
        this.shouldChunkLoad = shouldChunkLoad;
        this.blockStatePredicate = blockStatePredicate;

        int xLimit = this.chunkAccesses.xMin+this.chunkAccesses.xLength;
        int zLimit = this.chunkAccesses.zMin+this.chunkAccesses.zLength;
        int yMin = this.possibleChunkSections.yMin;
        int yMax = yMin+this.possibleChunkSections.yLength;
        for(int x=chunkAccesses.xMin; x<xLimit; x++) {
            for (int z = chunkAccesses.zMin; z < zLimit; z++) {
                //Never load chunks in the first pass to avoid observably altering chunk loading behavior
                ChunkAccess chunkAccess = levelReader.getChunk(x, z, ChunkStatus.FULL, false);
                if(chunkAccess != null){
                    this.chunkAccesses.set(x, 0, z, chunkAccess);
                    for(int y=yMin; y<yMax; y++){
                        checkChunkSection(chunkAccess, x, y, z);
                    }
                } else if(this.shouldChunkLoad){
                    //If the search could chunk load, we cannot exclude null chunks because they may be loaded later
                    //So if the subchunk is inside build limit then it might be valid later
                    for(int y=yMin; y<yMax; y++){
                        this.possibleChunkSections.set(x, y, z,
                                !levelReader.isOutsideBuildHeight(SectionPos.sectionToBlockCoord(y)));
                    }
                }
            }
        }
    }

    private boolean checkChunkSection(ChunkAccess chunkAccess, int chunkX, int chunkY, int chunkZ){
        int chunkSectionYIndex = chunkAccess.getSectionIndexFromSectionY(chunkY);
        LevelChunkSection[] chunkSections = chunkAccess.getSections();
        if (chunkSectionYIndex >= 0
                && chunkSectionYIndex < chunkSections.length
                && chunkSections[chunkSectionYIndex].maybeHas(blockStatePredicate)) {
            this.possibleChunkSections.set(chunkX, chunkY, chunkZ, true);
            return true;
        }
        return false;
    }

    public boolean shouldStop(){
        return this.possibleChunkSections.data.nextSetBit(0) == -1;
    }

    public boolean checkPosition(BlockPos blockPos){
        if(!possibleChunkSections.get(blockPos)) return false;
        ChunkAccess chunkAccess = this.chunkAccesses.get(blockPos.offset(0,-blockPos.getY(),0));
        if(chunkAccess == null && this.shouldChunkLoad){
            int chunkX = SectionPos.blockToSectionCoord(blockPos.getX());
            int chunkY = SectionPos.blockToSectionCoord(blockPos.getY());
            int chunkZ = SectionPos.blockToSectionCoord(blockPos.getZ());
            chunkAccess = levelReader.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
            //this chunkAccess cannot be null and reach here because it should throw earlier
            assert chunkAccess != null;
            this.chunkAccesses.set(blockPos.offset(0,-blockPos.getY(),0), chunkAccess);
            if (!checkChunkSection(chunkAccess, chunkX, chunkY, chunkZ)) return false;
        }

        return blockStatePredicate.test(chunkAccess.getBlockState(blockPos));
    }
}
