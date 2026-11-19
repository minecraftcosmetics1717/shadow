
package com.ghosts.abyss.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.TechniqueDecideProcedure",
        remap = false
)
public class DomainDisplayMixin {

    private static final double DOMAIN_SLOT = 20.0D;

    @ModifyVariable(
            method = "execute",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            remap = false
    )
    private static String abyss$domainDisplayName(
            String originalName,
            net.minecraft.world.entity.Entity entity,
            boolean passive,
            boolean physical,
            double cost,
            double playerCT,
            double playerSelect
    ) {
        // Only rename Domain Expansion abilities.
        if (playerSelect != DOMAIN_SLOT) {
            return originalName;
        }

        // Use the actual technique ID supplied by
        // Jujutsucraft's TechniqueDecideProcedure.
        //
        // This supports primary and secondary
        // cursed techniques automatically.

        return switch ((int) playerCT) {

            // Gojo
            case 2 -> "Infinite Void";

            // Jogo
            case 4 -> "Coffin of the Iron Mountain";

            // Yuta Okkotsu
            case 5 -> "Authentic Mutual Love";

            // Megumi Fushiguro
            case 6 -> "Chimera Shadow Garden";

            // Dagon
            case 8 -> "Horizon of the Captivating Skandha";
// Ryu Ishigori
            case 12 -> "All-Satiating Flavor Festival";
            // Mahito
            case 15 -> "Self-Embodiment of Perfection";

            // Naoya Zenin
            case 19 -> "Time Cell Moon Palace";

            // Uraume
            case 24 -> "Distant Winds of Commitment";

            // Kinji Hakari
            case 29 -> "Idle Death Gamble";

            // Yorozu
            case 39 -> "Threefold Affliction";

            // All other cursed techniques
            // keep their original names.
            default -> originalName;
        };
    }
}
