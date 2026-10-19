package com.ghosts.abyss.util;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;

import java.lang.reflect.Field;

public class InnerselfHelper {

    public static boolean isInnerselfUser(Player player) {
        try {
            Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
            Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
            capField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Capability<Object> cap = (Capability<Object>) capField.get(null);
            Object vars = player.getCapability(cap, null).orElse(null);
            if (vars == null) return false;

            Field ct1 = vars.getClass().getDeclaredField("PlayerCurseTechnique");
            ct1.setAccessible(true);
            double t1 = (Double) ct1.get(vars);

            Field ct2 = vars.getClass().getDeclaredField("PlayerCurseTechnique2");
            ct2.setAccessible(true);
            double t2 = (Double) ct2.get(vars);

            return t1 == 15.0D || t2 == 15.0D;
        } catch (Exception e) {
            return false;
        }
    }

    public static int getSlotCount(ServerPlayer player) {
        return 9; // always fully unlocked
    }}