package net.caffeinemc.mods.lithium.common.entity.movement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;


final public class ChunkAwareBlockCollisionSweeperBlockPos extends ChunkAwareBlockCollisionSweeper<BlockPos.MutableBlockPos> {
    private boolean collided;
    final private BlockPos.MutableBlockPos prevMaxPos = new BlockPos.MutableBlockPos();

    public ChunkAwareBlockCollisionSweeperBlockPos(Level world, @Nullable Entity entity, AABB box) {
        super(world, entity, box, false);
    }
    public ChunkAwareBlockCollisionSweeperBlockPos(Level world, @Nullable Entity entity, AABB box, boolean hideLastCollision) {
        super(world, entity, box, hideLastCollision);
        this.collided = false;
    }

    /**
     * Advances the sweep forward until finding a block with a box-colliding VoxelShape
     *
     * @return null if no VoxelShape is left in the area, otherwise the next VoxelShape
     */
    @Override
    public BlockPos.MutableBlockPos computeNext() {
        while (true) {
            if (this.cIterated >= this.cTotalSize) {
                if (!this.nextSection()) {
                    break;
                }
            }

            this.cIterated++;


            final int x = this.cX;
            final int y = this.cY;
            final int z = this.cZ;

            //The iteration order within a chunk section is chosen so that it causes a mostly linear array access in the storage.
            //In net.minecraft.world.chunk.PalettedContainer.toIndex x gets the 4 least significant bits, z the 4 above, and y the 4 even higher ones.
            //Linearly accessing arrays is faster than other access patterns.
            if (this.cX < this.cEndX) {
                this.cX++;
            } else if (this.cZ < this.cEndZ) {
                this.cX = this.cStartX;
                this.cZ++;
            } else {
                this.cX = this.cStartX;
                this.cZ = this.cStartZ;
                this.cY++;
                //stop condition was already checked using this.cIterated at the start of the method
            }

            //using < minX and > maxX instead of <= and >= in vanilla, because minX, maxX are the coordinates
            //of the box that wasn't extended for oversized blocks yet.
            final int edgesHit = this.sectionOversizedBlocks ?
                    (x < this.minX || x > this.maxX ? 1 : 0) +
                            (y < this.minY || y > this.maxY ? 1 : 0) +
                            (z < this.minZ || z > this.maxZ ? 1 : 0) : 0;

            if (edgesHit == 3) {
                continue;
            }

            final BlockState state = this.cachedChunkSection.getBlockState(x & 15, y & 15, z & 15);

            if (!canInteractWithBlock(state, edgesHit)) {
                continue;
            }

            this.pos.set(x, y, z);

            VoxelShape collisionShape = this.context.getCollisionShape(state, this.world, this.pos);

            //noinspection ConstantValue
            if (collisionShape != Shapes.empty() && collisionShape != null /*collisionShape should never be null, but we received crash reports.*/) {
                VoxelShape collidedShape = getCollidedShape(this.box, this.shape, collisionShape, x, y, z);
                if (collidedShape != null) {
                    if (z >= this.maxHitZ && (z > this.maxHitZ || y >= this.maxHitY && (y > this.maxHitY || x > this.maxHitX))) {
                        this.prevMaxPos.set(maxHitX, maxHitY, maxHitZ);

                        this.maxHitX = x;
                        this.maxHitY = y;
                        this.maxHitZ = z;
                        //Always make sure the shape at the maximum position is the last one returned, because
                        // the last shape has a different 1e-7 behavior (no next shape that clips movement to 0).
                        // This does affect certain contraptions: https://github.com/CaffeineMC/lithium-fabric/issues/443
                        this.maxShape = collidedShape;
                        if (collided) {
                            return prevMaxPos;
                        } else {
                            collided = true;
                        }
                    } else {
                        return pos;
                    }
                }
            }
        }

        if (!this.hideLastCollision && this.maxShape != null) {
            this.prevMaxPos.set(maxHitX, maxHitY, maxHitZ);
            this.maxShape = null;
            return prevMaxPos;
        }

        return this.endOfData();
    }

}
