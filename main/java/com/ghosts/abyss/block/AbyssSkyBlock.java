
package com.ghosts.abyss.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

public class AbyssSkyBlock extends Block {

    private final ResourceLocation skyId;

    public AbyssSkyBlock(String skyName) {
        super(Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(-1.0F, 3600000.0F)
                .noLootTable()
                .noOcclusion()
                .isValidSpawn((state, level, pos, type) -> false));

        this.skyId = new ResourceLocation("abyss", skyName);
    }

    public ResourceLocation getSkyId() {
        return skyId;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // The custom sky renderer will draw the visible surface.
        return RenderShape.INVISIBLE;
    }

    @Override
    public int getLightBlock(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return 0;
    }
}
