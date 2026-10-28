package com.ghosts.abyss.block;

import com.ghosts.abyss.Abyss;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Abyss.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Abyss.MOD_ID);

    public static final RegistryObject<Block> DOMAIN_BARRIER =
            register("domain_barrier", () -> new DomainBlock(MapColor.COLOR_BLACK, SoundType.GLASS,false));

    public static final RegistryObject<Block> DOMAIN_SAND =
            register("domain_sand", () -> new DomainBlock(MapColor.SAND, SoundType.SAND,false));

    public static final RegistryObject<Block> DOMAIN_GRAVEL =
            register("domain_gravel", () -> new DomainBlock(MapColor.STONE, SoundType.GRAVEL,false));

    public static final RegistryObject<Block> DOMAIN_RED =
            register("domain_red", () -> new DomainBlock(MapColor.COLOR_RED, SoundType.STONE,false));

    public static final RegistryObject<Block> DOMAIN_FLOWER =
            register("domain_flower", () -> new DomainBlock(MapColor.COLOR_PINK, SoundType.GRASS,false));

    public static final RegistryObject<Block> DOMAIN_BONE =
            register("domain_bone", () -> new DomainBlock(MapColor.WOOL, SoundType.BONE_BLOCK,false));

    public static final RegistryObject<Block> DOMAIN_BIRCH =
            register("domain_birch", () -> new DomainBlock(MapColor.WOOD, SoundType.WOOD,false));

    public static final RegistryObject<Block> DOMAIN_WHITE =
            register("domain_white", () -> new DomainBlock(MapColor.SNOW, SoundType.STONE,true));

    public static final RegistryObject<Block> DOMAIN_BLACK =
            register("domain_black",
                    () -> new DomainBlock(MapColor.COLOR_BLACK, SoundType.STONE,true));

    public static final RegistryObject<Block> DOMAIN_ROCK =
            register("domain_rock", () -> new DomainBlock(MapColor.STONE, SoundType.STONE,false));
    public static final RegistryObject<Block> DOMAIN_BLUE_SKY =
            register("domain_blue_sky", () -> new DomainBlock(MapColor.COLOR_LIGHT_BLUE, SoundType.GLASS,true));


    private static RegistryObject<Block> register(String name, Supplier<Block> block) {
        RegistryObject<Block> reg = BLOCKS.register(name, block);
        ITEMS.register(name, () -> new BlockItem(reg.get(), new Item.Properties()));
        return reg;
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}