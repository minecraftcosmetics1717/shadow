package com.ghosts.abyss;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.ghosts.abyss.network.PacketRequestUnlockOpenDomain;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
        modid = "abyss",
        bus = Bus.FORGE
)
public class ModCommands {

   @SubscribeEvent
   public static void onRegisterCommands(RegisterCommandsEvent event) {
      LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("jslop")
              .requires(src -> src.hasPermission(2));

      // /jslop refinement add/remove/set
      root.then(Commands.literal("refinement")
              .then(Commands.literal("add")
                      .then(Commands.argument("player", EntityArgument.player())
                              .then(Commands.argument("amount", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
                                      .executes(ctx -> {
                                         ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                         int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                         int before = RefinementPoints.getAvailable(target);
                                         int granted = Math.min(amount, Integer.MAX_VALUE - before);
                                         target.getPersistentData().putDouble("refinement_points_available", before + granted);
                                         RefinementPoints.sync(target);
                                         ctx.getSource().sendSuccess(() -> Component.literal(
                                                 "Added " + granted + " refinement point(s) to " + target.getName().getString() +
                                                         ". Available: " + (before + granted) + "/2147483647"), true);
                                         return granted;
                                      }))))
              .then(Commands.literal("remove")
                      .then(Commands.argument("player", EntityArgument.player())
                              .then(Commands.argument("amount", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
                                      .executes(ctx -> {
                                         ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                         int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                         int before = RefinementPoints.getAvailable(target);
                                         int removed = Math.min(amount, before);
                                         target.getPersistentData().putDouble("refinement_points_available", before - removed);
                                         RefinementPoints.sync(target);
                                         ctx.getSource().sendSuccess(() -> Component.literal(
                                                 "Removed " + removed + " refinement point(s) from " + target.getName().getString() +
                                                         ". Available: " + (before - removed) + "/2147483647"), true);
                                         return removed;
                                      }))))
              .then(Commands.literal("set")
                      .then(Commands.argument("player", EntityArgument.player())
                              .then(Commands.argument("amount", IntegerArgumentType.integer(0, 50))
                                      .executes(ctx -> {
                                         ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                         int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                         target.getPersistentData().putDouble("domain_refinement", amount);
                                         RefinementPoints.sync(target);
                                         ctx.getSource().sendSuccess(() -> Component.literal(
                                                 "Set " + target.getName().getString() + "'s refinement level to " + amount + "/50"), true);
                                         return amount;
                                      })))));

      // /jslop advtech add/remove
      root.then(Commands.literal("advtech")
              .then(Commands.literal("add")
                      .then(Commands.argument("player", EntityArgument.player())
                              .then(Commands.argument("amount", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
                                      .executes(ctx -> {
                                         ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                         int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                         int before = AdvTechPoints.getAvailable(target);
                                         int granted = Math.min(amount, Integer.MAX_VALUE - before);
                                         target.getPersistentData().putDouble("advtech_points_available", before + granted);
                                         AdvTechPoints.sync(target);
                                         ctx.getSource().sendSuccess(() -> Component.literal(
                                                 "Added " + granted + " advtech point(s) to " + target.getName().getString() +
                                                         ". Available: " + (before + granted) + "/2147483647"), true);
                                         return granted;
                                      }))))
              .then(Commands.literal("remove")
                      .then(Commands.argument("player", EntityArgument.player())
                              .then(Commands.argument("amount", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
                                      .executes(ctx -> {
                                         ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                         int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                         int before = AdvTechPoints.getAvailable(target);
                                         int removed = Math.min(amount, before);
                                         target.getPersistentData().putDouble("advtech_points_available", before - removed);
                                         AdvTechPoints.sync(target);
                                         ctx.getSource().sendSuccess(() -> Component.literal(
                                                 "Removed " + removed + " advtech point(s) from " + target.getName().getString() +
                                                         ". Available: " + (before - removed) + "/2147483647"), true);
                                         return removed;
                                      })))));

      // /jslop rctexp add
      root.then(Commands.literal("rctexp")
              .then(Commands.literal("add")
                      .then(Commands.argument("player", EntityArgument.player())
                              .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                      .executes(ctx -> {
                                         ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                         int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                         int before = target.getPersistentData().getInt("rct_exp");
                                         target.getPersistentData().putInt("rct_exp", before + amount);
                                         RctPoints.sync(target);
                                         ctx.getSource().sendSuccess(() -> Component.literal(
                                                 "[JSlop] Gave " + amount + " RCT exp to " + target.getName().getString() +
                                                         " (total: " + (before + amount) + ")"), true);
                                         return amount;
                                      })))));

      // /jslop bfexp add
      root.then(Commands.literal("bfexp")
              .then(Commands.literal("add")
                      .then(Commands.argument("player", EntityArgument.player())
                              .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                      .executes(ctx -> {
                                         ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                         int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                         int before = target.getPersistentData().getInt("bf_exp");
                                         target.getPersistentData().putInt("bf_exp", before + amount);
                                         BlackFlashPoints.sync(target);
                                         ctx.getSource().sendSuccess(() -> Component.literal(
                                                 "[JSlop] Gave " + amount + " Black Flash exp to " + target.getName().getString() +
                                                         " (total: " + (before + amount) + ")"), true);
                                         return amount;
                                      })))));

      // Unlock commands
      root.then(Commands.literal("unlockHWB")
              .executes(ctx -> grantHWB(ctx.getSource(), ctx.getSource().getPlayerOrException()))
              .then(Commands.argument("player", EntityArgument.player())
                      .executes(ctx -> grantHWB(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));

      root.then(Commands.literal("unlockPerfectBody")
              .executes(ctx -> grantPerfectBody(ctx.getSource(), ctx.getSource().getPlayerOrException()))
              .then(Commands.argument("player", EntityArgument.player())
                      .executes(ctx -> grantPerfectBody(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));

      root.then(Commands.literal("unlockOpenDomain")
              .executes(ctx -> grantOpenDomain(ctx.getSource(), ctx.getSource().getPlayerOrException()))
              .then(Commands.argument("player", EntityArgument.player())
                      .executes(ctx -> grantOpenDomain(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));

      root.then(Commands.literal("unlockCOT")
              .executes(ctx -> grantCOT(ctx.getSource(), ctx.getSource().getPlayerOrException()))
              .then(Commands.argument("player", EntityArgument.player())
                      .executes(ctx -> grantCOT(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));

      event.getDispatcher().register(root);
   }

   private static int grantHWB(CommandSourceStack src, ServerPlayer player) {
      player.getPersistentData().putBoolean("witnessed_hwb", true);
      player.getPersistentData().putBoolean("unlocked_hwb", true);
      AdvTechPoints.sync(player);
      src.sendSuccess(() -> Component.literal("[JJS] Unlocked HWB for " + player.getName().getString()), false);
      return 1;
   }

   private static int grantPerfectBody(CommandSourceStack src, ServerPlayer player) {
      player.getPersistentData().putBoolean("perfect_body", true);
      src.sendSuccess(() -> Component.literal("[JSlop] Unlocked Perfect Body for " + player.getName().getString()), false);
      return 1;
   }

   private static int grantOpenDomain(CommandSourceStack src, ServerPlayer player) {
      if (PacketRequestUnlockOpenDomain.isRestrictedCT(player)) {
         src.sendFailure(Component.literal("[JJS] " + player.getName().getString() +
                 " has a restricted CT (Sukuna/Kenjaku/Kaori) — denied."));
         return 0;
      }

      player.getPersistentData().putBoolean("witnessed_open_domain", true);
      player.getPersistentData().putBoolean("open_domain_unlocked", true);
      OpenDomain.sync(player);
      src.sendSuccess(() -> Component.literal("[JJS] Unlocked Open Domain for " + player.getName().getString()), false);
      return 1;
   }

   private static int grantCOT(CommandSourceStack src, ServerPlayer player) {
      player.getPersistentData().putBoolean("cursed_object_transform_unlocked", true);
      src.sendSuccess(() -> Component.literal("[JJS] Unlocked COT for " + player.getName().getString()), false);
      return 1;
   }
}