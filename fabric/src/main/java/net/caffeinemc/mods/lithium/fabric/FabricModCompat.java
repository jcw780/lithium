package net.caffeinemc.mods.lithium.fabric;

import net.caffeinemc.mods.lithium.common.services.PlatformModCompat;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FabricModCompat implements PlatformModCompat {

    @Override
    public boolean canHopperInteractWithApiInventory(HopperBlockEntity hopperBlockEntity, BlockState hopperState, boolean extracting) {
        Direction direction = extracting ? Direction.UP : hopperState.getValue(HopperBlock.FACING);
        BlockPos targetPos = hopperBlockEntity.getBlockPos().relative(direction);

        Object target = ItemStorage.SIDED.find(hopperBlockEntity.getLevel(), targetPos, direction.getOpposite());
        return target != null;
    }
}
