package com.ghosts.abyss;

import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketSyncAbilities;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.network.PacketDistributor;

public class AbilityScanHandler {
   private static final ThreadLocal<Boolean> PERMISSIVE = ThreadLocal.withInitial(() -> false);

   public static boolean isPermissive() {
      return Boolean.TRUE.equals(PERMISSIVE.get());
   }

   public static void scanAndSync(ServerPlayer player) {
      try {
         double ct = getPlayerCT(player);
         if (ct < -1.0) {
            return;
         }

         List<ScannedAbility> withAvailability = new ArrayList<>();
         List<ScannedAbility> primary = permissiveScan(player, ct);
         withAvailability.addAll(checkAvailability(player, ct, primary));

         double secondaryCT = getSecondaryCT(player);
         double originalCT = getOriginalCT(player);
         double otherCT = -999.0;

         if (secondaryCT >= -1.0 && secondaryCT != ct) {
            otherCT = secondaryCT;
         } else if (originalCT >= -1.0 && originalCT != ct) {
            otherCT = originalCT;
         }

         if (otherCT >= -1.0) {
            List<ScannedAbility> other = permissiveScan(player, otherCT);
            withAvailability.addAll(checkAvailability(player, otherCT, other));
         }

         // Special handling for Yuta (CT 5)
         if ((int) ct == 5 || (int) otherCT == 5) {
            ListTag registered = player.getPersistentData().getList("jslop_yuta_registered", 10);

            for (int i = 0; i < registered.size(); ++i) {
               CompoundTag tech = registered.getCompound(i);
               withAvailability.add(new ScannedAbility(
                       5, 100 + i,
                       tech.getString("name"),
                       tech.getDouble("ce_cost"),
                       false, false, true
               ));
            }
         }

         ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PacketSyncAbilities(withAvailability));
      } catch (Exception e) {
         Abyss.LOGGER.warn("[JSlop] AbilityScan failed: {}", e.getMessage());
      }
   }

   private static List<ScannedAbility> permissiveScan(ServerPlayer player, double ct) throws Exception {
      List<ScannedAbility> result = new ArrayList<>();
      LevelAccessor world = player.level();
      double x = player.getX();
      double y = player.getY();
      double z = player.getZ();
      Object vars = getVars(player);
      if (vars == null) {
         return result;
      }

      Field ctField = vars.getClass().getDeclaredField("PlayerCurseTechnique");
      Field ct2Field = vars.getClass().getDeclaredField("PlayerCurseTechnique2");
      Field secondField = vars.getClass().getDeclaredField("SecondTechnique");
      Field selectField = vars.getClass().getDeclaredField("PlayerSelectCurseTechnique");
      Field noChangeField = vars.getClass().getDeclaredField("noChangeTechnique");
      Field nameField = vars.getClass().getDeclaredField("PlayerSelectCurseTechniqueName");
      Field costField = vars.getClass().getDeclaredField("PlayerSelectCurseTechniqueCost");
      Field physField = vars.getClass().getDeclaredField("PhysicalAttack");
      Field passField = vars.getClass().getDeclaredField("PassiveTechnique");

      for (Field f : new Field[]{ctField, ct2Field, secondField, selectField, noChangeField, nameField, costField, physField, passField}) {
         f.setAccessible(true);
      }

      Method syncMethod = vars.getClass().getDeclaredMethod("syncPlayerVariables", Entity.class);
      Method mainProc = null;
      List<Method> subProcs = new ArrayList<>();

      try {
         mainProc = Class.forName("net.mcreator.jujutsucraft.procedures.KeyChangeTechniqueOnKeyPressedProcedure")
                 .getMethod("execute", LevelAccessor.class, double.class, double.class, double.class, Entity.class);
      } catch (Exception ignored) {}

      for (int p = 2; p <= 6; ++p) {
         try {
            subProcs.add(Class.forName("net.mcreator.jujutsucraft.procedures.KeyChangeTechniqueOnKeyPressed" + p + "Procedure")
                    .getMethod("execute", LevelAccessor.class, double.class, double.class, double.class, Entity.class, double.class, double.class));
         } catch (Exception ignored) {}
      }

      double savedCT = (Double) ctField.get(vars);
      double savedCT2 = (Double) ct2Field.get(vars);
      boolean savedSecond = (Boolean) secondField.get(vars);
      double savedSelect = (Double) selectField.get(vars);
      boolean savedNoChange = (Boolean) noChangeField.get(vars);
      String savedName = (String) nameField.get(vars);
      double savedDomainRadius = 0.0;
      Field domainRadiusField = null;
      Object mapVars = null;

      try {
         Class<?> mapVarsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables$MapVariables");
         Method getMethod = mapVarsClass.getMethod("get", LevelAccessor.class);
         mapVars = getMethod.invoke(null, world);
         domainRadiusField = mapVarsClass.getDeclaredField("DomainExpansionRadius");
         domainRadiusField.setAccessible(true);
         savedDomainRadius = (Double) domainRadiusField.get(mapVars);
      } catch (Exception e) {
         Abyss.LOGGER.warn("[JSlop] Could not snapshot DomainExpansionRadius: {}", e.getMessage());
      }

      try {
         PERMISSIVE.set(true);
         ctField.set(vars, ct);
         noChangeField.set(vars, true);
         syncMethod.invoke(vars, player);

         for (int sel = 0; sel <= 21; ++sel) {
            int finalSel = sel;

            for (Method sub : subProcs) {
               nameField.set(vars, "-----");
               selectField.set(vars, (double) sel);
               syncMethod.invoke(vars, player);

               try {
                  sub.invoke(null, world, x, y, z, player, ct, (double) sel);
               } catch (Exception ignored) {}

               String name = (String) nameField.get(vars);
               double resultSel = (Double) selectField.get(vars);
               if (name != null && !name.equals("-----") && (int) resultSel == finalSel
                       && result.stream().noneMatch(a -> a.ct() == (int) ct && a.select() == finalSel)) {
                  double cost = (Double) costField.get(vars);
                  boolean phys = (Boolean) physField.get(vars);
                  boolean pass = (Boolean) passField.get(vars);
                  result.add(new ScannedAbility((int) ct, sel, name, cost, phys, pass, false));
                  break;
               }
            }

            if (result.stream().noneMatch(a -> a.ct() == (int) ct && a.select() == finalSel) && mainProc != null) {
               nameField.set(vars, "-----");
               selectField.set(vars, (double) sel);
               noChangeField.set(vars, true);
               syncMethod.invoke(vars, player);

               try {
                  mainProc.invoke(null, world, x, y, z, player);
               } catch (Exception ignored) {}

               String name = (String) nameField.get(vars);
               double resultSel = (Double) selectField.get(vars);
               if (name != null && !name.equals("-----") && (int) resultSel == sel) {
                  double cost = (Double) costField.get(vars);
                  boolean phys = (Boolean) physField.get(vars);
                  boolean pass = (Boolean) passField.get(vars);
                  result.add(new ScannedAbility((int) ct, sel, name, cost, phys, pass, false));
               }
            }
         }
      } finally {
         PERMISSIVE.set(false);
         ctField.set(vars, savedCT);
         ct2Field.set(vars, savedCT2);
         secondField.set(vars, savedSecond);
         selectField.set(vars, savedSelect);
         noChangeField.set(vars, savedNoChange);
         nameField.set(vars, savedName);
         syncMethod.invoke(vars, player);

         if (domainRadiusField != null && mapVars != null) {
            try {
               domainRadiusField.set(mapVars, savedDomainRadius);
               Method syncMap = mapVars.getClass().getDeclaredMethod("syncData", LevelAccessor.class);
               syncMap.setAccessible(true);
               syncMap.invoke(mapVars, world);
            } catch (Exception e) {
               Abyss.LOGGER.warn("[JSlop] Could not restore DomainExpansionRadius: {}", e.getMessage());
            }
         }
      }

      return result;
   }

   public static double getCostForSkill(ServerPlayer player, double skill) {
      try {
         int ct = (int) (skill / 100.0);
         int select = (int) (skill % 100.0);
         if (ct < 0 || select < 0 || select > 21) {
            return 0.0;
         }

         Object vars = getVars(player);
         if (vars == null) {
            return 0.0;
         }

         Field ctField = vars.getClass().getDeclaredField("PlayerCurseTechnique");
         Field selectField = vars.getClass().getDeclaredField("PlayerSelectCurseTechnique");
         Field nameField = vars.getClass().getDeclaredField("PlayerSelectCurseTechniqueName");
         Field costField = vars.getClass().getDeclaredField("PlayerSelectCurseTechniqueCost");
         Field noChangeField = vars.getClass().getDeclaredField("noChangeTechnique");

         for (Field f : new Field[]{ctField, selectField, nameField, costField, noChangeField}) {
            f.setAccessible(true);
         }

         Method syncMethod = vars.getClass().getDeclaredMethod("syncPlayerVariables", Entity.class);
         double savedCT = (Double) ctField.get(vars);
         double savedSelect = (Double) selectField.get(vars);
         boolean savedNoChange = (Boolean) noChangeField.get(vars);
         String savedName = (String) nameField.get(vars);
         double result = 0.0;

         try {
            PERMISSIVE.set(true);
            ctField.set(vars, (double) ct);
            noChangeField.set(vars, true);
            syncMethod.invoke(vars, player);

            for (int p = 2; p <= 6; ++p) {
               try {
                  nameField.set(vars, "-----");
                  selectField.set(vars, (double) select);
                  syncMethod.invoke(vars, player);

                  Class.forName("net.mcreator.jujutsucraft.procedures.KeyChangeTechniqueOnKeyPressed" + p + "Procedure")
                          .getMethod("execute", LevelAccessor.class, double.class, double.class, double.class, Entity.class, double.class, double.class)
                          .invoke(null, player.level(), player.getX(), player.getY(), player.getZ(), player, (double) ct, (double) select);

                  String name = (String) nameField.get(vars);
                  double resultSel = (Double) selectField.get(vars);
                  if (name != null && !name.equals("-----") && (int) resultSel == select) {
                     result = (Double) costField.get(vars);
                     break;
                  }
               } catch (Exception ignored) {}
            }
         } finally {
            PERMISSIVE.set(false);
            ctField.set(vars, savedCT);
            selectField.set(vars, savedSelect);
            noChangeField.set(vars, savedNoChange);
            nameField.set(vars, savedName);
            syncMethod.invoke(vars, player);
         }

         return result;
      } catch (Exception e) {
         Abyss.LOGGER.warn("[JSlop] getCostForSkill skill={} failed: {}", skill, e.getMessage());
         return 0.0;
      }
   }

   private static List<ScannedAbility> checkAvailability(ServerPlayer player, double ct, List<ScannedAbility> abilities) throws Exception {
      Class<?> testClass = Class.forName("net.mcreator.jujutsucraft.procedures.ChangeTechniqueTestProcedure");
      Method testExec = testClass.getMethod("execute", LevelAccessor.class, double.class, double.class, double.class, Entity.class, double.class, double.class);
      LevelAccessor world = player.level();
      double x = player.getX();
      double y = player.getY();
      double z = player.getZ();
      List<ScannedAbility> result = new ArrayList<>();

      for (ScannedAbility ab : abilities) {
         boolean blocked = false;
         try {
            blocked = (Boolean) testExec.invoke(null, world, x, y, z, player, (double) ab.ct(), (double) ab.select());
         } catch (Exception ignored) {}

         result.add(new ScannedAbility(ab.ct(), ab.select(), ab.name(), ab.cost(), ab.physical(), ab.passive(), !blocked));
      }

      return result;
   }

   private static Object getVars(ServerPlayer player) {
      try {
         Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
         Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
         capField.setAccessible(true);
         Capability<Object> cap = (Capability) capField.get(null);
         return player.getCapability(cap, null).orElse(null);
      } catch (Exception e) {
         return null;
      }
   }

   private static double getPlayerCT(ServerPlayer player) {
      return getCurrentCT(player);
   }

   public static double getCurrentCT(ServerPlayer player) {
      try {
         Object vars = getVars(player);
         if (vars == null) {
            return -999.0;
         }

         Field secondField = vars.getClass().getDeclaredField("SecondTechnique");
         Field ctField = vars.getClass().getDeclaredField("PlayerCurseTechnique");
         Field ct2Field = vars.getClass().getDeclaredField("PlayerCurseTechnique2");
         secondField.setAccessible(true);
         ctField.setAccessible(true);
         ct2Field.setAccessible(true);

         boolean second = (Boolean) secondField.get(vars);
         return second ? (Double) ct2Field.get(vars) : (Double) ctField.get(vars);
      } catch (Exception e) {
         return -999.0;
      }
   }

   private static double getSecondaryCT(ServerPlayer player) {
      String key = "jujutsuslop_secondary_ct";
      if (!player.getPersistentData().contains(key)) {
         return -999.0;
      }
      double val = player.getPersistentData().getDouble(key);
      return val == -1.0 ? -999.0 : val;
   }

   private static double getOriginalCT(ServerPlayer player) {
      String key = "jujutsuslop_original_ct";
      if (!player.getPersistentData().contains(key)) {
         return -999.0;
      }
      double val = player.getPersistentData().getDouble(key);
      return val == -1.0 ? -999.0 : val;
   }
}