
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.DomainInstanceManager;
import com.ghosts.abyss.domain.OpenDomainLifecycle;
import net.minecraft.core.Direction;
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
     * Redirect Cancel Domain to Abyss when the player owns an active
     * Abyss domain. Otherwise, leave JujutsuCraft's normal key handling alone.
     */
    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$handleCancelDomain(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }


        boolean openDomainActive =
                OpenDomainLifecycle.isActive(living.getUUID());

        boolean cancelSelected = isCancelSlotSelected(living);

// Allow cancellation if JC has not yet updated the
// selected technique during an active open domain.
        if (!cancelSelected && !openDomainActive) {
            return;
        }


        if (DomainInstanceManager.getByOwner(living.getUUID()) == null
                && !OpenDomainLifecycle.isActive(living.getUUID())) {
            return;
        }

        var data = living.getPersistentData();

        data.putBoolean("abyss_cancel_domain_auth", true);
        data.putBoolean("PRESS_Z", true);

        // Prevent JujutsuCraft's default Cancel Domain procedure from running.
        ci.cancel();
    }

    private static boolean isCancelSlotSelected(Entity entity) {
        try {
            Class<?> varsClass = Class.forName(
                    "net.mcreator.jujutsucraft.network.JujutsucraftModVariables"
            );

            Field capField = varsClass.getDeclaredField(
                    "PLAYER_VARIABLES_CAPABILITY"
            );
            capField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Capability<Object> capability =
                    (Capability<Object>) capField.get(null);

            Object vars = entity.getCapability(
                    capability,
                    (Direction) null
            ).orElse(null);

            if (vars == null) {
                return false;
            }

            Field selectField = vars.getClass().getDeclaredField(
                    "PlayerSelectCurseTechnique"
            );
            selectField.setAccessible(true);

            return ((Number) selectField.get(vars)).doubleValue() == 21.0;
        } catch (ReflectiveOperationException | ClassCastException e) {
            return false;
        }
    }
}