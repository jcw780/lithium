package net.caffeinemc.mods.lithium.mixin.ai.useless_behaviors;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Iterator;

@Mixin(Brain.class)
public abstract class BrainMixin<E extends LivingEntity> {

    /**
     * @author jcw780, 2No2Name
     * @reason Prevent EMPTY_BEHAVIOR_SENTINEL from being added - those are what useless behaviors are replaced with.
     */
    @WrapOperation(method = "addActivityAndRemoveMemoriesWhenStopped",
            at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList;iterator()Lcom/google/common/collect/UnmodifiableIterator;"))
    private UnmodifiableIterator<E> filterSentinels(ImmutableList<E> instance, Operation<UnmodifiableIterator<E>> original) {
        Iterator<E> wrapped = original.call(instance);
        return new AbstractIterator<>() {
            @Override
            protected @Nullable E computeNext() {
                if (wrapped.hasNext()) {
                    return wrapped.next();
                }
                return this.endOfData();
            }
        };
    }
}
