package com.noel.fortnitemod.client;

import com.noel.fortnitemod.BlockInit;
import com.noel.fortnitemod.BlueprintHandler;
import com.noel.fortnitemod.BuildSnapHelper;
import com.noel.fortnitemod.Fortnite;
import com.noel.fortnitemod.ItemInit;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Fortnite.MODID, value = Dist.CLIENT)
public class BuildPreviewRenderer {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        
        if (!level.isClientSide()) return;

        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.is(ItemInit.BLUEPRINT.get())) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        HitResult hitResult = mc.hitResult;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos clickedPos = blockHit.getBlockPos();
        Direction clickedFace = blockHit.getDirection();
        BlockPos basePos = clickedPos.relative(clickedFace);

        BlueprintHandler.BuildMode mode = BlueprintHandler.getBuildModeStatic(mainHand);
        
        // Apply snapping based on mode
        if (mode == BlueprintHandler.BuildMode.WALL) {
            basePos = BuildSnapHelper.getSnappedPosition(level, basePos, player, BlockInit.WOOD_WALL.get());
        } else if (mode == BlueprintHandler.BuildMode.RAMP) {
            basePos = BuildSnapHelper.getSnappedRampPosition(level, basePos, player);
        }
        
        List<BlockPos> previewPositions = getPreviewPositions(player, basePos, mode, level);

        // Use INSTANT particle type - disappears immediately
        for (BlockPos pos : previewPositions) {
            spawnInstantOutline(level, pos);
        }
    }

    public static List<BlockPos> getPreviewPositions(Player player, BlockPos basePos, BlueprintHandler.BuildMode mode, Level level) {
        List<BlockPos> positions = new ArrayList<>();
        
        if (mode == BlueprintHandler.BuildMode.WALL) {
            Direction playerFacing = player.getDirection();
            Direction wallWidthDir = playerFacing.getClockWise();
            
            // 3x3 vertical wall
            for (int y = 0; y < 3; y++) {
                for (int w = -1; w <= 1; w++) {
                    BlockPos pos = basePos.relative(wallWidthDir, w).above(y);
                    if (level.getBlockState(pos).canBeReplaced()) {
                        positions.add(pos);
                    }
                }
            }
        } else if (mode == BlueprintHandler.BuildMode.FLOOR) {
            // 3x3 horizontal floor - centered on basePos
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = basePos.offset(x, 0, z);
                    if (level.getBlockState(pos).canBeReplaced()) {
                        positions.add(pos);
                    }
                }
            }
        } else {
            // Ramp - 3 wide, 3 deep stepping up in player's facing direction
            Direction playerFacing = player.getDirection();
            Direction rampWidthDir = playerFacing.getClockWise();
            
            for (int depth = 0; depth < 3; depth++) {
                for (int w = -1; w <= 1; w++) {
                    // Each row going forward is 1 block higher
                    BlockPos pos = basePos.relative(playerFacing, depth).relative(rampWidthDir, w).above(depth);
                    if (level.getBlockState(pos).canBeReplaced()) {
                        positions.add(pos);
                    }
                }
            }
        }
        
        return positions;
    }

    private static void spawnInstantOutline(Level level, BlockPos pos) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        
        // ELECTRIC_SPARK - very short lifetime, bright flash
        var particleType = ParticleTypes.ELECTRIC_SPARK;
        
        // All 12 edges with particles along each edge
        // Bottom edges (4 edges)
        for (float t = 0; t <= 1; t += 0.25f) {
            level.addParticle(particleType, x + t, y, z, 0, 0, 0);
            level.addParticle(particleType, x + t, y, z + 1, 0, 0, 0);
            level.addParticle(particleType, x, y, z + t, 0, 0, 0);
            level.addParticle(particleType, x + 1, y, z + t, 0, 0, 0);
        }
        
        // Top edges (4 edges)
        for (float t = 0; t <= 1; t += 0.25f) {
            level.addParticle(particleType, x + t, y + 1, z, 0, 0, 0);
            level.addParticle(particleType, x + t, y + 1, z + 1, 0, 0, 0);
            level.addParticle(particleType, x, y + 1, z + t, 0, 0, 0);
            level.addParticle(particleType, x + 1, y + 1, z + t, 0, 0, 0);
        }
        
        // Vertical edges (4 edges)
        for (float t = 0; t <= 1; t += 0.25f) {
            level.addParticle(particleType, x, y + t, z, 0, 0, 0);
            level.addParticle(particleType, x + 1, y + t, z, 0, 0, 0);
            level.addParticle(particleType, x, y + t, z + 1, 0, 0, 0);
            level.addParticle(particleType, x + 1, y + t, z + 1, 0, 0, 0);
        }
    }
}
