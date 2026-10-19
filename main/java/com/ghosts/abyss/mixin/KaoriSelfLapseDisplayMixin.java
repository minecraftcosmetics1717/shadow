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

@Pseudo
@Mixin(
        targets = {"net.mcreator.jujutsucraft.procedures.KeyChangeTechniqueOnKeyPressedProcedure"},
        remap = false
)
public class KaoriSelfLapseDisplayMixin {

    private static final double SKILL_COST = 100.0D;

    @Inject(
            method = {"execute"},
            at = {@At("HEAD")},
            cancellable = true,
            remap = false
    )
    private static void jreborn$kaoriSelfLapseDisplay(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        try {
            Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
            Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
            capField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Capability<Object> cap = (Capability<Object>) capField.get(null);
            Object vars = entity.getCapability(cap, null).orElse(null);

            if (vars == null) {
                return;
            }

            Field secondField = vars.getClass().getDeclaredField("SecondTechnique");
            secondField.setAccessible(true);
            boolean secondActive = (Boolean) secondField.get(vars);

            Field ctField = vars.getClass().getDeclaredField(secondActive ? "PlayerCurseTechnique2" : "PlayerCurseTechnique");
            ctField.setAccessible(true);

            double playerSkill = (Double) ctField.get(vars);
            // Technique ID 41.0 = Kaori Itadori
            if (playerSkill != 41.0D) {
                return;
            }

            // Read selected technique slot
            Field selectField = vars.getClass().getDeclaredField("PlayerSelectCurseTechnique");
            selectField.setAccessible(true);
            double selectNumber = (Double) selectField.get(vars);

            boolean isShiftKeyDown = entity.isShiftKeyDown();
            boolean noChange = false;

            try {
                Field ncField = vars.getClass().getDeclaredField("noChangeTechnique");
                ncField.setAccessible(true);
                noChange = (Boolean) ncField.get(vars);
            } catch (NoSuchFieldException ignored) {
            }

            boolean commitSelfLapse;
            // Next move from slot 10 (forward scroll without shift)
            if (!isShiftKeyDown && selectNumber == 10.0D && !noChange) {
                commitSelfLapse = true;
            }
            // Previous move from slot 20 (backward scroll with shift)
            else if (isShiftKeyDown && selectNumber == 20.0D && !noChange) {
                commitSelfLapse = true;
            }
            // Staying on slot 11
            else if (selectNumber == 11.0D && noChange) {
                commitSelfLapse = true;
            } else {
                commitSelfLapse = false;
            }

            if (!commitSelfLapse) {
                return;
            }

            // Set technique display to "Antigravity System: Self-Lapse" (ID 11.0) for Technique 41.0
            Class<?> decideClass = Class.forName("net.mcreator.jujutsucraft.procedures.TechniqueDecideProcedure");
            decideClass.getMethod("execute", Entity.class, Boolean.TYPE, Boolean.TYPE, Double.TYPE, Double.TYPE, Double.TYPE, String.class)
                    .invoke(null, entity, false, false, SKILL_COST, 41.0D, 11.0D, "Antigravity System: Self-Lapse");

            ci.cancel();
        } catch (Exception e) {
            Abyss.LOGGER.warn("[JReborn] KaoriSelfLapseDisplayMixin failed: {}", e.getMessage());
        }
    }
}