package net.caffeinemc.mods.lithium.mixin.ai.useless_behaviors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.Set;

@Mixin(Brain.class)
public abstract class BrainMixin<E extends LivingEntity> {
    @Shadow
    @Final
    private Map<Integer, Map<Activity, Set<BehaviorControl<? super E>>>> availableBehaviorsByPriority;

    @Shadow
    @Final
    private Map<Activity, Set<Pair<MemoryModuleType<?>, MemoryStatus>>> activityRequirements;

    @Shadow
    @Final
    private Map<Activity, Set<MemoryModuleType<?>>> activityMemoriesToEraseWhenStopped;

    /**
     * @author jcw780
     * @reason This is so one can just null out behaviors earlier to prevent them from being added - otherwise the game will crash.
     * Note: Could also use a sentinel instead of null if preferred.
     */
    @Overwrite
    public void addActivityAndRemoveMemoriesWhenStopped(
            Activity activity,
            ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> immutableList,
            Set<Pair<MemoryModuleType<?>, MemoryStatus>> set,
            Set<MemoryModuleType<?>> set2
    ) {
        this.activityRequirements.put(activity, set);
        if (!set2.isEmpty()) {
            this.activityMemoriesToEraseWhenStopped.put(activity, set2);
        }

        for (Pair<Integer, ? extends BehaviorControl<? super E>> pair : immutableList) {
            if (pair.getSecond() != null) {
                ((Set)((Map)this.availableBehaviorsByPriority.computeIfAbsent(pair.getFirst(), integer -> Maps.newHashMap()))
                        .computeIfAbsent(activity, activityx -> Sets.newLinkedHashSet()))
                        .add(pair.getSecond());
            }
        }
    }
}
