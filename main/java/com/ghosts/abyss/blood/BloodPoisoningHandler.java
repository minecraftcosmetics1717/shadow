
package com.ghosts.abyss.blood;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.effect.ModEffects;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Abyss.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class BloodPoisoningHandler {

    private static final ResourceKey<DamageType> CURSE_DAMAGE =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    new ResourceLocation(
                            "jujutsucraft",
                            "damage_combat"
                    )
            );

    private BloodPoisoningHandler() {}

    private static boolean isBloodAttack(int skill) {
        return switch (skill) {
            case 1005, // Slicing Exorcism
                 1007, // Piercing Blood
                 1008, // Supernova
                 1018, // Wing King
                 1019  // Kechizu Poison
                    -> true;

            default -> false;
        };
    }

    @SubscribeEvent
    public static void onLivingDamage(
            LivingDamageEvent event
    ) {

        LivingEntity victim = event.getEntity();

        if (victim.level().isClientSide) {
            return;
        }

        if (event.getAmount() <= 0.0f) {
            return;
        }
        Entity debugSource = event.getSource().getEntity();

        if (debugSource instanceof ServerPlayer debugPlayer
                && DeathWombPainting.is(debugPlayer)) {

            debugPlayer.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "[Abyss Blood Debug] "
                                    + "Damage Type: "
                                    + event.getSource().type().msgId()
                                    + " | Skill: "
                                    + debugPlayer.getPersistentData().getDouble("skill")
                                    + " | Damage: "
                                    + event.getAmount()
                    )
            );
        }
        // Only handle Jujutsucraft technique damage.
        if (!event.getSource().is(CURSE_DAMAGE)) {
            return;
        }

        Entity source = event.getSource().getEntity();

        if (!(source instanceof ServerPlayer attacker)) {
            return;
        }

        if (attacker == victim) {
            return;
        }

        // Only Death Womb Paintings can poison.
        if (!DeathWombPainting.is(attacker)) {
            return;
        }

        int skill = (int) attacker
                .getPersistentData()
                .getDouble("skill");

        if (!isBloodAttack(skill)) {
            return;
        }

        // Refresh Blood Poisoning for 5 seconds.
        victim.addEffect(
                new MobEffectInstance(
                        ModEffects.BLOOD_POISONING.get(),
                        100,
                        0,
                        false,
                        true,
                        true
                ),
                attacker
        );
    }
}
