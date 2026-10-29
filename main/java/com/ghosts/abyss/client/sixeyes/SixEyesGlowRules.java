
package com.ghosts.abyss.client.sixeyes;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

public final class SixEyesGlowRules {

    private static final TagKey<EntityType<?>> NOT_LIVING =
            TagKey.create(
                    Registries.ENTITY_TYPE,
                    new ResourceLocation("forge", "not_living")
            );

    private SixEyesGlowRules() {}

    public static boolean shouldGlow(Entity entity) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null
                || !SixEyesVisionHandler.shouldRender(mc.player)) {
            return false;
        }

        if (!(entity instanceof LivingEntity living)) {
            return false;
        }

        if (entity == mc.player || !living.isAlive()) {
            return false;
        }

        // Some JujutsuCraft living characters appear in this tag.
        // Exclude only ordinary entries for now.
        if (entity.getType().is(NOT_LIVING)) {
            return false;
        }

        return true;
    }

    public static int getGlowColor(LivingEntity entity) {

        ResourceLocation id =
                ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());

        // RCT overrides every other classification.
        if (SixEyesEntityColors.getType(entity)
                == SixEyesEntityColors.ThermalType.REVERSE_CURSED_TECHNIQUE) {
            return 0xFFFFFF;
        }

        if (id != null && "jujutsucraft".equals(id.getNamespace())) {
            switch (id.getPath()) {

                // Sukuna: red
                case "sukuna":
                case "sukuna_fushiguro":
                case "sukuna_perfect":
                    return 0xFF253A;

                // Yuta and Rika: pink
                case "okkotsu_yuta":
                case "okkotsu_yuta_culling_game":
                case "rika":
                case "rika_2":
                    return 0xFF67BC;
            }
        }

        // Toji, awakened Maki, etc.: black
        if (SixEyesEntityColors.getType(entity)
                == SixEyesEntityColors.ThermalType.NO_CURSED_ENERGY) {
            return 0x08080C;
        }

        // Everyone else: orange
        return 0xFFB82E;
    }
}
