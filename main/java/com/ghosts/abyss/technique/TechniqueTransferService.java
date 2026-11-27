package com.ghosts.abyss.technique;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.ability.CursedTechniqueEvents;
import com.ghosts.abyss.domain.DomainDimension;
import com.ghosts.abyss.domain.DomainInstanceManager;
import com.ghosts.abyss.domain.OpenDomain;
import com.ghosts.abyss.domain.OpenDomainLifecycle;
import com.ghosts.abyss.effect.ModEffects;
import com.ghosts.abyss.gojo.ControlledBlueManager;
import com.ghosts.abyss.gojo.ControlledRedManager;
import com.ghosts.abyss.grab.GrabManager;
import com.ghosts.abyss.item.TechniqueImprintItem;
import com.ghosts.abyss.mahito.MahitoForms;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.AdvancementGojoProcedure;
import net.mcreator.jujutsucraft.procedures.AdvancementMahitoProcedure;
import net.mcreator.jujutsucraft.procedures.AdvancementMegumiProcedure;
import net.mcreator.jujutsucraft.procedures.AdvancementOkkotsuProcedure;
import net.mcreator.jujutsucraft.procedures.AdvancementOtherProcedure;
import net.mcreator.jujutsucraft.procedures.KeyResetTechniqueOnKeyPressedProcedure;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Server-side replacement for a player's single innate technique. */
public final class TechniqueTransferService {
    private TechniqueTransferService() {}

    public record Result(boolean success, Component message) {}

    /** Temporary operator-only test command. The future outfall can call
     * transferHeldImprint after validating the player's accepted vow. */
    public static int executeTest(CommandSourceStack source) throws CommandSyntaxException {
        if (!source.hasPermission(2)) return 0;
        Result result = transferHeldImprint(source.getPlayerOrException());
        if (result.success()) {
            source.sendSuccess(result::message, false);
            return 1;
        }
        source.sendFailure(result.message());
        return 0;
    }

    public static Result transferHeldImprint(ServerPlayer player) {
        if (!player.server.isSameThread()) return fail("Technique transfer must run on the server thread.");
        if (!player.isAlive() || player.isSpectator()) return fail("You cannot change your technique right now.");

        ItemStack offering = player.getMainHandItem();
        if (offering.isEmpty() || !(offering.getItem() instanceof TechniqueImprintItem imprint)) {
            return fail("Hold a technique imprint in your main hand.");
        }
        int next = imprint.getTechniqueId();
        String base = switch (next) {
            case 1 -> "skill_sukuna";
            case 2 -> "skill_gojo";
            case 5 -> "skill_copy";
            case 6 -> "skill_ten_shadows_technique";
            case 15 -> "skill_mahito";
            default -> null;
        };
        if (base == null) return fail("This imprint is not supported yet.");

        var vars = player.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY).orElse(null);
        if (vars == null) return fail("Your cursed technique data could not be loaded.");
        double previous = vars.PlayerCurseTechnique;
        double previousSecondary = vars.PlayerCurseTechnique2;
        double previousBaseEnergy = vars.PlayerCursePowerFormer;
        double previousMaxEnergy = vars.PlayerCursePowerMAX;
        double previousEnergy = vars.PlayerCursePower;
        CompoundTag data = player.getPersistentData();
        // JC mirrors the primary technique into the secondary field for single-technique players.
        if (vars.SecondTechnique || (previousSecondary != 0.0D && previousSecondary != previous)
                || data.getBoolean("jujutsuslop_absorbed_active")
                || hasStoredTechnique(data, "jujutsuslop_secondary_ct")
                || hasStoredTechnique(data, "jujutsuslop_original_ct")) {
            return fail("Technique transfer for players with a second or vessel technique is not supported yet.");
        }
        if (previous == next) return fail("You already have this cursed technique.");
        if (previous == -1.0D && hasAdvancement(player, "completely_physically_gifted")) {
            return fail("Full Heavenly Restriction is not supported by this test transfer.");
        }
        if (player.level().dimension().equals(DomainDimension.ID)
                || DomainInstanceManager.getByOwner(player.getUUID()) != null
                || OpenDomainLifecycle.isActive(player.getUUID())
                || data.getBoolean(OpenDomain.KEY_ACTIVE)
                || player.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
            return fail("You cannot change your technique during a domain expansion.");
        }
        if (data.getBoolean("PRESS_Z")
                || player.hasEffect(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get())
                || GrabManager.isGrabbing(player) || GrabManager.isGrabbed(player)
                || data.getBoolean("jreborn_shadow_diving")
                || data.getInt("abyss_gojo_flight_mode") != 0
                || player.isFallFlying()
                || MahitoForms.blades(player) || MahitoForms.diminution(player) || MahitoForms.trueForm(player)
                || player.hasEffect(JujutsucraftModMobEffects.JACKPOT.get())
                || player.hasEffect(JujutsucraftModMobEffects.MYTHICAL_BEAST_AMBER_EFFECT.get())
                || player.hasEffect(JujutsucraftModMobEffects.INSTANT_SPIRIT_BODYOF_DISTORTED_KILLING_EFFECT.get())) {
            return fail("Finish your active techniques and transformations before transferring.");
        }
        // Validate the known base advancements before changing the capability.
        if (findAdvancement(player, base) == null
                || (next == 15 && findAdvancement(player, "observation_of_the_soul") == null)) {
            return fail("A required Jujutsucraft advancement is missing.");
        }

        Component name = offering.getHoverName().copy();
        try {
            // Discard any lingering controlled Limitless projectiles using
            // their existing lifecycle helpers, rather than deleting entities by type.
            if (previous == 2.0D) {
                ControlledBlueManager.cleanup(player);
                ControlledRedManager.cleanup(player);
                player.removeEffect(JujutsucraftModMobEffects.INFINITY_EFFECT.get());
            }
            if (previous == 38.0D) player.removeEffect(ModEffects.FLIGHT.get());

            // Preserve other progression, including grade and Six Eyes. CE is refreshed below.
            vars.PlayerCurseTechnique = next;
            vars.PlayerCurseTechnique2 = next;
            vars.SecondTechnique = false;
            vars.PlayerCharge = 0.0D;
            data.putDouble("skill", 0.0D);
            data.putDouble("select", 0.0D);
            data.putDouble("skill_domain", 0.0D);
            vars.syncPlayerVariables(player);

            // Assign CT BEFORE granting advancements: JC's event listeners
            // inspect the current technique when an advancement is awarded.
            grant(player, base);
            if (next == 15) grant(player, "observation_of_the_soul");
            refreshAdvancements(player, next);
            CursedTechniqueEvents.initializePlayer(player);
            resetSelection(player, vars);
            CursedEnergyCapacity.refreshAfterTransfer(player);
        } catch (RuntimeException ex) {
            // Preserve the offering if a native procedure fails. Already awarded
            // advancements are intentionally not revoked; temporary effects may
            // already have ended. This is not a full transactional rollback.
            vars.PlayerCurseTechnique = previous;
            vars.PlayerCurseTechnique2 = previousSecondary;
            vars.SecondTechnique = false;
            vars.PlayerCursePowerFormer = previousBaseEnergy;
            vars.PlayerCursePowerMAX = previousMaxEnergy;
            vars.PlayerCursePower = previousEnergy;
            vars.syncPlayerVariables(player);
            Abyss.LOGGER.error("Technique transfer failed for {}", player.getGameProfile().getName(), ex);
            return fail("Technique transfer failed. Your imprint was not consumed. Check the server log.");
        }

        offering.shrink(1);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return new Result(true, Component.literal("Your cursed technique has been replaced using ").append(name).append("."));
    }

    private static boolean hasStoredTechnique(CompoundTag data, String key) {
        return data.contains(key) && data.getDouble(key) >= 0.0D;
    }

    private static void refreshAdvancements(ServerPlayer player, int technique) {
        switch (technique) {
            case 1 -> AdvancementOtherProcedure.execute(player);
            case 2 -> AdvancementGojoProcedure.execute(player);
            case 5 -> AdvancementOkkotsuProcedure.execute(player);
            case 6 -> AdvancementMegumiProcedure.execute(player);
            case 15 -> AdvancementMahitoProcedure.execute(player);
            default -> throw new IllegalArgumentException("Unsupported technique: " + technique);
        }
    }

    private static void resetSelection(ServerPlayer player, JujutsucraftModVariables.PlayerVariables vars) {
        vars.PlayerSelectCurseTechnique = 0.0D;
        vars.PlayerSelectCurseTechniqueCost = 0.0D;
        vars.PlayerSelectCurseTechniqueCostOrgin = 0.0D;
        vars.PlayerSelectCurseTechniqueName = "";
        vars.PassiveTechnique = false;
        vars.PhysicalAttack = false;
        vars.noChangeTechnique = true;
        vars.syncPlayerVariables(player);
        KeyResetTechniqueOnKeyPressedProcedure.execute(player.serverLevel(), player.getX(), player.getY(), player.getZ(), player);
    }

    private static Advancement findAdvancement(ServerPlayer player, String path) {
        return player.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraft", path));
    }

    private static boolean hasAdvancement(ServerPlayer player, String path) {
        Advancement adv = findAdvancement(player, path);
        return adv != null && player.getAdvancements().getOrStartProgress(adv).isDone();
    }

    private static void grant(ServerPlayer player, String path) {
        Advancement adv = findAdvancement(player, path);
        if (adv == null) throw new IllegalStateException("Missing advancement: " + path);
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(adv);
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(adv, criterion);
        }
    }

    private static Result fail(String message) {
        return new Result(false, Component.literal(message));
    }
}
