
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.chat.ProximityMessages;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;

@Mixin(PlayerList.class)
public abstract class ProximityChatMixin {

    @Redirect(
            method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;sendChatMessage(Lnet/minecraft/network/chat/OutgoingChatMessage;ZLnet/minecraft/network/chat/ChatType$Bound;)V"
            )
    )
    private void abyss$localChat(
            ServerPlayer receiver,
            OutgoingChatMessage message,
            boolean filtered,
            ChatType.Bound chatType,
            PlayerChatMessage originalMessage,
            Predicate<ServerPlayer> filterPredicate,
            ServerPlayer sender,
            ChatType.Bound originalChatType
    ) {
        if (sender == null ||
                ProximityMessages.canHear(sender, receiver)) {

            receiver.sendChatMessage(
                    message,
                    filtered,
                    chatType
            );
        }
    }
}
