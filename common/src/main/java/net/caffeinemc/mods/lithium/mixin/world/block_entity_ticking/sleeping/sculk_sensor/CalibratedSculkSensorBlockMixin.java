package net.caffeinemc.mods.lithium.mixin.world.block_entity_ticking.sleeping.sculk_sensor;

import net.caffeinemc.mods.lithium.common.block.entity.SleepingBlockEntity;
import net.caffeinemc.mods.lithium.common.block.entity.sleeping_sculk_sensor.CustomVibrationTicker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.CalibratedSculkSensorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(CalibratedSculkSensorBlock.class)
public abstract class CalibratedSculkSensorBlockMixin extends BaseEntityBlock {
    public CalibratedSculkSensorBlockMixin(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /**
     * @author
     * @reason
     */
    @Nullable
    @Overwrite
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> blockEntityType) {
        return !level.isClientSide()
                ? createTickerHelper(
                blockEntityType,
                BlockEntityType.CALIBRATED_SCULK_SENSOR,
                (levelx, blockPos, blockStatex, calibratedSculkSensorBlockEntity) ->
                        CustomVibrationTicker.lithium$sleepingTicker(levelx, calibratedSculkSensorBlockEntity.getVibrationData(),
                                calibratedSculkSensorBlockEntity.getVibrationUser(),
                                ((SleepingBlockEntity) calibratedSculkSensorBlockEntity)::lithium$startSleeping
                        )
        )
                : null;
    }
}
