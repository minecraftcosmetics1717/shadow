
package com.ghosts.abyss.domain;

import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.procedures.PlayAnimationEntity2Procedure;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "abyss")
public final class ChosoHeartHeartbeat {

    private ChosoHeartHeartbeat() {}

    @SubscribeEvent
    public static void onLivingTick(
            LivingEvent.LivingTickEvent event
    ) {
        LivingEntity entity = event.getEntity();

        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        // Only handle Choso's heart entity.
        if (entity.getType() !=
                JujutsucraftModEntities.ENTITY_HEART.get()) {
            return;
        }

        // Only affect hearts spawned as Abyss
        // domain decorations.
        if (!entity.getPersistentData()
                .getBoolean("abyss_domain_decoration")) {
            return;
        }

        // Match the original approximate
        // 23-tick heartbeat interval.
        if (entity.tickCount % 23 != 0) {
            return;
        }

        PlayAnimationEntity2Procedure.execute(
                entity,
                "heart_beat"
        );

        level.playSound(
                null,
                entity.blockPosition(),
                SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.NEUTRAL,
                2.0F,
                1.0F
        );
    }
}
