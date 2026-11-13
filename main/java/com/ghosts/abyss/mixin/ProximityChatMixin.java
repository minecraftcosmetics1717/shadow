
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.chat.ProximityMessages;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Predicate;

@Mixin(PlayerList.class)
public abstract class ProximityChatMixin {

    @ModifyVariable(
            method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private Predicate<ServerPlayer> abyss$limitChatRange(
            Predicate<ServerPlayer> original,
            PlayerChatMessage message,
            Predicate<ServerPlayer> filterPredicate,
            ServerPlayer sender,
            ChatType.Bound chatType
    ) {
        if (sender == null) {
            return original;
        }

        return receiver ->
                original.test(receiver)
                        && ProximityMessages.canHear(sender, receiver);
    }
}
