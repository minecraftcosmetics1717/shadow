package com.ghosts.abyss.csm;

import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-side cache for tracking Cursed Spirit Manipulation (CSM) state.
 * Updated via network packets from the server.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientCsmCache {
   private static List<CsmSpiritData> spirits = new ArrayList<>();
   private static String selectedUuid = "";
   private static int selectedAbility = 0;
   private static int selectedRosterIdx = 0;
   private static List<String[]> deployedSpirits = new ArrayList<>();
   private static long cooldownExpiresAt = 0L;

   private ClientCsmCache() {
      // Private constructor to prevent instantiation
   }

   /**
    * Updates the local client cache with new spirit data received from the server.
    */
   public static void update(List<CsmSpiritData> newSpirits, String newSelectedUuid, int newSelectedAbility, int newSelectedRosterIdx, List<String[]> newDeployed) {
      spirits = new ArrayList<>(newSpirits);
      selectedUuid = (newSelectedUuid == null) ? "" : newSelectedUuid;
      selectedAbility = newSelectedAbility;
      selectedRosterIdx = newSelectedRosterIdx;
      deployedSpirits = new ArrayList<>(newDeployed);
   }

   public static List<CsmSpiritData> getAll() {
      return spirits;
   }

   public static int getCount() {
      return spirits.size();
   }

   public static String getSelectedUuid() {
      return selectedUuid;
   }

   public static int getSelectedAbility() {
      return selectedAbility;
   }

   public static int getSelectedRosterIdx() {
      return selectedRosterIdx;
   }

   public static List<String[]> getDeployedSpirits() {
      return deployedSpirits;
   }

   public static long getCooldownExpiresAt() {
      return cooldownExpiresAt;
   }

   public static void setCooldownExpiresAt(long expiresAt) {
      cooldownExpiresAt = expiresAt;
   }

   /**
    * Retrieves the currently selected unique spirit matching {@link #selectedUuid}.
    *
    * @return The active {@link CsmSpiritData}, or {@code null} if none is selected.
    */
   public static CsmSpiritData getSelectedSpirit() {
      if (selectedUuid != null && !selectedUuid.isEmpty()) {
         for (CsmSpiritData spirit : spirits) {
            if (spirit.isUnique && spirit.uuid.equals(selectedUuid)) {
               return spirit;
            }
         }
      }
      return null;
   }

   /**
    * Resets all cached client data to default values.
    */
   public static void clear() {
      spirits.clear();
      selectedUuid = "";
      selectedAbility = 0;
      selectedRosterIdx = 0;
      deployedSpirits.clear();
      cooldownExpiresAt = 0L;
   }
}