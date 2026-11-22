
package com.ghosts.abyss.block;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.extensions.common.IClientBlockExtensions;

import java.util.function.Consumer;

public class DomainShortGrassBlock extends BushBlock {

    public DomainShortGrassBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.GRASS)
                .noCollission()
                .instabreak()
                .noLootTable()
                .sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ)
        );
    }

    @Override
    protected boolean mayPlaceOn(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return state.is(ModBlocks.DOMAIN_GRASS.get());
    }

    @Override
    public void initializeClient(
            Consumer<IClientBlockExtensions> consumer
    ) {
        consumer.accept(new IClientBlockExtensions() {

            @Override
            public boolean addDestroyEffects(
                    BlockState state,
                    Level level,
                    BlockPos pos,
                    ParticleEngine manager
            ) {
                return true;
            }

            @Override
            public boolean addHitEffects(
                    BlockState state,
                    Level level,
                    HitResult target,
                    ParticleEngine manager
            ) {
                return true;
            }
        });
    }

    @Override
    public boolean addRunningEffects(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity
    ) {
        return true;
    }

    @Override
    public boolean addLandingEffects(
            BlockState state1,
            ServerLevel level,
            BlockPos pos,
            BlockState state2,
            LivingEntity entity,
            int numberOfParticles
    ) {
        return true;
    }
}
