package com.ghosts.abyss;

import java.lang.reflect.Field;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.Capability;

public class YutaTechniqueRegistry {
   public static final String KEY_LIST = "jslop_yuta_registered";
   public static final String KEY_DOMAIN = "jslop_yuta_domain_idx";
   public static final int MAX_COUNT = 40;

   public static ListTag getList(ServerPlayer player) {
      return player.getPersistentData().getList(KEY_LIST, Tag.TAG_COMPOUND);
   }

   public static int getCount(ServerPlayer player) {
      return getList(player).size();
   }

   public static boolean canRegister(ServerPlayer player) {
      return getCount(player) < MAX_COUNT;
   }

   public static int getDomainIdx(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      return data.contains(KEY_DOMAIN, Tag.TAG_INT) ? data.getInt(KEY_DOMAIN) : -1;
   }

   public static void register(ServerPlayer player, double skill, double cooldownTicks, String name, String shikigamiName, double shikigamiHp, double ceCost) {
      CompoundTag entry = new CompoundTag();
      entry.putDouble("skill", skill);
      entry.putDouble("cooldown_ticks", cooldownTicks);
      entry.putString("name", name);
      entry.putString("shikigami_name", shikigamiName);
      entry.putDouble("shikigami_hp", shikigamiHp);
      entry.putDouble("ce_cost", ceCost);

      CompoundTag data = player.getPersistentData();
      ListTag list = data.getList(KEY_LIST, Tag.TAG_COMPOUND);
      list.add(entry);
      data.put(KEY_LIST, list);
   }

   public static void remove(ServerPlayer player, int idx) {
      CompoundTag data = player.getPersistentData();
      ListTag list = data.getList(KEY_LIST, Tag.TAG_COMPOUND);

      if (idx >= 0 && idx < list.size()) {
         list.remove(idx);
         data.put(KEY_LIST, list);

         int domainIdx = getDomainIdx(player);
         if (domainIdx == idx) {
            setDomainIdx(player, -1);
         } else if (domainIdx > idx) {
            setDomainIdx(player, domainIdx - 1);
         }
      }
   }

   public static void setDomainIdx(ServerPlayer player, int idx) {
      player.getPersistentData().putInt(KEY_DOMAIN, idx);
   }

   public static boolean hasYutaCT(ServerPlayer player) {
      try {
         Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
         Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
         capField.setAccessible(true);

         @SuppressWarnings("unchecked")
         Capability<Object> cap = (Capability<Object>) capField.get(null);
         Object vars = player.getCapability(cap, (Direction) null).orElse(null);

         if (vars == null) {
            return false;
         }

         Field ct1Field = vars.getClass().getDeclaredField("PlayerCurseTechnique");
         Field ct2Field = vars.getClass().getDeclaredField("PlayerCurseTechnique2");
         ct1Field.setAccessible(true);
         ct2Field.setAccessible(true);

         double ct1 = (Double) ct1Field.get(vars);
         double ct2 = (Double) ct2Field.get(vars);

         return ct1 == 5.0D || ct2 == 5.0D;
      } catch (Exception e) {
         return false;
      }
   }
}