package com.noel.fortnitemod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Fortnite.MODID)
public class BlueprintHandler {
    
    public enum BuildMode {
        WALL("Wall", 0),
        RAMP("Ramp", 1),
        FLOOR("Floor", 2);
        
        private final String displayName;
        private final int index;
        
        BuildMode(String displayName, int index) {
            this.displayName = displayName;
            this.index = index;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public BuildMode next() {
            return values()[(this.index + 1) % values().length];
        }
        
        public static BuildMode fromIndex(int index) {
            return values()[Math.abs(index) % values().length];
        }
    }
    
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        Level level = event.getLevel();
        
        // Only handle our blueprint item
        if (!stack.is(ItemInit.BLUEPRINT.get())) {
            return;
        }
        
        // Shift + Right-click to cycle modes
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                BuildMode currentMode = getBuildMode(stack);
                BuildMode newMode = currentMode.next();
                setBuildMode(stack, newMode);
                player.displayClientMessage(
                    Component.literal("§6Build Mode: §f" + newMode.getDisplayName()),
                    true
                );
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        
        // Regular right-click to place block
        if (!level.isClientSide()) {
            BlockHitResult hitResult = event.getHitVec();
            BlockPos clickedPos = hitResult.getBlockPos();
            Direction clickedFace = hitResult.getDirection();
            BlockPos placePos = clickedPos.relative(clickedFace);
            
            // Check if we can place at this position
            if (!level.getBlockState(placePos).canBeReplaced()) {
                return;
            }
            
            BuildMode mode = getBuildMode(stack);
            Block blockToPlace = getBlockForMode(mode);
            
            if (blockToPlace != null) {
                // Determine orientation for 3x3 wall
                Direction playerFacing = player.getDirection();
                Direction wallWidthDir = playerFacing.getClockWise(); // Perpendicular to look direction
                BlockState stateToPlace = blockToPlace.defaultBlockState();

                // Set facing if applicable (for our custom wall)
                if (stateToPlace.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
                    stateToPlace = stateToPlace.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, playerFacing.getOpposite());
                }

                // If it's a wall mode, place 3x3 grid
                if (mode == BuildMode.WALL) {
                    boolean placedAny = false;
                    // Loop 3 wide (centered) and 3 high
                    for (int y = 0; y < 3; y++) {
                        for (int w = -1; w <= 1; w++) {
                            BlockPos targetPos = placePos.relative(wallWidthDir, w).above(y);
                            
                            // Check if valid replacement
                            if (level.getBlockState(targetPos).canBeReplaced() && level.isInWorldBounds(targetPos)) {
                                level.setBlock(targetPos, stateToPlace, 3);
                                placedAny = true;
                            }
                        }
                    }
                    
                    if (placedAny) {
                         level.playSound(null, placePos, 
                            stateToPlace.getSoundType().getPlaceSound(), 
                            net.minecraft.sounds.SoundSource.BLOCKS, 
                            1.0f, 1.0f);
                    }
                } else {
                    // Regular placement for other modes (Ramp/Floor) - simplified for now
                    if (level.getBlockState(placePos).canBeReplaced()) {
                        level.setBlock(placePos, stateToPlace, 3);
                        level.playSound(null, placePos, 
                             stateToPlace.getSoundType().getPlaceSound(), 
                             net.minecraft.sounds.SoundSource.BLOCKS, 
                             1.0f, 1.0f);
                    }
                }
            }
        }
        
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
    
    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        Level level = event.getLevel();
        
        // Only handle our blueprint item
        if (!stack.is(ItemInit.BLUEPRINT.get())) {
            return;
        }
        
        // Shift + Right-click to cycle modes (when not looking at a block)
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                BuildMode currentMode = getBuildMode(stack);
                BuildMode newMode = currentMode.next();
                setBuildMode(stack, newMode);
                player.displayClientMessage(
                    Component.literal("§6Build Mode: §f" + newMode.getDisplayName()),
                    true
                );
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
    
    private static Block getBlockForMode(BuildMode mode) {
        return switch (mode) {
            case WALL -> BlockInit.WOOD_WALL.get();
            case RAMP -> BlockInit.WOOD_RAMP.get();
            case FLOOR -> BlockInit.WOOD_FLOOR.get();
        };
    }
    
    private static BuildMode getBuildMode(ItemStack stack) {
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if (customData != null) {
                var tag = customData.copyTag();
                if (tag.contains("BuildMode")) {
                    int modeIndex = tag.getInt("BuildMode").orElse(0);
                    return BuildMode.fromIndex(modeIndex);
                }
            }
        }
        return BuildMode.WALL; // Default to wall
    }
    
    private static void setBuildMode(ItemStack stack, BuildMode mode) {
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("BuildMode", mode.index);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                  net.minecraft.world.item.component.CustomData.of(tag));
    }
}
