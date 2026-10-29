
package com.ghosts.abyss.client.sixeyes;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

public final class SixEyesEntityColors {

    public enum ThermalType {
        NORMAL,
        NO_CURSED_ENERGY,
        REVERSE_CURSED_TECHNIQUE
    }

    private static final TagKey<EntityType<?>>
            NO_CURSE_POWER = tag("no_curse_power");

    private static final TagKey<EntityType<?>>
            NOT_LIVING = tag("not_living");

    private static final ResourceLocation RCT_ID =
            new ResourceLocation(
                    "jujutsucraft",
                    "reverse_cursed_technique"
            );

    private SixEyesEntityColors() {}

    private static TagKey<EntityType<?>> tag(String name) {
        return TagKey.create(
                Registries.ENTITY_TYPE,
                new ResourceLocation("forge", name)
        );
    }

    public static ThermalType getType(LivingEntity entity) {

        if (entity == null) {
            return ThermalType.NORMAL;
        }

        MobEffect rct =
                ForgeRegistries.MOB_EFFECTS.getValue(RCT_ID);

        if (entity.level().isClientSide
                && entity.tickCount % 100 == 0
                && ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()) != null
                && "jujutsucraft".equals(
                ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).getNamespace()
        )) {

            System.out.println(
                    "[SixEyes RCT] Entity: " + entity.getName().getString()
                            + " | Effect registered: " + (rct != null)
                            + " | Has RCT: " + (rct != null && entity.hasEffect(rct))
            );
        }

        // RCT overrides other energy classifications.
        if (rct != null && entity.hasEffect(rct)) {
            return ThermalType.REVERSE_CURSED_TECHNIQUE;
        }
        if (entity.level().isClientSide
                && entity.tickCount % 100 == 0
                && entity.getType().toString().contains("itadori")) {

            System.out.println(
                    "[SixEyes] RCT registered: " + (rct != null)
                            + " | RCT active: "
                            + (rct != null && entity.hasEffect(rct))
            );
        }
        // Toji, awakened Maki, and other no-CE entities.
        if (entity.getType().is(NO_CURSE_POWER)) {
            return ThermalType.NO_CURSED_ENERGY;
        }

        return ThermalType.NORMAL;
    }

    public static boolean shouldShowAura(LivingEntity entity) {

        if (entity == null || !entity.isAlive()) {
            return false;
        }

        // Some living entities are listed as not_living,
        // so exclude them unless they have an explicit
        // zero-cursed-energy classification.
        if (entity.getType().is(NOT_LIVING)
                && !entity.getType().is(NO_CURSE_POWER)) {
            return false;
        }

        return true;
    }
}
