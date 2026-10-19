package com.ghosts.abyss.mixin;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketSyncShadowDive;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Pseudo
@Mixin(
        targets = {"net.mcreator.jujutsucraft.procedures.CursedTechniqueFushiguroProcedure"},
        remap = false
)
public class TenShadowsExecuteMixin {

   @Inject(
           method = {"execute"},
           at = {@At("HEAD")},
           cancellable = true,
           remap = false
   )
   private static void jreborn$shadowDiveExecute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
      if (!(entity instanceof ServerPlayer player)) {
         return;
      }

      boolean authorized = player.getPersistentData().getBoolean("jreborn_shadowdive_auth");
      player.getPersistentData().putBoolean("jreborn_shadowdive_auth", false);

      if (!authorized) {
         return;
      }

      try {
         Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
         Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
         capField.setAccessible(true);
         Capability<Object> cap = (Capability<Object>) capField.get(null);

         Object vars = player.getCapability(cap, (Direction) null).orElse(null);
         if (vars != null) {
            Field selectField = vars.getClass().getDeclaredField("PlayerSelectCurseTechnique");
            selectField.setAccessible(true);
            double selectedTechnique = (Double) selectField.get(vars);

            if (selectedTechnique != 19.0) {
               return;
            }
         }
      } catch (Exception ignored) {
         return;
      }

      player.getPersistentData().putDouble("skill", 0.0);
      MobEffect cursedTechEffect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "cursed_technique"));
      if (cursedTechEffect != null) {
         player.removeEffect(cursedTechEffect);
      }

      boolean isDiving = player.getPersistentData().getBoolean("jreborn_shadow_diving");
      boolean newState = !isDiving;
      player.getPersistentData().putBoolean("jreborn_shadow_diving", newState);

      // ===== Sync to clients =====
      if (player.level() instanceof ServerLevel) {
         PacketSyncShadowDive packet = new PacketSyncShadowDive(player.getUUID(), newState);
         ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);
      }

      if (newState) {
         Abyss.LOGGER.info("[JReborn] {} entered Shadow Dive form.", player.getScoreboardName());
      } else {
         player.setMaxUpStep(0.6F);

         if (player.hasEffect(MobEffects.INVISIBILITY)) {
            player.removeEffect(MobEffects.INVISIBILITY);
         }

         Abyss.LOGGER.info("[JReborn] {} exited Shadow Dive form.", player.getScoreboardName());
      }

      ci.cancel();
   }
}