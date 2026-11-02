
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.DomainDimension;
import net.mcreator.jujutsucraft.client.particle.ParticleUnlimitedVoidHoleParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ParticleUnlimitedVoidHoleParticle.class, remap = false)
public abstract class GojoVoidHoleScaleMixin extends TextureSheetParticle {

    protected GojoVoidHoleScaleMixin(
            ClientLevel level,
            double x,
            double y,
            double z
    ) {
        super(level, x, y, z);
    }

    @Inject(
            method = "<init>",
            at = @At("TAIL"),
            remap = false
    )
    private void abyss$scaleVoidHole(
            ClientLevel level,
            double x,
            double y,
            double z,
            double vx,
            double vy,
            double vz,
            SpriteSet spriteSet,
            CallbackInfo ci
    ) {
        // Only enlarge the particle inside Abyss's domain.
        if (!level.dimension().equals(DomainDimension.ID)) {
            return;
        }

        // Original quadSize is 7.0F.
        // Triple its rendered size.
        this.quadSize *= 3.0F;
    }
}
