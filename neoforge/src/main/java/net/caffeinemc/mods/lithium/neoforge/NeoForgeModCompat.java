package net.caffeinemc.mods.lithium.neoforge;

import net.caffeinemc.mods.lithium.common.services.PlatformModCompat;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class NeoForgeModCompat implements PlatformModCompat {

    @Override
    public boolean canHopperInteractWithApiInventory(HopperBlockEntity hopperBlockEntity, BlockState hopperState, boolean extracting) {
        return true; //TODO IMPLEMENT
    }
}
