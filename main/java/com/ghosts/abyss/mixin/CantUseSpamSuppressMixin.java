package com.ghosts.abyss.mixin;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class CantUseSpamSuppressMixin {
   @Unique
   private final Map<String, Long> jslop$recentMessages = new HashMap<>();

   @Inject(
           method = "displayClientMessage",
           at = @At("HEAD"),
           cancellable = true
   )
   private void jslop$rateLimitRepeatMessages(Component message, boolean actionBar, CallbackInfo ci) {
      ServerPlayer player = (ServerPlayer) (Object) this;
      long now = player.level().getGameTime();
      String text = message.getString();
      Long last = this.jslop$recentMessages.get(text);

      if (last != null && now - last < 60L) {
         ci.cancel();
      } else {
         this.jslop$recentMessages.put(text, now);
      }
   }
}