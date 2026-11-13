
package com.ghosts.abyss.gojo;

import net.mcreator.jujutsucraft.entity.BlueEntity;
import net.mcreator.jujutsucraft.entity.PurpleEntity;
import net.mcreator.jujutsucraft.entity.RedEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.procedures.SetRangedAmmoProcedure;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class RedBluePurple {

    public static final String NUKE = "abyss_red_blue_nuke";
    public static final String CASTER = "abyss_red_blue_caster";

    private static final double CHARGE = 2.0D;
    private static final double SIZE = 5.0D;

    private RedBluePurple() {}

    public static boolean combine(
            RedEntity red,
            BlueEntity blue,
            Vec3 position
    ) {
        if (!(red.level() instanceof ServerLevel level)
                || !red.isAlive()
                || !blue.isAlive()) {
            return false;
        }

        CompoundTag redData = red.getPersistentData();

        if (!redData.hasUUID("abyss_red_caster")) {
            return false;
        }

        UUID casterId = redData.getUUID("abyss_red_caster");
        ServerPlayer caster = level.getServer()
                .getPlayerList().getPlayer(casterId);

        // Only combine Red and Blue belonging to the same player.
        if (caster == null
                || !caster.serverLevel().equals(level)
                || !casterId.toString().equals(
                blue.getPersistentData()
                        .getString("OWNER_UUID"))) {
            return false;
        }

        PurpleEntity purple = JujutsucraftModEntities.PURPLE.get()
                .create(level);

        if (purple == null) {
            return false;
        }

        purple.moveTo(
                position.x,
                position.y,
                position.z,
                caster.getYRot(),
                caster.getXRot()
        );

        // Initialize Jujutsucraft's original projectile ownership.
        SetRangedAmmoProcedure.execute(caster, purple);

        CompoundTag purpleData = purple.getPersistentData();

        // Activate the original Hollow Nuke behavior.
        purpleData.putBoolean("explode", true);
        purpleData.putBoolean("betrayal", true);

        // Abyss-specific identification.
        purpleData.putBoolean(NUKE, true);
        purpleData.putUUID(CASTER, casterId);

        // Equivalent to the original charged sneaking version.
        purpleData.putDouble("cnt6", CHARGE);
        purpleData.putDouble("size", SIZE);

        // Start the original nuke's buildup.
        purpleData.putDouble("cnt1", 0.0D);
        purpleData.putDouble("cnt3", 1.0D);
        purpleData.putDouble("cnt_life", 0.0D);

        var health = purple.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(400.0D);
            purple.setHealth(400.0F);
        }

        var sizeAttribute = purple.getAttribute(
                JujutsucraftModAttributes.SIZE.get()
        );

        if (sizeAttribute != null) {
            sizeAttribute.setBaseValue(SIZE);
        }

        if (!level.addFreshEntity(purple)) {
            return false;
        }

        red.discard();
        blue.discard();
        return true;
    }
}
