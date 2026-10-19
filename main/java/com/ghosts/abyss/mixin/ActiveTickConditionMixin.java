package com.ghosts.abyss.mixin;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
        targets = {"net.mcreator.jujutsucraft.procedures.ActiveTickConditionProcedure"},
        remap = false
)
public class ActiveTickConditionMixin {
   private static final List<String> HWB_ALLOWED_EFFECTS = List.of(
           "jujutsucraft:reverse_cursed_technique",
           "jujutsucraft:infinity_effect"
   );

   @Inject(
           method = {"execute"},
           at = {@At("HEAD")},
           remap = false,
           cancellable = true
   )
   private static void onExecute(Entity entity, CallbackInfoReturnable<Boolean> cir) {
      if (entity == null) {
         return;
      }

      if (entity.getPersistentData().getBoolean("hwb_active")) {
         if (!entity.getPersistentData().getBoolean("perfect_body")) {
            boolean hasAllowedEffect = entity instanceof LivingEntity living && living.getActiveEffects().stream().anyMatch(e -> {
               ResourceLocation loc = ForgeRegistries.MOB_EFFECTS.getKey(e.getEffect());
               return loc != null && HWB_ALLOWED_EFFECTS.contains(loc.toString());
            });

            if (!hasAllowedEffect) {
               cir.setReturnValue(false);
            }
         }
      }
   }
}