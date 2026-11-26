package com.ghosts.abyss.domain.higuruma;

import com.ghosts.abyss.domain.DomainDimension;
import com.ghosts.abyss.technique.TechniqueCooldownManager;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/** Applies completed courtroom verdicts to returned entities, by UUID. */
@Mod.EventBusSubscriber(modid = "abyss")
public final class HigurumaPunishments {
    private HigurumaPunishments() {}

    private static final int DURATION = 20 * 90;
    private static final String DATA_NAME = "abyss_higuruma_punishments";
    private static final TagKey<Item> CURSED_TOOL = forgeTag("cursed_tool");
    private static final List<TagKey<Item>> WEAPON_TAGS = List.of(
            forgeTag("whip"), forgeTag("sword"), forgeTag("spear"), forgeTag("rifle"));
    private static final Set<ResourceLocation> GAVELS = Set.of(
            jc("gavel"), jc("gavel_long"), jc("gavel_big"), jc("executioners_sword"));

    private static ResourceLocation jc(String name) {
        return new ResourceLocation("jujutsucraft", name);
    }

    private static TagKey<Item> forgeTag(String name) {
        return ItemTags.create(new ResourceLocation("forge", name));
    }

    // World-saved queue: pending verdicts and held tools survive logout/restart.
    // Do not retain references to entities across dimension transfers.
    private static final class Data extends SavedData {
        final ListTag jobs = new ListTag();

        static Data load(CompoundTag tag) {
            Data data = new Data();
            data.jobs.addAll(tag.getList("jobs", Tag.TAG_COMPOUND));
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.put("jobs", jobs.copy());
            return tag;
        }
    }

    private static Data data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                Data::load, Data::new, DATA_NAME);
    }

    public static void queue(MinecraftServer server, UUID owner,
                             Map<UUID, HigurumaCourtroom.Verdict> verdicts) {
        if (verdicts.isEmpty()) return;
        Data data = data(server);
        ListTag targets = new ListTag();
        for (var entry : verdicts.entrySet()) {
            UUID target = entry.getKey();
            if (target.equals(owner)) continue;
            switch (entry.getValue()) {
                case NOT_GUILTY -> { }
                case CONFISCATION -> {
                    CompoundTag job = new CompoundTag();
                    job.putString("kind", "confiscation");
                    job.putUUID("recipient", target);
                    data.jobs.add(job);
                }
                case DEATH_SENTENCE -> {
                    CompoundTag entryTag = new CompoundTag();
                    entryTag.putUUID("target", target);
                    targets.add(entryTag);
                }
            }
        }
        if (!targets.isEmpty()) {
            CompoundTag job = new CompoundTag();
            job.putString("kind", "sword");
            job.putUUID("recipient", owner);
            job.put("targets", targets);
            data.jobs.add(job);
        }
        data.setDirty();
    }

    private static LivingEntity findReturned(MinecraftServer server, UUID id) {
        // Player lookup also handles respawned instances with the same UUID.
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player != null) {
            return player.level().dimension().equals(DomainDimension.ID) ? null : player;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().equals(DomainDimension.ID)) continue;
            Entity entity = level.getEntity(id);
            if (entity instanceof LivingEntity living) return living;
        }
        return null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        // No need to scan every tick; effects start within one second of return.
        if (server.getTickCount() % 20 != 0) return;
        Data data = data(server);
        long now = server.overworld().getGameTime();
        for (int i = data.jobs.size() - 1; i >= 0; i--) {
            CompoundTag job = data.jobs.getCompound(i);
            if (!job.hasUUID("recipient")) {
                data.jobs.remove(i);
                data.setDirty();
                continue;
            }
            String kind = job.getString("kind");
            if (kind.equals("return_tool") && now < job.getLong("return_at")) continue;
            LivingEntity recipient = findReturned(server, job.getUUID("recipient"));
            if (recipient == null || !recipient.isAlive() || recipient.isDeadOrDying()
                    || recipient.getPersistentData().getBoolean("abyss_domain_transferring")) continue;

            switch (kind) {
                case "sword" -> {
                    if (!awardSword(recipient, job.getList("targets", Tag.TAG_COMPOUND))) continue;
                }
                case "confiscation" -> {
                    ItemStack held = removeCursedTool(recipient);
                    if (!held.isEmpty()) {
                        // Transform the same queue entry into an item escrow record.
                        // Full item NBT, count, enchantments and durability are retained.
                        job.putString("kind", "return_tool");
                        job.put("item", held.save(new CompoundTag()));
                        job.putLong("return_at", now + DURATION);
                        notify(recipient, "Your cursed tools have been confiscated", ChatFormatting.YELLOW);
                        data.setDirty();
                        continue;
                    }
                    applySkillConfiscation(recipient);
                    notify(recipient, "Your cursed technique has been confiscated", ChatFormatting.YELLOW);
                }
                case "return_tool" -> {
                    ItemStack original = ItemStack.of(job.getCompound("item"));
                    if (!original.isEmpty()) giveBack(recipient, original);
                    notify(recipient, "", ChatFormatting.GREEN);
                }
                default -> { }
            }
            data.jobs.remove(i);
            data.setDirty();
        }
    }

    private static boolean awardSword(LivingEntity owner, ListTag targets) {
        Item swordItem = ForgeRegistries.ITEMS.getValue(jc("executioners_sword"));
        if (swordItem == null || swordItem == Items.AIR) return false;
        ItemStack sword = new ItemStack(swordItem);
        CompoundTag tag = sword.getOrCreateTag();
        // Exact JC format, including string (not NBT UUID-array) values.
        tag.putString("OWNER_UUID", owner.getUUID().toString());
        int index = 1;
        Set<UUID> distinct = new LinkedHashSet<>();
        for (int i = 0; i < targets.size() && index <= 200; i++) {
            CompoundTag target = targets.getCompound(i);
            if (!target.hasUUID("target")) continue;
            UUID id = target.getUUID("target");
            if (distinct.add(id)) tag.putString("TARGET" + index++, id.toString());
        }
        if (distinct.isEmpty()) return true;
        if (owner instanceof ServerPlayer player) {
            // Same priority as JC: turn a gavel into the sword, then check inventory.
            if (isGavel(player.getMainHandItem())) player.setItemInHand(InteractionHand.MAIN_HAND, sword);
            else if (isGavel(player.getOffhandItem())) player.setItemInHand(InteractionHand.OFF_HAND, sword);
            else {
                boolean replaced = false;
                for (int i = 0; i < player.getInventory().items.size(); i++) {
                    if (isGavel(player.getInventory().items.get(i))) {
                        player.getInventory().setItem(i, sword);
                        replaced = true;
                        break;
                    }
                }
                if (!replaced) ItemHandlerHelper.giveItemToPlayer(player, sword);
            }
            player.getInventory().setChanged();
            player.getCooldowns().removeCooldown(swordItem);
        } else {
            ItemStack old = owner.getMainHandItem().copy();
            if (!old.isEmpty() && !isGavel(old)) {
                if (owner.getOffhandItem().isEmpty()) owner.setItemInHand(InteractionHand.OFF_HAND, old);
                else owner.spawnAtLocation(old);
            }
            owner.setItemInHand(InteractionHand.MAIN_HAND, sword);
        }
        notify(owner, "", ChatFormatting.GOLD);
        return true;
    }

    private static boolean isGavel(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return !stack.isEmpty() && id != null && GAVELS.contains(id);
    }

    private static boolean isCursedWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!stack.is(CURSED_TOOL) && (stack.getTag() == null
                || stack.getTag().getDouble("CursePower") == 0.0D)) return false;
        Item item = stack.getItem();
        for (TagKey<Item> tag : WEAPON_TAGS) if (stack.is(tag)) return true;
        return item instanceof SwordItem || item instanceof AxeItem
                || item instanceof PickaxeItem || item instanceof ShovelItem
                || item instanceof TridentItem || item instanceof HoeItem
                || item instanceof BowItem || item instanceof CrossbowItem;
    }

    private static ItemStack removeCursedTool(LivingEntity recipient) {
        // One weapon per defendant, with held weapons preferred.
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = recipient.getItemInHand(hand);
            if (!isCursedWeapon(stack)) continue;
            ItemStack copy = stack.copy();
            recipient.setItemInHand(hand, ItemStack.EMPTY);
            if (recipient instanceof ServerPlayer p) p.getInventory().setChanged();
            return copy;
        }
        if (recipient instanceof ServerPlayer player) {
            for (int i = 0; i < player.getInventory().items.size(); i++) {
                ItemStack stack = player.getInventory().items.get(i);
                if (!isCursedWeapon(stack)) continue;
                ItemStack copy = stack.copy();
                player.getInventory().setItem(i, ItemStack.EMPTY);
                player.getInventory().setChanged();
                return copy;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void giveBack(LivingEntity recipient, ItemStack original) {
        if (recipient instanceof ServerPlayer player) {
            ItemHandlerHelper.giveItemToPlayer(player, original);
            player.getInventory().setChanged();
        } else if (recipient.getMainHandItem().isEmpty()) {
            recipient.setItemInHand(InteractionHand.MAIN_HAND, original);
        } else if (recipient.getOffhandItem().isEmpty()) {
            recipient.setItemInHand(InteractionHand.OFF_HAND, original);
        } else {
            recipient.spawnAtLocation(original);
        }
    }

    private static void applySkillConfiscation(LivingEntity recipient) {
        // Reuse Abyss's own nonphysical-skill gate as well as JC's cooldowns.
        TechniqueCooldownManager.startPostDomainCooldown(recipient, DURATION);
        addEffect(recipient, "unstable", 0);
        addEffect(recipient, "cooldown_time", 0);
        addEffect(recipient, "cooldown_time_combat", 0);
    }

    private static void addEffect(LivingEntity entity, String name, int amplifier) {
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(jc(name));
        if (effect != null) entity.addEffect(new MobEffectInstance(effect, DURATION,
                amplifier, false, false, true));
    }

    private static void notify(LivingEntity recipient, String message, ChatFormatting color) {
        if (recipient instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.literal(message).withStyle(color));
        }
    }
}
