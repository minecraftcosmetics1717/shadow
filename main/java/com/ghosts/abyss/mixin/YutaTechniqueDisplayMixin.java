package com.ghosts.abyss.mixin;

import com.ghosts.abyss.Abyss;

import java.lang.reflect.Field;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
        targets = {"net.mcreator.jujutsucraft.procedures.KeyChangeTechniqueOnKeyPressedProcedure"},
        remap = false
)
public class YutaTechniqueDisplayMixin {

   @Inject(
           method = {"execute"},
           at = {@At("HEAD")},
           cancellable = true,
           remap = false
   )
   private static void jslop$yutaRegisteredDisplay(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
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

         // Technique ID 5.0 = Yuta Okkotsu / Rika
         if ((Double) ctField.get(vars) != 5.0D) {
            return;
         }

         ListTag registered = entity.getPersistentData().getList("jslop_yuta_registered", 10);
         int count = registered.size();
         if (count == 0) {
            return;
         }

         if (!entity.level().isClientSide()) {
            boolean rikaPresent = false;

            try {
               rikaPresent = (Boolean) Class.forName("net.mcreator.jujutsucraft.procedures.LocateRikaProcedure")
                       .getMethod("execute", LevelAccessor.class, Entity.class)
                       .invoke(null, world, entity);
            } catch (Exception ignored) {
            }

            MobEffect domainFx = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse("jujutsucraft:domain_expansion"));
            boolean domainActive = entity instanceof LivingEntity living && domainFx != null && living.hasEffect(domainFx);

            Abyss.LOGGER.debug("[JSlop] DisplayMixin gate: rika={} domainFxFound={} domainActive={}", rikaPresent, domainFx != null, domainActive);

            // Yuta can only cycle through copied techniques if Rika is present or Domain Expansion is active
            if (!rikaPresent && !domainActive) {
               return;
            }
         }

         Field selectField = vars.getClass().getDeclaredField("PlayerSelectCurseTechnique");
         selectField.setAccessible(true);
         double select = (Double) selectField.get(vars);

         boolean isShiftKeyDown = entity.isShiftKeyDown();
         boolean noChange = false;

         try {
            Field ncField = vars.getClass().getDeclaredField("noChangeTechnique");
            ncField.setAccessible(true);
            noChange = (Boolean) ncField.get(vars);
         } catch (NoSuchFieldException ignored) {
         }

         int commitIdx = -1;

         // Technique index offsets 100+ represent Yuta's copied techniques list
         if (select >= 100.0D) {
            int idx = (int) select - 100;
            if (idx < 0 || idx >= count) {
               selectField.set(vars, isShiftKeyDown ? 3.0D : 2.0D);
               return;
            }

            if (noChange) {
               commitIdx = idx;
            } else if (!isShiftKeyDown) {
               if (idx + 1 >= count) {
                  selectField.set(vars, 2.0D);
                  return;
               }
               commitIdx = idx + 1;
            } else {
               if (idx <= 0) {
                  selectField.set(vars, 3.0D);
                  return;
               }
               commitIdx = idx - 1;
            }
         } else if (select == 2.0D && !isShiftKeyDown && !noChange) {
            commitIdx = 0;
         } else if (select == 3.0D && isShiftKeyDown && !noChange) {
            commitIdx = count - 1;
         }

         if (commitIdx < 0) {
            return;
         }

         CompoundTag tech = registered.getCompound(commitIdx);
         String name = tech.getString("name");
         double ceCost = tech.getDouble("ce_cost");

         // Register current technique selection display
         Class<?> decideClass = Class.forName("net.mcreator.jujutsucraft.procedures.TechniqueDecideProcedure");
         decideClass.getMethod("execute", Entity.class, Boolean.TYPE, Boolean.TYPE, Double.TYPE, Double.TYPE, Double.TYPE, String.class)
                 .invoke(null, entity, false, false, ceCost, 5.0D, (double) (100 + commitIdx), name);

         ci.cancel();
      } catch (Exception e) {
         Abyss.LOGGER.warn("[JSlop] YutaTechniqueDisplayMixin failed: {}", e.getMessage());
      }
   }
}