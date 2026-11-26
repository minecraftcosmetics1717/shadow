package com.ghosts.abyss.domain.higuruma;

import com.ghosts.abyss.domain.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/** Server-authoritative courtroom and trial state. */
@Mod.EventBusSubscriber(modid = "abyss")
public final class HigurumaCourtroom {
    private HigurumaCourtroom() {}

    private static final int ROUNDS = 5;
    private static final int ROUND_TIMEOUT = 200;
    private static final double SELECT_RANGE = 60.0D;
    private static final ResourceLocation JUDGEMAN = jc("judgeman");
    private static final ResourceLocation WITNESS_STAND = jc("entity_witness_stand");
    private static final ResourceLocation SUKUNA_EFFECT = jc("sukuna_effect");
    private static final Set<ResourceLocation> SUKUNA_TYPES = Set.of(
            jc("sukuna"), jc("sukuna_fushiguro"), jc("sukuna_perfect"));
    private static final TagKey<EntityType<?>> RANGED = TagKey.create(
            Registries.ENTITY_TYPE, new ResourceLocation("forge", "ranged_ammo"));
    private static final TagKey<EntityType<?>> NOT_LIVING = TagKey.create(
            Registries.ENTITY_TYPE, new ResourceLocation("forge", "not_living"));

    // Identity keys prevent an old trial from affecting a reused arena slot.
    private static final Map<DomainInstance, Court> COURTS = new IdentityHashMap<>();

    public enum Verdict {
        NOT_GUILTY("Not Guilty"), CONFISCATION("Confiscation"), DEATH_SENTENCE("Death Sentence");
        final String label;
        Verdict(String label) { this.label = label; }
    }

    private enum Answer {
        DENIAL("Denial", ChatFormatting.RED),
        SILENCE("Silence", ChatFormatting.GRAY),
        CONFESSION("Confession", ChatFormatting.GOLD);
        final String label;
        final ChatFormatting color;
        Answer(String label, ChatFormatting color) { this.label = label; this.color = color; }
    }

    private record Seat(double lateral, double forward) {}
    private static final class Trial {
        final UUID token = UUID.randomUUID();
        final UUID defendant;
        int round = 1;
        int matches;
        int elapsed;
        int prosecutorDelay;
        int defendantDelay;
        Answer prosecutorAnswer;
        Answer defendantAnswer;
        Trial(UUID defendant) { this.defendant = defendant; }
    }

    private static final class Court {
        final DomainInstance inst;
        final UUID ownerId;
        final Map<UUID, Seat> seats = new LinkedHashMap<>();
        final Map<UUID, UUID> stands = new HashMap<>();
        final Map<UUID, Verdict> verdicts = new LinkedHashMap<>();
        final Map<UUID, LivingEntity> members = new LinkedHashMap<>();
        Trial trial;
        boolean active;
        int idleTicks;
        Court(DomainInstance inst) { this.inst = inst; ownerId = inst.ownerId; }
    }

    private static ResourceLocation jc(String path) { return new ResourceLocation("jujutsucraft", path); }

    private static Vec3 center(DomainInstance inst) {
        return new Vec3(inst.arenaCenter.getX() + 0.5D,
                inst.arenaCenter.getY() + 1.0D, inst.arenaCenter.getZ() + 0.5D);
    }

    private static AABB arenaBox(DomainInstance inst) {
        Vec3 c = center(inst);
        double r = DomainDimension.OUTER_RADIUS + 4.0D;
        return new AABB(c.x-r, c.y-r, c.z-r, c.x+r, c.y+r, c.z+r);
    }

    private static boolean inside(DomainInstance inst, Entity e) {
        return e.position().distanceToSqr(center(inst)) <=
                (double) DomainDimension.INNER_RADIUS * DomainDimension.INNER_RADIUS;
    }

    private static boolean eligible(LivingEntity e) {
        return e.isAlive() && !e.isDeadOrDying() && !e.isSpectator()
                && !(e instanceof ArmorStand)
                && !e.getPersistentData().getBoolean("abyss_domain_decoration")
                && !e.getType().is(RANGED) && !e.getType().is(NOT_LIVING);
    }

    private static boolean uncontested(ServerLevel level, DomainInstance inst, LivingEntity owner) {
        if (inst.skillDomain != 27.0D || !inst.ownerEntryConfirmed
                || inst.participants.size() != 1 || inst.clashTimerPaused
                || owner == null || !owner.isAlive() || owner.isDeadOrDying()
                || owner.getPersistentData().getBoolean("Failed")
                || owner.getPersistentData().getBoolean("DomainDefeated")
                || owner.getPersistentData().getBoolean("abyss_domain_transferring")
                || owner.getPersistentData().getBoolean("abyss_domain_end_scheduled")) return false;
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(DomainEvents.DOMAIN_EFFECT_ID);
        return effect != null && owner.hasEffect(effect)
                && inside(inst, owner)
                && DomainClash.findActiveDomainInArena(level, inst, inst.ownerId) == null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ServerLevel level = event.getServer().getLevel(DomainDimension.ID);
        if (level == null) return;

        Iterator<Map.Entry<DomainInstance, Court>> iterator = COURTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<DomainInstance, Court> entry = iterator.next();
            DomainInstance inst = entry.getKey();
            Court court = entry.getValue();
            if (DomainInstanceManager.getBySlot(inst.slot) != inst
                    || inst.skillDomain != 27.0D || !inst.ownerId.equals(court.ownerId)) {
                // Queue completed verdicts once, after this courtroom has ended.
                // Resolve by UUID outside the domain, never on stale NPC instances.
                HigurumaPunishments.queue(event.getServer(), court.ownerId, court.verdicts);
                clearStands(level, court);
                iterator.remove();
            }
        }

        for (int slot = 0; slot < DomainDimension.MAX_SLOTS; slot++) {
            DomainInstance inst = DomainInstanceManager.getBySlot(slot);
            if (inst == null) continue;

            // Restore following even when Higuruma is a challenger in a clash.
            for (DomainClashParticipants.Participant p : inst.participants.all()) {
                if (p.skillDomain == 27.0D) followJudgeman(level, inst, p.entityId);
            }
            if (inst.skillDomain != 27.0D) continue;
            LivingEntity owner = DomainArenaService.findLivingIn(level, inst.ownerId);
            Court court = COURTS.get(inst);
            if (!uncontested(level, inst, owner)) {
                if (court != null && court.active) {
                    court.active = false;
                    clearStands(level, court);
                    court.members.clear();
                }
                continue;
            }
            if (court == null) {
                court = new Court(inst);
                COURTS.put(inst, court);
            }
            boolean resumed = !court.active;
            court.active = true;
            updateMembers(level, court, owner);
            if (court.trial != null && !court.members.containsKey(court.trial.defendant)) {
                broadcast(court, Component.literal("Judgeman: Trial interrupted").withStyle(ChatFormatting.GRAY));
                court.trial = null;
            }
            positionMembers(level, court);
            if (resumed) {
                if (owner instanceof ServerPlayer player) {
                    player.sendSystemMessage(Component.literal("Deadly Sentencing: Right-click a subject to begin a trial")
                            .withStyle(ChatFormatting.GOLD));
                }
                if (court.trial != null) prompt(court);
            }
            tickTrial(level, court, owner);
        }
    }

    // Coordinates are relative to the winner's retained side of the arena.
    private static Vec3 position(Court court, Seat seat) {
        Vec3 c = center(court.inst);
        DomainClashParticipants.Participant p = court.inst.participants.get(court.ownerId);
        DomainClashParticipants.Side side = p == null ? DomainClashParticipants.Side.NORTH : p.side;
        return switch (side) {
            case NORTH -> c.add(seat.lateral, 0, -seat.forward);
            case SOUTH -> c.add(-seat.lateral, 0, seat.forward);
            case WEST -> c.add(-seat.forward, 0, -seat.lateral);
            case EAST -> c.add(seat.forward, 0, seat.lateral);
        };
    }

    private static List<Seat> audienceSeats() {
        List<Seat> seats = new ArrayList<>();
        for (int row = 0; row < 6; row++) {
            double lateral = 15.0D + row * 5.0D;
            // Fill from the middle out, alternating between the two sides.
            for (int i = 0; i <= 12; i++) {
                double forward = i == 0 ? 0 : ((i + 1) / 2) * 5.0D * (i % 2 == 1 ? 1 : -1);
                if (lateral * lateral + forward * forward > 41.0D * 41.0D) continue;
                seats.add(new Seat(-lateral, forward));
                seats.add(new Seat(lateral, forward));
            }
        }
        return List.copyOf(seats);
    }
    private static final List<Seat> AUDIENCE = audienceSeats();
    private static final Seat PROSECUTOR = new Seat(0, 12);
    private static final Seat DEFENDANT = new Seat(0, -12);

    private static void updateMembers(ServerLevel level, Court court, LivingEntity owner) {
        court.members.clear();
        court.members.put(owner.getUUID(), owner);
        List<LivingEntity> entities = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class,
                arenaBox(court.inst), e -> eligible(e) && inside(court.inst, e)));
        entities.sort(Comparator.comparing(Entity::getUUID));
        for (LivingEntity e : entities) court.members.put(e.getUUID(), e);
        court.seats.keySet().removeIf(id -> !court.members.containsKey(id));
        Set<Seat> used = new HashSet<>(court.seats.values());
        for (UUID id : court.members.keySet()) {
            if (id.equals(court.ownerId) || court.seats.containsKey(id)) continue;
            for (Seat seat : AUDIENCE) {
                if (used.add(seat)) {
                    court.seats.put(id, seat);
                    break;
                }
            }
        }
        // Overflow entities remain protected but are not assigned overlapping stands.
        Iterator<Map.Entry<UUID, UUID>> it = court.stands.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, UUID> entry = it.next();
            if (!court.members.containsKey(entry.getKey())) {
                Entity stand = level.getEntity(entry.getValue());
                if (stand != null) stand.discard();
                it.remove();
            }
        }
    }

    private static void positionMembers(ServerLevel level, Court court) {
        Vec3 c = center(court.inst);
        for (Map.Entry<UUID, LivingEntity> entry : court.members.entrySet()) {
            UUID id = entry.getKey();
            LivingEntity member = entry.getValue();
            Seat seat = id.equals(court.ownerId) ? PROSECUTOR :
                    court.trial != null && id.equals(court.trial.defendant) ? DEFENDANT : court.seats.get(id);
            if (seat == null) continue;
            Vec3 pos = position(court, seat);
            float facing = (float) Math.toDegrees(Math.atan2(c.z - pos.z, c.x - pos.x)) - 90.0F;
            // Never overwrite the player's camera direction.
            member.stopRiding();
            member.setDeltaMovement(Vec3.ZERO);
            member.fallDistance = 0;
            if (member instanceof Mob mob) {
                mob.getNavigation().stop();
                mob.setTarget(null);
            }
            if (member.position().distanceToSqr(pos) > 0.0225D) {
                if (member instanceof ServerPlayer player) {
                    player.connection.teleport(pos.x, pos.y, pos.z, player.getYRot(), player.getXRot());
                } else {
                    member.moveTo(pos.x, pos.y, pos.z, facing, 0);
                }
            }
            ensureStand(level, court, id, pos, facing);
        }
    }

    private static void ensureStand(ServerLevel level, Court court, UUID member, Vec3 pos, float yaw) {
        UUID standId = court.stands.get(member);
        Entity stand = standId == null ? null : level.getEntity(standId);
        if (stand == null || stand.isRemoved()) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(WITNESS_STAND);
            if (type == null) return;
            stand = type.create(level);
            if (stand == null) return;
            stand.moveTo(pos.x, pos.y, pos.z, yaw, 0);
            stand.setNoGravity(true);
            stand.setInvulnerable(true);
            if (stand instanceof Mob mob) mob.setNoAi(true);
            stand.getPersistentData().putBoolean("abyss_domain_decoration", true);
            stand.getPersistentData().putBoolean("abyss_higuruma_stand", true);
            stand.getPersistentData().putUUID("abyss_decoration_owner", court.ownerId);
            stand.getPersistentData().putString("OWNER_UUID", court.ownerId.toString());
            stand.getPersistentData().putString("TARGET", member.toString());
            if (!level.addFreshEntity(stand)) return;
            court.stands.put(member, stand.getUUID());
        }
        stand.setDeltaMovement(Vec3.ZERO);
        if (stand.position().distanceToSqr(pos) > 0.001D) stand.setPos(pos.x, pos.y, pos.z);
        stand.setYRot(yaw);
        if (stand instanceof LivingEntity living) {
            living.setYBodyRot(yaw);
            living.setYHeadRot(yaw);
        }
    }

    private static void clearStands(ServerLevel level, Court court) {
        for (UUID id : court.stands.values()) {
            Entity stand = level.getEntity(id);
            if (stand != null) stand.discard();
        }
        court.stands.clear();
    }

    private static void followJudgeman(ServerLevel level, DomainInstance inst, UUID casterId) {
        LivingEntity caster = DomainArenaService.findLivingIn(level, casterId);
        if (caster == null || !caster.isAlive()) return;
        Vec3 direction = Vec3.directionFromRotation(0.0F, caster.getYRot());
        Vec3 target = caster.position().subtract(direction.scale(5.0D + caster.getBbWidth()));
        Vec3 c = center(inst);
        Vec3 offset = target.subtract(c);
        if (offset.lengthSqr() > 55.0D * 55.0D) target = c.add(offset.normalize().scale(55.0D));
        for (Entity judge : level.getEntities((Entity) null, arenaBox(inst), e ->
                JUDGEMAN.equals(ForgeRegistries.ENTITY_TYPES.getKey(e.getType()))
                        && e.getPersistentData().getBoolean("abyss_domain_decoration")
                        && e.getPersistentData().hasUUID("abyss_decoration_owner")
                        && casterId.equals(e.getPersistentData().getUUID("abyss_decoration_owner")))) {
            Vec3 delta = target.subtract(judge.position());
            double distance = delta.length();
            Vec3 next = distance > 16.0D || distance <= 0.5D ? target
                    : judge.position().add(delta.scale(0.5D / distance));
            judge.setPos(next.x, next.y, next.z);
            judge.setDeltaMovement(Vec3.ZERO);
            judge.setYRot(caster.getYRot());
            judge.setXRot(0);
            if (judge instanceof LivingEntity living) {
                living.setYHeadRot(caster.getYRot());
                living.setYBodyRot(caster.getYRot());
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("abyss_trial")
                .then(Commands.literal("select").executes(ctx -> {
                    if (ctx.getSource().getEntity() instanceof ServerPlayer player) select(player);
                    return 1;
                }))
                .then(Commands.literal("answer")
                        .then(Commands.argument("trial", StringArgumentType.word())
                                .then(Commands.argument("round", IntegerArgumentType.integer(1, ROUNDS))
                                        .then(Commands.argument("choice", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) return 0;
                                                    return submit(player, StringArgumentType.getString(ctx, "trial"),
                                                            IntegerArgumentType.getInteger(ctx, "round"),
                                                            StringArgumentType.getString(ctx, "choice")) ? 1 : 0;
                                                }))))));
    }

    private static Court courtFor(LivingEntity e) {
        if (!(e.level() instanceof ServerLevel level) || !level.dimension().equals(DomainDimension.ID)) return null;
        DomainInstance inst = DomainClash.findInstanceContaining(e.position());
        if (inst == null) return null;
        Court court = COURTS.get(inst);
        if (court == null || !court.active || !court.members.containsKey(e.getUUID())
                || DomainInstanceManager.getBySlot(inst.slot) != inst) return null;
        LivingEntity owner = DomainArenaService.findLivingIn(level, court.ownerId);
        return court.ownerId.equals(inst.ownerId) && uncontested(level, inst, owner) ? court : null;
    }

    private static void select(ServerPlayer player) {
        Court court = courtFor(player);
        if (court == null || !court.ownerId.equals(player.getUUID()) || court.trial != null) return;
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(SELECT_RANGE));
        HitResult block = player.level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() == HitResult.Type.BLOCK) end = block.getLocation();
        LivingEntity closest = null;
        double best = start.distanceToSqr(end);
        for (LivingEntity candidate : court.members.values()) {
            if (candidate.getUUID().equals(court.ownerId) || !eligible(candidate)
                    || !court.seats.containsKey(candidate.getUUID())) continue;
            Optional<Vec3> hit = candidate.getBoundingBox().inflate(0.35D).clip(start, end);
            if (hit.isPresent() && start.distanceToSqr(hit.get()) < best) {
                closest = candidate;
                best = start.distanceToSqr(hit.get());
            }
        }
        if (closest == null) return;
        if (court.verdicts.containsKey(closest.getUUID())) {
            player.displayClientMessage(Component.literal("This defendant has already been judged"), true);
            return;
        }
        startTrial((ServerLevel) player.level(), court, closest);
    }

    private static void startTrial(ServerLevel level, Court court, LivingEntity defendant) {
        court.trial = new Trial(defendant.getUUID());
        court.idleTicks = 0;
        positionMembers(level, court);
        broadcast(court, Component.literal("Judgeman: Trial of " + defendant.getName().getString())
                .withStyle(ChatFormatting.GOLD));
        beginRound(level, court);
    }

    private static void beginRound(ServerLevel level, Court court) {
        Trial t = court.trial;
        t.elapsed = 0;
        t.prosecutorAnswer = null;
        t.defendantAnswer = null;
        t.prosecutorDelay = 20 + level.random.nextInt(21);
        t.defendantDelay = 20 + level.random.nextInt(21);
        prompt(court);
    }

    private static boolean confessionOnly(LivingEntity e) {
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(SUKUNA_EFFECT);
        ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        return (effect != null && e.hasEffect(effect))
                || (type != null && SUKUNA_TYPES.contains(type));
    }

    private static void prompt(Court court) {
        Trial t = court.trial;
        if (t == null) return;
        LivingEntity owner = court.members.get(court.ownerId);
        LivingEntity defendant = court.members.get(t.defendant);
        if (owner instanceof ServerPlayer p && t.prosecutorAnswer == null) sendOptions(p, t, false);
        if (defendant instanceof ServerPlayer p && t.defendantAnswer == null) sendOptions(p, t, confessionOnly(p));
    }

    private static void sendOptions(ServerPlayer player, Trial t, boolean confessionOnly) {
        player.sendSystemMessage(Component.literal("Judgeman — Round " + t.round + "/" + ROUNDS)
                .withStyle(ChatFormatting.GOLD));
        MutableComponent line = Component.literal("");
        for (Answer answer : Answer.values()) {
            if (confessionOnly && answer != Answer.CONFESSION) continue;
            String command = "/abyss_trial answer " + t.token + " " + t.round + " " + answer.name().toLowerCase(Locale.ROOT);
            line.append(Component.literal("[" + answer.label + "]")
                    .withStyle(answer.color)
                    .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))));
            line.append(Component.literal("  "));
        }
        player.sendSystemMessage(line);
    }

    private static boolean submit(ServerPlayer player, String token, int round, String choice) {
        Court court = courtFor(player);
        if (court == null || court.trial == null) return false;
        Trial t = court.trial;
        if (!t.token.toString().equals(token) || round != t.round) return false;
        Answer answer;
        try { answer = Answer.valueOf(choice.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { return false; }
        if (player.getUUID().equals(court.ownerId)) {
            if (t.prosecutorAnswer != null) return false;
            t.prosecutorAnswer = answer;
        } else if (player.getUUID().equals(t.defendant)) {
            if (t.defendantAnswer != null) return false;
            if (confessionOnly(player) && answer != Answer.CONFESSION) {
                player.sendSystemMessage(Component.literal("You may only confess"));
                sendOptions(player, t, true);
                return false;
            }
            t.defendantAnswer = answer;
        } else return false;
        player.sendSystemMessage(Component.literal("Answer submitted").withStyle(ChatFormatting.GRAY));
        return true;
    }

    private static Answer randomAnswer(ServerLevel level) {
        Answer[] answers = Answer.values();
        return answers[level.random.nextInt(answers.length)];
    }

    private static void tickTrial(ServerLevel level, Court court, LivingEntity owner) {
        if (court.trial == null) {
            if (!(owner instanceof ServerPlayer) && ++court.idleTicks >= 30) {
                court.idleTicks = 0;
                for (LivingEntity e : court.members.values()) {
                    UUID id = e.getUUID();
                    if (!id.equals(court.ownerId) && court.seats.containsKey(id)
                            && !court.verdicts.containsKey(id)) {
                        startTrial(level, court, e);
                        break;
                    }
                }
            }
            return;
        }
        Trial t = court.trial;
        LivingEntity defendant = court.members.get(t.defendant);
        if (defendant == null || !eligible(defendant)) return;
        t.elapsed++;
        if (!(owner instanceof ServerPlayer) && t.prosecutorAnswer == null && t.elapsed >= t.prosecutorDelay)
            t.prosecutorAnswer = randomAnswer(level);
        if (!(defendant instanceof ServerPlayer) && t.defendantAnswer == null && t.elapsed >= t.defendantDelay)
            t.defendantAnswer = confessionOnly(defendant) ? Answer.CONFESSION : randomAnswer(level);
        if (t.elapsed >= ROUND_TIMEOUT) {
            if (t.prosecutorAnswer == null) t.prosecutorAnswer = Answer.SILENCE;
            if (t.defendantAnswer == null) t.defendantAnswer = confessionOnly(defendant) ? Answer.CONFESSION : Answer.SILENCE;
        }
        if (t.prosecutorAnswer == null || t.defendantAnswer == null) return;
        // Also enforce the rule if the effect appeared after the answer was submitted.
        if (confessionOnly(defendant)) t.defendantAnswer = Answer.CONFESSION;
        if (t.prosecutorAnswer == t.defendantAnswer) t.matches++;
        broadcast(court, Component.literal("Prosecutor: " + t.prosecutorAnswer.label));
        broadcast(court, Component.literal("Defendant: " + t.defendantAnswer.label));
        broadcast(court, Component.literal("Judgement: " + t.matches + "/" + ROUNDS).withStyle(ChatFormatting.GOLD));
        if (t.prosecutorAnswer == Answer.CONFESSION && t.defendantAnswer == Answer.CONFESSION) {
            finishTrial(level, court, Verdict.DEATH_SENTENCE);
        } else if (t.round == ROUNDS) {
            finishTrial(level, court, calculateVerdict(level, t.matches));
        } else {
            t.round++;
            beginRound(level, court);
        }
    }

    private static Verdict calculateVerdict(ServerLevel level, int matches) {
        if (matches >= 5) return Verdict.DEATH_SENTENCE;
        int roll = level.random.nextInt(100);
        if (matches == 4) return roll < 10 ? Verdict.DEATH_SENTENCE
                : roll < 90 ? Verdict.CONFISCATION : Verdict.NOT_GUILTY;
        return roll < matches * 20 ? Verdict.CONFISCATION : Verdict.NOT_GUILTY;
    }

    private static void finishTrial(ServerLevel level, Court court, Verdict verdict) {
        Trial t = court.trial;
        LivingEntity defendant = court.members.get(t.defendant);
        court.verdicts.put(t.defendant, verdict);
        String name = defendant == null ? "Defendant" : defendant.getName().getString();
        broadcast(court, Component.literal("Judgeman: " + name + " — " + verdict.label)
                .withStyle(verdict == Verdict.NOT_GUILTY ? ChatFormatting.GREEN
                        : verdict == Verdict.CONFISCATION ? ChatFormatting.GOLD : ChatFormatting.RED));
        if (defendant instanceof ServerPlayer player && verdict != Verdict.NOT_GUILTY) {
            // Only the current defendant receives the large on-screen title.
            boolean death = verdict == Verdict.DEATH_SENTENCE;
            player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 40, 0));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.empty()));
            player.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal(death ? "Death Penalty" : "Confiscation")
                            .withStyle(death ? ChatFormatting.RED : ChatFormatting.YELLOW,
                                    ChatFormatting.BOLD)));
        }
        court.trial = null;
        court.idleTicks = 0;
        positionMembers(level, court);
        if (court.members.get(court.ownerId) instanceof ServerPlayer player)
            player.sendSystemMessage(Component.literal("Select another defendant by right-clicking").withStyle(ChatFormatting.GRAY));
    }

    /** Snapshot of completed verdicts for other Abyss systems. */
    public static Map<UUID, Verdict> completedVerdicts(DomainInstance inst) {
        Court court = COURTS.get(inst);
        return court == null || !court.ownerId.equals(inst.ownerId) ? Map.of() : Map.copyOf(court.verdicts);
    }

    private static void broadcast(Court court, Component message) {
        for (LivingEntity member : court.members.values())
            if (member instanceof ServerPlayer player) player.sendSystemMessage(message);
    }

    // Keep damage suppression separate from domain activation so clashes can still start.
    private static boolean protectedEntity(Entity e) {
        return e instanceof LivingEntity living && courtFor(living) != null;
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(LivingAttackEvent event) {
        if (protectedEntity(event.getEntity()) || protectedEntity(event.getSource().getEntity())) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHurt(LivingHurtEvent event) {
        if (protectedEntity(event.getEntity()) || protectedEntity(event.getSource().getEntity())) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (protectedEntity(event.getEntity()) || protectedEntity(event.getTarget())) event.setCanceled(true);
    }
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) { COURTS.clear(); }
}
