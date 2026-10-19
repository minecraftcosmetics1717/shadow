package com.ghosts.abyss;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientYutaTechniqueCache {
   private static List<Entry> entries = Collections.emptyList();
   private static int domainIdx = -1;

   public static void update(List<Entry> list, int idx) {
      entries = new ArrayList(list);
      domainIdx = idx;
      Abyss.LOGGER.debug("[JSlop] ClientYutaTechniqueCache updated: {} techniques, domainIdx={}", entries.size(), domainIdx);
   }

   public static List<Entry> getAll() {
      return Collections.unmodifiableList(entries);
   }

   public static int getDomainIdx() {
      return domainIdx;
   }

   public static int getCount() {
      return entries.size();
   }

   public static void clear() {
      entries = Collections.emptyList();
      domainIdx = -1;
   }

   public static record Entry(double skill, double cooldownTicks, String name, double ceCost) {
   }
}
