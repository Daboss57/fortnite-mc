package com.noel.fortnitemod;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ItemInit {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Fortnite.MODID);

    // Use a simple item for now - we'll add Blueprint behavior via events
    public static final DeferredItem<Item> BLUEPRINT = ITEMS.registerSimpleItem("blueprint");
    
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}