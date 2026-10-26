package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.AbyssDomainCast;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.CursedTechniqueDagonProcedure",
        remap = false
)
public class CursedTechniqueDagonMixin {

    /** skill 820 = Horizon of the Captivating Skandha — take over fully */
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void abyss$redirectDomain(
            LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (entity == null) return;
        double skill = entity.getPersistentData().getDouble("skill");
        if (skill != 820.0) return;

        if (entity instanceof LivingEntity living) {
            AbyssDomainCast.tickDagonDomain(world, living);
        }
        ci.cancel(); // never run JC Horizon / barrier procedure
    }
}