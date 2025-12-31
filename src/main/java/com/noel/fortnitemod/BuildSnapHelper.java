package com.noel.fortnitemod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Utility class for Fortnite-style wall snapping.
 * Finds nearby walls and snaps new builds to align with them.
 */
public class BuildSnapHelper {
    
    // How far to search for walls to snap to (in blocks)
    private static final int SNAP_RANGE = 4;
    
    /**
     * Attempts to find a snap position for a new wall.
     * Searches for nearby wood_wall blocks and returns:
     * - A position that would align with/connect to them
     * - Or the original position if no snap target found
     */
    public static BlockPos getSnappedPosition(Level level, BlockPos originalPos, Player player, Block wallBlock) {
        Direction playerFacing = player.getDirection();
        Direction wallWidthDir = playerFacing.getClockWise();
        
        // Search for nearby walls to snap to
        BlockPos bestSnap = null;
        double bestDist = Double.MAX_VALUE;
        
        // Check positions around the original placement
        for (int dx = -SNAP_RANGE; dx <= SNAP_RANGE; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -SNAP_RANGE; dz <= SNAP_RANGE; dz++) {
                    BlockPos checkPos = originalPos.offset(dx, dy, dz);
                    
                    // Is there a wall block here?
                    if (level.getBlockState(checkPos).is(wallBlock)) {
                        // Found a wall! Calculate snap positions adjacent to it
                        // Walls are 3 wide (center + 1 on each side), so we need to offset by 2 from edge
                        
                        // Try snapping to the right of this wall segment
                        // Find the rightmost edge, then go 2 more blocks right (so new wall doesn't overlap)
                        BlockPos rightEdge = findWallEdge(level, checkPos, wallWidthDir, wallBlock);
                        BlockPos snapRight = rightEdge.relative(wallWidthDir, 2); // +2 to place center of new wall
                        if (isValidSnapTarget(level, snapRight, wallWidthDir)) {
                            double dist = originalPos.distSqr(snapRight);
                            if (dist < bestDist) {
                                bestDist = dist;
                                bestSnap = snapRight;
                            }
                        }
                        
                        // Try snapping to the left
                        BlockPos leftEdge = findWallEdge(level, checkPos, wallWidthDir.getOpposite(), wallBlock);
                        BlockPos snapLeft = leftEdge.relative(wallWidthDir.getOpposite(), 2);
                        if (isValidSnapTarget(level, snapLeft, wallWidthDir)) {
                            double dist = originalPos.distSqr(snapLeft);
                            if (dist < bestDist) {
                                bestDist = dist;
                                bestSnap = snapLeft;
                            }
                        }
                        
                        // Try snapping above (walls are 3 high, so offset by 3)
                        BlockPos topEdge = findWallTop(level, checkPos, wallBlock);
                        BlockPos snapUp = topEdge.above(1); // Top of existing wall + 1 = bottom of new wall
                        if (isValidSnapTarget(level, snapUp, wallWidthDir)) {
                            double dist = originalPos.distSqr(snapUp);
                            if (dist < bestDist) {
                                bestDist = dist;
                                bestSnap = snapUp;
                            }
                        }
                        
                        // Try snapping below (find bottom and go 3 down)
                        BlockPos bottomEdge = findWallBottom(level, checkPos, wallBlock);
                        BlockPos snapDown = bottomEdge.below(3); // 3 blocks down for new wall's top to touch old wall's bottom
                        if (isValidSnapTarget(level, snapDown, wallWidthDir)) {
                            double dist = originalPos.distSqr(snapDown);
                            if (dist < bestDist) {
                                bestDist = dist;
                                bestSnap = snapDown;
                            }
                        }
                    }
                }
            }
        }
        
        // Only snap if close enough (within 3 blocks of original aim)
        if (bestSnap != null && bestDist <= 9) { // 3^2 = 9
            return bestSnap;
        }
        
        return originalPos;
    }
    
    /**
     * Finds the edge of a wall in a given direction.
     */
    private static BlockPos findWallEdge(Level level, BlockPos start, Direction dir, Block wallBlock) {
        BlockPos current = start;
        while (level.getBlockState(current.relative(dir)).is(wallBlock)) {
            current = current.relative(dir);
        }
        return current;
    }
    
    /**
     * Finds the top of a wall column.
     */
    private static BlockPos findWallTop(Level level, BlockPos start, Block wallBlock) {
        BlockPos current = start;
        while (level.getBlockState(current.above()).is(wallBlock)) {
            current = current.above();
        }
        return current;
    }
    
    /**
     * Finds the bottom of a wall column.
     */
    private static BlockPos findWallBottom(Level level, BlockPos start, Block wallBlock) {
        BlockPos current = start;
        while (level.getBlockState(current.below()).is(wallBlock)) {
            current = current.below();
        }
        return current;
    }
    
    /**
     * Checks if a position is valid for placing a new wall.
     */
    private static boolean isValidSnapTarget(Level level, BlockPos pos, Direction wallDir) {
        // Make sure we can place at least the bottom-center block
        if (!level.getBlockState(pos).canBeReplaced()) {
            return false;
        }
        return level.isInWorldBounds(pos);
    }
    
    /**
     * Snaps ramp placement to continue from existing stairs.
     * Finds nearby stairs and returns a position to continue the staircase diagonally.
     */
    public static BlockPos getSnappedRampPosition(Level level, BlockPos originalPos, Player player) {
        Direction playerFacing = player.getDirection();
        
        BlockPos bestSnap = null;
        double bestDist = Double.MAX_VALUE;
        
        // Search for nearby stairs
        for (int dx = -SNAP_RANGE; dx <= SNAP_RANGE; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -SNAP_RANGE; dz <= SNAP_RANGE; dz++) {
                    BlockPos checkPos = originalPos.offset(dx, dy, dz);
                    
                    if (isStairBlock(level, checkPos)) {
                        // Found stairs! Try snapping to continue in same direction
                        // Ramps are 3 deep and 3 high, so next ramp starts 3 forward and 3 up
                        
                        // Snap above/forward (continue going up)
                        BlockPos snapUp = checkPos.relative(playerFacing, 3).above(3);
                        if (level.getBlockState(snapUp).canBeReplaced()) {
                            double dist = originalPos.distSqr(snapUp);
                            if (dist < bestDist) {
                                bestDist = dist;
                                bestSnap = snapUp;
                            }
                        }
                        
                        // Snap below/backward (continue going down)
                        BlockPos snapDown = checkPos.relative(playerFacing.getOpposite(), 3).below(3);
                        if (level.getBlockState(snapDown).canBeReplaced()) {
                            double dist = originalPos.distSqr(snapDown);
                            if (dist < bestDist) {
                                bestDist = dist;
                                bestSnap = snapDown;
                            }
                        }
                    }
                }
            }
        }
        
        // Only snap if close enough
        if (bestSnap != null && bestDist <= 16) { // 4^2 = 16
            return bestSnap;
        }
        
        return originalPos;
    }
    
    private static boolean isStairBlock(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.getBlock() instanceof net.minecraft.world.level.block.StairBlock;
    }
}
