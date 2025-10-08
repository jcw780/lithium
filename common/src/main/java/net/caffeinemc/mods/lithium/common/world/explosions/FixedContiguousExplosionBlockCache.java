package net.caffeinemc.mods.lithium.common.world.explosions;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;

public class FixedContiguousExplosionBlockCache {
    private final FloatArrayList blastResistances;
    private final ObjectArrayList<BlockState> blockStates;

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
    }

    public boolean isInRange(final int blockX, final int blockY, final int blockZ) {
        return blockX >= this.xMin && blockX <= this.xMax
                && blockY >= this.yMin && blockY <= this.yMax
                && blockZ >= this.zMin && blockZ <= this.zMax;
    }

    public int getIndex(final int blockX, final int blockY, final int blockZ) {
        return ((blockY - this.yMin) * this.zLength + (blockZ - this.zMin)) * this.xLength + (blockX - this.xMin);
    }

    public void set(final int blockX, final int blockY, final int blockZ, ExplosionBlockEntry mutableEntry) {
        final int index = this.getIndex(blockX, blockY, blockZ);
        this.blastResistances.set(index, mutableEntry.blastResistance);
        this.blockStates.set(index, mutableEntry.blockState);
    }

    public boolean isPositionPopulated(final int blockX, final int blockY, final int blockZ) {
        return this.blastResistances.getFloat(this.getIndex(blockX, blockY, blockZ)) != Float.NEGATIVE_INFINITY;
    }

    public void getMutate(final int blockX, final int blockY, final int blockZ, ExplosionBlockEntry mutableEntry) {
        final int index = this.getIndex(blockX, blockY, blockZ);
        mutableEntry.blastResistance = this.blastResistances.getFloat(index);
        mutableEntry.blockState = this.blockStates.get(index);
    }

}
