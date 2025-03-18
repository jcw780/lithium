package net.caffeinemc.mods.lithium.mixin.util.block_tracking;

import net.caffeinemc.mods.lithium.common.block.BlockStateFlagHolder;
import net.caffeinemc.mods.lithium.common.block.BlockStateFlags;
import net.caffeinemc.mods.lithium.common.block.TrackedBlockStatePredicate;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = BlockBehaviour.BlockStateBase.class, priority = 1010)
public class BlockStateBaseMixin implements BlockStateFlagHolder {
    @Unique
    private int flags = -1;

    @Override
    public void lithium$initializeFlags() {
        TrackedBlockStatePredicate.FULLY_INITIALIZED.set(true);

        int flags = 0;

        for (int i = 0; i < BlockStateFlags.FLAGS.length; i++) {
            //noinspection ConstantConditions
            if (BlockStateFlags.FLAGS[i].test((BlockState) (Object) this)) {
                flags |= 1 << i;
            }
        }

        this.flags = flags;
    }

    @Override
    public int lithium$getAllFlags() {
        int blockStateFlags = this.flags;
        if (blockStateFlags == -1) {
            throw new IllegalStateException("Tried to get uninitialized block state flags!");
        }
        return blockStateFlags;
    }
}
