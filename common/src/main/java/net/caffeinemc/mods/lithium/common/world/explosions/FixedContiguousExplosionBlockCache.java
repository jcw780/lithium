package net.caffeinemc.mods.lithium.common.world.explosions;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.level.block.state.BlockState;

import java.util.BitSet;
import java.util.Collections;

public class FixedContiguousExplosionBlockCache {
    private final FloatArrayList blastResistances;
    private final ObjectArrayList<BlockState> blockStates;
    private final BitSet exploded;

    public final int xMin;
    public final int xMax;
    public final int xLength;
    public final int yMin;
    public final int yMax;
    public final int yLength;
    public final int zMin;
    public final int zMax;
    public final int zLength;

    public FixedContiguousExplosionBlockCache(final int centerX, final int centerY, final int centerZ, final int range) {
        this.xMin = centerX - range;
        this.xMax = centerX + range;
        this.xLength = this.xMax - this.xMin + 1;
        this.yMin = centerY - range;
        this.yMax = centerY + range;
        this.yLength = this.yMax - this.yMin + 1;
        this.zMin = centerZ - range;
        this.zMax = centerZ + range;
        this.zLength = this.zMax - this.zMin + 1;

        final int length = xLength * yLength * zLength;
        this.blastResistances = new FloatArrayList(Collections.nCopies(length, Float.NEGATIVE_INFINITY));
        this.blockStates = new ObjectArrayList<>(Collections.nCopies(length, null));
        this.exploded = new BitSet(length);
    }

    public boolean isInRange(final int blockX, final int blockY, final int blockZ) {
        return blockX >= this.xMin && blockX <= this.xMax
                && blockY >= this.yMin && blockY <= this.yMax
                && blockZ >= this.zMin && blockZ <= this.zMax;
    }

    public int getIndex(final int blockX, final int blockY, final int blockZ) {
        return ((blockY - this.yMin) * this.zLength + (blockZ - this.zMin)) * this.xLength + (blockX - this.xMin);
    }

    public void setBlastResistance(int index, float blastResistance) {
        this.blastResistances.set(index, blastResistance);
    }

    public void setBlockState(int index, BlockState blockState) {
        this.blockStates.set(index, blockState);
    }

    public void setExploded(final int index) {
        this.exploded.set(index, true);
    }

    public boolean getExploded(final int index) {
        return this.exploded.get(index);
    }

    public boolean isIndexEmpty(final int index) {
        return this.blastResistances.getFloat(index) == Float.NEGATIVE_INFINITY;
    }

    public float getBlastResistance(final int index) {
        return this.blastResistances.getFloat(index);
    }

    public BlockState getBlockState(final int index){
        return this.blockStates.get(index);
    }

}
