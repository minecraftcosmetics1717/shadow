package com.ghosts.abyss.mixin;

import com.ghosts.abyss.ability.AbilityScanHandler;
import com.ghosts.abyss.ability.ScannedAbility;
import com.ghosts.abyss.mahito.MahitoForms;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Keep Abyss's quick-select skills consistent with Mahito's wheel rotation. */
@Mixin(value = AbilityScanHandler.class, remap = false)
public final class MahitoAbilityScanMixin {
    private static final int[] NORMAL = {0, 1, 2, 3, 4, 5, 11, 6, 7, 8, 9, 10, 12, 13, 15, 20, 21};
    private static final int[] TRUE_FORM = {0, 1, 2, 3, 4, 5, 11, 7, 8, 9, 10, 15, 16, 20, 21};

    @Inject(method = "checkAvailability", at = @At("RETURN"), cancellable = true, remap = false)
    private static void abyss$mahitoAvailability(ServerPlayer player, double ct,
                                                   List<ScannedAbility> abilities,
                                                   CallbackInfoReturnable<List<ScannedAbility>> cir) {
        if ((int) Math.round(ct) != 15) return;
        boolean transformed = MahitoForms.trueForm(player);
        boolean unlocked = MahitoForms.hasTrueForm(player);
        int[] order = transformed ? TRUE_FORM : NORMAL;
        List<ScannedAbility> changed = new ArrayList<>();

        boolean domainActive = player.hasEffect(
                JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()
        );

        for (ScannedAbility ability : cir.getReturnValue()) {
            int slot = ability.select();
            if (indexOf(order, slot) < 0) continue;
            if (!transformed && (slot == 3 || slot == 4)) continue;
            // Hide skills that don't apply to the current domain state.
            if (slot == 20 && domainActive) continue;
            if (slot == 21 && !domainActive) continue;
            if (slot == 15 && !unlocked) continue;

            boolean available = ability.available();
            String name = ability.name();
            double cost = ability.cost();
            boolean physical = ability.physical();

            if (slot == 11) { name = "Soul Grasp"; cost = 50; physical = true; available = true; }
            if (slot == 12) { name = "Idle: Blades"; cost = MahitoForms.blades(player) ? 0 : 100; physical = true; available = true; }
            if (slot == 13) { name = "Idle: Diminution"; cost = MahitoForms.diminution(player) ? 0 : 100; physical = true; available = true; }
            if (slot == 15) { name = "Idle: Instant Spirit Body of Distorted Killing";
                cost = transformed ? 0 : 300; physical = true; available = unlocked; }
            if (slot == 20) { name = "Self-Embodiment of Perfection"; }

            // JC's original form effect disallows some normal techniques.
            // Abyss only hides Hand3, Blades and Diminution while transformed.
            if (transformed && (slot == 5 || slot == 7 || slot == 8 || slot == 9 || slot == 10)) {
                available = slot < 8
                        || (slot == 10 ? has(player, "jujutsucraft:skill_polymorphic_soul_isomer")
                        : has(player, "jujutsucraft:skill_soul_multiplicity"));
            }
            changed.add(new ScannedAbility(15, slot, name, cost, physical,
                    ability.passive(), available));
        }
        changed.sort(Comparator.comparingInt(a -> indexOf(order, a.select())));
        cir.setReturnValue(changed);
    }

    private static int indexOf(int[] order, int slot) {
        for (int i = 0; i < order.length; i++) if (order[i] == slot) return i;
        return -1;
    }

    private static boolean has(ServerPlayer player, String name) {
        Advancement a = player.server.getAdvancements().getAdvancement(new ResourceLocation(name));
        return a != null && player.getAdvancements().getOrStartProgress(a).isDone();
    }
}
