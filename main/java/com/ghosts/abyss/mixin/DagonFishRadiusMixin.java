
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.DomainDimension;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.HorizonOfTheCaptivatingSkandhaActiveProcedure",
        remap = false
)
public class DagonFishRadiusMixin {

    private static final ThreadLocal<Deque<Double>> SAVED_RADIUS =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "execute", at = @At("HEAD"), remap = false)
    private static void abyss$beforeFishSpawn(
            LevelAccessor world,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity == null
                || !entity.level().dimension().equals(DomainDimension.ID)
                || !entity.getPersistentData().getBoolean("abyss_domain_handled")) {
            SAVED_RADIUS.get().push(Double.NaN);
            return;
        }

        var vars = JujutsucraftModVariables.MapVariables.get(world);
        double original = vars.DomainExpansionRadius;

        SAVED_RADIUS.get().push(original);
        vars.DomainExpansionRadius = 22.0;

        if (original != 22.0) {
            com.ghosts.abyss.Abyss.LOGGER.info(
                    "[Abyss FishRadius] Corrected {} -> 22 for {}",
                    original,
                    entity.getName().getString()
            );
        }
    }

    @Inject(method = "execute", at = @At("RETURN"), remap = false)
    private static void abyss$afterFishSpawn(
            LevelAccessor world,
            Entity entity,
            CallbackInfo ci
    ) {
        Deque<Double> stack = SAVED_RADIUS.get();
        if (stack.isEmpty()) return;

        double original = stack.pop();

        if (!Double.isNaN(original)) {
            JujutsucraftModVariables.MapVariables.get(world)
                    .DomainExpansionRadius = original;
        }

        if (stack.isEmpty()) {
            SAVED_RADIUS.remove();
        }
    }
}
