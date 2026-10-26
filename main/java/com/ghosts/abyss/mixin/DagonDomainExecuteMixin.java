package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.AbyssDomainCast;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.CursedTechniqueDagonProcedure",
        remap = false
)
public class DagonDomainExecuteMixin {

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void abyss$dagonDomainExecute(
            LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (!(entity instanceof LivingEntity living)) return;

        double skill = living.getPersistentData().getDouble("skill");
        if (skill != 820.0) return; // only domain skill

        // First tick from key: require auth (same idea as Uro). Later ticks while held: PRESS_Z
        boolean auth = living.getPersistentData().getBoolean("abyss_dagon_domain_auth");
        boolean holding = living.getPersistentData().getBoolean("PRESS_Z");
        boolean alreadyCasting = living.getPersistentData().getBoolean("abyss_domain_active")
                || living.getPersistentData().getBoolean("abyss_domain_committed")
                || living.getPersistentData().getDouble("cnt3") > 0;

        if (!auth && !holding && !alreadyCasting) {
            return; // let other Dagon skills alone if something odd sets 820
        }

        living.getPersistentData().putBoolean("abyss_dagon_domain_auth", false);

        AbyssDomainCast.tickDagonDomain(world, living);
        ci.cancel(); // never JC Horizon / CreateBarrier
    }
}