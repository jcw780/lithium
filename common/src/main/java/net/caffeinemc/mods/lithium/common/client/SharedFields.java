package net.caffeinemc.mods.lithium.common.client;

import net.caffeinemc.mods.lithium.common.ai.brain.BrainExtended;
import net.minecraft.world.entity.ai.Brain;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.atomic.AtomicInteger;

public class SharedFields {
    public static final AtomicInteger MAXIMUM_BIOME_PARTICLE_CHANCE = new AtomicInteger(Float.floatToIntBits(0.0F)); //Using atomic integer as replacement for atomic float
    @Unique
    public static final Brain<?> DUMMY_BRAIN;

    static {
        var brain = new Brain<>();
        ((BrainExtended) brain).lithium$pretendAllMemoryTypesRegistered();
        DUMMY_BRAIN = brain;
    }
}
