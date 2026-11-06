
package com.ghosts.abyss.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = "abyss",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class HakariJackpotMusic {

    private HakariJackpotMusic() {}

    private static final ResourceLocation JACKPOT_EFFECT =
            new ResourceLocation(
                    "jujutsucraft",
                    "jackpot"
            );

    // Provisional sound ID from Jujutsucraft.
    private static final ResourceLocation MUSIC_ID =
            new ResourceLocation(
                    "jujutsucraft",
                    "hakari1"
            );

    private static final Map<UUID, HakariJackpotSound> SOUNDS =
            new HashMap<>();

    public static boolean hasJackpot(LivingEntity entity) {
        MobEffect effect =
                ForgeRegistries.MOB_EFFECTS.getValue(
                        JACKPOT_EFFECT
                );

        return effect != null
                && entity.hasEffect(effect);
    }

    @SubscribeEvent
    public static void onClientTick(
            TickEvent.ClientTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            clearAll();
            return;
        }

        SoundEvent music =
                ForgeRegistries.SOUND_EVENTS.getValue(
                        MUSIC_ID
                );

        if (music == null) {
            clearAll();
            return;
        }

        Set<UUID> nearby = new HashSet<>();

        // Find Jackpot users within hearing range.
        for (LivingEntity entity :
                mc.level.getEntitiesOfClass(
                        LivingEntity.class,
                        mc.player.getBoundingBox().inflate(80.0)
                )) {

            if (!entity.isAlive()) {
                continue;
            }

            if (!hasJackpot(entity)) {
                continue;
            }

            double distance = mc.player.distanceTo(entity);

            if (distance >= HakariJackpotSound.MAX_DISTANCE) {
                continue;
            }

            UUID id = entity.getUUID();
            nearby.add(id);

            HakariJackpotSound existing = SOUNDS.get(id);

            if (existing == null || existing.isFinished()) {

                HakariJackpotSound sound =
                        new HakariJackpotSound(entity, music);

                SOUNDS.put(id, sound);
                mc.getSoundManager().play(sound);
            }
        }

        // Stop music when a Jackpot user leaves range,
        // loses the effect, or disappears.
        Iterator<Map.Entry<UUID, HakariJackpotSound>> iterator =
                SOUNDS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, HakariJackpotSound> entry =
                    iterator.next();

            if (!nearby.contains(entry.getKey())) {
                entry.getValue().finish();
                iterator.remove();
            }
        }
    }

    private static void clearAll() {
        for (HakariJackpotSound sound : SOUNDS.values()) {
            sound.finish();
        }

        SOUNDS.clear();
    }
}
