package net.caffeinemc.mods.lithium.common.block.entity.sleeping_sculk_sensor;

import net.minecraft.world.level.gameevent.vibrations.VibrationInfo;

import java.util.function.Consumer;

public interface ListeningVibrationData {
    void lithium$setCurrentVibrationUpdateListener(Runnable listener);
}
