
package com.ghosts.abyss.gojo;

import com.ghosts.abyss.Abyss;

import net.mcreator.jujutsucraft.entity.PurpleEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Abyss.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class RedBluePurpleDamage {

    private RedBluePurpleDamage() {}

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();

        if (victim.level().isClientSide) {
            return;
        }

        Entity source = event.getSource().getDirectEntity();

        if (!(source instanceof PurpleEntity purple)) {
            return;
        }

        var data = purple.getPersistentData();

        // Only modify Hollow Nukes created by Abyss.
        if (!data.getBoolean(RedBluePurple.NUKE)) {
            return;
        }

        if (!data.hasUUID(RedBluePurple.CASTER)) {
            return;
        }

        // Everyone except the caster receives normal damage.
        if (!victim.getUUID().equals(
                data.getUUID(RedBluePurple.CASTER))) {
            return;
        }

        // The original caster receives half damage.
        event.setAmount(event.getAmount() * 0.5F);
    }
}
