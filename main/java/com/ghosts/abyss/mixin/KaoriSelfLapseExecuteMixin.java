package com.ghosts.abyss.mixin;

import com.ghosts.abyss.Abyss;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = {"net.mcreator.jujutsucraft.procedures.CursedTechniqueKaoriProcedure"},
        remap = false
)
public class KaoriSelfLapseExecuteMixin {

    @Inject(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void jreborn$kaoriSelfLapseExecute(LevelAccessor world, Entity entity, CallbackInfo ci) {
        if (!(entity instanceof ServerPlayer player)) return;

        boolean authorized = player.getPersistentData().getBoolean("jreborn_selflapse_auth");
        player.getPersistentData().putBoolean("jreborn_selflapse_auth", false);

        if (!authorized) return;

        // Reset skill persistent data so default Jujutsucraft behavior is cancelled
        player.getPersistentData().putDouble("skill", 0.0D);

        // Toggle Self-Lapse state
        boolean currentActive = player.getPersistentData().getBoolean("jreborn_selflapse_active");
        boolean newState = !currentActive;
        player.getPersistentData().putBoolean("jreborn_selflapse_active", newState);

        ci.cancel();
    }
}