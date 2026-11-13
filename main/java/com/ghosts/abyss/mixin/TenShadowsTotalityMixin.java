
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.command.TotalityCommand;
import net.mcreator.jujutsucraft.procedures.LogicAttackProcedure;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LogicAttackProcedure.class, remap = false)
public abstract class TenShadowsTotalityMixin {

    private static final TagKey<net.minecraft.world.entity.EntityType<?>> SHIKIGAMI =
            TagKey.create(
                    Registries.ENTITY_TYPE,
                    new ResourceLocation("jujutsucraft", "ten_shadows_technique")
            );

    @Inject(
            method = "execute",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void abyss$totalityFriendlyFire(
            LevelAccessor world,
            Entity attacker,
            Entity target,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (cir.getReturnValueZ()) return;
        if (attacker == null || target == null) return;
        if (attacker.level().isClientSide) return;

        // Only affect tamed Ten Shadows shikigami.
        if (!target.getType().is(SHIKIGAMI)) return;
        if (!target.getPersistentData().getBoolean("Ambush")) return;
        if (target.getPersistentData().getBoolean("domain_entity")) return;

        String ownerId = target.getPersistentData()
                .getString("OWNER_UUID");

        if (ownerId.isEmpty()) return;

        ServerPlayer owner = null;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        try {
            owner = server.getPlayerList().getPlayer(
                    java.util.UUID.fromString(ownerId)
            );
        } catch (IllegalArgumentException ignored) {
            return;
        }

        if (owner == null) return;
        if (!TotalityCommand.hasTenShadows(owner)) return;
        if (!owner.getPersistentData().getBoolean(
                TotalityCommand.TAG
        )) return;

        // The shikigami must belong to this owner.
        double friend = owner.getPersistentData()
                .getDouble("friend_num");

        if (friend == 0.0D) return;

        if (target.getPersistentData()
                .getDouble("friend_num") != friend) return;

        // Allow the owner or an attack entity owned by them.
        boolean ownerAttack = attacker == owner;

        if (!ownerAttack) {
            ownerAttack = ownerId.equals(
                    attacker.getPersistentData()
                            .getString("OWNER_UUID")
            );
        }

        if (!ownerAttack) return;

        // Restore attack eligibility for this specific case.
        cir.setReturnValue(true);
    }
}
