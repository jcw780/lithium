package net.caffeinemc.mods.lithium.common.util.collections.fixed_contiguous_region;

public abstract class FixedContiguousRegionType {
    public final int xMin, yMin, zMin;
    public final int xLength, yLength, zLength, length;

    public FixedContiguousRegionType(int x0, int x1, int y0, int y1, int z0, int z1){
        this.xMin = Math.min(x0, x1);
        this.yMin = Math.min(y0, y1);
        this.zMin = Math.min(z0, z1);

        this.xLength = Math.max(x0, x1) - this.xMin + 1;
        this.yLength = Math.max(y0, y1) - this.yMin + 1;
        this.zLength = Math.max(z0, z1) - this.zMin + 1;

        this.length = yLength*xLength*zLength;
    }

    public int getIndex(int x, int y, int z){
        int dx = x - this.xMin;
        int dy = y - this.yMin;
        int dz = z - this.zMin;

        return (dx * this.zLength + dz) * this.yLength + dy;
    }
}
