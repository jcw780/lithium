package net.caffeinemc.mods.lithium.common.block.entity.sleeping_sculk_sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationInfo;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;

public class CustomVibrationTicker {
    private static void trySelectAndScheduleVibration(ServerLevel serverLevel, VibrationSystem.Data data, VibrationSystem.User user) {
        data.getSelectionStrategy()
                .chosenCandidate(serverLevel.getGameTime())
                .ifPresent(
                        vibrationInfo -> {
                            data.setCurrentVibration(vibrationInfo);
                            Vec3 vec3 = vibrationInfo.pos();
                            data.setTravelTimeInTicks(user.calculateTravelTimeInTicks(vibrationInfo.distance()));
                            serverLevel.sendParticles(
                                    new VibrationParticleOption(user.getPositionSource(), data.getTravelTimeInTicks()), vec3.x, vec3.y, vec3.z, 1, 0.0, 0.0, 0.0, 0.0
                            );
                            user.onDataChanged();
                            data.getSelectionStrategy().startOver();
                        }
                );
    }

    private static void tryReloadVibrationParticle(ServerLevel serverLevel, VibrationSystem.Data data, VibrationSystem.User user) {
        if (data.shouldReloadVibrationParticle()) {
            if (data.getCurrentVibration() == null) {
                data.setReloadVibrationParticle(false);
            } else {
                Vec3 vec3 = data.getCurrentVibration().pos();
                PositionSource positionSource = user.getPositionSource();
                Vec3 vec32 = (Vec3)positionSource.getPosition(serverLevel).orElse(vec3);
                int i = data.getTravelTimeInTicks();
                int j = user.calculateTravelTimeInTicks(data.getCurrentVibration().distance());
                double d = 1.0 - (double)i / j;
                double e = Mth.lerp(d, vec3.x, vec32.x);
                double f = Mth.lerp(d, vec3.y, vec32.y);
                double g = Mth.lerp(d, vec3.z, vec32.z);
                boolean bl = serverLevel.sendParticles(new VibrationParticleOption(positionSource, i), e, f, g, 1, 0.0, 0.0, 0.0, 0.0) > 0;
                if (bl) {
                    data.setReloadVibrationParticle(false);
                }
            }
        }
    }

    private static boolean receiveVibration(ServerLevel serverLevel, VibrationSystem.Data data, VibrationSystem.User user, VibrationInfo vibrationInfo) {
        BlockPos blockPos = BlockPos.containing(vibrationInfo.pos());
        BlockPos blockPos2 = (BlockPos)user.getPositionSource().getPosition(serverLevel).map(BlockPos::containing).orElse(blockPos);
        if (user.requiresAdjacentChunksToBeTicking() && !areAdjacentChunksTicking(serverLevel, blockPos2)) {
            return false;
        } else {
            user.onReceiveVibration(
                    serverLevel,
                    blockPos,
                    vibrationInfo.gameEvent(),
                    (Entity)vibrationInfo.getEntity(serverLevel).orElse(null),
                    (Entity)vibrationInfo.getProjectileOwner(serverLevel).orElse(null),
                    VibrationSystem.Listener.distanceBetweenInBlocks(blockPos, blockPos2)
            );
            data.setCurrentVibration(null);
            return true;
        }
    }

    private static boolean areAdjacentChunksTicking(Level level, BlockPos blockPos) {
        ChunkPos chunkPos = new ChunkPos(blockPos);

        for (int i = chunkPos.x - 1; i <= chunkPos.x + 1; i++) {
            for (int j = chunkPos.z - 1; j <= chunkPos.z + 1; j++) {
                if (!level.shouldTickBlocksAt(ChunkPos.asLong(i, j)) || level.getChunkSource().getChunkNow(i, j) == null) {
                    return false;
                }
            }
        }

        return true;
    }

    public static void lithium$sleepingTicker(Level level, VibrationSystem.Data data, VibrationSystem.User user, Runnable sleep) {
        if (level instanceof ServerLevel serverLevel) {
            if (data.getCurrentVibration() == null) {
                trySelectAndScheduleVibration(serverLevel, data, user);
                if (data.getCurrentVibration() == null) {
                    sleep.run();
                }
            }

            if (data.getCurrentVibration() != null) {
                boolean bl = data.getTravelTimeInTicks() > 0;
                tryReloadVibrationParticle(serverLevel, data, user);
                data.decrementTravelTime();
                if (data.getTravelTimeInTicks() <= 0) {
                    bl = receiveVibration(serverLevel, data, user, data.getCurrentVibration());
                    sleep.run();
                }

                if (bl) {
                    user.onDataChanged();
                }
            }
        }
    }
}
