package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.DomainInstanceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.common.capabilities.Capability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.KeyStartTechniqueOnKeyPressedProcedure",
        remap = false
)
public class CancelDomainKeyMixin {

    /**
     * When Cancel Domain is selected, mark cancel mode.
     * Actual long-press ticks run from LivingTick or StartTechnique repeat — see below.
     */
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void abyss$cancelDomainKey(
            LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (!(entity instanceof LivingEntity living)) return;

        if (!isCancelSlotSelected(living)) return;
        if (DomainInstanceManager.getByOwner(living.getUUID()) == null) return;

        living.getPersistentData().putBoolean("abyss_cancel_domain_auth", true);
        living.getPersistentData().putBoolean("PRESS_Z", true);
        // Don't let JC run default cancel on press — we long-press instead
        ci.cancel();
    }

    private static boolean isCancelSlotSelected(Entity entity) {
        try {
            Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
            Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
            capField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Capability<Object> cap = (Capability<Object>) capField.get(null);
            Object vars = entity.getCapability(cap, null).orElse(null);
            if (vars == null) return false;
            Field selectField = vars.getClass().getDeclaredField("PlayerSelectCurseTechnique");
            selectField.setAccessible(true);
            return ((Double) selectField.get(vars)) == 21.0;
        } catch (Exception e) {
            return false;
        }
    }
}