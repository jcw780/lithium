package net.caffeinemc.mods.lithium.mixin.world.block_entity_ticking.sleeping.sculk_sensor;

import net.caffeinemc.mods.lithium.common.block.entity.SleepingBlockEntity;
import net.caffeinemc.mods.lithium.common.block.entity.sleeping_sculk_sensor.CustomVibrationTicker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.BaseEntityBlock;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(SculkSensorBlock.class)
public abstract class SculkSensorBlockMixin extends BaseEntityBlock {
    public SculkSensorBlockMixin(Properties properties) {
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
                BlockEntityType.SCULK_SENSOR,
                (levelx, blockPos, blockStatex, sculkSensorBlockEntity) ->
                        CustomVibrationTicker.lithium$sleepingTicker(levelx, sculkSensorBlockEntity.getVibrationData(),
                                sculkSensorBlockEntity.getVibrationUser(),
                                ((SleepingBlockEntity) sculkSensorBlockEntity)::lithium$startSleeping
                )
        )
                : null;
    }

}
