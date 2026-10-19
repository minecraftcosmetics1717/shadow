package com.ghosts.abyss;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientAbilityCache {
   private static List<ScannedAbility> cached = Collections.emptyList();
   private static int version = 0;

   public static void update(List<ScannedAbility> abilities) {
      cached = new ArrayList<>(abilities);
      ++version;
      Abyss.LOGGER.debug("[JSlop] ClientAbilityCache updated: {} abilities", cached.size());
   }

   public static List<ScannedAbility> getForCT(int ct) {
      List<ScannedAbility> result = new ArrayList<>();

      for (ScannedAbility ab : cached) {
         if (ab.ct() == ct) {
            result.add(ab);
         }
      }

      return result;
   }

   public static List<ScannedAbility> getAll() {
      return Collections.unmodifiableList(cached);
   }

   public static int getVersion() {
      return version;
   }

   public static void clear() {
      cached = Collections.emptyList();
      ++version;
   }
}