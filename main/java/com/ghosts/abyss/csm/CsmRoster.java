package com.ghosts.abyss.csm;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;

public final class CsmRoster {
   private static final String ROOT_KEY = "csm_roster";
   private static final String SPIRITS_KEY = "spirits";
   private static final String SEL_UUID_KEY = "selected_uuid";
   private static final String SEL_ABILITY_KEY = "selected_ability";
   private static final String SEL_ROSTER_IDX_KEY = "selected_roster_idx";
   private static final String CD_UNTIL_KEY = "csm_cd_until";
   public static final long ABILITY_COOLDOWN_TICKS = 200L;

   private CsmRoster() {
      // Private constructor to prevent instantiation
   }

   private static CompoundTag getRoot(Entity player) {
      CompoundTag persistentData = player.getPersistentData();
      if (!persistentData.contains(ROOT_KEY, 10)) { // 10 = CompoundTag
         persistentData.put(ROOT_KEY, new CompoundTag());
      }
      return persistentData.getCompound(ROOT_KEY);
   }

   public static List<CsmSpiritData> getSpirits(Entity player) {
      CompoundTag root = getRoot(player);
      if (!root.contains(SPIRITS_KEY, 9)) { // 9 = ListTag
         return new ArrayList<>();
      } else {
         ListTag list = root.getList(SPIRITS_KEY, 10);
         List<CsmSpiritData> result = new ArrayList<>(list.size());

         for (int i = 0; i < list.size(); ++i) {
            result.add(CsmSpiritData.fromNbt(list.getCompound(i)));
         }

         return result;
      }
   }

   public static String getSelectedUuid(Entity player) {
      return getRoot(player).getString(SEL_UUID_KEY);
   }

   public static int getSelectedAbility(Entity player) {
      return getRoot(player).getInt(SEL_ABILITY_KEY);
   }

   public static int getSelectedRosterIdx(Entity player) {
      return getRoot(player).getInt(SEL_ROSTER_IDX_KEY);
   }

   public static boolean hasRoster(Entity player) {
      return player.getPersistentData().contains(ROOT_KEY, 10);
   }

   public static void setSpirits(Entity player, List<CsmSpiritData> spirits) {
      CompoundTag root = getRoot(player);
      ListTag list = new ListTag();

      for (CsmSpiritData spirit : spirits) {
         list.add(spirit.toNbt());
      }

      root.put(SPIRITS_KEY, list);
      player.getPersistentData().put(ROOT_KEY, root);

      // Updates JJC compatibility data tags
      CompoundTag persistentData = player.getPersistentData();
      int jjcIdx = 1;

      for (CsmSpiritData spirit : spirits) {
         if (spirit.count > 0) {
            String key = "data_cursed_spirit_manipulation" + jjcIdx;
            persistentData.putDouble(key, 64.0D * (double) jjcIdx);
            persistentData.putString(key + "_name", spirit.displayName);
            persistentData.putDouble(key + "_num", (double) spirit.count);
            ++jjcIdx;
         }
      }

      String termKey = "data_cursed_spirit_manipulation" + jjcIdx;
      persistentData.putDouble(termKey, 0.0D);
      persistentData.putString(termKey + "_name", "");
      persistentData.putDouble(termKey + "_num", 0.0D);
   }

   public static void setSelectedUuid(Entity player, String uuid) {
      CompoundTag root = getRoot(player);
      root.putString(SEL_UUID_KEY, uuid == null ? "" : uuid);
      player.getPersistentData().put(ROOT_KEY, root);
   }

   public static void setSelectedAbility(Entity player, int idx) {
      CompoundTag root = getRoot(player);
      root.putInt(SEL_ABILITY_KEY, idx);
      player.getPersistentData().put(ROOT_KEY, root);
   }

   public static void setSelectedRosterIdx(Entity player, int idx) {
      CompoundTag root = getRoot(player);
      root.putInt(SEL_ROSTER_IDX_KEY, idx);
      player.getPersistentData().put(ROOT_KEY, root);
   }

   public static void cycleRosterIdx(Entity player) {
      List<CsmSpiritData> spirits = getSpirits(player);
      int size = spirits.size();
      if (size != 0) {
         int current = getSelectedRosterIdx(player);

         for (int i = 1; i <= size; ++i) {
            int next = (current + i) % size;
            if (spirits.get(next).count > 0) {
               setSelectedRosterIdx(player, next);
               return;
            }
         }
      }
   }

   public static void captureSpirit(Entity player, String entityType, String displayName, double baseHp, double baseAtk, int witherLevel) {
      List<CsmSpiritData> spirits = getSpirits(player);

      for (int i = 0; i < spirits.size(); ++i) {
         CsmSpiritData spirit = spirits.get(i);
         if (!spirit.isUnique && spirit.entityType.equals(entityType)) {
            spirits.set(i, spirit.withCount(spirit.count + 1));
            setSpirits(player, spirits);
            return;
         }
      }

      List<Integer> skillIds = new ArrayList<>();
      List<String> skillNames = new ArrayList<>();

      CsmSpiritData newSpirit = new CsmSpiritData(
              entityType, displayName, false, "", 1,
              baseHp, baseAtk, witherLevel, 0, false,
              skillNames, skillIds
      );

      spirits.add(newSpirit);
      setSpirits(player, spirits);
   }

   public static void mergeDiscoveredSkills(Entity player, String entityType, String uniqueUuid, Collection<Integer> observed) {
      if (observed == null || observed.isEmpty()) {
         return;
      }

      List<CsmSpiritData> spirits = getSpirits(player);
      boolean changed = false;

      for (int i = 0; i < spirits.size(); ++i) {
         CsmSpiritData spirit = spirits.get(i);
         boolean matches = spirit.isUnique ? spirit.uuid.equals(uniqueUuid) : !spirit.isUnique && spirit.entityType.equals(entityType);

         if (matches) {
            CsmSpiritData updated = spirit.withDiscoveredSkills(new ArrayList<>(observed));
            if (updated != spirit) {
               spirits.set(i, updated);
               changed = true;
            }
            break;
         }
      }

      if (changed) {
         setSpirits(player, spirits);
      }
   }


   public static void restoreSpirit(Entity player, String entityType, String uniqueUuid, String displayName) {
      List<CsmSpiritData> spirits = getSpirits(player);

      for (int i = 0; i < spirits.size(); ++i) {
         CsmSpiritData spirit = spirits.get(i);
         // Match by unique UUID if present, or by entityType for non-unique spirits
         boolean matches = spirit.isUnique
                 ? (uniqueUuid != null && !uniqueUuid.isEmpty() && spirit.uuid.equals(uniqueUuid))
                 : spirit.entityType.equals(entityType);

         if (matches) {
            // Restore by setting count back to spirit.count + 1
            spirits.set(i, spirit.withCount(spirit.count + 1));
            setSpirits(player, spirits);
            return; // Stop here! Do NOT call captureSpirit.
         }
      }

      captureSpirit(player, entityType, displayName != null && !displayName.isEmpty() ? displayName : entityType, 20.0D, 3.0D, 0);
   }
   public static CsmSpiritData deploySpirit(Entity player, int idx) {
      List<CsmSpiritData> spirits = getSpirits(player);
      if (idx >= 0 && idx < spirits.size()) {
         CsmSpiritData spirit = spirits.get(idx);
         if (spirit.count <= 0) {
            return null;
         } else {
            spirits.set(idx, spirit.withCount(spirit.count - 1));
            setSpirits(player, spirits);
            return spirit;
         }
      } else {
         return null;
      }
   }

   public static void markUnique(Entity player, int idx) {
      List<CsmSpiritData> spirits = getSpirits(player);
      if (idx >= 0 && idx < spirits.size()) {
         CsmSpiritData spirit = spirits.get(idx);
         if (!spirit.isUnique && spirit.count > 0) {
            String newUuid = UUID.randomUUID().toString();
            CsmSpiritData[] split = spirit.splitUnique(newUuid);
            if (split[0].count == 0) {
               spirits.set(idx, split[1]);
            } else {
               spirits.set(idx, split[0]);
               spirits.add(split[1]);
            }

            setSpirits(player, spirits);
         }
      }
   }

   public static void renameSpirit(Entity player, int idx, String newName) {
      List<CsmSpiritData> spirits = getSpirits(player);
      if (idx >= 0 && idx < spirits.size()) {
         spirits.set(idx, spirits.get(idx).withDisplayName(newName));
         setSpirits(player, spirits);
      }
   }

   public static void renameAbility(Entity player, int spiritIdx, int abilityIdx, String name) {
      List<CsmSpiritData> spirits = getSpirits(player);
      if (spiritIdx >= 0 && spiritIdx < spirits.size()) {
         spirits.set(spiritIdx, spirits.get(spiritIdx).withAbilityName(abilityIdx, name));
         setSpirits(player, spirits);
      }
   }

   public static void toggleSpared(Entity player, int idx) {
      List<CsmSpiritData> spirits = getSpirits(player);
      if (idx >= 0 && idx < spirits.size()) {
         CsmSpiritData spirit = spirits.get(idx);
         spirits.set(idx, spirit.withSpared(!spirit.spared));
         setSpirits(player, spirits);
      }
   }

   public static boolean feedFinger(Entity player, int idx) {
      List<CsmSpiritData> spirits = getSpirits(player);
      if (idx >= 0 && idx < spirits.size()) {
         CsmSpiritData spirit = spirits.get(idx);
         if (spirit.isUnique && spirit.fingers < 20) {
            spirits.set(idx, spirit.withFingers(spirit.fingers + 1));
            setSpirits(player, spirits);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static double uzumakiConsume(Entity player) {
      List<CsmSpiritData> spirits = getSpirits(player);
      double witherSum = 0.0D;
      List<CsmSpiritData> surviving = new ArrayList<>();

      for (CsmSpiritData spirit : spirits) {
         if (!spirit.spared && spirit.count != 0) {
            witherSum += (double) (spirit.witherLevel + 1) * 0.1D * (double) spirit.count;
         } else {
            surviving.add(spirit);
         }
      }

      setSpirits(player, surviving);
      return witherSum;
   }

   public static void removeSlot(Entity player, int idx) {
      List<CsmSpiritData> spirits = getSpirits(player);
      if (idx >= 0 && idx < spirits.size()) {
         spirits.remove(idx);
         setSpirits(player, spirits);
         CompoundTag root = getRoot(player);
         int sel = root.getInt(SEL_ABILITY_KEY);
         if (sel >= spirits.size()) {
            root.putInt(SEL_ABILITY_KEY, 0);
         }

         player.getPersistentData().put(ROOT_KEY, root);
      }
   }

   public static boolean isAbilityOnCooldown(Entity player, long currentGameTime) {
      return currentGameTime < player.getPersistentData().getLong(CD_UNTIL_KEY);
   }

   public static void startAbilityCooldown(Entity player, long currentGameTime) {
      player.getPersistentData().putLong(CD_UNTIL_KEY, currentGameTime + ABILITY_COOLDOWN_TICKS);
   }
   public static void removeDeployedSpirit(Entity player, String entityType, String uniqueUuid) {
      List<CsmSpiritData> spirits = getSpirits(player);
      for (int i = 0; i < spirits.size(); i++) {
         CsmSpiritData spirit = spirits.get(i);
         boolean matches = spirit.isUnique ? spirit.uuid.equals(uniqueUuid) : !spirit.isUnique && spirit.entityType.equals(entityType);

         if (matches) {
            if (spirit.isUnique || spirit.count <= 1) {
               spirits.remove(i);
            } else {
               spirits.set(i, spirit.withCount(spirit.count - 1));
            }
            setSpirits(player, spirits);
            break;
         }
      }
   }
   public static void cycleAbility(Entity player, String selectedSpiritEntityType, String selectedUuid) {
      List<CsmSpiritData> spirits = getSpirits(player);
      CsmSpiritData target = null;

      for (CsmSpiritData spirit : spirits) {
         if (spirit.isUnique) {
            if (!spirit.uuid.equals(selectedUuid)) {
               continue;
            }
         } else if (!spirit.entityType.equals(selectedSpiritEntityType)) {
            continue;
         }

         target = spirit;
         break;
      }

      if (target != null) {
         int maxAbilities = target.discoveredSkills.size();
         if (maxAbilities > 0) {
            int current = getSelectedAbility(player);
            setSelectedAbility(player, (current + 1) % maxAbilities);
         }
      }
   }
}