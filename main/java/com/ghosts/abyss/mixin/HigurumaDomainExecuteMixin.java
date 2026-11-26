package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.AbyssDomainCast;
import com.ghosts.abyss.domain.DomainInstance;
import com.ghosts.abyss.domain.DomainInstanceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.mcreator.jujutsucraft.procedures.CursedTechniqueHigurumaProcedure", remap = false)
public class HigurumaDomainExecuteMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void abyss$replaceHigurumaDomain(
            LevelAccessor world, double x, double y, double z,
            Entity entity, CallbackInfo ci
    ) {
        if (!(entity instanceof LivingEntity living)) return;
        double skill = entity.getPersistentData().getDouble("skill");

        if (skill == 2720.0D) {
            ci.cancel();
            AbyssDomainCast.tickHigurumaDomain(world, living);
            return;
        }

        // Suppress the original judgement while Abyss owns this domain.
        if (skill == 2719.0D) {
            DomainInstance inst = DomainInstanceManager.getByOwner(entity.getUUID());
            if (inst != null && inst.skillDomain == 27.0D) {
                ci.cancel();
                if (!entity.level().isClientSide) {
                    entity.getPersistentData().putDouble("skill", 0.0D);
                    entity.getPersistentData().putDouble("cnt3", 0.0D);
                }
            }
        }
    }
}
