package com.ghosts.abyss.client.sixeyes;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

public final class SixEyesEntityColors {

    public enum ThermalType {
        NORMAL,
        NO_CURSED_ENERGY,
        REVERSE_CURSED_TECHNIQUE
    }

    private static final TagKey<net.minecraft.world.entity.EntityType<?>>
            NO_CURSE_POWER = TagKey.create(
            Registries.ENTITY_TYPE,
            new ResourceLocation("forge", "no_curse_power")
    );

    private static final ResourceLocation RCT_ID =
            new ResourceLocation(
                    "jujutsucraft",
                    "reverse_cursed_technique"
            );

    private SixEyesEntityColors() {}

    public static ThermalType getType(LivingEntity entity) {
        if (entity == null) {
            return ThermalType.NORMAL;
        }

        MobEffect rct = ForgeRegistries.MOB_EFFECTS.getValue(RCT_ID);

        // RCT takes priority.
        if (rct != null && entity.hasEffect(rct)) {
            return ThermalType.REVERSE_CURSED_TECHNIQUE;
        }

        // Uses the merged Forge entity-type tag.
        if (entity.getType().is(NO_CURSE_POWER)) {
            return ThermalType.NO_CURSED_ENERGY;
        }

        return ThermalType.NORMAL;
    }
}