package com.ghosts.abyss;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraftforge.fml.loading.FMLPaths;

public class KeybindConfig {
   private static final Path CONFIG_PATH;
   public static final int SLOT_COUNT = 8;
   private static final Ability[] slots;

   public static void loadFromNbt(CompoundTag playerData) {
      Arrays.fill(slots, Ability.UNBOUND);

      if (playerData.contains("jujutsuslop_keybinds", 9)) {
         ListTag list = playerData.getList("jujutsuslop_keybinds", 10);

         for (int i = 0; i < Math.min(list.size(), 8); ++i) {
            CompoundTag tag = list.getCompound(i);
            int ct = tag.getInt("ct");
            int sel = tag.getInt("select");
            String name = tag.getString("name");
            slots[i] = ct >= 0 && sel >= 0 ? new Ability(ct, sel, name) : Ability.UNBOUND;
         }
      }
   }

   public static Ability[] getSlots() {
      return Arrays.copyOf(slots, 8);
   }

   public static void save() {
      JsonObject obj = new JsonObject();

      for (int i = 0; i < 8; ++i) {
         JsonObject s = new JsonObject();
         if (!slots[i].isUnbound()) {
            s.addProperty("ct", slots[i].ct());
            s.addProperty("select", slots[i].selectValue());
            s.addProperty("name", slots[i].displayName());
         }
         obj.add("slot_" + (i + 1), s);
      }

      try {
         Writer writer = new OutputStreamWriter(Files.newOutputStream(CONFIG_PATH), StandardCharsets.UTF_8);
         try {
            new GsonBuilder().setPrettyPrinting().create().toJson(obj, writer);
         } catch (Throwable t) {
            try {
               writer.close();
            } catch (Throwable t2) {
               t.addSuppressed(t2);
            }
            throw t;
         }
         writer.close();
      } catch (Exception e) {
         Abyss.LOGGER.warn("Could not save keybind config: {}", e.getMessage());
      }
   }

   public static Ability getSlot(int index) {
      return index >= 0 && index < 8 ? slots[index] : Ability.UNBOUND;
   }

   public static void setSlot(int index, Ability ability) {
      if (index >= 0 && index < 8) {
         slots[index] = ability == null ? Ability.UNBOUND : ability;
      }
   }

   public static List<ScannedAbility> getAbilitiesForCT(int ct) {
      return ClientAbilityCache.getForCT(ct);
   }

   static {
      CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("jujutsuslop_keybinds.json");
      slots = new Ability[8];
      Arrays.fill(slots, Ability.UNBOUND);
   }

   public static record Ability(int ct, int selectValue, String displayName) {
      public static final Ability UNBOUND = new Ability(-1, -1, "— Unbound —");

      public boolean isUnbound() {
         return this.selectValue == -1;
      }

      public String name() {
         return this.displayName;
      }
   }
}