package com.ghosts.abyss.csm;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;

public final class CsmSpiritData {
   public static final int MAX_FINGERS = 20;
   public static final int MAX_ABILITIES = 8;
   public static final double FINGER_HP_BONUS_PER = 0.05;
   public static final double FINGER_ATK_BONUS_PER = 0.03;
   public static final double FINGER_CLASH_BONUS_PER = 0.02;

   public final String entityType;
   public final String displayName;
   public final boolean isUnique;
   public final String uuid;
   public final int count;
   public final double baseHp;
   public final double baseAtk;
   public final int witherLevel;
   public final int fingers;
   public final boolean spared;
   public final List<String> abilityNames;
   public final List<Integer> discoveredSkills;

   private static final String KEY_TYPE = "type";
   private static final String KEY_DISPLAY = "display";
   private static final String KEY_UNIQUE = "unique";
   private static final String KEY_UUID = "uuid";
   private static final String KEY_COUNT = "count";
   private static final String KEY_BASE_HP = "base_hp";
   private static final String KEY_BASE_ATK = "base_atk";
   private static final String KEY_WITHER = "wither";
   private static final String KEY_FINGERS = "fingers";
   private static final String KEY_SPARED = "spared";
   private static final String KEY_ABILITY_NAMES = "ability_names";
   private static final String KEY_DISCOVERED_SKILLS = "discovered_skills";

   public CsmSpiritData(
           String entityType,
           String displayName,
           boolean isUnique,
           String uuid,
           int count,
           double baseHp,
           double baseAtk,
           int witherLevel,
           int fingers,
           boolean spared,
           List<String> abilityNames,
           List<Integer> discoveredSkills
   ) {
      this.entityType = entityType == null ? "" : entityType;
      this.displayName = displayName == null ? "" : displayName;
      this.isUnique = isUnique;
      this.uuid = uuid == null ? "" : uuid;
      this.count = Math.max(0, count);
      this.baseHp = Math.max(1.0D, baseHp);
      this.baseAtk = Math.max(0.0D, baseAtk);
      this.witherLevel = Math.max(0, witherLevel);
      this.fingers = Math.min(20, Math.max(0, fingers));
      this.spared = spared;

      List<String> names = abilityNames == null ? new ArrayList<>() : new ArrayList<>(abilityNames);
      List<Integer> skills = discoveredSkills == null ? new ArrayList<>() : new ArrayList<>(discoveredSkills);

      while (names.size() < skills.size()) {
         names.add("");
      }

      this.abilityNames = List.copyOf(names);
      this.discoveredSkills = List.copyOf(skills);
   }

   public double getEffectiveMaxHp() {
      double fingerBonus = 1.0D + (double) this.fingers * FINGER_HP_BONUS_PER;
      return this.baseHp * fingerBonus;
   }

   public double getEffectiveAttack() {
      double fingerBonus = 1.0D + (double) this.fingers * FINGER_ATK_BONUS_PER;
      return this.baseAtk * fingerBonus;
   }

   /**
    * Resolves missing method 'scaledHp'
    */
   public double scaledHp() {
      return getEffectiveMaxHp();
   }

   /**
    * Resolves missing method 'scaledAtk'
    */
   public double scaledAtk() {
      return getEffectiveAttack();
   }

   public double getClashBonusMultiplier() {
      return 1.0D + (double) this.fingers * FINGER_CLASH_BONUS_PER;
   }

   public CsmSpiritData withCount(int newCount) {
      return new CsmSpiritData(this.entityType, this.displayName, this.isUnique, this.uuid, newCount, this.baseHp, this.baseAtk, this.witherLevel, this.fingers, this.spared, this.abilityNames, this.discoveredSkills);
   }

   public CsmSpiritData withDisplayName(String newName) {
      return new CsmSpiritData(this.entityType, newName, this.isUnique, this.uuid, this.count, this.baseHp, this.baseAtk, this.witherLevel, this.fingers, this.spared, this.abilityNames, this.discoveredSkills);
   }

   public CsmSpiritData withFingers(int newFingers) {
      return new CsmSpiritData(this.entityType, this.displayName, this.isUnique, this.uuid, this.count, this.baseHp, this.baseAtk, this.witherLevel, newFingers, this.spared, this.abilityNames, this.discoveredSkills);
   }

   public CsmSpiritData withSpared(boolean newSpared) {
      return new CsmSpiritData(this.entityType, this.displayName, this.isUnique, this.uuid, this.count, this.baseHp, this.baseAtk, this.witherLevel, this.fingers, newSpared, this.abilityNames, this.discoveredSkills);
   }

   public CsmSpiritData withAbilityName(int index, String name) {
      if (index >= 0 && index < this.abilityNames.size()) {
         List<String> updated = new ArrayList<>(this.abilityNames);
         updated.set(index, name == null ? "" : name);
         return new CsmSpiritData(this.entityType, this.displayName, this.isUnique, this.uuid, this.count, this.baseHp, this.baseAtk, this.witherLevel, this.fingers, this.spared, updated, this.discoveredSkills);
      } else {
         return this;
      }
   }

   public CsmSpiritData withDiscoveredSkills(List<Integer> newSkillsList) {
      if (newSkillsList == null || newSkillsList.isEmpty()) {
         return this;
      }

      TreeSet<Integer> mergedIds = new TreeSet<>(this.discoveredSkills);
      mergedIds.addAll(newSkillsList);

      List<Integer> newSkills = new ArrayList<>(mergedIds);
      List<String> newNames = new ArrayList<>(this.abilityNames);

      while (newNames.size() < newSkills.size()) {
         newNames.add("");
      }

      return newSkills.equals(this.discoveredSkills) && newNames.equals(this.abilityNames)
              ? this
              : new CsmSpiritData(this.entityType, this.displayName, this.isUnique, this.uuid, this.count, this.baseHp, this.baseAtk, this.witherLevel, this.fingers, this.spared, newNames, newSkills);
   }

   public CsmSpiritData[] splitUnique(String newUuid) {
      CsmSpiritData decremented = this.withCount(Math.max(0, this.count - 1));
      List<String> copiedNames = new ArrayList<>(this.abilityNames);
      List<Integer> copiedSkills = new ArrayList<>(this.discoveredSkills);
      CsmSpiritData uniqueSingle = new CsmSpiritData(this.entityType, this.displayName, true, newUuid, 1, this.baseHp, this.baseAtk, this.witherLevel, this.fingers, this.spared, copiedNames, copiedSkills);
      return new CsmSpiritData[]{decremented, uniqueSingle};
   }

   public String getAbilityLabel(int index) {
      return abilityLabel(index);
   }

   public String abilityLabel(int index) {
      if (index >= 0 && index < this.discoveredSkills.size()) {
         String customName = index < this.abilityNames.size() ? this.abilityNames.get(index) : "";
         if (customName != null && !customName.trim().isEmpty()) {
            return customName.trim();
         } else {
            return "Skill #" + this.discoveredSkills.get(index);
         }
      } else {
         return "None";
      }
   }

   public String shortLabel() {
      if (this.displayName != null && !this.displayName.isEmpty()) {
         return this.displayName;
      }
      if (this.entityType != null && !this.entityType.isEmpty()) {
         int colonIdx = this.entityType.indexOf(':');
         return colonIdx >= 0 ? this.entityType.substring(colonIdx + 1) : this.entityType;
      }
      return "Cursed Spirit";
   }

   public CompoundTag toNbt() {
      CompoundTag tag = new CompoundTag();
      tag.putString(KEY_TYPE, this.entityType);
      tag.putString(KEY_DISPLAY, this.displayName);
      tag.putBoolean(KEY_UNIQUE, this.isUnique);
      tag.putString(KEY_UUID, this.uuid);
      tag.putInt(KEY_COUNT, this.count);
      tag.putDouble(KEY_BASE_HP, this.baseHp);
      tag.putDouble(KEY_BASE_ATK, this.baseAtk);
      tag.putInt(KEY_WITHER, this.witherLevel);
      tag.putInt(KEY_FINGERS, this.fingers);
      tag.putBoolean(KEY_SPARED, this.spared);

      ListTag nameList = new ListTag();
      for (String name : this.abilityNames) {
         nameList.add(net.minecraft.nbt.StringTag.valueOf(name));
      }
      tag.put(KEY_ABILITY_NAMES, nameList);

      ListTag skillList = new ListTag();
      for (Integer id : this.discoveredSkills) {
         skillList.add(IntTag.valueOf(id));
      }
      tag.put(KEY_DISCOVERED_SKILLS, skillList);

      return tag;
   }

   public static CsmSpiritData fromNbt(CompoundTag tag) {
      if (tag == null) {
         return new CsmSpiritData("", "", false, "", 0, 100.0D, 10.0D, 0, 0, false, List.of(), List.of());
      } else {
         String type = tag.getString(KEY_TYPE);
         String display = tag.getString(KEY_DISPLAY);
         boolean unique = tag.getBoolean(KEY_UNIQUE);
         String uuid = tag.getString(KEY_UUID);
         int count = tag.getInt(KEY_COUNT);
         double baseHp = tag.contains(KEY_BASE_HP) ? tag.getDouble(KEY_BASE_HP) : 100.0D;
         double baseAtk = tag.contains(KEY_BASE_ATK) ? tag.getDouble(KEY_BASE_ATK) : 10.0D;
         int wither = tag.getInt(KEY_WITHER);
         int fingers = tag.getInt(KEY_FINGERS);
         boolean spared = tag.getBoolean(KEY_SPARED);

         List<String> names = new ArrayList<>();
         if (tag.contains(KEY_ABILITY_NAMES, 9)) { // 9 = ListTag
            ListTag list = tag.getList(KEY_ABILITY_NAMES, 8); // 8 = StringTag
            for (int i = 0; i < list.size(); ++i) {
               names.add(list.getString(i));
            }
         }

         List<Integer> skills = new ArrayList<>();
         if (tag.contains(KEY_DISCOVERED_SKILLS, 9)) {
            ListTag list = tag.getList(KEY_DISCOVERED_SKILLS, 3); // 3 = IntTag
            for (int i = 0; i < list.size(); ++i) {
               skills.add(list.getInt(i));
            }
         }

         return new CsmSpiritData(type, display, unique, uuid, count, baseHp, baseAtk, wither, fingers, spared, names, skills);
      }
   }
}