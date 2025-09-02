package net.caffeinemc.mods.lithium.mixin.ai.non_poi_block_search;

import net.caffeinemc.mods.lithium.common.ai.non_poi_block_search.LithiumMoveToBlockGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.RemoveBlockGoal;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(RemoveBlockGoal.class)
public abstract class RemoveBlockGoalMixin extends MoveToBlockGoal implements LithiumMoveToBlockGoal {
    @Shadow
    @Final
    private Block blockToRemove;

    public RemoveBlockGoalMixin(PathfinderMob pathfinderMob, double d, int i) {
        super(pathfinderMob, d, i);
    }

    // Trim down isValidTarget equivalent because the block is already checked in lithium$findNearestBlock
    // Use cached ChunkAccess instead of getting it in isValidTarget since getting it is remarkably expensive
    @Redirect(method = "canUse",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/goal/RemoveBlockGoal;findNearestBlock()Z"))
    protected boolean findNearestBlock(RemoveBlockGoal removeBlockGoal) {
        return ((LithiumMoveToBlockGoal)removeBlockGoal).lithium$findNearestBlock(
                (blockState) -> blockState.is(this.blockToRemove),
                (chunkAccess, blockPos) ->
                        chunkAccess.getBlockState(blockPos.above()).isAir()
                        && chunkAccess.getBlockState(blockPos.above(2)).isAir()
        );
    }
}
