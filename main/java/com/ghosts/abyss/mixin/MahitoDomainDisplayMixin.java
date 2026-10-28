
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.Abyss;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.common.capabilities.Capability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.KeyChangeTechniqueOnKeyPressedProcedure",
        remap = false
)
public class MahitoDomainDisplayMixin {

    private static final double MAHITO_CT = 15.0;
    private static final double DOMAIN_SLOT = 20.0;
    private static final double DOMAIN_COST = 1250.0;
    private static final String DOMAIN_NAME = "Self-Embodiment of Perfection";

    @Inject(method = "execute", at = @At("RETURN"), remap = false)
    private static void abyss$mahitoDomainName(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity == null) return;

        try {
            Class<?> varsClass = Class.forName(
                    "net.mcreator.jujutsucraft.network.JujutsucraftModVariables"
            );

            Field capField = varsClass.getDeclaredField(
                    "PLAYER_VARIABLES_CAPABILITY"
            );
            capField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Capability<Object> cap =
                    (Capability<Object>) capField.get(null);

            Object vars = entity.getCapability(cap, null).orElse(null);
            if (vars == null) return;

            Field secondField = vars.getClass().getDeclaredField(
                    "SecondTechnique"
            );
            secondField.setAccessible(true);

            boolean secondActive = (Boolean) secondField.get(vars);

            Field ctField = vars.getClass().getDeclaredField(
                    secondActive
                            ? "PlayerCurseTechnique2"
                            : "PlayerCurseTechnique"
            );
            ctField.setAccessible(true);

            double playerSkill = (Double) ctField.get(vars);
            if (playerSkill != MAHITO_CT) return;

            Field selectField = vars.getClass().getDeclaredField(
                    "PlayerSelectCurseTechnique"
            );
            selectField.setAccessible(true);

            double selectNumber = (Double) selectField.get(vars);
            if (selectNumber != DOMAIN_SLOT) return;

            Class<?> decideClass = Class.forName(
                    "net.mcreator.jujutsucraft.procedures.TechniqueDecideProcedure"
            );

            Method execute = decideClass.getMethod(
                    "execute",
                    Entity.class,
                    boolean.class,
                    boolean.class,
                    double.class,
                    double.class,
                    double.class,
                    String.class
            );

            execute.invoke(
                    null,
                    entity,
                    false,
                    false,
                    DOMAIN_COST,
                    MAHITO_CT,
                    DOMAIN_SLOT,
                    DOMAIN_NAME
            );

        } catch (Exception e) {
            Abyss.LOGGER.warn(
                    "[Abyss] MahitoDomainDisplayMixin failed: {}",
                    e.getMessage()
            );
        }
    }
}
