package com.ghosts.abyss.event;

import com.ghosts.abyss.Abyss; //
import net.minecraft.core.Direction; //
import net.minecraft.resources.ResourceLocation; //
import net.minecraft.server.level.ServerPlayer; //
import net.minecraft.world.effect.MobEffect; //
import net.minecraft.world.effect.MobEffectInstance; //
import net.minecraft.world.effect.MobEffects; //
import net.minecraft.world.entity.ai.attributes.AttributeInstance; //
import net.minecraft.world.entity.ai.attributes.AttributeModifier; //
import net.minecraft.world.entity.ai.attributes.Attributes; //
import net.minecraftforge.common.capabilities.Capability; //
import net.minecraftforge.event.TickEvent; //
import net.minecraftforge.eventbus.api.SubscribeEvent; //
import net.minecraftforge.fml.common.Mod; //
import net.minecraftforge.registries.ForgeRegistries; //

import java.lang.reflect.Field; //
import java.util.UUID; //

@Mod.EventBusSubscriber(modid = "abyss")
public class ShadowDiveTickHandler {

    private static final UUID SLOWDOWN_UUID = UUID.fromString("7f4e2c8a-1123-4b67-a890-bcdef1234567");
    private static final UUID STEP_HEIGHT_UUID = UUID.fromString("1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d");

    private static final double BASE_CE_DRAIN_PER_SECOND = 50.0;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.player.level().isClientSide() || !(event.player instanceof ServerPlayer player)) { //
            return;
        }

        boolean isDiving = player.getPersistentData().getBoolean("jreborn_shadow_diving");
        MobEffect unstableEffect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "unstable"));
        if (unstableEffect != null && player.hasEffect(unstableEffect)) {
            player.getPersistentData().putBoolean("jreborn_shadow_diving", false);
            isDiving = false;
        }
        AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        net.minecraft.world.entity.ai.attributes.Attribute stepHeightAttr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("forge", "step_height_addition")); //

        if (!isDiving) {
            if (speedAttribute != null && speedAttribute.getModifier(SLOWDOWN_UUID) != null) {
                speedAttribute.removeModifier(SLOWDOWN_UUID);
            }
            if (stepHeightAttr != null) {
                AttributeInstance stepAttributeInstance = player.getAttribute(stepHeightAttr);
                if (stepAttributeInstance != null && stepAttributeInstance.getModifier(STEP_HEIGHT_UUID) != null) {
                    stepAttributeInstance.removeModifier(STEP_HEIGHT_UUID);
                }
            }
            return;
        }

        try {
            Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
            Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
            capField.setAccessible(true);
            Capability<Object> cap = (Capability<Object>) capField.get(null);
            Object vars = player.getCapability(cap, (Direction) null).orElse(null);

            if (vars != null) {
                Field ceField = vars.getClass().getDeclaredField("PlayerCursePower");
                ceField.setAccessible(true); //
                double currentCE = (Double) ceField.get(vars);

                double[] finalCeDrain = { BASE_CE_DRAIN_PER_SECOND };

                try {
                    Field sixEyesField = vars.getClass().getDeclaredField("FlagSixEyes");
                    sixEyesField.setAccessible(true);
                    if ((Boolean) sixEyesField.get(vars)) {
                        finalCeDrain[0] *= 0.1;
                    }
                } catch (NoSuchFieldException ignored) {}

                try {
                    Field sukunaField = vars.getClass().getDeclaredField("flag_sukuna");
                    sukunaField.setAccessible(true);
                    if ((Boolean) sukunaField.get(vars)) {
                        finalCeDrain[0] *= 0.5;
                    }
                } catch (NoSuchFieldException ignored) {}

                if (player.tickCount % 20 == 0) {
                    if (currentCE < finalCeDrain[0]) {
                        player.getPersistentData().putBoolean("jreborn_shadow_diving", false);
                        if (speedAttribute != null) speedAttribute.removeModifier(SLOWDOWN_UUID);
                        if (stepHeightAttr != null) {
                            AttributeInstance stepInstance = player.getAttribute(stepHeightAttr);
                            if (stepInstance != null) stepInstance.removeModifier(STEP_HEIGHT_UUID);
                        }
                        return;
                    } else {
                        ceField.set(vars, currentCE - finalCeDrain[0]);
                    }
                }
            }
        } catch (Exception ignored) {}

        if (speedAttribute != null && speedAttribute.getModifier(SLOWDOWN_UUID) == null) {
            speedAttribute.addTransientModifier(new AttributeModifier(SLOWDOWN_UUID, "Shadow dive slowdown", 0.4D, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }

        if (stepHeightAttr != null) {
            AttributeInstance stepAttributeInstance = player.getAttribute(stepHeightAttr);
            if (stepAttributeInstance != null && stepAttributeInstance.getModifier(STEP_HEIGHT_UUID) == null) {
                stepAttributeInstance.addTransientModifier(new AttributeModifier(STEP_HEIGHT_UUID, "Shadow dive step height", 3.0D, AttributeModifier.Operation.ADDITION));
            }
        }

        if (player.getDeltaMovement().y > 0 && !player.onGround() && !player.isSwimming()) {
            player.setDeltaMovement(player.getDeltaMovement().x, 0, player.getDeltaMovement().z);
            player.hurtMarked = true;
        }
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK, player.getX(), player.getY() + 0.05, player.getZ(), 4, 0.3, 0.0, 0.3, 0.02);
        }

        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 2, 0, false, false, false));
        applyJJKEffect(player, "cooldown_time_combat", 2, 1);
        applyJJKEffect(player, "cooldown_time_simple_domain", 2, 0);
        applyJJKEffect(player, "brain_damage", 2, 0);
        applyJJKEffect(player, "infinity_effect", 2, 0);
    }

    private static void applyJJKEffect(ServerPlayer player, String effectId, int duration, int amplifier) {
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", effectId));
        if (effect != null) { //
            player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, false));
        }
    }
}