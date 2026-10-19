package com.ghosts.abyss.util;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;

import java.lang.reflect.Field;

public class TenShadowsHelper {

    public static boolean isTenShadows(Player player) {
        try {
            Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
            Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
            capField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Capability<Object> cap = (Capability<Object>) capField.get(null);
            Object vars = player.getCapability(cap, null).orElse(null);
            if (vars == null) return false;

            // Check primary technique
            Field ct1Field = vars.getClass().getDeclaredField("PlayerCurseTechnique");
            ct1Field.setAccessible(true);
            double technique1 = (Double) ct1Field.get(vars);

            // Check secondary technique
            Field ct2Field = vars.getClass().getDeclaredField("PlayerCurseTechnique2");
            ct2Field.setAccessible(true);
            double technique2 = (Double) ct2Field.get(vars);

            // Allow if either is Ten Shadows (6.0)
            return technique1 == 6.0D || technique2 == 6.0D;

        } catch (Exception e) {
            return false;
        }
    }

    public static int getShadowSlotCount(ServerPlayer player) {
        if (hasAdvancement(player, "jujutsucraft:sorcerer_grade_special")) return 9;
        if (hasAdvancement(player, "jujutsucraft:sorcerer_grade_1"))      return 6;
        if (hasAdvancement(player, "jujutsucraft:sorcerer_grade_1_semi")) return 5;
        if (hasAdvancement(player, "jujutsucraft:sorcerer_grade_2"))      return 4;
        if (hasAdvancement(player, "jujutsucraft:sorcerer_grade_2_semi")) return 3;
        if (hasAdvancement(player, "jujutsucraft:sorcerer_grade_3"))      return 2;
        if (hasAdvancement(player, "jujutsucraft:sorcerer_grade_4"))      return 1;
        return 1;
    }

    private static boolean hasAdvancement(ServerPlayer player, String id) {
        Advancement adv = player.server.getAdvancements().getAdvancement(new ResourceLocation(id));
        if (adv == null) return false;
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(adv);
        return progress.isDone();
    }
}