
package com.ghosts.abyss.client.sixeyes;

import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

public final class SixEyesVisionHandler {

    // Only affects the local client's Six Eyes visuals.
    private static boolean wheelVisionDisabled = false;
    private static boolean wheelPreviouslyEquipped = false;
    private static UUID trackedPlayer = null;

    private SixEyesVisionHandler() {}

    private static boolean hasSixEyes(Player player) {
        return player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
        ).map(data -> data.FlagSixEyes).orElse(false);
    }

    private static String getHelmetId(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);

        if (helmet.isEmpty()) {
            return "";
        }

        ResourceLocation id =
                ForgeRegistries.ITEMS.getKey(helmet.getItem());

        return id == null ? "" : id.toString();
    }

    private static void updateWheelState(Player player) {
        if (player == null) {
            wheelVisionDisabled = false;
            wheelPreviouslyEquipped = false;
            trackedPlayer = null;
            return;
        }

        UUID playerId = player.getUUID();

        if (!playerId.equals(trackedPlayer)) {
            trackedPlayer = playerId;
            wheelVisionDisabled = false;
            wheelPreviouslyEquipped = false;
        }

        boolean wheelEquipped = getHelmetId(player).equals(
                "jujutsucraft:mahoraga_wheel_helmet"
        );

        // Taking off the wheel resets the toggle.
        if (!wheelEquipped && wheelPreviouslyEquipped) {
            wheelVisionDisabled = false;
        }

        wheelPreviouslyEquipped = wheelEquipped;
    }

    public static void toggleWheelVision(Player player) {
        updateWheelState(player);

        if (player == null || !hasSixEyes(player)) {
            return;
        }

        // Only the Mahoraga wheel allows manual toggling.
        if (!getHelmetId(player).equals(
                "jujutsucraft:mahoraga_wheel_helmet"
        )) {
            return;
        }

        wheelVisionDisabled = !wheelVisionDisabled;
    }

    public static boolean shouldRender(Player player) {
        updateWheelState(player);

        if (player == null || player.isSpectator()) {
            return false;
        }

        if (!hasSixEyes(player)) {
            return false;
        }

        String helmetId = getHelmetId(player);

        switch (helmetId) {
            case "jujutsucraft:sunglasses_helmet":
            case "jujutsucraft:blindfold_bandage_helmet":
            case "jujutsucraft:uniform_gojo_helmet":
                return false;

            case "jujutsucraft:mahoraga_wheel_helmet":
                return !wheelVisionDisabled;

            default:
                return true;
        }
    }
}
