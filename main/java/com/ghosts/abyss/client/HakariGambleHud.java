
package com.ghosts.abyss.client;

import com.ghosts.abyss.network.PacketSyncHakariGamble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "abyss", value = Dist.CLIENT)
public final class HakariGambleHud {

    private HakariGambleHud() {}

    private static final int WIDTH = 120;
    private static final int HEIGHT = 52;

    private static final int REEL_WIDTH = 28;
    private static final int REEL_HEIGHT = 22;
    private static final int REEL_GAP = 6;

    private static final int RED = 0xFFCC2536;
    private static final int GREEN = 0xFF70D995;

    private static final int TOTAL_TICKS = 60;

    // First and third reels stop after 40 ticks.
    // Middle reel stops after 60 ticks.
    private static final int OUTER_STOP = 40;
    private static final int MIDDLE_STOP = 60;

    private static boolean visible;
    private static boolean rolling;

    private static int remainingTicks;
    private static int first;
    private static int second;
    private static int third;
    private static int indicators;

    private static long receivedAt;
    private static long lastUpdate;

    public static void update(PacketSyncHakariGamble packet) {

        // Prevent old results from appearing
        // when a new domain session starts.
        if (!packet.visible) {
            visible = false;
            rolling = false;
            return;
        }

        boolean newRoll =
                packet.rolling && !rolling;

        if (newRoll) {
            first = 0;
            second = 0;
            third = 0;
        }

        visible = packet.visible;
        rolling = packet.rolling;
        remainingTicks = packet.remainingTicks;

        first = packet.first;
        second = packet.second;
        third = packet.third;

        indicators = packet.inputs;

        receivedAt = System.currentTimeMillis();
        lastUpdate = receivedAt;
    }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {

        if (event.getOverlay()
                != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }

        if (!visible) return;

        long now = System.currentTimeMillis();

        // Hide stale domain information.
        if (now - lastUpdate > 3000) return;

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.options.hideGui) {
            return;
        }

        GuiGraphics g = event.getGuiGraphics();

        int x = 7;
        int y = 7;

        // Main background.
        g.fill(
                x, y,
                x + WIDTH, y + HEIGHT,
                0xE5122119
        );

        // Green top border.
        g.fill(
                x, y,
                x + WIDTH, y + 2,
                GREEN
        );

        // Header.
        g.drawString(
                mc.font,
                "IDLE DEATH GAMBLE",
                x + 6,
                y + 7,
                0xFFB7EAC3,
                false
        );

        // Two indicator lights.
        drawIndicator(
                g,
                x + WIDTH - 19,
                y + 7,
                indicators >= 1
        );

        drawIndicator(
                g,
                x + WIDTH - 10,
                y + 7,
                indicators >= 2
        );

        int reelY = y + 21;

        int firstX = x + 11;
        int secondX = firstX + REEL_WIDTH + REEL_GAP;
        int thirdX = secondX + REEL_WIDTH + REEL_GAP;

        double elapsedTicks = 0;

        if (rolling) {
            // Each packet contains the remaining server ticks.
            // Include elapsed client time for smooth movement.
            elapsedTicks =
                    TOTAL_TICKS - remainingTicks
                            + (now - receivedAt) / 50.0;

            elapsedTicks = Math.max(
                    0,
                    Math.min(TOTAL_TICKS, elapsedTicks)
            );
        }

        drawReel(
                g, mc,
                firstX, reelY,
                0,
                first,
                elapsedTicks,
                rolling,
                OUTER_STOP
        );

        drawReel(
                g, mc,
                secondX, reelY,
                1,
                second,
                elapsedTicks,
                rolling,
                MIDDLE_STOP
        );

        drawReel(
                g, mc,
                thirdX, reelY,
                2,
                third,
                elapsedTicks,
                rolling,
                OUTER_STOP
        );
    }

    private static void drawIndicator(
            GuiGraphics g,
            int x,
            int y,
            boolean active
    ) {
        int color = active
                ? 0xFF83F5A2
                : 0xFF395444;

        g.fill(
                x, y,
                x + 5, y + 5,
                color
        );
    }

    private static void drawReel(
            GuiGraphics g,
            Minecraft mc,
            int x,
            int y,
            int reelIndex,
            int result,
            double elapsedTicks,
            boolean isRolling,
            int stopTick
    ) {
        // Reel background.
        g.fill(
                x, y,
                x + REEL_WIDTH,
                y + REEL_HEIGHT,
                0xFFF0F2EA
        );

        // Dark inner border.
        g.fill(
                x, y,
                x + REEL_WIDTH,
                y + 1,
                0xFF8DAB95
        );

        g.fill(
                x, y + REEL_HEIGHT - 1,
                x + REEL_WIDTH,
                y + REEL_HEIGHT,
                0xFF8DAB95
        );

        boolean spinning =
                isRolling && elapsedTicks < stopTick;

        if (!spinning) {
            drawCenteredNumber(
                    g, mc,
                    result,
                    x,
                    y + 7
            );
            return;
        }


// Faster visual spinning:
// One digit transition every 1.25 ticks.
// Approximately 3.2x faster than before.
        double position = elapsedTicks / 1.25;


        int step = (int) Math.floor(position);

        double fraction = position - step;

        int current = 1 + Math.floorMod(
                step + reelIndex * 3,
                9
        );

        int next = 1 + Math.floorMod(
                step + reelIndex * 3 + 1,
                9
        );

        // Number moves downward by 14 pixels.
        int offset = (int) Math.round(
                fraction * 14.0
        );

        // Clip numbers to the reel window.
        g.enableScissor(
                x + 1,
                y + 1,
                x + REEL_WIDTH - 1,
                y + REEL_HEIGHT - 1
        );

        // Current number moves down and out.
        drawCenteredNumber(
                g, mc,
                current,
                x,
                y + 7 + offset
        );

        // Next number enters from above.
        drawCenteredNumber(
                g, mc,
                next,
                x,
                y + 7 - 14 + offset
        );

        g.disableScissor();
    }

    private static void drawCenteredNumber(
            GuiGraphics g,
            Minecraft mc,
            int number,
            int x,
            int y
    ) {
        String text = number <= 0
                ? "-"
                : Integer.toString(number);

        g.drawCenteredString(
                mc.font,
                text,
                x + REEL_WIDTH / 2,
                y,
                RED
        );
    }
}
