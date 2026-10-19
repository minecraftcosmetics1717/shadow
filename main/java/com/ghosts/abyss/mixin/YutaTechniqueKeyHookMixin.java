package com.ghosts.abyss.mixin;

import com.ghosts.abyss.ClientYutaTechniqueCache;
import com.ghosts.abyss.Abyss;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.registries.ForgeRegistries;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = {"net.mcreator.jujutsucraft.procedures.KeyStartTechniqueOnKeyPressedProcedure"},
        remap = false
)
public class YutaTechniqueKeyHookMixin {

   @Inject(
           method = {"execute"},
           at = {@At("HEAD")},
           cancellable = true,
           remap = false
   )
   private static void jslop$yutaRegisteredActivate(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
      if (entity instanceof ServerPlayer player) {
         try {
            Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
            Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
            capField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Capability<Object> cap = (Capability<Object>) capField.get(null);
            Object vars = player.getCapability(cap, null).orElse(null);

            if (vars == null) {
               return;
            }

            Field secondField = vars.getClass().getDeclaredField("SecondTechnique");
            secondField.setAccessible(true);
            boolean secondActive = (Boolean) secondField.get(vars);

            Field ctField = vars.getClass().getDeclaredField(secondActive ? "PlayerCurseTechnique2" : "PlayerCurseTechnique");
            ctField.setAccessible(true);

            // Technique ID 5.0 = Yuta Okkotsu / Rika
            if ((Double) ctField.get(vars) != 5.0D) {
               return;
            }

            Field selectField = vars.getClass().getDeclaredField("PlayerSelectCurseTechnique");
            selectField.setAccessible(true);
            double select = (Double) selectField.get(vars);

            // Selection index below 100 indicates standard techniques, not Yuta's copied ones
            if (select < 100.0D) {
               return;
            }

            int regIdx = (int) select - 100;
            ListTag registered = player.getPersistentData().getList("jslop_yuta_registered", 10);

            if (regIdx >= registered.size()) {
               ci.cancel();
               return;
            }

            boolean rikaPresent = false;

            try {
               rikaPresent = (Boolean) Class.forName("net.mcreator.jujutsucraft.procedures.LocateRikaProcedure")
                     .getMethod("execute", LevelAccessor.class, Entity.class)
                     .invoke(null, world, entity);
            } catch (Exception ignored) {
            }

            MobEffect domainFx = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse("jujutsucraft:domain_expansion"));
            boolean domainActive = domainFx != null && player.hasEffect(domainFx);

            // Execution requires Rika present in-world or an active Domain Expansion
            if (!rikaPresent && !domainActive) {
               player.displayClientMessage(Component.translatable("message.abyss.yuta_rika_required"), true);
               ci.cancel();
               return;
            }

            MobEffect cooldownFx = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse("jujutsucraft:cooldown_time"));
            if (!player.isCreative() && cooldownFx != null && player.hasEffect(cooldownFx)) {
               ci.cancel();
               return;
            }

            CompoundTag tech = registered.getCompound(regIdx);
            double regSkill = tech.getDouble("skill");
            double regCooldown = tech.getDouble("cooldown_ticks");
            double baseCeCost = tech.getDouble("ce_cost");
            String techName = tech.getString("name");

            try {
               Class.forName("net.mcreator.jujutsucraft.procedures.TechniqueDecideProcedure")
                     .getMethod("execute", Entity.class, Boolean.TYPE, Boolean.TYPE, Double.TYPE, Double.TYPE, Double.TYPE, String.class)
                     .invoke(null, player, false, false, baseCeCost, 5.0D, (double) (100 + regIdx), techName);
            } catch (Exception ignored) {
            }

            try {
               Field ceCostField = vars.getClass().getDeclaredField("PlayerSelectCurseTechniqueCost");
               ceCostField.setAccessible(true);
               double effectiveCost = (Double) ceCostField.get(vars);

               // Consume Cursed Energy if player is not in Creative Mode
               if (!player.isCreative() && effectiveCost > 0.0D) {
                  Field ceField = vars.getClass().getDeclaredField("PlayerCursePower");
                  ceField.setAccessible(true);
                  double currentCE = (Double) ceField.get(vars);

                  if (currentCE < effectiveCost) {
                     player.displayClientMessage(Component.literal("Not enough cursed energy."), true);
                     ci.cancel();
                     return;
                  }

                  ceField.set(vars, currentCE - effectiveCost);
               }

               vars.getClass().getDeclaredMethod("syncPlayerVariables", Entity.class).invoke(vars, player);
            } catch (Exception ceEx) {
               Abyss.LOGGER.warn("[JSlop] YutaTechniqueKeyHookMixin CE cost setup failed: {}", ceEx.getMessage());
            }

            player.getPersistentData().putDouble("skill", regSkill);
            player.getPersistentData().putDouble("COOLDOWN_TICKS", regCooldown);
            player.getPersistentData().putBoolean("PRESS_Z", true);

            MobEffect cursedTechFx = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse("jujutsucraft:cursed_technique"));
            if (cursedTechFx != null) {
               player.removeEffect(cursedTechFx);
               player.addEffect(new MobEffectInstance(cursedTechFx, Integer.MAX_VALUE, 0, false, false));
            }

            if (!player.isCreative() && cooldownFx != null) {
               player.removeEffect(cooldownFx);
               player.addEffect(new MobEffectInstance(cooldownFx, (int) regCooldown, 0, false, false));
            }

            Abyss.LOGGER.info("[JSlop] Yuta native Z activate: player={} idx={} skill={}", player.getName().getString(), regIdx, regSkill);
            ci.cancel();
         } catch (Exception e) {
            Abyss.LOGGER.warn("[JSlop] YutaTechniqueKeyHookMixin failed: {}", e.getMessage());
         }

      } else if (world.isClientSide()) {
         try {
            Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
            Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
            capField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Capability<Object> cap = (Capability<Object>) capField.get(null);
            Object vars = entity.getCapability(cap, null).orElse(null);

            if (vars == null) {
               return;
            }

            Field secondField = vars.getClass().getDeclaredField("SecondTechnique");
            secondField.setAccessible(true);
            boolean secondActive = (Boolean) secondField.get(vars);

            Field ctField = vars.getClass().getDeclaredField(secondActive ? "PlayerCurseTechnique2" : "PlayerCurseTechnique");
            ctField.setAccessible(true);

            if ((Double) ctField.get(vars) != 5.0D) {
               return;
            }

            Field selectField = vars.getClass().getDeclaredField("PlayerSelectCurseTechnique");
            selectField.setAccessible(true);
            double select = (Double) selectField.get(vars);

            if (select < 100.0D) {
               return;
            }

            int regIdx = (int) select - 100;
            List<ClientYutaTechniqueCache.Entry> regEntries = ClientYutaTechniqueCache.getAll();

            if (regIdx >= regEntries.size()) {
               ci.cancel();
               return;
            }

            double regSkill = regEntries.get(regIdx).skill();

            try {
               Class.forName("net.mcreator.jujutsucraft.procedures.ResetCounterProcedure")
                     .getMethod("execute", Entity.class)
                     .invoke(null, entity);
            } catch (Exception ignored) {
            }

            entity.getPersistentData().putDouble("skill", regSkill);
            entity.getPersistentData().putBoolean("PRESS_Z", true);
            ci.cancel();
         } catch (Exception ignored) {
         }
      }
   }
}