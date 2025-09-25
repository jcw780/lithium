package net.caffeinemc.mods.lithium.mixin.ai.non_poi_block_search;

import net.caffeinemc.mods.lithium.common.ai.non_poi_block_search.LithiumMoveToBlockGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.RemoveBlockGoal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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

    @Redirect(method = "canUse",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/goal/RemoveBlockGoal;findNearestBlock()Z"))
    protected boolean redirectFindNearestBlock(RemoveBlockGoal removeBlockGoal) {
        return ((LithiumMoveToBlockGoal)removeBlockGoal).lithium$findNearestBlock(
                this::lithium$isValidTargetBlock, this::lithium$isValidTargetAbove, false
        );
    }

    //Split check condition in order to use maybeHas
    @Unique
    private boolean lithium$isValidTargetBlock(BlockState blockState){
        return blockState.is(this.blockToRemove);
    }

    @Unique
    private boolean lithium$isValidTargetAbove(ChunkAccess chunkAccess, BlockPos blockPos){
        return chunkAccess.getBlockState(blockPos.above()).isAir()
                && chunkAccess.getBlockState(blockPos.above(2)).isAir();
    }
}
