package com.noel.fortnitemod.item;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BlueprintItem extends Item {
    
    // Use suppliers to get blocks lazily - avoids class loading issues
    private static Supplier<Block> wallSupplier;
    private static Supplier<Block> rampSupplier;
    private static Supplier<Block> floorSupplier;
    
    public static void setBlockSuppliers(Supplier<Block> wall, Supplier<Block> ramp, Supplier<Block> floor) {
        wallSupplier = wall;
        rampSupplier = ramp;
        floorSupplier = floor;
    }
    
    public enum BuildMode {
        WALL("Wall"),
        RAMP("Ramp"),
        FLOOR("Floor");
        
        private final String displayName;
        
        BuildMode(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public BuildMode next() {
            BuildMode[] values = BuildMode.values();
            return values[(this.ordinal() + 1) % values.length];
        }
        
        public static BuildMode fromIndex(int index) {
            BuildMode[] values = BuildMode.values();
            return values[Math.abs(index) % values.length];
        }
    }
    
    public BlueprintItem(Properties properties) {
        super(properties.stacksTo(1));
    }
    
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // Shift + Right-click to cycle modes
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                BuildMode currentMode = getBuildMode(stack);
                BuildMode newMode = currentMode.next();
                setBuildMode(stack, newMode);
                player.displayClientMessage(
                    Component.literal("§6Build Mode: §f" + newMode.getDisplayName()),
                    true // Action bar
                );
            }
            return InteractionResult.SUCCESS;
        }
        
        return InteractionResult.PASS;
    }
    
    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        
        if (player == null) return InteractionResult.PASS;
        
        // Shift + Right-click to cycle modes (handled in use())
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        
        // Regular right-click to place block
        if (!level.isClientSide()) {
            BlockPos clickedPos = context.getClickedPos();
            Direction clickedFace = context.getClickedFace();
            BlockPos placePos = clickedPos.relative(clickedFace);
            
            // Check if we can place at this position
            if (!level.getBlockState(placePos).canBeReplaced()) {
                return InteractionResult.PASS;
            }
            
            BuildMode mode = getBuildMode(stack);
            Block blockToPlace = getBlockForMode(mode);
            
            if (blockToPlace != null) {
                BlockPlaceContext placeContext = new BlockPlaceContext(context);
                BlockState stateToPlace = blockToPlace.getStateForPlacement(placeContext);
                
                if (stateToPlace != null) {
                    level.setBlock(placePos, stateToPlace, 3);
                    level.playSound(null, placePos, 
                        stateToPlace.getSoundType().getPlaceSound(), 
                        net.minecraft.sounds.SoundSource.BLOCKS, 
                        1.0f, 1.0f);
                }
            }
        }
        
        return InteractionResult.SUCCESS;
    }
    
    private Block getBlockForMode(BuildMode mode) {
        return switch (mode) {
            case WALL -> wallSupplier != null ? wallSupplier.get() : null;
            case RAMP -> rampSupplier != null ? rampSupplier.get() : null;
            case FLOOR -> floorSupplier != null ? floorSupplier.get() : null;
        };
    }
    
    private BuildMode getBuildMode(ItemStack stack) {
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
    
    private void setBuildMode(ItemStack stack, BuildMode mode) {
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("BuildMode", mode.ordinal());
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                  net.minecraft.world.item.component.CustomData.of(tag));
    }
    
    @Override
    public Component getName(ItemStack stack) {
        BuildMode mode = getBuildMode(stack);
        return Component.literal("Blueprint (" + mode.getDisplayName() + ")");
    }
}
