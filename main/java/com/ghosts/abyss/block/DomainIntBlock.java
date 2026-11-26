
package com.ghosts.abyss.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class DomainIntBlock extends Block {

    public DomainIntBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(-1.0F, 3600000.0F)
                .noCollission()
                .noOcclusion()
                .noLootTable()
                .lightLevel(state -> 15)
                .sound(SoundType.GLASS)
                .isValidSpawn((state, level, pos, type) -> false)
                .isRedstoneConductor((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false));
    }
}
