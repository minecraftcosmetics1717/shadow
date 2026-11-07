
package com.ghosts.abyss.event;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketSyncAscendantAdvancements;

import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = Abyss.MOD_ID)
public class AscendantAdvancementSync {

    private static final ResourceLocation GRADE_1 =
            new ResourceLocation(
                    "jujutsucraft", "sorcerer_grade_1"
            );

    private static final ResourceLocation SPECIAL_GRADE =
            new ResourceLocation(
                    "jujutsucraft", "sorcerer_grade_special"
            );

    private static boolean hasAdvancement(
            ServerPlayer player, ResourceLocation id
    ) {
        Advancement advancement = player.server
                .getAdvancements().getAdvancement(id);

        return advancement != null
                && player.getAdvancements()
                .getOrStartProgress(advancement)
                .isDone();
    }

    public static void sync(ServerPlayer player) {
        boolean grade1 = hasAdvancement(player, GRADE_1);
        boolean special = hasAdvancement(player, SPECIAL_GRADE);

        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PacketSyncAscendantAdvancements(
                        grade1, special
                )
        );
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onAdvancement(
            AdvancementEvent.AdvancementEarnEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }
}
