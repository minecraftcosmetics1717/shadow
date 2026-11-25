package com.ghosts.abyss.mixin;

import com.ghosts.abyss.mahito.MahitoForms;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.TechniqueDecideProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** One explicit order in BOTH directions, including Soul Grasp after Idle Transfiguration. */
@Pseudo
@Mixin(targets = "net.mcreator.jujutsucraft.procedures.KeyChangeTechniqueOnKeyPressedProcedure", remap = false)
public final class MahitoSkillSelectionMixin {
    private static final int[] NORMAL = {0, 1, 2, 3, 4, 5, 11, 6, 7, 8, 9, 10, 12, 13, 15, 20, 21};
    private static final int[] TRUE_FORM = {0, 1, 2, 3, 4, 5, 11, 7, 8, 9, 10, 15, 16, 20, 21};

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void abyss$mahitoSelection(LevelAccessor world, double x, double y, double z,
                                               Entity entity, CallbackInfo ci) {
        if (entity == null || !MahitoForms.isMahito(entity)) return;
        var vars = entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY).orElse(null);
        if (vars == null) return;

        int current = (int) Math.round(vars.PlayerSelectCurseTechnique);
        int[] order = MahitoForms.trueForm(entity) ? TRUE_FORM : NORMAL;
        if (vars.noChangeTechnique) {
            if (isAllowed(entity, current, order)) {
                decide(world, x, y, z, entity, current);
            } else if (current == 20 && isAllowed(entity, 21, order)) {
                decide(world, x, y, z, entity, 21);
            } else {
                decide(world, x, y, z, entity, 0);
            }
            ci.cancel();
            return;
        }

        int position = -1;
        for (int i = 0; i < order.length; i++) if (order[i] == current) { position = i; break; }
        if (position < 0) {
            decide(world, x, y, z, entity, 0);
            ci.cancel();
            return;
        }
        int direction = entity.isShiftKeyDown() ? -1 : 1;
        for (int i = 0; i < order.length; i++) {
            int index = Math.floorMod(position + direction * (i + 1), order.length);
            int next = order[index];
            if (!isAllowed(entity, next, order)) continue;
            decide(world, x, y, z, entity, next);
            ci.cancel();
            return;
        }
        ci.cancel();
    }

    private static boolean isAllowed(Entity entity, int slot, int[] order) {
        boolean inOrder = false;
        for (int value : order) {
            if (value == slot) {
                inOrder = true;
                break;
            }
        }

        if (!inOrder) return false;

        if ((slot == 3 || slot == 4 || slot == 16)
                && !MahitoForms.trueForm(entity)) {
            return false;
        }

        // Match Jujutsucraft's domain skill behavior.
        if (slot == 20 || slot == 21) {
            if (!(entity instanceof LivingEntity living)) return false;

            boolean domainActive = living.hasEffect(
                    JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()
            );

            // Don't show Domain Expansion while already expanded.
            if (slot == 20 && domainActive) return false;

            // Don't show Cancel Domain without an active expansion.
            if (slot == 21 && !domainActive) return false;
        }

        return MahitoForms.unlocked(entity, slot);
    }

    private static String local(String key) {
        return Component.translatable(key).getString();
    }

    private static void decide(LevelAccessor world, double x, double y, double z,
                               Entity entity, int slot) {
        final String multiplicity = local("jujutsu.technique.mahito_soul_multiplicity") + ": ";
        switch (slot) {
            case 0, 1, 2 -> TechniqueDecideProcedure.execute(entity, false, true,
                    0, 15, slot, local("jujutsu.technique.attack" + (slot + 1)));
            case 3 -> TechniqueDecideProcedure.execute(entity, false, true,
                    50, 15, slot, local("jujutsu.technique.attack6"));
            case 4 -> TechniqueDecideProcedure.execute(entity, false, true,
                    50, 15, slot, local("jujutsu.technique.attack7"));
            case 5 -> TechniqueDecideProcedure.execute(entity, false, false,
                    100, 15, slot, local("jujutsu.technique.idle_transfiguration"));
            case 11 -> TechniqueDecideProcedure.execute(entity, false, true,
                    50, 15, slot, "Soul Grasp");
            case 6 -> TechniqueDecideProcedure.execute(entity, false, false,
                    100, 15, slot, local("entity.jujutsucraft.entity_mahito_hand_3"));
            case 7 -> TechniqueDecideProcedure.execute(entity, false, false,
                    25, 15, slot, local("jujutsu.technique.mahito7"));
            case 8 -> TechniqueDecideProcedure.execute(entity, false, false,
                    150, 15, slot, multiplicity + local("jujutsu.technique.mahito_body_repel1"));
            case 9 -> TechniqueDecideProcedure.execute(entity, false, false,
                    300, 15, slot, multiplicity + local("jujutsu.technique.mahito_body_repel2"));
            case 10 -> TechniqueDecideProcedure.execute(entity, false, false,
                    200, 15, slot, multiplicity + local("entity.jujutsucraft.polymorphic_soul_isomer"));
            case 12 -> TechniqueDecideProcedure.execute(entity, false, true,
                    MahitoForms.blades(entity) ? 0 : 100, 15, slot, "Idle: Blades");
            case 13 -> TechniqueDecideProcedure.execute(entity, false, true,
                    MahitoForms.diminution(entity) ? 0 : 100, 15, slot, "Idle: Diminution");
            case 15 -> TechniqueDecideProcedure.execute(entity, false, true,
                    MahitoForms.trueForm(entity) ? 0 : 300, 15, slot,
                    "Idle: Instant Spirit Body of Distorted Killing");
            case 16 -> TechniqueDecideProcedure.execute(entity, false, true,
                    200, 15, slot, local("jujutsu.technique.attack8"));
            case 20 -> TechniqueDecideProcedure.execute(entity, false, false,
                    1250, 15, slot, "Self-Embodiment of Perfection");
            case 21 -> TechniqueDecideProcedure.execute(entity, true, true,
                    0, 15, slot, local("jujutsu.technique.cancel_domain"));
            default -> { }
        }
    }
}
