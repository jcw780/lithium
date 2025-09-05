package net.caffeinemc.mods.lithium.common.util.collections.fixed_contiguous_region;

import java.util.ArrayList;
import java.util.Collections;

public class FixedContiguousRegionBuffer<T> extends FixedContiguousRegionType{
    public final ArrayList<T> data;

    public FixedContiguousRegionBuffer(T initialValue, int x0, int x1, int y0, int y1, int z0, int z1){
        super(x0, x1, y0, y1, z0, z1);
        this.data = new ArrayList<>(Collections.nCopies(length, initialValue));
    }

    public T get(int x, int y, int z){
        return this.data.get(this.getIndex(x, y, z));
    }

    public void set(int x, int y, int z, T input){
        this.data.set(this.getIndex(x, y, z), input);
    }
}
