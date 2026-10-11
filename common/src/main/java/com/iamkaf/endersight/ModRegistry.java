package com.iamkaf.endersight;

import com.iamkaf.amber.api.registry.v1.DeferredRegister;
import com.iamkaf.amber.api.registry.v1.RegistrySupplier;
import com.iamkaf.amber.api.registry.v1.creativetabs.CreativeTabHelper;
import com.iamkaf.endersight.block.WatchersEyeBlock;
import com.iamkaf.endersight.block.WatchersEyeBlockEntity;
import com.iamkaf.endersight.item.Attunement;
import com.iamkaf.endersight.item.EnderMarkItem;
import com.iamkaf.endersight.item.EnderSpyglassItem;
import com.iamkaf.endersight.item.EnderVeilItem;
import com.iamkaf.endersight.item.SeersPearlItem;
import com.iamkaf.endersight.item.SightEyeItem;
import com.iamkaf.endersight.item.VeiledEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.Set;
import java.util.function.Function;

public final class ModRegistry {
    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(EnderSight.MOD_ID, Registries.DATA_COMPONENT_TYPE);
    private static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(EnderSight.MOD_ID, Registries.MOB_EFFECT);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(EnderSight.MOD_ID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(EnderSight.MOD_ID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(EnderSight.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<DataComponentType<Attunement>> ATTUNEMENT = DATA_COMPONENT_TYPES.register(
            "attunement",
            () -> DataComponentType.<Attunement>builder()
                    .persistent(Attunement.CODEC)
                    .networkSynchronized(Attunement.STREAM_CODEC)
                    .build()
    );

    public static final RegistrySupplier<MobEffect> VEILED = MOB_EFFECTS.register("veiled", VeiledEffect::new);

    public static final RegistrySupplier<Block> WATCHERS_EYE = BLOCKS.register(
            "watchers_eye",
            key -> new WatchersEyeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OBSERVER).setId(key))
    );

    public static final RegistrySupplier<Item> ENDER_SPYGLASS =
            item("ender_spyglass", properties -> new EnderSpyglassItem(properties.stacksTo(1)));
    public static final RegistrySupplier<Item> ATTUNED_EYE =
            item("attuned_eye", properties -> new SightEyeItem(properties, false));
    public static final RegistrySupplier<Item> EVERLASTING_EYE =
            item("everlasting_eye", properties -> new SightEyeItem(properties, true));
    public static final RegistrySupplier<Item> ENDER_MARK =
            item("ender_mark", properties -> new EnderMarkItem(properties.stacksTo(16)));
    public static final RegistrySupplier<Item> SEERS_PEARL =
            item("seers_pearl", properties -> new SeersPearlItem(properties.stacksTo(16)));
    public static final RegistrySupplier<Item> ENDER_VEIL = item(
            "ender_veil",
            properties -> new EnderVeilItem(properties.component(DataComponents.CONSUMABLE, Consumables.defaultFood().build()))
    );
    public static final RegistrySupplier<Item> WATCHERS_EYE_ITEM = item(
            "watchers_eye",
            properties -> new BlockItem(WATCHERS_EYE.get(), properties.useBlockDescriptionPrefix())
    );

    public static final RegistrySupplier<BlockEntityType<WatchersEyeBlockEntity>> WATCHERS_EYE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "watchers_eye",
                    () -> new BlockEntityType<>(WatchersEyeBlockEntity::new, Set.of(WATCHERS_EYE.get()))
            );

    private ModRegistry() {
    }

    public static void init() {
        DATA_COMPONENT_TYPES.register();
        MOB_EFFECTS.register();
        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITY_TYPES.register();

        ResourceKey<CreativeModeTab> tools = vanillaTab("tools_and_utilities");
        CreativeTabHelper.addItem(tools, () -> ENDER_SPYGLASS.get());
        CreativeTabHelper.addItem(tools, () -> ATTUNED_EYE.get());
        CreativeTabHelper.addItem(tools, () -> EVERLASTING_EYE.get());
        CreativeTabHelper.addItem(tools, () -> ENDER_MARK.get());
        CreativeTabHelper.addItem(tools, () -> SEERS_PEARL.get());
        CreativeTabHelper.addItem(vanillaTab("food_and_drinks"), () -> ENDER_VEIL.get());
        CreativeTabHelper.addItem(vanillaTab("redstone_blocks"), () -> WATCHERS_EYE_ITEM.get());
    }

    public static Holder<MobEffect> veiled() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(VEILED.get());
    }

    private static RegistrySupplier<Item> item(String id, Function<Item.Properties, Item> factory) {
        return ITEMS.register(id, key -> factory.apply(new Item.Properties().setId(key)));
    }

    private static ResourceKey<CreativeModeTab> vanillaTab(String path) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace(path));
    }
}
