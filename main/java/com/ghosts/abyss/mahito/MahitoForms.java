package com.ghosts.abyss.mahito;

import com.ghosts.abyss.ability.AbilityScanHandler;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketSyncMahitoForms;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.network.PacketDistributor;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.KeyChangeTechniqueOnKeyPressedProcedure;
import net.minecraft.advancements.Advancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleTypes;

import java.util.UUID;

/** Server-owned toggle state for the three Mahito skills. */
@Mod.EventBusSubscriber(modid = "abyss", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MahitoForms {
    public static final int BLADES = 12;
    public static final int DIMINUTION = 13;
    public static final int TRUE_FORM = 15;

    private static final String BLADES_KEY = "abyss_mahito_idle_blades";
    private static final String SMALL_KEY = "abyss_mahito_idle_diminution";
    private static final String TRUE_KEY = "abyss_mahito_true_form";
    private static final String ORIGINAL_SIZE = "abyss_mahito_original_base_scale";
    private static final String SIZE_SAVED = "abyss_mahito_original_base_scale_saved";
    private static final ResourceLocation TRUE_ADV = new ResourceLocation("abyss", "true_form");
    private static final String UNLOCK_TRUE = "abyss_mahito_unlock_true_form";
    private static final String UNLOCK_MULTIPLICITY = "abyss_mahito_unlock_multiplicity";
    private static final String UNLOCK_POLYMORPHIC = "abyss_mahito_unlock_polymorphic";
    private static final String UNLOCK_DOMAIN = "abyss_mahito_unlock_domain";
    private static final String ORIGINAL_MOTION = "abyss_mahito_original_motion";
    private static final String MOTION_SAVED = "abyss_mahito_motion_saved";
    private static final String TEMP_ARMOR_TAG = "abyss_mahito_generated_armor";

    private static final UUID SMALL_SPEED_UUID =
            UUID.fromString("03898458-6d58-4f5f-97b6-10f33d2a8704");

    // Additional Abyss buffs, on top of Jujutsucraft's existing form effect.
    private static final UUID ATTACK_UUID = UUID.fromString("03898458-6d58-4f5f-97b6-10f33d2a8701");
    private static final UUID ARMOR_UUID = UUID.fromString("03898458-6d58-4f5f-97b6-10f33d2a8702");
    private static final UUID TOUGH_UUID = UUID.fromString("03898458-6d58-4f5f-97b6-10f33d2a8703");

    private MahitoForms() {}

    public static boolean isMahito(Entity entity) {
        if (entity == null) return false;
        var vars = entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY)
                .orElse(null);
        if (vars == null) return false;
        double ct = vars.SecondTechnique ? vars.PlayerCurseTechnique2 : vars.PlayerCurseTechnique;
        return (int) Math.round(ct) == 15;
    }

    private static boolean hasAdvancement(ServerPlayer player, ResourceLocation id) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(id);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    public static boolean hasTrueForm(ServerPlayer player) {
        return hasAdvancement(player, TRUE_ADV);
    }

    /** Server checks the real advancements; client reads their synced flags. */
    public static boolean unlocked(Entity entity, int slot) {
        if (entity instanceof ServerPlayer player) {
            return switch (slot) {
                case 15 -> hasTrueForm(player);
                case 8, 9 -> hasAdvancement(player, new ResourceLocation("jujutsucraft", "skill_soul_multiplicity"));
                case 10 -> hasAdvancement(player, new ResourceLocation("jujutsucraft", "skill_polymorphic_soul_isomer"));
                case 20 -> hasAdvancement(player, new ResourceLocation("jujutsucraft", "mastery_domain_expansion"));
                default -> true;
            };
        }
        var data = entity.getPersistentData();
        return switch (slot) {
            case 15 -> data.getBoolean(UNLOCK_TRUE);
            case 8, 9 -> data.getBoolean(UNLOCK_MULTIPLICITY);
            case 10 -> data.getBoolean(UNLOCK_POLYMORPHIC);
            case 20 -> data.getBoolean(UNLOCK_DOMAIN);
            default -> true;
        };
    }

    public static boolean blades(Entity entity) {
        return isMahito(entity) && !trueForm(entity)
                && entity.getPersistentData().getBoolean(BLADES_KEY);
    }

    public static boolean diminution(Entity entity) {
        return isMahito(entity) && !trueForm(entity)
                && entity.getPersistentData().getBoolean(SMALL_KEY);
    }

    public static boolean trueForm(Entity entity) {
        return isMahito(entity) && entity.getPersistentData().getBoolean(TRUE_KEY);
    }

    /** Only invoke from the server's technique-key press, once per press. */
    public static void toggle(ServerPlayer player, int slot) {
        if (!isMahito(player) || !player.isAlive()) return;
        if (slot != BLADES && slot != DIMINUTION && slot != TRUE_FORM) return;

        CompoundTag data = player.getPersistentData();
        if (slot == TRUE_FORM) {
            if (data.getBoolean(TRUE_KEY)) {
                disableTrueForm(player);
                refreshSelection(player);
                AbilityScanHandler.scanAndSync(player);
                sync(player);
                return;
            }
            if (!hasTrueForm(player)) {
                player.displayClientMessage(Component.literal("Unlock True Form by landing two consecutive Black Flashes."), true);
                return;
            }
            if (!spendCE(player, 300.0D)) return;

            // Both other transformations are removed, even when combined.
            data.putBoolean(BLADES_KEY, false);
            disableDiminution(player);
            data.putBoolean(TRUE_KEY, true);
            player.removeEffect(
                    JujutsucraftModMobEffects.INSTANT_SPIRIT_BODYOF_DISTORTED_KILLING_EFFECT.get()
            );
            applyModifiers(player);
            equipTrueFormArmor(player);
        } else {
            if (data.getBoolean(TRUE_KEY)) return;
            String key = slot == BLADES ? BLADES_KEY : SMALL_KEY;
            if (data.getBoolean(key)) {
                if (slot == DIMINUTION) disableDiminution(player);
                else data.putBoolean(BLADES_KEY, false);
            } else {
                if (!spendCE(player, 100.0D)) return;
                if (slot == BLADES) data.putBoolean(BLADES_KEY, true);
                else enableDiminution(player);
            }
        }
        refreshSelection(player);
        AbilityScanHandler.scanAndSync(player);
        sync(player);
    }

    public static void onBlackFlashSuccess(ServerPlayer player, int consecutive) {
        if (consecutive < 2 || !isMahito(player) || hasTrueForm(player)) return;
        Advancement a = player.server.getAdvancements().getAdvancement(TRUE_ADV);
        if (a == null) return;
        for (String criterion : player.getAdvancements().getOrStartProgress(a).getRemainingCriteria()) {
            player.getAdvancements().award(a, criterion);
        }
        AbilityScanHandler.scanAndSync(player);
        sync(player);
    }

    private static void sync(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new PacketSyncMahitoForms(data.getBoolean(BLADES_KEY),
                        data.getBoolean(SMALL_KEY), data.getBoolean(TRUE_KEY),
                        unlocked(player, 15), unlocked(player, 8),
                        unlocked(player, 10), unlocked(player, 20)));
    }

    private static boolean spendCE(ServerPlayer player, double cost) {
        if (player.isCreative()) return true;
        var vars = player.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY)
                .orElse(null);
        if (vars == null) return false;
        if (vars.PlayerCursePower < cost) {
            player.displayClientMessage(Component.literal("Not enough cursed energy."), true);
            return false;
        }
        vars.PlayerCursePower -= cost;
        vars.syncPlayerVariables(player);
        return true;
    }

    private static void refreshSelection(ServerPlayer player) {
        var vars = player.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY)
                .orElse(null);
        if (vars == null) return;
        vars.noChangeTechnique = true;
        KeyChangeTechniqueOnKeyPressedProcedure.execute(player.serverLevel(),
                player.getX(), player.getY(), player.getZ(), player);
    }

    private static void enableDiminution(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();

        ScaleData size = ScaleTypes.BASE.getScaleData(player);
        ScaleData motion = ScaleTypes.MOTION.getScaleData(player);

        if (!data.getBoolean(SIZE_SAVED)) {
            data.putFloat(ORIGINAL_SIZE, size.getBaseScale());
            data.putBoolean(SIZE_SAVED, true);
        }

        if (!data.getBoolean(MOTION_SAVED)) {
            data.putFloat(ORIGINAL_MOTION, motion.getBaseScale());
            data.putBoolean(MOTION_SAVED, true);
        }

        // Half-size player.
        size.setScale(
                Math.max(0.05F, data.getFloat(ORIGINAL_SIZE) * 0.5F)
        );

        // Compensate for Pehkui's BASE movement reduction.
        // This also avoids reducing jump movement.
        motion.setScale(data.getFloat(ORIGINAL_MOTION) * 2.0F);

        // Slightly slower walking/running, without reducing jump height.
        AttributeInstance speed =
                player.getAttribute(Attributes.MOVEMENT_SPEED);

        if (speed != null && speed.getModifier(SMALL_SPEED_UUID) == null) {
            speed.addTransientModifier(new AttributeModifier(
                    SMALL_SPEED_UUID,
                    "Abyss Mahito diminution speed",
                    -0.10D,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }

        data.putBoolean(SMALL_KEY, true);
    }

    private static void disableDiminution(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        data.putBoolean(SMALL_KEY, false);

        if (data.getBoolean(SIZE_SAVED)) {
            ScaleTypes.BASE.getScaleData(player)
                    .setScale(data.getFloat(ORIGINAL_SIZE));

            data.remove(ORIGINAL_SIZE);
            data.remove(SIZE_SAVED);
        }

        if (data.getBoolean(MOTION_SAVED)) {
            ScaleTypes.MOTION.getScaleData(player)
                    .setScale(data.getFloat(ORIGINAL_MOTION));

            data.remove(ORIGINAL_MOTION);
            data.remove(MOTION_SAVED);
        }

        AttributeInstance speed =
                player.getAttribute(Attributes.MOVEMENT_SPEED);

        if (speed != null && speed.getModifier(SMALL_SPEED_UUID) != null) {
            speed.removeModifier(SMALL_SPEED_UUID);
        }
    }

    private static void addModifier(AttributeInstance attribute, UUID id, String name, double amount) {
        if (attribute == null || attribute.getModifier(id) != null) return;
        attribute.addTransientModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
    }

    private static void applyModifiers(ServerPlayer player) {
        addModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_UUID,
                "Abyss Mahito true form attack", 12.0D);
        addModifier(player.getAttribute(Attributes.ARMOR), ARMOR_UUID,
                "Abyss Mahito true form armor", 12.0D);
        addModifier(player.getAttribute(Attributes.ARMOR_TOUGHNESS), TOUGH_UUID,
                "Abyss Mahito true form toughness", 4.0D);
    }

    private static void removeModifier(AttributeInstance a, UUID id) {
        if (a != null && a.getModifier(id) != null) a.removeModifier(id);
    }

    private static void removeModifiers(ServerPlayer player) {
        removeModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_UUID);
        removeModifier(player.getAttribute(Attributes.ARMOR), ARMOR_UUID);
        removeModifier(player.getAttribute(Attributes.ARMOR_TOUGHNESS), TOUGH_UUID);
    }
    private static final EquipmentSlot[] TRUE_FORM_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS
    };

    private static ItemStack createTrueFormArmor(EquipmentSlot slot) {
        ItemStack stack = switch (slot) {
            case HEAD -> new ItemStack(
                    JujutsucraftModItems
                            .ARMOR_INSTANT_SPIRIT_BODYOF_DISTORTED_KILLING_HELMET.get());

            case CHEST -> new ItemStack(
                    JujutsucraftModItems
                            .ARMOR_INSTANT_SPIRIT_BODYOF_DISTORTED_KILLING_CHESTPLATE.get());

            case LEGS -> new ItemStack(
                    JujutsucraftModItems
                            .ARMOR_INSTANT_SPIRIT_BODYOF_DISTORTED_KILLING_LEGGINGS.get());

            default -> ItemStack.EMPTY;
        };

        if (!stack.isEmpty()) {
            stack.getOrCreateTag().putBoolean(TEMP_ARMOR_TAG, true);
        }

        return stack;
    }

    private static boolean isTemporaryArmor(ItemStack stack) {
        return !stack.isEmpty()
                && stack.hasTag()
                && stack.getTag().getBoolean(TEMP_ARMOR_TAG);
    }

    // Remove spare temporary armor if it was moved into inventory.
    private static void clearTemporaryCopies(ServerPlayer player) {
        var inv = player.getInventory();
        boolean changed = false;

        for (int i = 0; i < inv.items.size(); i++) {
            if (isTemporaryArmor(inv.items.get(i))) {
                inv.items.set(i, ItemStack.EMPTY);
                changed = true;
            }
        }

        for (int i = 0; i < inv.offhand.size(); i++) {
            if (isTemporaryArmor(inv.offhand.get(i))) {
                inv.offhand.set(i, ItemStack.EMPTY);
                changed = true;
            }
        }

        if (changed) inv.setChanged();
    }

    private static void equipTrueFormArmor(ServerPlayer player) {
        clearTemporaryCopies(player);

        for (EquipmentSlot slot : TRUE_FORM_SLOTS) {
            ItemStack expected = createTrueFormArmor(slot);
            ItemStack current = player.getItemBySlot(slot);

            // Already wearing the correct temporary armor.
            if (isTemporaryArmor(current)
                    && current.is(expected.getItem())) {
                continue;
            }

            // Preserve normal armor by moving it into the inventory.
            if (!current.isEmpty() && !isTemporaryArmor(current)) {
                ItemStack displaced = current.copy();

                player.setItemSlot(slot, ItemStack.EMPTY);
                player.getInventory().add(displaced);

                if (!displaced.isEmpty()) {
                    player.drop(displaced, false);
                }
            }

            player.setItemSlot(slot, expected);
        }
    }

    private static void removeTrueFormArmor(ServerPlayer player) {
        for (EquipmentSlot slot : TRUE_FORM_SLOTS) {
            if (isTemporaryArmor(player.getItemBySlot(slot))) {
                player.setItemSlot(slot, ItemStack.EMPTY);
            }
        }

        clearTemporaryCopies(player);
    }

    // Temporary transformation armor must not become death loot.
    @SubscribeEvent
    public static void onTrueFormArmorDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;

        event.getDrops().removeIf(
                drop -> isTemporaryArmor(drop.getItem())
        );
    }
    private static void disableTrueForm(ServerPlayer player) {
        player.getPersistentData().putBoolean(TRUE_KEY, false);

        player.removeEffect(
                JujutsucraftModMobEffects.INSTANT_SPIRIT_BODYOF_DISTORTED_KILLING_EFFECT.get()
        );

        removeModifiers(player);
        removeTrueFormArmor(player);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        CompoundTag data = player.getPersistentData();
        if (!player.isAlive() || !isMahito(player)) {
            if (data.getBoolean(SMALL_KEY)
        || data.getBoolean(SIZE_SAVED)
        || data.getBoolean(MOTION_SAVED)) disableDiminution(player);
            data.putBoolean(BLADES_KEY, false);
            if (data.getBoolean(TRUE_KEY)) disableTrueForm(player);
            if (player.tickCount % 20 == 0) sync(player);
            return;
        }
        if (data.getBoolean(TRUE_KEY)) {
            if (data.getBoolean(SMALL_KEY)
        || data.getBoolean(SIZE_SAVED)
        || data.getBoolean(MOTION_SAVED)) disableDiminution(player);
            data.putBoolean(BLADES_KEY, false);
// Remove any leftover potion effect from the older implementation.
            if (player.hasEffect(
                    JujutsucraftModMobEffects.INSTANT_SPIRIT_BODYOF_DISTORTED_KILLING_EFFECT.get())) {
                player.removeEffect(
                        JujutsucraftModMobEffects.INSTANT_SPIRIT_BODYOF_DISTORTED_KILLING_EFFECT.get()
                );
            }

            applyModifiers(player);
            equipTrueFormArmor(player);
        } else {
            removeModifiers(player);
        }
        if (player.tickCount % 20 == 0) sync(player);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag old = event.getOriginal().getPersistentData();
        // Transformation is temporary; don't let it persist onto a fresh body.
        player.getPersistentData().putBoolean(BLADES_KEY, false);
        player.getPersistentData().putBoolean(SMALL_KEY, false);
        player.getPersistentData().putBoolean(TRUE_KEY, false);
        if (old.getBoolean(SIZE_SAVED)) {
            ScaleTypes.BASE.getScaleData(player)
                    .setScale(old.getFloat(ORIGINAL_SIZE));
        }

        if (old.getBoolean(MOTION_SAVED)) {
            ScaleTypes.MOTION.getScaleData(player)
                    .setScale(old.getFloat(ORIGINAL_MOTION));
        }

        AttributeInstance speed =
                player.getAttribute(Attributes.MOVEMENT_SPEED);

        if (speed != null && speed.getModifier(SMALL_SPEED_UUID) != null) {
            speed.removeModifier(SMALL_SPEED_UUID);
        }

        removeModifiers(player);
    }
}
