package net.caffeinemc.mods.lithium.mixin.ai.task.nitwit_job_search;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Nitwits still run AcquirePoi task for job sites even though it will always fail.
 * This removes that goal if the acquirable job site is PoiType.NONE.
 * Note: Villagers refresh their brains when modified via commands so this should work even when data modified.
 * @author jcw780
 *
 * Special thanks to a certain 1.12 boomer for running the tests that showed this was an issue.
 */
@Mixin(VillagerGoalPackages.class)
public abstract class VillagerGoalPackagesMixin {
    @Shadow
    private static boolean validateBedPoi(ServerLevel serverLevel, BlockPos blockPos) {
        return false;
    }

    @Inject(method = "getCorePackage", at = @At("HEAD"), cancellable = true)
    private static void noJobSearchGoalPackage(Holder<VillagerProfession> holder, float f, CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir){
        if (holder.value().acquirableJobSite() == PoiType.NONE) {
            cir.setReturnValue(ImmutableList.of(
                    Pair.of(0, new Swim<>(0.8F)),
                    Pair.of(0, InteractWithDoor.create()),
                    Pair.of(0, new LookAtTargetSink(45, 90)),
                    Pair.of(0, new VillagerPanicTrigger()),
                    Pair.of(0, WakeUp.create()),
                    Pair.of(0, ReactToBell.create()),
                    Pair.of(0, SetRaidStatus.create()),
                    Pair.of(0, ValidateNearbyPoi.create(holder.value().heldJobSite(), MemoryModuleType.JOB_SITE)),
                    Pair.of(0, ValidateNearbyPoi.create(holder.value().acquirableJobSite(), MemoryModuleType.POTENTIAL_JOB_SITE)),
                    Pair.of(1, new MoveToTargetSink()),
                    Pair.of(2, PoiCompetitorScan.create()),
                    Pair.of(3, new LookAndFollowTradingPlayerSink(f)),
                    Pair.of(5, GoToWantedItem.create(f, false, 4)),
                    // Removed Job-site AcquirePoi task
                    Pair.of(7, new GoToPotentialJobSite(f)),
                    Pair.of(8, YieldJobSite.create(f)),
                    Pair.of(
                            10, AcquirePoi.create(holderx -> holderx.is(PoiTypes.HOME), MemoryModuleType.HOME, false, Optional.of((byte)14), VillagerGoalPackagesMixin::validateBedPoi)
                    ),
                    Pair.of(10, AcquirePoi.create(holderx -> holderx.is(PoiTypes.MEETING), MemoryModuleType.MEETING_POINT, true, Optional.of((byte)14))),
                    Pair.of(10, AssignProfessionFromJobSite.create()),
                    Pair.of(10, ResetProfession.create())
            ));
            cir.cancel();
        }
    }
}
