
package com.ghosts.abyss.block;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.extensions.common.IClientBlockExtensions;

import java.util.function.Consumer;

public class DomainGrassBlock extends DomainBlock {

    public DomainGrassBlock() {
        super(MapColor.GRASS, SoundType.GRASS, true);
    }

    @Override
    public void initializeClient(
            Consumer<IClientBlockExtensions> consumer) {
        consumer.accept(new IClientBlockExtensions() {

            @Override
            public boolean addDestroyEffects(
                    BlockState state,
                    Level level,
                    BlockPos pos,
                    ParticleEngine manager
            ) {
                // No grass-block breaking particles.
                return true;
            }

            @Override
            public boolean addHitEffects(
                    BlockState state,
                    Level level,
                    HitResult target,
                    ParticleEngine manager) {
                // No digging/hitting particles.
                return true;
            }
        });
    }

    @Override
    public boolean addRunningEffects(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity) {
        return true;
    }

    @Override
    public boolean addLandingEffects(
            BlockState state1,
            ServerLevel level,
            BlockPos pos,
            BlockState state2,
            LivingEntity entity,
            int numberOfParticles) {
        return true;
    }
}
