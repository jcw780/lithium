package net.caffeinemc.mods.lithium.common.ai.non_poi_block_search;

import net.caffeinemc.mods.lithium.common.util.Distances;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public class NonPOISearchDistances {
    public static class MoveToBlockGoalDistances{
        public static int getMinimumDistanceOfChunk(BlockPos center, long chunkPos){
            return getMinimumDistanceOfChunk(center, ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
        }

        public static int getMinimumDistanceOfChunk(BlockPos center, int chunkX, int chunkZ){
            long closest = Distances.getClosestPositionWithinChunk(center, chunkX, chunkZ);

            int dX = BlockPos.getX(closest) - center.getX();
            int dZ = BlockPos.getZ(closest) - center.getZ();

            //This will always get the closest one due to the nature of the search
            return getRelativeDistance(getRing(dX, dZ), dX, dZ);
        }

        public static int getRing(int dX, int dZ){
            return Math.max(Math.abs(dX), Math.abs(dZ));
        }

        /**
         * Distance function for 1 layer of MoveToBlockGoal findNearestBlock
         * This is equivalent to:
         * int ringX = Math.abs(dX) * 2 - (dX > 0 ? 1 : 0);
         * int ringZ = Math.abs(dZ) * 2 - (dZ > 0 ? 1 : 0);
         * return ring << 16 | ringX << 8 | ringZ;
         * <p>
         * This works because the search prioritizes in order of:
         * 1. The distance of y from the center - Not used
         * 2. Whether y is - or + (- is closer) - Not used
         * 3. The square ring that the block is in (outer is further)
         * 4. The distance of x from the center
         * 5. Whether x is - or + (- is closer)
         * 6. The distance of z from the center
         * 7. Whether z is - or + (- is closer)
         * <p>
         * Note: The bit-packing only works for horizontal search ranges of <=128.
         * You can convert to longs if you somehow exceed that, but also seriously consider POIs instead.
         *
         * @param ring Which square ring the block is at relative to the center
         * @param dX Relative x position of the block to the center
         * @param dZ Relative z position of the block to the center
         */
        public static int getRelativeDistance(int ring, int dX, int dZ){
            return (((Math.abs(dX) << 9) | (Math.abs(dZ) << 1))
                    - (((dX > 0 ? 1 : 0) << 8) | (dZ > 0 ? 1 : 0)))
                    | (ring << 16);
        }
    }
}
