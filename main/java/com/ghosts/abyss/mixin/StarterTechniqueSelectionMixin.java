package com.ghosts.abyss.mixin;

import com.ghosts.abyss.ability.AbilityScanHandler;
import com.ghosts.abyss.technique.TechniqueIds;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.TechniqueDecideProcedure;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** CT 9101 is Abyss's no-innate-technique starter. Only native basic attacks. */
@Pseudo
@Mixin(targets = "net.mcreator.jujutsucraft.procedures.KeyChangeTechniqueOnKeyPressedProcedure", remap = false)
public abstract class StarterTechniqueSelectionMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void abyss$starterSelection(LevelAccessor world, double x, double y, double z,
                                               Entity entity, CallbackInfo ci) {
        if (!(entity instanceof Player)) return;
        var vars = entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY)
                .orElse(null);
        if (vars == null) return;

        double ct = vars.SecondTechnique ? vars.PlayerCurseTechnique2 : vars.PlayerCurseTechnique;
        if (ct != TechniqueIds.NO_INNATE_TECHNIQUE) return;

        int current = (int) Math.round(vars.PlayerSelectCurseTechnique);
        int selected;
        if (AbilityScanHandler.isPermissive()) {
            // The scanner probes all 22 slots. Report only 0, 1 and 2.
            // Invalid probes must not be remapped to a valid attack.
            if (current < 0 || current > 2) {
                ci.cancel();
                return;
            }
            selected = current;
        } else if (vars.noChangeTechnique) {
            selected = current >= 0 && current <= 2 ? current : 0;
        } else if (current < 0 || current > 2) {
            selected = 0;
        } else {
            selected = Math.floorMod(current + (entity.isShiftKeyDown() ? -1 : 1), 3);
        }

        // JC executes these three slots generically using skill % 100.
        // Use its existing translation, physical attack flag and zero cost.
        TechniqueDecideProcedure.execute(entity, false, true, 0.0D, TechniqueIds.NO_INNATE_TECHNIQUE,
                (double) selected,
                Component.translatable("jujutsu.technique.attack" + (selected + 1)).getString());
        ci.cancel();
    }
}
