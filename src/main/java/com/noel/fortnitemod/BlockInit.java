package com.noel.fortnitemod;

import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.noel.fortnitemod.block.WoodWallBlock;

public class BlockInit {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Fortnite.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Fortnite.MODID);

    // Wood Wall - thin wall (model makes it thin, not code)
    public static final DeferredBlock<Block> WOOD_WALL = BLOCKS.registerSimpleBlock(
            "wood_wall",
            p -> p.mapColor(MapColor.WOOD).strength(0.5f).sound(SoundType.WOOD).noOcclusion()
    );
    public static final DeferredItem<BlockItem> WOOD_WALL_ITEM = ITEMS.registerSimpleBlockItem("wood_wall", WOOD_WALL);

    // Wood Ramp - simple block for now
    public static final DeferredBlock<Block> WOOD_RAMP = BLOCKS.registerSimpleBlock(
            "wood_ramp",
            p -> p.mapColor(MapColor.WOOD).strength(0.5f).sound(SoundType.WOOD)
    );
    public static final DeferredItem<BlockItem> WOOD_RAMP_ITEM = ITEMS.registerSimpleBlockItem("wood_ramp", WOOD_RAMP);

    // Wood Floor - simple block for now
    public static final DeferredBlock<Block> WOOD_FLOOR = BLOCKS.registerSimpleBlock(
            "wood_floor",
            p -> p.mapColor(MapColor.WOOD).strength(0.5f).sound(SoundType.WOOD)
    );
    public static final DeferredItem<BlockItem> WOOD_FLOOR_ITEM = ITEMS.registerSimpleBlockItem("wood_floor", WOOD_FLOOR);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
    
}
