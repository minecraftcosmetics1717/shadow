
package com.ghosts.abyss.mahito;

import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;

import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "abyss")
public final class MahitoSoulDefense {

    // =========================================
    // CONFIGURATION
    // =========================================

    // Mahito's Jujutsucraft technique ID.
    private static final double MAHITO_TECHNIQUE = 15.0D;

    // 4 cursed energy per heart (2 HP).
    private static final double CE_PER_HEART = 4.0D;

    // Therefore, 2 cursed energy per HP.
    private static final double CE_PER_HP =
            CE_PER_HEART / 2.0D;

    private MahitoSoulDefense() {}

    // =========================================
    // PASSIVE SOUL DEFENSE
    // =========================================

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {

        // For now, only players receive this defense.
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Preserve damage that explicitly bypasses
        // Minecraft's invulnerability protections.
        if (event.getSource().is(
                DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }

        float incomingDamage = event.getAmount();

        if (incomingDamage <= 0.0F) {
            return;
        }

        // =====================================
        // GET JUJUTSUCRAFT PLAYER VARIABLES
        // =====================================

        JujutsucraftModVariables.PlayerVariables vars =
                player.getCapability(
                        JujutsucraftModVariables
                                .PLAYER_VARIABLES_CAPABILITY
                ).orElse(null);

        if (vars == null) {
            return;
        }

        // =====================================
        // CHECK FOR MAHITO'S TECHNIQUE
        // =====================================

        boolean hasMahitoTechnique =
                vars.PlayerCurseTechnique == MAHITO_TECHNIQUE
                        || vars.PlayerCurseTechnique2 == MAHITO_TECHNIQUE;

        if (!hasMahitoTechnique) {
            return;
        }

        // =====================================
        // SOUL DAMAGE BYPASS
        // =====================================

        // Future Yuji / soul-aware attack checks
        // can be added inside this method.
        if (bypassesSoulDefense(event)) {
            return;
        }

        // =====================================
        // CALCULATE CE ABSORPTION
        // =====================================

        double availableCE = Math.max(
                0.0D,
                vars.PlayerCursePower
        );

        if (availableCE <= 0.0D) {
            // No CE available, so normal damage applies.
            return;
        }

        // How much HP damage can current CE absorb?
        double maximumAbsorption =
                availableCE / CE_PER_HP;

        // Cannot absorb more than the incoming hit.
        double absorbedDamage = Math.min(
                incomingDamage,
                maximumAbsorption
        );

        double ceConsumed =
                absorbedDamage * CE_PER_HP;

        // =====================================
        // CONSUME CURSED ENERGY
        // =====================================

        vars.PlayerCursePower = Math.max(
                0.0D,
                availableCE - ceConsumed
        );

        vars.syncPlayerVariables(player);

        // =====================================
        // APPLY REMAINING HEALTH DAMAGE
        // =====================================

        float remainingDamage = (float) Math.max(
                0.0D,
                incomingDamage - absorbedDamage
        );

        if (remainingDamage <= 0.0001F) {

            // Fully absorbed.
            event.setCanceled(true);

        } else {

            // Partially absorbed.
            // Only the uncovered portion hurts Mahito.
            event.setAmount(remainingDamage);
        }
    }

    // =========================================
    // FUTURE SOUL DAMAGE SUPPORT
    // =========================================

    private static boolean bypassesSoulDefense(
            LivingHurtEvent event
    ) {

        // No custom soul attacks bypass the defense yet.
        //
        // Later:
        // - Yuji's soul punches
        // - Soul-aware attackers
        // - Soul-damaging techniques
        // - Special soul-related weapons

        return false;
    }
}
