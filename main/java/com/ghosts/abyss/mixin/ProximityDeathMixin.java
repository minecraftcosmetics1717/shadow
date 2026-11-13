
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.chat.ProximityMessages;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.damagesource.DamageSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayer.class)
public abstract class ProximityDeathMixin {

    @Shadow
    public net.minecraft.server.MinecraftServer server;

    @Redirect(
            method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"
            )
    )
    private void abyss$localDeathMessage(
            PlayerList playerList,
            Component message,
            boolean overlay,
            DamageSource source
    ) {
        ServerPlayer deceased = (ServerPlayer) (Object) this;

        for (ServerPlayer receiver : playerList.getPlayers()) {
            if (ProximityMessages.canHear(deceased, receiver)) {
                receiver.sendSystemMessage(message, overlay);
            }
        }
    }
}
