
package com.ghosts.abyss.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

public class DomainSkyBlock extends Block {

    public enum SkyType {
        DAY,
        MAHITO,
        BLACK,
        WHITE,
        INFINITE_VOID
    }

    private final SkyType skyType;

    public DomainSkyBlock(SkyType skyType) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .strength(-1.0F, 3600000.0F)
                .noLootTable()
                .noOcclusion()
                .sound(SoundType.GLASS)
                .isValidSpawn((state, level, pos, type) -> false)
                .isRedstoneConductor((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false)
                .lightLevel(state -> 15));

        this.skyType = skyType;
    }

    public SkyType getSkyType() {
        return skyType;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public boolean skipRendering(
            BlockState state,
            BlockState adjacentState,
            net.minecraft.core.Direction direction
    ) {
        return adjacentState.getBlock() instanceof DomainSkyBlock;
    }
}
