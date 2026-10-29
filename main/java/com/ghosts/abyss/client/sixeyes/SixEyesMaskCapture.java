
package com.ghosts.abyss.client.sixeyes;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SixEyesMaskCapture {

    public record CapturedEntity(
            LivingEntity entity,
            SixEyesEntityColors.ThermalType type
    ) {}

    private static final List<CapturedEntity> ENTITIES =
            new ArrayList<>();

    private SixEyesMaskCapture() {}

    public static void beginFrame() {
        ENTITIES.clear();
    }

    public static void capture(Entity entity) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null
                || !SixEyesVisionHandler.shouldRender(mc.player)) {
            return;
        }

        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        if (entity == mc.player) {
            return;
        }

        ResourceLocation id =
                ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());

        if (id == null) {
            return;
        }

        if ("jujutsucraft".equals(id.getNamespace())) {
            String path = id.getPath();

            if (path.equals("entity_attack_slash")
                    || path.equals("entity_attack_strike")
                    || path.equals("entity_attack_sweep")) {
                return;
            }
        }

        for (CapturedEntity captured : ENTITIES) {
            if (captured.entity() == living) {
                return;
            }
        }

        ENTITIES.add(new CapturedEntity(
                living,
                SixEyesEntityColors.getType(living)
        ));
    }

    public static List<CapturedEntity> getEntities() {
        return Collections.unmodifiableList(ENTITIES);
    }
}
