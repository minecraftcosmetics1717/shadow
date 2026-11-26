package com.ghosts.abyss.domain;

import com.ghosts.abyss.Abyss;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.util.*;

/** Stage 2 belongs to the existing arena; it is not a new domain instance. */
@Mod.EventBusSubscriber(modid = "abyss")
public final class HakariSubwayStage {
    private static final ResourceLocation RESOURCE = new ResourceLocation("abyss", "structures/subway.nbt");
    private static final int WIDTH = 42, HEIGHT = 13, LENGTH = 101;
    private static final int HEIGHT_OFFSET = 80;
    private static final String PLAYER_MARK = "abyss_hakari_subway";
    private static final Map<DomainInstance, Stage> STAGES = new IdentityHashMap<>();
    // A broken resource should be logged once per domain, not retried every tick.
    private static final Set<DomainInstance> FAILED_BUILDS =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private static ServerLevel clearingLevel;
    private static Stage clearingStage;
    private static final ResourceLocation NEUTRALIZATION = new ResourceLocation("jujutsucraft", "neutralization");

    private HakariSubwayStage() {}

    private static final class Stage {
        final BlockPos origin;
        final UUID session = UUID.randomUUID();
        final Map<UUID, Vec3> basePositions = new LinkedHashMap<>();
        HakariScenarioManager.Scene scene;
        List<Vec3> spawnPositions = List.of();
        boolean active;
        Stage(DomainInstance inst) {
            origin = inst.arenaCenter.offset(-WIDTH / 2, HEIGHT_OFFSET, -LENGTH / 2);
        }
        AABB bounds() {
            return new AABB(origin.getX() - 1, origin.getY() - 1, origin.getZ() - 1,
                    origin.getX() + WIDTH + 1, origin.getY() + HEIGHT + 1,
                    origin.getZ() + LENGTH + 1);
        }
        Vec3 platform(int x, int z) {
            return new Vec3(origin.getX() + x + 0.5, origin.getY() + 4.0, origin.getZ() + z + 0.5);
        }
    }

    public static boolean isActive(DomainInstance inst) {
        Stage stage = STAGES.get(inst);
        return stage != null && stage.active;
    }

    /** Used by the existing arena lookup (HUD, clashes, and similar systems). */
    public static DomainInstance findContaining(Vec3 pos) {
        for (var entry : STAGES.entrySet()) {
            if (entry.getValue().active && entry.getValue().bounds().contains(pos)
                    && DomainInstanceManager.getBySlot(entry.getKey().slot) == entry.getKey()) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static Vec3 center(DomainInstance inst) {
        return new Vec3(inst.arenaCenter.getX() + 0.5, inst.arenaCenter.getY() + 1.1,
                inst.arenaCenter.getZ() + 0.5);
    }
    private static List<LivingEntity> baseParticipants(ServerLevel level, DomainInstance inst) {
        Vec3 center = center(inst);
        double r = DomainDimension.INNER_RADIUS;
        return level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center.x-r, center.y-r, center.z-r, center.x+r, center.y+r, center.z+r),
                e -> eligible(e) && e.position().distanceToSqr(center) <= r*r);
    }
    private static boolean eligible(LivingEntity e) {
        return e.isAlive() && !e.isSpectator() && !e.getPersistentData().getBoolean("abyss_domain_decoration");
    }

    /** Called when the card sequence finishes. A failed load leaves Stage 1 intact. */
    public static boolean enter(LivingEntity owner, DomainInstance inst, boolean jackpot) {
        if (!(owner.level() instanceof ServerLevel level) || inst == null || inst.skillDomain != 29.0D
                || inst.participants.size() != 1 || !inst.ownerEntryConfirmed) return false;
        Stage stage = ensureBuilt(level, inst);
        if (stage == null) return false;
        if (stage.active) return true;
        stage.scene = HakariScenarioManager.start(level, stage.origin, jackpot);
        if (stage.scene == null) return false;
        stage.active = true;
        stage.spawnPositions = stage.scene.participantSpawns();
        stage.basePositions.clear();
        List<LivingEntity> captured = new ArrayList<>(baseParticipants(level, inst));
        if (!captured.contains(owner)) captured.add(owner);
        captured.sort(Comparator.comparing(e -> e.getUUID().equals(owner.getUUID()) ? 0 : 1));
        for (LivingEntity entity : captured) {
            if (!moveIntoStage(level, inst, stage, entity) && entity == owner) {
                // An oversized owner must not start a scene they cannot enter safely.
                returnToBase(level, inst);
                return false;
            }
        }
        return true;
    }

    /** Prepare one station per domain session, independent of the current Riichi. */
    private static Stage ensureBuilt(ServerLevel level, DomainInstance inst) {
        Stage existing = STAGES.get(inst);
        if (existing != null) return existing;
        if (FAILED_BUILDS.contains(inst)) return null;
        Stage stage = new Stage(inst);
        try {
            build(level, stage);
            STAGES.put(inst, stage);
            return stage;
        } catch (Exception ex) {
            FAILED_BUILDS.add(inst);
            Abyss.LOGGER.error("[Hakari Subway] Could not place subway; keeping original roll", ex);
            return null;
        }
    }

    private static void build(ServerLevel level, Stage stage) throws Exception {
        CompoundTag root;
        try (InputStream in = level.getServer().getResourceManager().getResourceOrThrow(RESOURCE).open()) {
            root = NbtIo.readCompressed(in);
        }
        ListTag size = root.getList("size", Tag.TAG_INT);
        if (size.size() != 3 || size.getInt(0) != WIDTH || size.getInt(1) != HEIGHT || size.getInt(2) != LENGTH)
            throw new IllegalStateException("Unexpected subway NBT size");
        if (stage.origin.getY() - 1 < level.getMinBuildHeight() || stage.origin.getY() + HEIGHT >= level.getMaxBuildHeight())
            throw new IllegalStateException("Subway is outside dimension build height");

        // Do not silently replace missing modded blocks with air.
        for (Tag raw : root.getList("palette", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            ResourceLocation id = new ResourceLocation(entry.getString("Name"));
            if (!ForgeRegistries.BLOCKS.containsKey(id)) throw new IllegalStateException("Missing subway block: " + id);
            Block block = ForgeRegistries.BLOCKS.getValue(id);
            CompoundTag properties = entry.getCompound("Properties");
            for (String name : properties.getAllKeys()) {
                Property<?> property = block.getStateDefinition().getProperty(name);
                if (property == null || property.getValue(properties.getString(name)).isEmpty())
                    throw new IllegalStateException("Incompatible subway block state: " + id + " " + name + "=" + properties.getString(name));
            }
        }
        // Newer structure files may store blank sign text as an empty string.
        // Minecraft 1.20.1 expects a JSON text component for each sign line.
        // Preserve the original NBT resource; normalize blank lines only in memory.
        for (Tag raw : root.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag blockData = ((CompoundTag) raw).getCompound("nbt");
            for (String side : new String[]{"front_text", "back_text"}) {
                CompoundTag signText = blockData.getCompound(side);
                for (String key : new String[]{"messages", "filtered_messages"}) {
                    ListTag lines = signText.getList(key, Tag.TAG_STRING);
                    for (int i = 0; i < lines.size(); i++) {
                        if (lines.getString(i).isEmpty()) {
                            lines.set(i, StringTag.valueOf("\"\""));
                        }
                    }
                }
            }
        }

        // Read the actual palette directly with 1.20.1's block registry, instead
        // of asking the data fixer to downgrade a newer-version structure.
        StructureTemplate template = new StructureTemplate();
        template.load(BuiltInRegistries.BLOCK.asLookup(), root);
        DomainDimBlockGuard.open();
        try {
            eraseStage(level, stage);
            boolean placed = template.placeInWorld(level, stage.origin, stage.origin,
                    new StructurePlaceSettings().setIgnoreEntities(true), level.random, 2);
            if (!placed) throw new IllegalStateException("StructureTemplate.placeInWorld returned false");
            // The invisible shell prevents flying or falling out of the station.
            fillStage(level, stage, true);
        } catch (Exception ex) {
            eraseStage(level, stage);
            throw ex;
        } finally {
            DomainDimBlockGuard.close();
        }
    }

    private static void fillStage(ServerLevel level, Stage stage, boolean shellOnly) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x=-1; x<=WIDTH; x++) for (int z=-1; z<=LENGTH; z++) for (int y=-1; y<=HEIGHT; y++) {
            boolean edge = x==-1 || x==WIDTH || z==-1 || z==LENGTH || y==-1 || y==HEIGHT;
            if (shellOnly && !edge) continue;
            pos.set(stage.origin.getX()+x, stage.origin.getY()+y, stage.origin.getZ()+z);
            if (!shellOnly && level.getBlockState(pos).isAir()) continue;
            // UPDATE_KNOWN_SHAPE prevents neighboring support-dependent blocks
            // from breaking themselves; UPDATE_SUPPRESS_DROPS prevents shape-update loot.
            int flags = shellOnly ? Block.UPDATE_CLIENTS :
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
            level.setBlock(pos, shellOnly ? Blocks.BARRIER.defaultBlockState() : Blocks.AIR.defaultBlockState(), flags);
        }
    }

    /** Only suppress item entities created synchronously by station removal. */
    private static void eraseStage(ServerLevel level, Stage stage) {
        ServerLevel previousLevel = clearingLevel;
        Stage previousStage = clearingStage;
        clearingLevel = level;
        clearingStage = stage;
        try {
            fillStage(level, stage, false);
        } finally {
            clearingLevel = previousLevel;
            clearingStage = previousStage;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void suppressCleanupDrops(EntityJoinLevelEvent event) {
        Stage stage = clearingStage;
        if (stage == null || event.getLevel() != clearingLevel) return;
        if (event.getEntity() instanceof ItemEntity item
                && stage.bounds().inflate(2.0D).contains(item.position())) {
            event.setCanceled(true);
        }
    }

    /** Advance exactly once per gambling-manager tick, not on a separate timer. */
    public static boolean tickScenario(ServerLevel level, DomainInstance inst) {
        Stage stage = STAGES.get(inst);
        return stage == null || !stage.active || stage.scene == null || stage.scene.tick(level);
    }

    private static void removeActors(ServerLevel level, Stage stage) {
        if (stage.scene != null) {
            stage.scene.clear(level);
            stage.scene = null;
        }
    }

    private static Vec3 sceneSpawn(ServerLevel level, Stage stage, LivingEntity entity) {
        for (Vec3 p : stage.spawnPositions) {
            if (!stage.bounds().contains(p) || !canFit(level, entity, p)) continue;
            BlockPos below = BlockPos.containing(p).below();
            if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) continue;
            // noCollision alone doesn't reliably reject overlapping living entities.
            // Explicitly reserve space around the actual hitbox of each arrival.
            AABB target = entity.getBoundingBox().move(p.subtract(entity.position()));
            if (!level.getEntitiesOfClass(LivingEntity.class, target.inflate(0.5D, 0.0D, 0.5D),
                    other -> other != entity && eligible(other)).isEmpty()) continue;
            return p;
        }
        // No shared fallback position. Leave the entity in Stage 1 and retry.
        return null;
    }

    private static boolean moveIntoStage(ServerLevel level, DomainInstance inst, Stage stage, LivingEntity entity) {
        if (!eligible(entity)) return false;
        if (stage.basePositions.containsKey(entity.getUUID())) return true;
        Vec3 destination = sceneSpawn(level, stage, entity);
        if (destination == null) return false;
        stage.basePositions.put(entity.getUUID(), entity.position());
        if (entity instanceof ServerPlayer player) {
            CompoundTag mark = new CompoundTag();
            mark.putUUID("session", stage.session);
            mark.putInt("slot", inst.slot);
            mark.putString("dimension", inst.returnDim.location().toString());
            Vec3 original = inst.returnPositions.getOrDefault(player.getUUID(), inst.overworldCenter);
            mark.putDouble("x", original.x); mark.putDouble("y", original.y); mark.putDouble("z", original.z);
            player.getPersistentData().put(PLAYER_MARK, mark);
        }
        move(level, entity, destination);
        return true;
    }

    private static boolean canFit(ServerLevel level, LivingEntity e, Vec3 p) {
        return level.noCollision(e, e.getBoundingBox().move(p.subtract(e.position())));
    }
    private static void move(ServerLevel level, LivingEntity entity, Vec3 p) {
        entity.stopRiding();
        entity.ejectPassengers();
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(level, p.x, p.y, p.z, player.getYRot(), player.getXRot());
        } else {
            entity.teleportTo(p.x, p.y, p.z);
        }
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0;
    }

    /** Called on failure, on clash entry, and BEFORE every normal arena evacuation. */
    public static void returnToBase(ServerLevel level, DomainInstance inst) {
        Stage stage = STAGES.get(inst);
        if (stage == null || !stage.active) return;
        stage.active = false;
        removeActors(level, stage);
        Set<LivingEntity> participants = new LinkedHashSet<>(level.getEntitiesOfClass(LivingEntity.class, stage.bounds(), HakariSubwayStage::eligible));
        for (UUID id : stage.basePositions.keySet()) {
            Entity entity = level.getEntity(id);
            if (entity instanceof LivingEntity living && eligible(living)) participants.add(living);
        }
        for (LivingEntity living : participants) {
            Vec3 p = stage.basePositions.getOrDefault(living.getUUID(), center(inst));
            if (!canFit(level, living, p)) p = center(inst);
            move(level, living, p);
            living.getPersistentData().remove(PLAYER_MARK);
        }
        // Return dropped inventory and XP as well; base cleanup already knows how to handle items.
        Vec3 p = center(inst);
        for (Entity e : level.getEntities((Entity)null, stage.bounds(), e -> e instanceof ItemEntity || e instanceof ExperienceOrb)) {
            e.teleportTo(p.x, p.y, p.z);
            e.setDeltaMovement(Vec3.ZERO);
        }
        stage.basePositions.clear();
        stage.spawnPositions = List.of();
    }

    /** Called by normal arena cleanup, after evacuation has succeeded. */
    public static void clear(ServerLevel level, DomainInstance inst) {
        Stage stage = STAGES.get(inst);
        if (stage == null) return;
        returnToBase(level, inst);
        removeActors(level, stage);
        DomainDimBlockGuard.open();
        try { eraseStage(level, stage); }
        finally { DomainDimBlockGuard.close(); }
        STAGES.remove(inst);
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ServerLevel level = event.getServer().getLevel(DomainDimension.ID);
        if (level == null) return;

        FAILED_BUILDS.removeIf(inst -> DomainInstanceManager.getBySlot(inst.slot) != inst);
        // Build the station after the owner has finished entering Stage 1.
        // Actors still spawn only when enter() starts a Riichi scenario.
        for (int slot = 0; slot < DomainDimension.MAX_SLOTS; slot++) {
            DomainInstance inst = DomainInstanceManager.getBySlot(slot);
            if (inst == null || inst.skillDomain != 29.0D || !inst.ownerEntryConfirmed
                    || inst.participants.size() != 1 || STAGES.containsKey(inst)) continue;
            if (level.getEntity(inst.ownerId) instanceof LivingEntity owner && owner.isAlive()) {
                ensureBuilt(level, inst);
            }
        }
        for (DomainInstance inst : new ArrayList<>(STAGES.keySet())) {
            Stage stage = STAGES.get(inst);
            if (stage == null) continue;
            if (DomainInstanceManager.getBySlot(inst.slot) != inst) {
                // Normal end clears the stage before the slot is released.
                // Keep unexpected orphan sessions for safe recovery instead of deleting occupied space.
                continue;
            }
            if (!stage.active) continue;
            if (inst.participants.size() != 1 || inst.skillDomain != 29.0D) {
                returnToBase(level, inst);
                continue;
            }
            // Late entrants to the overworld barrier first arrive at Stage 1.
            for (LivingEntity e : baseParticipants(level, inst)) moveIntoStage(level, inst, stage, e);
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(NEUTRALIZATION);
            for (UUID id : new ArrayList<>(stage.basePositions.keySet())) {
                Entity entity = level.getEntity(id);
                if (!(entity instanceof LivingEntity living) || !eligible(living)) continue;
                if (!stage.bounds().contains(living.position())) {
                    Vec3 safe = sceneSpawn(level, stage, living);
                    if (safe != null) move(level, living, safe);
                    else {
                        // Stay in the base arena if no safe platform space is available.
                        Vec3 base = stage.basePositions.remove(id);
                        if (base == null || !canFit(level, living, base)) base = center(inst);
                        move(level, living, base);
                        living.getPersistentData().remove(PLAYER_MARK);
                        continue;
                    }
                }
                if (effect != null) {
                    MobEffectInstance current = living.getEffect(effect);
                    if (current == null || current.getDuration() <= 5)
                        living.addEffect(new MobEffectInstance(effect, 10, 0, false, false, false));
                }
            }
        }
    }

    @SubscribeEvent public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag data = player.getPersistentData();
        if (!data.contains(PLAYER_MARK, Tag.TAG_COMPOUND)) return;
        CompoundTag mark = data.getCompound(PLAYER_MARK);
        if (!player.level().dimension().equals(DomainDimension.ID)) { data.remove(PLAYER_MARK); return; }
        DomainInstance inst = DomainInstanceManager.getBySlot(mark.getInt("slot"));
        Stage stage = inst == null ? null : STAGES.get(inst);
        if (stage != null && mark.hasUUID("session") && mark.getUUID("session").equals(stage.session)) {
            if (stage.active) {
                Vec3 safe = sceneSpawn(player.serverLevel(), stage, player);
                if (safe != null) {
                    stage.basePositions.putIfAbsent(player.getUUID(), center(inst));
                    move(player.serverLevel(), player, safe);
                } else {
                    Vec3 base = stage.basePositions.remove(player.getUUID());
                    if (base == null || !canFit(player.serverLevel(), player, base)) base = center(inst);
                    move(player.serverLevel(), player, base);
                    data.remove(PLAYER_MARK);
                }
            } else {
                move(player.serverLevel(), player, center(inst));
                data.remove(PLAYER_MARK);
            }
            return;
        }
        // A disconnected participant must not remain above a deleted arena.
        ServerLevel destination = player.serverLevel().getServer().getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, new ResourceLocation(mark.getString("dimension"))));
        if (destination == null) destination = player.serverLevel().getServer().overworld();
        Vec3 p = new Vec3(mark.getDouble("x"), mark.getDouble("y"), mark.getDouble("z"));
        destination.getChunkAt(BlockPos.containing(p));
        if (!canFit(destination, player, p)) p = Vec3.atBottomCenterOf(destination.getSharedSpawnPos());
        move(destination, player, p);
        data.remove(PLAYER_MARK);
    }

    @SubscribeEvent public static void onStopping(ServerStoppingEvent event) {
        // Don't persist a station or actors after a normal server shutdown.
        ServerLevel level = event.getServer().getLevel(DomainDimension.ID);
        if (level != null) {
            for (DomainInstance inst : new ArrayList<>(STAGES.keySet())) clear(level, inst);
        }
        STAGES.clear();
        FAILED_BUILDS.clear();
    }
}
