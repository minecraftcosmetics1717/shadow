package com.ghosts.abyss.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.KeyStartTechniqueOnKeyPressedProcedure",
        remap = false
)
public class DivergentFistKeyMixin {

    private static final int WINDOW_START_TICK = 5; // 0.25s
    private static final int WINDOW_END_TICK = 8;   // 0.40s

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void jreborn$dfSecondPress(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (entity == null) return;

        CompoundTag nbt = entity.getPersistentData();

        // Only intercept while our DF window is open
        if (!nbt.getBoolean("jreborn_df_active") || !nbt.getBoolean("jreborn_df_window_open")) {
            return;
        }

        // Cancel the normal skill start so it cannot spam / restart
        ci.cancel();

        int ticks = nbt.getInt("jreborn_df_ticks");

        // Pressed too early → permanently lock BF for this attempt
        if (ticks > 0 && ticks < WINDOW_START_TICK) {
            nbt.putBoolean("jreborn_df_early", true);
            nbt.putBoolean("jreborn_df_bf", false);
            return;
        }

        // Valid window (0.25s – 0.40s)
        if (ticks >= WINDOW_START_TICK && ticks <= WINDOW_END_TICK) {
            if (!nbt.getBoolean("jreborn_df_early")) {
                nbt.putBoolean("jreborn_df_bf", true);
            }
        }
    }
}