package net.caffeinemc.mods.lithium.common.ai.non_poi_block_search;

import net.caffeinemc.mods.lithium.common.util.collections.FixedChunkAccessSectionBitBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.function.Predicate;

/**This is intended to be used to optimize Non-POI block searches by pre-checking the ChunkSections, getting the
 * ChunkAccesses which then allows for returning early or only calling getBlockState in possible ChunkSections.
 * Note: Please correctly specify shouldChunkLoad based on whether the getBlockState in vanilla can chunk load or not.
 * The default in game is that it can. Setting it to true will mean that the search will assume that unloaded chunks
 * may have the target and will chunk load then search them if and when it reaches it.
 */
public class CheckAndCacheBlockChecker {
    private final FixedChunkAccessSectionBitBuffer chunkAccessSectionStatusBuffer;
    private final LevelReader levelReader;
    private final boolean shouldChunkLoad;
    private final Predicate<BlockState> blockStatePredicate;

    public CheckAndCacheBlockChecker(BlockPos start, BlockPos end, LevelReader levelReader,
                                     Predicate<BlockState> blockStatePredicate, boolean shouldChunkLoad){
        this.chunkAccessSectionStatusBuffer = new FixedChunkAccessSectionBitBuffer(start, end);
        this.levelReader = levelReader;
        this.shouldChunkLoad = shouldChunkLoad;
        this.blockStatePredicate = blockStatePredicate;

        for (long chunkPos : this.chunkAccessSectionStatusBuffer.getChunkPosInRange()){
            int x = ChunkPos.getX(chunkPos);
            int z = ChunkPos.getZ(chunkPos);

            //Never load chunks in the first pass to avoid observably altering chunk loading behavior
            ChunkAccess chunkAccess = levelReader.getChunk(x, z, ChunkStatus.FULL, false);
            if(chunkAccess != null){
                this.chunkAccessSectionStatusBuffer.setChunkAccess(chunkPos, chunkAccess);
                for(int y: this.chunkAccessSectionStatusBuffer.getSectionYInRange()){
                    checkChunkSection(chunkAccess, x, y, z);
                }
            } else if(this.shouldChunkLoad){
                //If the search could chunk load, we cannot exclude null chunks because they may be loaded later
                //So if the subchunk is inside build limit then it might be valid later
                for(int y: this.chunkAccessSectionStatusBuffer.getSectionYInRange()){
                    this.chunkAccessSectionStatusBuffer.setChunkSectionStatus(SectionPos.asLong(x, y, z),
                            !levelReader.isOutsideBuildHeight(SectionPos.sectionToBlockCoord(y)));
                }
            }
        }
    }

    private boolean checkChunkSection(ChunkAccess chunkAccess, int chunkX, int chunkY, int chunkZ){
        final int chunkSectionYIndex = chunkAccess.getSectionIndexFromSectionY(chunkY);
        LevelChunkSection[] chunkSections = chunkAccess.getSections();
        if (chunkSectionYIndex >= 0
                && chunkSectionYIndex < chunkSections.length
                && chunkSections[chunkSectionYIndex].maybeHas(blockStatePredicate)) {
            this.chunkAccessSectionStatusBuffer.setChunkSectionStatus(
                    SectionPos.asLong(chunkX, chunkY, chunkZ), true);
            return true;
        }
        return false;
    }

    public boolean shouldStop(){
        return this.chunkAccessSectionStatusBuffer.hasTrueChunkSections();
    }

    public boolean checkPosition(BlockPos blockPos){
        if(!this.chunkAccessSectionStatusBuffer.getChunkSectionBit(blockPos)) return false;
        ChunkAccess chunkAccess = this.chunkAccessSectionStatusBuffer.getChunkAccess(blockPos);
        if(chunkAccess == null && this.shouldChunkLoad){
            int chunkX = SectionPos.blockToSectionCoord(blockPos.getX());
            int chunkY = SectionPos.blockToSectionCoord(blockPos.getY());
            int chunkZ = SectionPos.blockToSectionCoord(blockPos.getZ());
            chunkAccess = levelReader.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
            //this chunkAccess cannot be null and reach here because it should throw earlier
            assert chunkAccess != null;
            this.chunkAccessSectionStatusBuffer.setChunkAccess(blockPos, chunkAccess);
            if (!checkChunkSection(chunkAccess, chunkX, chunkY, chunkZ)) {
                return false;
            }
        }

        return blockStatePredicate.test(chunkAccess.getBlockState(blockPos));
    }
}
