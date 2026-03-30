package net.caffeinemc.mods.lithium.mixin.world.chunk_ticking.random_block_ticking;

import net.caffeinemc.mods.lithium.common.world.section.PlayerClosestToSection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.PlayerMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkMap.class)
public class ChunkMapMixin implements PlayerClosestToSection {
    @Shadow
    @Final
    PlayerMap playerMap;

    @Override
    public boolean lithium$anyPlayerCloseEnoughToSection(final long sectionPos, final int maxDistance) {
        final int sectionX = SectionPos.x(sectionPos);
        final int sectionY = SectionPos.y(sectionPos);
        final int sectionZ = SectionPos.z(sectionPos);
        for (ServerPlayer player : this.playerMap.getAllPlayers()) {
            if (player.isSpectator()) {
                continue;
            }

            final Vec3 playerPos = player.position();
            final double xd = Mth.clamp(playerPos.x, sectionX, sectionX + 15) - playerPos.x;
            final double yd = Mth.clamp(playerPos.y, sectionY, sectionY + 15) - playerPos.y;
            final double zd = Mth.clamp(playerPos.z, sectionZ, sectionZ + 15) - playerPos.z;
            if (Math.sqrt(xd * xd + yd * yd + zd * zd) < maxDistance) {
                return true;
            }
        }
        return false;
    }
}
