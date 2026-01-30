package net.caffeinemc.mods.lithium.mixin.ai.useless_behaviors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import net.caffeinemc.mods.lithium.common.ai.useless_behaviors.LithiumEmptyBehavior;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Set;

@Mixin(Brain.class)
public abstract class BrainMixin<E extends LivingEntity> {
    @Shadow
    @Final
    private Map<Integer, Map<Activity, Set<BehaviorControl<? super E>>>> availableBehaviorsByPriority;

    /**
     * @author jcw780
     * @reason Prevent EMPTY_BEHAVIOR_SENTINEL from being added - those are what useless behaviors are replaced with.
     */
    @Inject(method = "addActivityAndRemoveMemoriesWhenStopped", cancellable = true,
            at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList;iterator()Lcom/google/common/collect/UnmodifiableIterator;"))
    private void replaceLoop(Activity activity, ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> immutableList, Set<Pair<MemoryModuleType<?>, MemoryStatus>> set, Set<MemoryModuleType<?>> set2, CallbackInfo ci) {
        for (Pair<Integer, ? extends BehaviorControl<? super E>> pair : immutableList) {
            if (pair.getSecond() != LithiumEmptyBehavior.EMPTY_BEHAVIOR_SENTINEL) {
                ((Set)((Map)this.availableBehaviorsByPriority.computeIfAbsent(pair.getFirst(), integer -> Maps.newHashMap()))
                        .computeIfAbsent(activity, activityx -> Sets.newLinkedHashSet()))
                        .add(pair.getSecond());
            }
        }
        ci.cancel();
    }
}
