package net.caffeinemc.mods.lithium.mixin.ai.useless_behaviors;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * This is so one can just null out behaviors earlier to prevent them from being added - otherwise the game will crash.
 * Note: Could also use a sentinel instead of null if preferred.
 *
 * @author jcw780
 */
@Mixin(Brain.class)
public class BrainMixin {
    @Inject(method = "addActivityAndRemoveMemoriesWhenStopped", cancellable = true, at = @At(value = "INVOKE", target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;"))
    private void removeNullBehaviors(Activity activity, ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<?>>> immutableList,
                                  Set<Pair<MemoryModuleType<?>, MemoryStatus>> set, Set<MemoryModuleType<?>> set2,
                                  CallbackInfo ci, @Local Pair<Integer, ? extends BehaviorControl<?>> pair) {
        if (pair.getSecond() == null) {
            ci.cancel();
        }
    }
}
