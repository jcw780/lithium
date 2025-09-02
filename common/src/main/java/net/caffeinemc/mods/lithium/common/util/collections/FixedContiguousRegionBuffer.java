package net.caffeinemc.mods.lithium.common.util.collections;

import java.lang.reflect.Array;
import java.lang.Math;
import java.util.ArrayList;

public class FixedContiguousRegionBuffer<T> {
    public final ArrayList<T> data;
    public final int xMin, yMin, zMin;
    public final int xLength, yLength, zLength, length;

    public FixedContiguousRegionBuffer(T initialValue, int x0, int x1, int y0, int y1, int z0, int z1){
        this.xMin = Math.min(x0, x1);
        this.yMin = Math.min(y0, y1);
        this.zMin = Math.min(z0, z1);

        this.xLength = Math.max(x0, x1) - this.xMin + 1;
        this.yLength = Math.max(y0, y1) - this.yMin + 1;
        this.zLength = Math.max(z0, z1) - this.zMin + 1;

        this.length = yLength*xLength*zLength;

        this.data = new ArrayList<>(length);
        for(int i=0; i<length; i++){
            data.add(initialValue);
        }
    }

    public int getIndex(int x, int y, int z){
        int dx = x - this.xMin;
        int dy = y - this.yMin;
        int dz = z - this.zMin;

        return (dx * this.zLength + dz) * this.yLength + dy;
    }

    public T get(int x, int y, int z){
        return this.data.get(this.getIndex(x, y, z));
    }

    public void set(int x, int y, int z, T input){
        this.data.set(this.getIndex(x, y, z), input);
    }
}
