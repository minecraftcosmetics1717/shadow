package com.ghosts.abyss;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.GameRules.Category;

public final class AbyssGamerules {
   public static final GameRules.Key<GameRules.BooleanValue> DOMAIN_REFINEMENT;
   public static final GameRules.Key<GameRules.BooleanValue> HOLLOW_WICKER_BASKET;
   public static final GameRules.Key<GameRules.BooleanValue> PERFECT_BODY;
   public static final GameRules.Key<GameRules.BooleanValue> CURSED_OBJECT_TRANSFORMATION;
   public static final GameRules.Key<GameRules.BooleanValue> OPEN_DOMAIN;
   public static final GameRules.Key<GameRules.BooleanValue> BLACK_FLASH;
   public static final GameRules.Key<GameRules.BooleanValue> REVERSE_CURSED_TECHNIQUE;
   public static final GameRules.Key<GameRules.BooleanValue> SHRINE_VISUAL;
   public static final GameRules.Key<GameRules.BooleanValue> CSM_KEEP_CURSES;

   private static boolean clientDomainRefinement;
   private static boolean clientHollowWickerBasket;
   private static boolean clientPerfectBody;
   private static boolean clientCursedObjectTransform;
   private static boolean clientOpenDomain;
   private static boolean clientBlackFlash;
   private static boolean clientReverseCursedTechnique;
   private static boolean clientShrineVisual;

   public static void setClientData(boolean domainRefinement, boolean hollowWickerBasket, boolean perfectBody,
                                    boolean cursedObjectTransform, boolean openDomain, boolean blackFlash,
                                    boolean reverseCursedTechnique, boolean shrineVisual) {
      clientDomainRefinement = domainRefinement;
      clientHollowWickerBasket = hollowWickerBasket;
      clientPerfectBody = perfectBody;
      clientCursedObjectTransform = cursedObjectTransform;
      clientOpenDomain = openDomain;
      clientBlackFlash = blackFlash;
      clientReverseCursedTechnique = reverseCursedTechnique;
      clientShrineVisual = shrineVisual;
   }

   public static boolean isClientDomainRefinementEnabled() {
      return clientDomainRefinement;
   }

   public static boolean isClientHollowWickerBasketEnabled() {
      return clientHollowWickerBasket;
   }

   public static boolean isClientPerfectBodyEnabled() {
      return clientPerfectBody;
   }

   public static boolean isClientCursedObjectTransformEnabled() {
      return clientCursedObjectTransform;
   }

   public static boolean isClientOpenDomainEnabled() {
      return clientOpenDomain;
   }

   public static boolean isClientBlackFlashEnabled() {
      return clientBlackFlash;
   }

   public static boolean isClientReverseCursedTechniqueEnabled() {
      return clientReverseCursedTechnique;
   }

   public static boolean isClientShrineVisualEnabled() {
      return clientShrineVisual;
   }

   public static boolean isEnabled(ServerPlayer player, GameRules.Key<GameRules.BooleanValue> key) {
      return player.getServer().getGameRules().getBoolean(key);
   }

   public static void init() {
      // Intentionally empty – static block handles registration
   }

   private AbyssGamerules() {
   }

   static {
      DOMAIN_REFINEMENT = GameRules.register("jslopDomainRefinement", Category.MISC, BooleanValue.create(true));
      HOLLOW_WICKER_BASKET = GameRules.register("jslopHollowWickerBasket", Category.MISC, BooleanValue.create(true));
      PERFECT_BODY = GameRules.register("jslopPerfectBody", Category.MISC, BooleanValue.create(true));
      CURSED_OBJECT_TRANSFORMATION = GameRules.register("jslopCursedObjectTransformation", Category.MISC, BooleanValue.create(true));
      OPEN_DOMAIN = GameRules.register("jslopOpenDomain", Category.MISC, BooleanValue.create(true));
      BLACK_FLASH = GameRules.register("jslopBlackFlash", Category.MISC, BooleanValue.create(true));
      REVERSE_CURSED_TECHNIQUE = GameRules.register("jslopReverseCursedTechnique", Category.MISC, BooleanValue.create(true));
      SHRINE_VISUAL = GameRules.register("jslopShrineVisual", Category.MISC, BooleanValue.create(true));
      CSM_KEEP_CURSES = GameRules.register("jslopCsmKeepCurses", Category.MISC, BooleanValue.create(false));

      clientDomainRefinement = true;
      clientHollowWickerBasket = true;
      clientPerfectBody = true;
      clientCursedObjectTransform = true;
      clientOpenDomain = true;
      clientBlackFlash = true;
      clientReverseCursedTechnique = true;
      clientShrineVisual = true;
   }
}