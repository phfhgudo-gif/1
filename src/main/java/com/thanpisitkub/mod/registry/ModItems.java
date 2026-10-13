package com.thanpisitkub.mod.registry;

import com.thanpisitkub.mod.ThanpisitkubMod;
import com.thanpisitkub.mod.item.PowerCoreItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private ModItems() {
    }

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ThanpisitkubMod.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ThanpisitkubMod.MOD_ID);

    public static final RegistryObject<Item> POWER_CORE = ITEMS.register("thanpisitkub_core",
            () -> new PowerCoreItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.thanpisitkub"))
                    .icon(() -> new ItemStack(POWER_CORE.get()))
                    .displayItems((params, output) -> output.accept(POWER_CORE.get()))
                    .build());
}
