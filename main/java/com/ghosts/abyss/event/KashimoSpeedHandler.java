package com.ghosts.abyss.event;

import com.ghosts.abyss.Abyss;
import java.lang.reflect.Field;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "abyss")
public class KashimoSpeedHandler {

    private static final UUID KASHIMO_SPEED_UUID = UUID.fromString("6a1b2c3d-4e5f-6a7b-8c9d-0e1f2a3b4c5d");
    private static final double KASHIMO_CT_ID = 7.0;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isClient() || event.phase != TickEvent.Phase.END) {
            return;
        }

        if (event.player instanceof ServerPlayer player) {
            AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttribute == null) return;

            boolean hasKashimo = false;

            try {
                Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
                Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
                capField.setAccessible(true);
                Capability<Object> cap = (Capability<Object>) capField.get(null);

                Object vars = player.getCapability(cap, (Direction) null).orElse(null);
                if (vars != null) {
                    Field secondField = vars.getClass().getDeclaredField("SecondTechnique");
                    secondField.setAccessible(true);
                    boolean secondActive = (Boolean) secondField.get(vars);

                    Field ctField = vars.getClass().getDeclaredField(secondActive ? "PlayerCurseTechnique2" : "PlayerCurseTechnique");
                    ctField.setAccessible(true);
                    double currentTechnique = (Double) ctField.get(vars);

                    if (currentTechnique == KASHIMO_CT_ID) {
                        hasKashimo = true;
                    }
                }
            } catch (Exception ignored) {}

            AttributeModifier existingModifier = speedAttribute.getModifier(KASHIMO_SPEED_UUID);

            if (hasKashimo) {
                if (existingModifier == null) {
                    AttributeModifier kashimoSpeed = new AttributeModifier(
                        KASHIMO_SPEED_UUID, 
                        "Kashimo Technique Speed Buff", 
                        1.4,
                        AttributeModifier.Operation.MULTIPLY_BASE
                    );
                    speedAttribute.addTransientModifier(kashimoSpeed);
                }
            } else {
                if (existingModifier != null) {
                    speedAttribute.removeModifier(KASHIMO_SPEED_UUID);
                }
            }
        }
    }
}