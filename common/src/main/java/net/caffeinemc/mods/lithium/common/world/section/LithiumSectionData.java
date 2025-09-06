package net.caffeinemc.mods.lithium.common.world.section;

import net.caffeinemc.mods.lithium.common.tracking.block.ChunkSectionChangeCallback;

import java.util.Arrays;

public interface LithiumSectionData {

    class SectionData {
        private short[] countsByFlag;
        private ChunkSectionChangeCallback changeListener;

        public SectionData() {
            this.countsByFlag = null;
            this.changeListener = null;
        }

        public void setCountsByFlag(short[] countsByFlag) {
            this.countsByFlag = countsByFlag;
        }

        public short[] getCountsByFlag() {
            return this.countsByFlag;
        }

        public void setChangeListener(ChunkSectionChangeCallback changeListener) {
            this.changeListener = changeListener;
        }

        public ChunkSectionChangeCallback getChangeListener() {
            return this.changeListener;
        }

        @Override
        public String toString() {
            return "SectionData[" +
                    "countsByFlag=" + Arrays.toString(this.countsByFlag) + ", " +
                    "changeListener=" + this.changeListener + ']';
        }


    }

    LithiumSectionData.SectionData lithium$getSectionData();
}
