package net.caffeinemc.mods.lithium.mixin.experimental.client_tick.entity.unused_brain;

import it.unimi.dsi.fastutil.objects.AbstractReference2ObjectFunction;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.caffeinemc.mods.lithium.common.ai.brain.memories.BrainExtended;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.Optional;

@Mixin(value = Brain.class)
public class BrainMixin implements BrainExtended {

    @Mutable
    @Shadow
    @Final
    private Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> memories;


    @Override
    public void lithium$pretendAllMemoryTypesRegistered() {
        if (this.memories instanceof AbstractReference2ObjectFunction<?, ?> memoryCollection) {
            //noinspection unchecked
            ((AbstractReference2ObjectFunction<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>>) memoryCollection).defaultReturnValue(Optional.empty());
        } else {
            Reference2ObjectOpenHashMap<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> memoryCollection = new Reference2ObjectOpenHashMap<>(this.memories);
            memoryCollection.defaultReturnValue(Optional.empty());
            this.memories = memoryCollection;
        }
    }

}
