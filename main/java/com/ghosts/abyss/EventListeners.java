package com.ghosts.abyss;

import com.ghosts.abyss.csm.CsmRoster;
import com.ghosts.abyss.csm.CsmSpiritData;
import com.ghosts.abyss.network.*;
import com.ghosts.abyss.rika.RikaInventory;
import com.ghosts.abyss.rika.RikaInventoryMenu;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber(
        modid = "abyss",
        bus = Bus.FORGE
)
public class EventListeners {
   private static final Map<UUID, CompoundTag> deathDataCache = new HashMap<>();
   private static final Map<UUID, CompoundTag> mbaPreDeathCache = new HashMap<>();
   private static final Map<UUID, Integer> hwbParticleTick = new HashMap<>();
   private static final Map<UUID, Double> lastKnownCT = new HashMap<>();
   private static final Map<UUID, Double> lastCnt5 = new HashMap<>();

   private static final UUID PERFECT_BODY_MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
   private static final String PERFECT_BODY_MODIFIER_NAME = "perfect_body_bonus";

   public static final String KEY_SECONDARY_CT = "jujutsuslop_secondary_ct";
   public static final String KEY_ORIGINAL_CT = "jujutsuslop_original_ct";
   public static final String KEY_ABSORBED_ACTIVE = "jujutsuslop_absorbed_active";
   public static final double NO_TECHNIQUE = -1.0D;
   public static final String KEY_COT_UNLOCKED = "cursed_object_transform_unlocked";

   private static final List<String> HWB_TEACHERS = List.of(
           "jujutsucraft:sukuna_perfect",
           "jujutsucraft:sukuna",
           "jujutsucraft:sukuna_fushiguro",
           "jujutsucraft:reggie_star",
           "jujutsucraft:kashimo_hajime",
           "jujutsucrafts:kashimo_old"
   );

   private static final List<String> SUKUNA_ENTITY_TYPES = List.of(
           "jujutsucraft:sukuna",
           "jujutsucraft:sukuna_fushiguro",
           "jujutsucraft:sukuna_perfect"
   );

   private static final Map<UUID, Integer> mahitoPunishCooldown = new HashMap<>();
   private static final Map<UUID, Boolean> lastKnownPerfectBody = new HashMap<>();
   private static final List<String> HWB_STRIP_EFFECTS = List.of("jujutsucraft:neutralization", "jujutsucraft:brain_damage");
   private static final List<String> OPEN_DOMAIN_NPC_TYPES = List.of(
           "jujutsucraft:kenjaku",
           "jujutsucraft:sukuna",
           "jujutsucraft:sukuna_fushiguro",
           "jujutsucraft:sukuna_perfect"
   );

   private static final Map<UUID, MobEffectInstance> sukunaEffectCache = new ConcurrentHashMap<>();

   private static final String NBT_RIKA_CE_BOOSTED = "jslop_rika_ce_boosted";
   private static final String NBT_RIKA_BASE_CE_MAX = "jslop_rika_base_ce_max";
   private static final String NBT_RIKA_LAST_SET_MAX = "jslop_rika_last_set_max";

   private static final ResourceLocation RIKA_RL = new ResourceLocation("jujutsucraft", "rika");
   private static final ResourceLocation RIKA_2_RL = new ResourceLocation("jujutsucraft", "rika_2");

   @SubscribeEvent
   public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
      if (event.phase == Phase.END && event.player instanceof ServerPlayer player) {
         AdvTechPoints.onTick(player);
         checkCTChanged(player);
         checkWitnessedHWB(player);
         checkWitnessedOpenDomain(player);
         stripDomainEffects(player);
         spawnHwbParticle(player);
         applyHwbActionLock(player);
         applyPerfectBodyStats(player);
         applyPerfectBodyEffects(player);
         boostChantIfPerfectBody(player);
         applyRikaCEBoost(player);
         enforceMahitoSukunaRestriction(player);
         applySpecialGradeDoubleJump(player);
         checkPerfectBodyChanged(player);

         if (player.tickCount % 10 == 0 && CsmRoster.hasRoster(player)) {
            observeDeployedSpiritSkills(player);
         }

         if (player.tickCount % 20 == 0) {
            boolean restricted = isRestrictedCT(player);
            boolean activeFlag = player.getPersistentData().getBoolean("open_domain");
            boolean toggledFlag = player.getPersistentData().getBoolean("open_domain_toggled");

            if (restricted && activeFlag) {
               player.getPersistentData().putBoolean("open_domain", false);
               OpenDomain.sync(player);
            } else if (!restricted && toggledFlag && !activeFlag) {
               boolean hasDomainEffect = player.getActiveEffects().stream().anyMatch(e -> {
                  ResourceLocation loc = ForgeRegistries.MOB_EFFECTS.getKey(e.getEffect());
                  return loc != null && loc.toString().equals("jujutsucraft:domain_expansion");
               });

               if (!hasDomainEffect) {
                  player.getPersistentData().putBoolean("open_domain", true);
                  OpenDomain.sync(player);
               }
            }
         }
      }
   }

   private static void observeDeployedSpiritSkills(ServerPlayer player) {
      if (player.level() instanceof ServerLevel sl) {
         Vec3 pos = player.position();
         sl.getEntitiesOfClass(Entity.class, new AABB(pos, pos).inflate(128.0D),
                 e -> e.isAlive() && e.getPersistentData().getBoolean("CursedSpirit")
                         && e.getPersistentData().getString("OWNER_UUID").equals(player.getStringUUID())
         ).forEach(e -> {
            double skill = e.getPersistentData().getDouble("skill");
            if (skill > 0.0D && skill == Math.floor(skill)) {
               int skillId = (int) skill;
               int[] existing = e.getPersistentData().getIntArray("CSM_PENDING_SKILLS");

               // Cap maximum tracked pending skills to 16
               if (existing.length >= 16) return;

               for (int v : existing) {
                  if (v == skillId) return;
               }

               int[] updated = new int[existing.length + 1];
               System.arraycopy(existing, 0, updated, 0, existing.length);
               updated[existing.length] = skillId;
               e.getPersistentData().putIntArray("CSM_PENDING_SKILLS", updated);
            }
         });
      }
   }

   private static double getPlayerCT(ServerPlayer player) {
      try {
         Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
         Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
         capField.setAccessible(true);
         @SuppressWarnings("unchecked")
         Capability<Object> cap = (Capability<Object>) capField.get(null);
         Object vars = player.getCapability(cap, null).orElse(null);

         if (vars == null) {
            return -1.0D;
         }

         Field ctField = vars.getClass().getDeclaredField("PlayerCurseTechnique");
         ctField.setAccessible(true);
         return (Double) ctField.get(vars);
      } catch (Exception e) {
         return -1.0D;
      }
   }

   private static double getPlayerCT2(ServerPlayer player) {
      try {
         Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
         Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
         capField.setAccessible(true);
         @SuppressWarnings("unchecked")
         Capability<Object> cap = (Capability<Object>) capField.get(null);
         Object vars = player.getCapability(cap, null).orElse(null);

         if (vars == null) {
            return -1.0D;
         }

         Field ct2Field = vars.getClass().getDeclaredField("PlayerCurseTechnique2");
         ct2Field.setAccessible(true);
         return (Double) ct2Field.get(vars);
      } catch (Exception e) {
         return -1.0D;
      }
   }

   private static boolean isRestrictedCT(ServerPlayer player) {
      try {
         Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
         Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
         capField.setAccessible(true);
         @SuppressWarnings("unchecked")
         Capability<Object> cap = (Capability<Object>) capField.get(null);
         Object vars = player.getCapability(cap, null).orElse(null);

         if (vars == null) {
            return false;
         }

         Field ctField = vars.getClass().getDeclaredField("PlayerCurseTechnique");
         ctField.setAccessible(true);
         double ct = (Double) ctField.get(vars);

         if (ct != 1.0D && ct != 18.0D && ct != 41.0D) {
            Field ct2Field = vars.getClass().getDeclaredField("PlayerCurseTechnique2");
            ct2Field.setAccessible(true);
            double ct2 = (Double) ct2Field.get(vars);

            Field secondField = vars.getClass().getDeclaredField("SecondTechnique");
            secondField.setAccessible(true);
            boolean secondActive = (Boolean) secondField.get(vars);

            return secondActive && (ct2 == 1.0D || ct2 == 18.0D || ct2 == 41.0D);
         } else {
            return true;
         }
      } catch (Exception e) {
         return false;
      }
   }

   private static void applyPerfectBodyStats(ServerPlayer player) {
      boolean perfectBody = player.getPersistentData().getBoolean("perfect_body");
      boolean fingerHbActive = hasSukunaEffect(player) && getHbAmplifier(player) > 9;
      boolean shouldApply = perfectBody && !fingerHbActive;

      for (Attribute attribute : List.of(Attributes.MAX_HEALTH, Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS)) {
         AttributeInstance inst = player.getAttribute(attribute);
         if (inst != null) {
            AttributeModifier existing = inst.getModifier(PERFECT_BODY_MODIFIER_UUID);
            if (shouldApply && existing == null) {
               inst.addPermanentModifier(new AttributeModifier(PERFECT_BODY_MODIFIER_UUID, PERFECT_BODY_MODIFIER_NAME, 0.25D, Operation.MULTIPLY_TOTAL));
               if (attribute == Attributes.MAX_HEALTH) {
                  double currentMax = player.getMaxHealth();
                  double roundedMax = Math.ceil(currentMax / 2.0D) * 2.0D;
                  inst.setBaseValue(roundedMax / 1.25D);
                  player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
               }
            } else if (!shouldApply && existing != null) {
               inst.removeModifier(PERFECT_BODY_MODIFIER_UUID);
               if (attribute == Attributes.MAX_HEALTH) {
                  inst.setBaseValue(20.0D);
                  player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
               }
            }
         }
      }

      AttributeInstance kbInst = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
      if (kbInst != null) {
         AttributeModifier kbExisting = kbInst.getModifier(PERFECT_BODY_MODIFIER_UUID);
         if (perfectBody && kbExisting == null) {
            kbInst.addPermanentModifier(new AttributeModifier(PERFECT_BODY_MODIFIER_UUID, PERFECT_BODY_MODIFIER_NAME, 1.0D, Operation.ADDITION));
         } else if (!perfectBody && kbExisting != null) {
            kbInst.removeModifier(PERFECT_BODY_MODIFIER_UUID);
         }
      }
   }

   private static void applyPerfectBodyEffects(ServerPlayer player) {
      if (isPerfectBodyActive(player)) {
         MobEffect resistance = MobEffects.DAMAGE_RESISTANCE;
         MobEffectInstance existingRes = player.getEffect(resistance);
         if (existingRes == null || existingRes.getAmplifier() < 3) {
            player.addEffect(new MobEffectInstance(resistance, Integer.MAX_VALUE, 3, false, false));
         }

         MobEffect healthBoost = MobEffects.HEALTH_BOOST;
         MobEffectInstance existingHb = player.getEffect(healthBoost);
         if (existingHb == null || existingHb.getAmplifier() < 9) {
            player.addEffect(new MobEffectInstance(healthBoost, Integer.MAX_VALUE, 9, false, false));
         }
      }
   }

   private static void boostChantIfPerfectBody(ServerPlayer player) {
      if (isPerfectBodyActive(player)) {
         double cnt5 = player.getPersistentData().getDouble("cnt5");
         Double prev = lastCnt5.put(player.getUUID(), cnt5);

         if (prev != null && cnt5 > prev && cnt5 < 20.0D) {
            double boosted = cnt5 + 1.0D;
            player.getPersistentData().putDouble("cnt5", boosted);
            lastCnt5.put(player.getUUID(), boosted);
            Abyss.LOGGER.debug("[JSlop] chantBoost {} cnt5 {} → {}", player.getGameProfile().getName(), cnt5, boosted);
         } else if (prev != null && cnt5 < prev && prev > 0.0D) {
            Abyss.LOGGER.info("[JSlop] chantBoost {} interrupted: cnt5 {} → {} cooldown_time={}", player.getGameProfile().getName(), prev, cnt5, hasCooldownTime(player));
         }
      }
   }

   private static boolean hasCooldownTime(ServerPlayer player) {
      MobEffect fx = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "cooldown_time"));
      return fx != null && player.hasEffect(fx);
   }

   private static boolean isPerfectBodyActive(ServerPlayer player) {
      return player.getPersistentData().getBoolean("perfect_body");
   }

   private static boolean hasSukunaEffect(ServerPlayer player) {
      MobEffect sukunaFx = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "sukuna_effect"));
      return sukunaFx != null && player.hasEffect(sukunaFx);
   }

   private static int getHbAmplifier(ServerPlayer player) {
      MobEffectInstance hb = player.getEffect(MobEffects.HEALTH_BOOST);
      return hb != null ? hb.getAmplifier() : -1;
   }

   private static void applyHwbActionLock(ServerPlayer player) {
      if (!player.getPersistentData().getBoolean("perfect_body")) {
         if (player.getPersistentData().getBoolean("hwb_active")) {
            try {
               Class<?> effectsClass = Class.forName("net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects");

               for (String fieldName : List.of("COOLDOWN_TIME", "COOLDOWN_TIME_COMBAT")) {
                  Field f = effectsClass.getDeclaredField(fieldName);
                  f.setAccessible(true);
                  Object supplier = f.get(null);
                  MobEffect effect = (MobEffect) supplier.getClass().getMethod("get").invoke(supplier);
                  player.addEffect(new MobEffectInstance(effect, 5, 0, true, false));
               }
            } catch (Exception e) {
               Abyss.LOGGER.warn("HWB: could not apply action lock: {}", e.getMessage());
            }
         }
      }
   }

   private static void spawnHwbParticle(ServerPlayer player) {
      if (player.getPersistentData().getBoolean("hwb_active")) {
         if (player.level() instanceof ServerLevel serverLevel) {
            int tick = hwbParticleTick.merge(player.getUUID(), 1, Integer::sum);
            if (tick % 2 == 1) {
               try {
                  Class<?> particleClass = Class.forName("net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes");
                  Field f = particleClass.getDeclaredField("PARTICLE_HOLLOW_WICKER_BASKET");
                  f.setAccessible(true);
                  Object supplier = f.get(null);
                  SimpleParticleType particleType = (SimpleParticleType) supplier.getClass().getMethod("get").invoke(supplier);

                  double yaw = Math.toRadians(player.getYRot() + 90.0F);
                  double pitch = Math.toRadians(player.getXRot());
                  double xPos = player.getX() + Math.cos(yaw) * Math.cos(pitch) * (-1.0D - player.getBbWidth());
                  double yPos = player.getY() + player.getBbHeight() * 0.5D;
                  double zPos = player.getZ() + Math.sin(yaw) * Math.cos(pitch) * (-1.0D - player.getBbWidth());

                  serverLevel.sendParticles(particleType, xPos, yPos, zPos, 0, 0.0D, 0.0D, 0.0D, 0.0D);
               } catch (Exception e) {
                  Abyss.LOGGER.warn("HWB: could not spawn particle: {}", e.getMessage());
               }
            }
         }
      }
   }

   private static void stripDomainEffects(ServerPlayer player) {
      if (player.getPersistentData().getBoolean("hwb_active")) {
         for (String effectId : HWB_STRIP_EFFECTS) {
            Optional<MobEffect> effectToRemove = player.getActiveEffects().stream()
                    .filter(e -> {
                       ResourceLocation loc = ForgeRegistries.MOB_EFFECTS.getKey(e.getEffect());
                       return loc != null && loc.toString().equals(effectId);
                    })
                    .map(MobEffectInstance::getEffect)
                    .findFirst();

            effectToRemove.ifPresent(player::removeEffect);
         }
      }
   }

   private static void checkWitnessedHWB(ServerPlayer player) {
      if (!player.getPersistentData().getBoolean("witnessed_hwb")) {
         if (player.level() instanceof ServerLevel) {
            if (hasAdvancement(player, "jujutsucraft:mastery_simple_domain")) {
               if (hasAdvancement(player, "jujutsucraft:sorcerer_grade_1")) {
                  Vec3 pos = player.position();
                  List<Entity> nearby = player.level().getEntities(player, new AABB(pos, pos).inflate(100.0D),
                          e -> isHwbTeacher(e) && hasSimpleDomainEffect(e)
                  );

                  if (!nearby.isEmpty()) {
                     player.getPersistentData().putBoolean("witnessed_hwb", true);
                     player.displayClientMessage(Component.translatable("message.abyss.hwb_witnessed"), true);
                     Abyss.LOGGER.debug("{} has witnessed Hollow Wicker Basket.", player.getDisplayName().getString());
                     AdvTechPoints.sync(player);
                  }
               }
            }
         }
      }
   }

   private static void checkWitnessedOpenDomain(ServerPlayer player) {
      if (!player.getPersistentData().getBoolean("witnessed_open_domain")) {
         if (player.level() instanceof ServerLevel) {
            if (hasAdvancement(player, "jujutsucraft:sorcerer_grade_special")) {
               Vec3 pos = player.position();
               List<Entity> nearby = player.level().getEntities(player, new AABB(pos, pos).inflate(100.0D),
                       e -> isOpenDomainActive(e, player)
               );

               if (!nearby.isEmpty()) {
                  player.getPersistentData().putBoolean("witnessed_open_domain", true);
                  player.displayClientMessage(Component.translatable("message.abyss.open_domain_witnessed"), true);
                  Abyss.LOGGER.debug("{} witnessed an Open Domain.", player.getDisplayName().getString());
                  OpenDomain.sync(player);
               }
            }
         }
      }
   }

   private static boolean isOpenDomainActive(Entity entity, ServerPlayer observer) {
      if (entity instanceof ServerPlayer other) {
         if (other != observer) {
            return other.getPersistentData().getBoolean("open_domain");
         }
      }

      ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
      if (type == null) {
         return false;
      } else if (!OPEN_DOMAIN_NPC_TYPES.contains(type.toString())) {
         return false;
      } else if (entity instanceof LivingEntity living) {
         return living.getActiveEffects().stream().anyMatch(effect -> {
            ResourceLocation loc = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
            return loc != null && loc.toString().equals("jujutsucraft:domain_expansion");
         });
      } else {
         return false;
      }
   }

   private static boolean isHwbTeacher(Entity entity) {
      ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
      return type != null && HWB_TEACHERS.contains(type.toString());
   }

   private static boolean hasSimpleDomainEffect(Entity entity) {
      if (entity instanceof LivingEntity living) {
         return living.getActiveEffects().stream().anyMatch(effect -> {
            ResourceLocation loc = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
            return loc != null && loc.toString().equals("jujutsucraft:simple_domain");
         });
      }
      return false;
   }

   private static boolean hasAdvancement(ServerPlayer player, String id) {
      Advancement adv = player.server.getAdvancements().getAdvancement(new ResourceLocation(id));
      return adv != null && player.getAdvancements().getOrStartProgress(adv).isDone();
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void onPlayerHurtCancelHwb(LivingHurtEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         if (player.getPersistentData().getBoolean("hwb_active")) {
            float maxHp = player.getMaxHealth();
            if (maxHp > 0.0F) {
               float healthAfter = player.getHealth() - event.getAmount();
               boolean heavyBlow = (event.getAmount() / maxHp) >= 0.3F;
               boolean lowHealth = (healthAfter / maxHp) < 0.35F;

               if (heavyBlow || lowHealth) {
                  player.getPersistentData().putBoolean("hwb_active", false);
                  player.getPersistentData().putLong("jslop_hwb_cooldown_until", player.level().getGameTime() + 200L);
                  hwbParticleTick.remove(player.getUUID());

                  try {
                     Class<?> attrsClass = Class.forName("net.mcreator.jujutsucraft.init.JujutsucraftModAttributes");

                     for (String field : new String[]{"ANIMATION_1", "ANIMATION_2"}) {
                        Field f = attrsClass.getDeclaredField(field);
                        f.setAccessible(true);
                        Object supplier = f.get(null);
                        Attribute attr = (Attribute) supplier.getClass().getMethod("get").invoke(supplier);
                        AttributeInstance inst = player.getAttribute(attr);
                        if (inst != null) {
                           inst.setBaseValue(0.0D);
                        }
                     }
                  } catch (Exception e) {
                     Abyss.LOGGER.warn("HWB break: could not reset animation: {}", e.getMessage());
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         if (player.getPersistentData().getBoolean("hwb_active")) {
            event.setCanceled(true);
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         RefinementPoints.sync(player);
         AdvTechPoints.sync(player);
         OpenDomain.sync(player);
         PacketSyncYutaTechniques.sync(player);
         RctPoints.sync(player);
         BlackFlashPoints.sync(player);
         PacketSyncGamerules.sync(player);
         boolean unlocked = player.getPersistentData().getBoolean("minimap_unlocked");
         boolean enabled  = player.getPersistentData().getBoolean("minimap_enabled");
         ModNetwork.CHANNEL.send(
                 PacketDistributor.PLAYER.with(() -> player),
                 new PacketSyncMinimapState(unlocked, enabled)
         );
         ModNetwork.CHANNEL.send(
                 PacketDistributor.PLAYER.with(() -> player),
                 new PacketSyncCopyToggle(player.getPersistentData().getBoolean("jslop_disable_copy"))
         );

         KeybindConfig.loadFromNbt(player.getPersistentData());
         ModNetwork.CHANNEL.send(
                 PacketDistributor.PLAYER.with(() -> player),
                 new PacketSyncKeybinds(KeybindConfig.getSlots())
         );

         double ct = AbilityScanHandler.getCurrentCT(player);
         lastKnownCT.put(player.getUUID(), ct);
         AbilityScanHandler.scanAndSync(player);
         CsmSync.sendRoster(player);

         boolean myState = player.getPersistentData().getBoolean("perfect_body");
         lastKnownPerfectBody.put(player.getUUID(), myState);
         broadcastPerfectBodyToAll(player, myState);

         for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            if (other != player) {
               boolean otherState = other.getPersistentData().getBoolean("perfect_body") && !other.getPersistentData().getBoolean("jslop_hide_four_arms");
               ModNetwork.CHANNEL.send(
                       PacketDistributor.PLAYER.with(() -> player),
                       new PacketSyncPerfectBodyVisible(other.getUUID(), otherState)
               );
            }
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onMbaEffectExpire(MobEffectEvent.Remove event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         if (event.getEffectInstance() != null) {
            ResourceLocation effectKey = ForgeRegistries.MOB_EFFECTS.getKey(event.getEffectInstance().getEffect());
            if (effectKey != null && effectKey.getNamespace().equals("jujutsucraft")) {
               if (effectKey.getPath().contains("mythical_beast_amber")) {
                  CompoundTag snap = new CompoundTag();
                  CompoundTag data = player.getPersistentData();

                  snap.putDouble("refinement_points_available", data.getDouble("refinement_points_available"));
                  snap.putDouble("domain_refinement", data.getDouble("domain_refinement"));
                  snap.putDouble("advtech_points_available", data.getDouble("advtech_points_available"));
                  snap.putDouble("advtech_last_known_fame", data.getDouble("advtech_last_known_fame"));
                  snap.putBoolean("unlocked_hwb", data.getBoolean("unlocked_hwb"));
                  snap.putBoolean("witnessed_hwb", data.getBoolean("witnessed_hwb"));
                  snap.putBoolean("perfect_body", data.getBoolean("perfect_body"));
                  snap.putBoolean("witnessed_open_domain", data.getBoolean("witnessed_open_domain"));
                  snap.putBoolean("open_domain_unlocked", data.getBoolean("open_domain_unlocked"));
                  snap.putBoolean("open_domain_toggled", data.getBoolean("open_domain_toggled"));
                  snap.putBoolean("cursed_object_transform_unlocked", data.getBoolean("cursed_object_transform_unlocked"));
                  snap.putBoolean("jujutsuslop_absorbed_active", false);

                  if (data.contains("jujutsuslop_secondary_ct")) {
                     snap.putDouble("jujutsuslop_secondary_ct", data.getDouble("jujutsuslop_secondary_ct"));
                  }

                  if (data.contains("jujutsuslop_original_ct")) {
                     snap.putDouble("jujutsuslop_original_ct", data.getDouble("jujutsuslop_original_ct"));
                  }

                  try {
                     Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
                     Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
                     capField.setAccessible(true);
                     @SuppressWarnings("unchecked")
                     Capability<Object> cap = (Capability<Object>) capField.get(null);
                     Object vars = player.getCapability(cap, null).orElse(null);

                     if (vars != null) {
                        Field ctField = vars.getClass().getDeclaredField("PlayerCurseTechnique");
                        ctField.setAccessible(true);
                        snap.putDouble("mba_pre_ct", (Double) ctField.get(vars));
                     }
                  } catch (Exception e) {
                     Abyss.LOGGER.warn("Could not snapshot CT for MBA: {}", e.getMessage());
                  }

                  mbaPreDeathCache.put(player.getUUID(), snap);
               }
            }
         }
      }
   }

   private static void checkCTChanged(ServerPlayer player) {
      if (player.tickCount % 20 == 0) {
         try {
            Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
            Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
            capField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Capability<Object> cap = (Capability<Object>) capField.get(null);
            Object vars = player.getCapability(cap, null).orElse(null);

            if (vars == null) {
               return;
            }

            Field ctField = vars.getClass().getDeclaredField("PlayerCurseTechnique");
            ctField.setAccessible(true);
            double ct = (Double) ctField.get(vars);
            Double prev = lastKnownCT.get(player.getUUID());

            if (prev == null || prev != ct) {
               lastKnownCT.put(player.getUUID(), ct);
               AbilityScanHandler.scanAndSync(player);
            }
         } catch (Exception ignored) {
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onPlayerDeath(LivingDeathEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         CompoundTag mbaSnap = mbaPreDeathCache.remove(player.getUUID());
         if (mbaSnap != null) {
            deathDataCache.put(player.getUUID(), mbaSnap);
         } else {
            player.getPersistentData().putBoolean("hwb_active", false);
            hwbParticleTick.remove(player.getUUID());

            CompoundTag snapshot = new CompoundTag();
            snapshot.putDouble("refinement_points_available", player.getPersistentData().getDouble("refinement_points_available"));
            snapshot.putDouble("domain_refinement", player.getPersistentData().getDouble("domain_refinement"));
            snapshot.putDouble("advtech_points_available", player.getPersistentData().getDouble("advtech_points_available"));
            snapshot.putDouble("advtech_last_known_fame", player.getPersistentData().getDouble("advtech_last_known_fame"));
            snapshot.putBoolean("unlocked_hwb", player.getPersistentData().getBoolean("unlocked_hwb"));
            snapshot.putBoolean("witnessed_hwb", player.getPersistentData().getBoolean("witnessed_hwb"));
            snapshot.putBoolean("perfect_body", player.getPersistentData().getBoolean("perfect_body"));
            snapshot.putBoolean("witnessed_open_domain", player.getPersistentData().getBoolean("witnessed_open_domain"));
            snapshot.putBoolean("open_domain_unlocked", player.getPersistentData().getBoolean("open_domain_unlocked"));
            snapshot.putBoolean("open_domain_toggled", player.getPersistentData().getBoolean("open_domain_toggled"));
            snapshot.putBoolean("cursed_object_transform_unlocked", player.getPersistentData().getBoolean("cursed_object_transform_unlocked"));

            if (player.getPersistentData().contains("jujutsuslop_secondary_ct")) {
               snapshot.putDouble("jujutsuslop_secondary_ct", player.getPersistentData().getDouble("jujutsuslop_secondary_ct"));
            }

            if (player.getPersistentData().contains("jujutsuslop_original_ct")) {
               snapshot.putDouble("jujutsuslop_original_ct", player.getPersistentData().getDouble("jujutsuslop_original_ct"));
            }

            snapshot.putBoolean("jujutsuslop_absorbed_active", false);

            if (player.getPersistentData().contains("jslop_yuta_registered")) {
               snapshot.put("jslop_yuta_registered", player.getPersistentData().getList("jslop_yuta_registered", 10).copy());
            }

            if (player.getPersistentData().contains("jslop_yuta_domain_idx")) {
               snapshot.putInt("jslop_yuta_domain_idx", player.getPersistentData().getInt("jslop_yuta_domain_idx"));
            }

            snapshot.putInt("rct_exp", player.getPersistentData().getInt("rct_exp"));
            snapshot.putFloat("rct_exp_accumulator", player.getPersistentData().getFloat("rct_exp_accumulator"));
            snapshot.putInt("rct_output_tier", player.getPersistentData().getInt("rct_output_tier"));
            snapshot.putInt("rct_proficiency_tier", player.getPersistentData().getInt("rct_proficiency_tier"));

            snapshot.putInt("bf_exp", player.getPersistentData().getInt("bf_exp"));
            snapshot.putInt("bf_chance_tier", player.getPersistentData().getInt("bf_chance_tier"));
            snapshot.putInt("bf_fatigue_tier", player.getPersistentData().getInt("bf_fatigue_tier"));
            snapshot.putInt("bf_energy_tier", player.getPersistentData().getInt("bf_energy_tier"));
            snapshot.putInt("bf_brain_tier", player.getPersistentData().getInt("bf_brain_tier"));
            snapshot.putBoolean("bf_at_will_unlocked", player.getPersistentData().getBoolean("bf_at_will_unlocked"));
            snapshot.putBoolean("bf_at_will_charged", false);
            snapshot.putLong("bf_at_will_cooldown_end", player.getPersistentData().getLong("bf_at_will_cooldown_end"));

            if (CsmRoster.hasRoster(player)) {
               snapshot.put("csm_roster", player.getPersistentData().getCompound("csm_roster").copy());

               if (player.getServer() != null && !AbyssGamerules.isEnabled(player, AbyssGamerules.CSM_KEEP_CURSES)) {
                  if (player.level() instanceof ServerLevel deathLevel) {
                     try {
                        double px = player.getX();
                        double py = player.getY();
                        double pz = player.getZ();

                        for (CsmSpiritData spirit : CsmRoster.getSpirits(player)) {
                           if (spirit.count > 0) {
                              ResourceLocation rl = new ResourceLocation(spirit.entityType);
                              EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(rl);

                              if (type != null) {
                                 int spawnCount = Math.min(spirit.count, 5);

                                 for (int i = 0; i < spawnCount; ++i) {
                                    double angle = Math.random() * Math.PI * 2.0D;
                                    double radius = 1.5D + Math.random() * 2.5D;
                                    Entity released = type.create(deathLevel);

                                    if (released != null) {
                                       released.moveTo(px + Math.cos(angle) * radius, py, pz + Math.sin(angle) * radius);
                                       if (!spirit.displayName.isEmpty()) {
                                          released.setCustomName(Component.literal(spirit.displayName));
                                       }
                                       deathLevel.addFreshEntity(released);
                                    }
                                 }
                              }
                           }
                        }
                     } catch (Exception e) {
                        Abyss.LOGGER.warn("[JSlop] CSM death-release failed: {}", e.getMessage());
                     }
                  }
               }
            }

            deathDataCache.put(player.getUUID(), snapshot);
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         UUID uuid = player.getUUID();
         CompoundTag snapshot;

         if (deathDataCache.containsKey(uuid)) {
            snapshot = deathDataCache.remove(uuid);
         } else if (mbaPreDeathCache.containsKey(uuid)) {
            snapshot = mbaPreDeathCache.remove(uuid);
         } else {
            return;
         }

         player.getPersistentData().putDouble("refinement_points_available", snapshot.getDouble("refinement_points_available"));
         player.getPersistentData().putDouble("domain_refinement", snapshot.getDouble("domain_refinement"));
         player.getPersistentData().putDouble("advtech_points_available", snapshot.getDouble("advtech_points_available"));
         player.getPersistentData().putDouble("advtech_last_known_fame", snapshot.getDouble("advtech_last_known_fame"));
         player.getPersistentData().putBoolean("unlocked_hwb", snapshot.getBoolean("unlocked_hwb"));
         player.getPersistentData().putBoolean("witnessed_hwb", snapshot.getBoolean("witnessed_hwb"));
         player.getPersistentData().putBoolean("perfect_body", snapshot.getBoolean("perfect_body"));
         player.getPersistentData().putBoolean("witnessed_open_domain", snapshot.getBoolean("witnessed_open_domain"));
         player.getPersistentData().putBoolean("open_domain_unlocked", snapshot.getBoolean("open_domain_unlocked"));
         player.getPersistentData().putBoolean("open_domain_toggled", snapshot.getBoolean("open_domain_toggled"));
         player.getPersistentData().putBoolean("open_domain", snapshot.getBoolean("open_domain_toggled"));
         player.getPersistentData().putBoolean("cursed_object_transform_unlocked", snapshot.getBoolean("cursed_object_transform_unlocked"));

         if (snapshot.contains("jujutsuslop_secondary_ct")) {
            player.getPersistentData().putDouble("jujutsuslop_secondary_ct", snapshot.getDouble("jujutsuslop_secondary_ct"));
         }

         if (snapshot.contains("jujutsuslop_original_ct")) {
            double originalCt = snapshot.getDouble("jujutsuslop_original_ct");
            player.getPersistentData().putDouble("jujutsuslop_original_ct", originalCt);

            try {
               Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
               Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
               capField.setAccessible(true);
               @SuppressWarnings("unchecked")
               Capability<Object> cap = (Capability<Object>) capField.get(null);
               Object vars = player.getCapability(cap, null).orElse(null);

               if (vars != null) {
                  Field ctField = vars.getClass().getDeclaredField("PlayerCurseTechnique");
                  ctField.setAccessible(true);
                  ctField.set(vars, originalCt);
                  vars.getClass().getDeclaredMethod("syncPlayerVariables", Entity.class).invoke(vars, player);
               }
            } catch (Exception e) {
               Abyss.LOGGER.warn("Could not restore PlayerCurseTechnique on respawn: {}", e.getMessage());
            }
         }

         player.getPersistentData().putBoolean("jujutsuslop_absorbed_active", false);
         player.getPersistentData().putInt("rct_exp", snapshot.getInt("rct_exp"));
         player.getPersistentData().putFloat("rct_exp_accumulator", snapshot.getFloat("rct_exp_accumulator"));
         player.getPersistentData().putInt("rct_output_tier", snapshot.getInt("rct_output_tier"));
         player.getPersistentData().putInt("rct_proficiency_tier", snapshot.getInt("rct_proficiency_tier"));
         RctPoints.sync(player);

         player.getPersistentData().putInt("bf_exp", snapshot.getInt("bf_exp"));
         player.getPersistentData().putInt("bf_chance_tier", snapshot.getInt("bf_chance_tier"));
         player.getPersistentData().putInt("bf_fatigue_tier", snapshot.getInt("bf_fatigue_tier"));
         player.getPersistentData().putInt("bf_energy_tier", snapshot.getInt("bf_energy_tier"));
         player.getPersistentData().putInt("bf_brain_tier", snapshot.getInt("bf_brain_tier"));
         player.getPersistentData().putBoolean("bf_at_will_unlocked", snapshot.getBoolean("bf_at_will_unlocked"));
         player.getPersistentData().putBoolean("bf_at_will_charged", false);
         player.getPersistentData().putLong("bf_at_will_cooldown_end", snapshot.getLong("bf_at_will_cooldown_end"));
         BlackFlashPoints.sync(player);

         CompoundTag deathSnap = deathDataCache.get(player.getUUID());
         if (deathSnap != null && deathSnap.contains("mba_pre_ct")) {
            double realCt = deathSnap.getDouble("mba_pre_ct");

            try {
               Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
               Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
               capField.setAccessible(true);
               @SuppressWarnings("unchecked")
               Capability<Object> cap = (Capability<Object>) capField.get(null);
               Object vars = player.getCapability(cap, null).orElse(null);

               if (vars != null) {
                  Field ctField = vars.getClass().getDeclaredField("PlayerCurseTechnique");
                  ctField.setAccessible(true);
                  ctField.set(vars, realCt);
                  vars.getClass().getDeclaredMethod("syncPlayerVariables", Entity.class).invoke(vars, player);
               }
            } catch (Exception e) {
               Abyss.LOGGER.warn("Could not restore CT after MBA death: {}", e.getMessage());
            }
         }

         if (snapshot.contains("csm_roster") && player.getServer() != null) {
            if (AbyssGamerules.isEnabled(player, AbyssGamerules.CSM_KEEP_CURSES)) {
               player.getPersistentData().put("csm_roster", snapshot.getCompound("csm_roster").copy());
               CsmRoster.setSpirits(player, CsmRoster.getSpirits(player));

               try {
                  Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
                  Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
                  capField.setAccessible(true);
                  @SuppressWarnings("unchecked")
                  Capability<Object> cap = (Capability<Object>) capField.get(null);
                  Object vars = player.getCapability(cap, null).orElse(null);

                  if (vars != null) {
                     Field selectField = vars.getClass().getDeclaredField("PlayerSelectCurseTechnique");
                     selectField.setAccessible(true);
                     selectField.set(vars, 0.0D);
                     vars.getClass().getDeclaredMethod("syncPlayerVariables", Entity.class).invoke(vars, player);
                  }
               } catch (Exception e) {
                  Abyss.LOGGER.warn("[JSlop] Could not reset cycling position on respawn: {}", e.getMessage());
               }
            } else {
               player.getPersistentData().remove("csm_roster");
            }

            CsmSync.sendRoster(player);
         }

         RefinementPoints.sync(player);
         AdvTechPoints.sync(player);
         OpenDomain.sync(player);

         if (snapshot.contains("jslop_yuta_registered")) {
            player.getPersistentData().put("jslop_yuta_registered", snapshot.getList("jslop_yuta_registered", 10).copy());
         }

         if (snapshot.contains("jslop_yuta_domain_idx")) {
            player.getPersistentData().putInt("jslop_yuta_domain_idx", snapshot.getInt("jslop_yuta_domain_idx"));
         }

         PacketSyncYutaTechniques.sync(player);
      }
   }

   private static void checkPerfectBodyChanged(ServerPlayer player) {
      if (player.tickCount % 20 == 3) {
         boolean current = player.getPersistentData().getBoolean("perfect_body");
         Boolean prev = lastKnownPerfectBody.get(player.getUUID());

         if (prev == null || prev != current) {
            lastKnownPerfectBody.put(player.getUUID(), current);
            broadcastPerfectBodyToAll(player, current);
         }
      }
   }

   public static void broadcastPerfectBodyToAll(ServerPlayer subject, boolean state) {
      boolean effectiveState = state && !subject.getPersistentData().getBoolean("jslop_hide_four_arms");
      PacketSyncPerfectBodyVisible pkt = new PacketSyncPerfectBodyVisible(subject.getUUID(), effectiveState);

      for (ServerPlayer recipient : subject.server.getPlayerList().getPlayers()) {
         ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> recipient), pkt);
      }
   }

   private static void enforceMahitoSukunaRestriction(ServerPlayer player) {
      int cd = mahitoPunishCooldown.getOrDefault(player.getUUID(), 0);
      if (cd > 0) {
         mahitoPunishCooldown.put(player.getUUID(), cd - 1);
      } else {
         double skill = player.getPersistentData().getDouble("skill");
         if (skill == 1505.0D || skill == 1520.0D) {
            if (getPlayerCT(player) == 15.0D) {
               if (!hasAdvancement(player, "jujutsucraft:sorcerer_grade_special")) {
                  double range = (skill == 1505.0D) ? 10.0D : 30.0D;
                  if (isSukunaNearby(player, range)) {
                     player.hurt(player.damageSources().magic(), 10.0F);
                     player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 3, false, false));
                     player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 2, false, false));

                     try {
                        Class<?> effectsClass = Class.forName("net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects");
                        Field f = effectsClass.getDeclaredField("COOLDOWN_TIME");
                        f.setAccessible(true);
                        Object supplier = f.get(null);
                        MobEffect cooldownFx = (MobEffect) supplier.getClass().getMethod("get").invoke(supplier);
                        player.addEffect(new MobEffectInstance(cooldownFx, 60, 0, false, false));
                     } catch (Exception e) {
                        Abyss.LOGGER.warn("[JSlop] Mahito restrict: could not apply COOLDOWN_TIME: {}", e.getMessage());
                     }

                     player.getPersistentData().putDouble("skill", 0.0D);
                     player.getPersistentData().putDouble("cnt1", 0.0D);
                     player.displayClientMessage(Component.literal("§cSukuna's soul repels your technique!"), true);
                     mahitoPunishCooldown.put(player.getUUID(), 40);
                  }
               }
            }
         }
      }
   }

   private static boolean isSukunaNearby(ServerPlayer player, double range) {
      Vec3 pos = player.position();
      List<Entity> nearby = player.level().getEntities(player, new AABB(pos, pos).inflate(range),
              e -> isSukunaTarget(e, player)
      );
      return !nearby.isEmpty();
   }

   private static boolean isSukunaTarget(Entity entity, ServerPlayer except) {
      ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
      if (type != null && SUKUNA_ENTITY_TYPES.contains(type.toString())) {
         return true;
      } else {
         if (entity instanceof ServerPlayer other && other != except) {
            MobEffect sukunaFx = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "sukuna_effect"));
            if (sukunaFx != null && other.hasEffect(sukunaFx)) {
               return true;
            }

            if (other.getPersistentData().getDouble("jujutsuslop_secondary_ct") == 1.0D) {
               return true;
            }

            return getPlayerCT(other) == 1.0D;
         }
         return false;
      }
   }

   private static void applySpecialGradeDoubleJump(ServerPlayer player) {
      if (player.tickCount % 2 == 1) {
         if (hasAdvancement(player, "jujutsucraft:sorcerer_grade_special")) {
            if (player.onGround()) {
               MobEffect djFx = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "double_jump_effect"));
               if (djFx != null) {
                  MobEffectInstance existing = player.getEffect(djFx);
                  int currentAmp = existing != null ? existing.getAmplifier() : -1;

                  if (currentAmp < 4) {
                     player.addEffect(new MobEffectInstance(djFx, 999999, currentAmp + 1, false, false));
                  } else if (existing.getDuration() < 20) {
                     player.addEffect(new MobEffectInstance(djFx, 999999, 4, false, false));
                  }
               }
            }
         }
      }
   }

   private static void applyRikaCEBoost(ServerPlayer player) {
      if (player.tickCount % 10 == 0 && !player.isSpectator()) {
         try {
            Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
            Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
            capField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Capability<Object> cap = (Capability<Object>) capField.get(null);
            Object vars = player.getCapability(cap, null).orElse(null);

            if (vars == null) {
               return;
            }

            Field ctField = vars.getClass().getDeclaredField("PlayerCurseTechnique");
            ctField.setAccessible(true);
            if ((Double) ctField.get(vars) == 0.0D) {
               return;
            }

            boolean rikaPresent = (Boolean) Class.forName("net.mcreator.jujutsucraft.procedures.LocateRikaProcedure")
                    .getMethod("execute", LevelAccessor.class, Entity.class)
                    .invoke(null, player.level(), player);

            Field ceField = vars.getClass().getDeclaredField("PlayerCursePower");
            Field ceMaxField = vars.getClass().getDeclaredField("PlayerCursePowerMAX");
            Field lvlField = vars.getClass().getDeclaredField("PlayerLevel");

            ceField.setAccessible(true);
            ceMaxField.setAccessible(true);
            lvlField.setAccessible(true);

            double currentCE = (Double) ceField.get(vars);
            double currentMax = (Double) ceMaxField.get(vars);
            double playerLevel = (Double) lvlField.get(vars);

            boolean currentlyBoosted = player.getPersistentData().getBoolean("jslop_rika_ce_boosted");
            double storedBaseMax = player.getPersistentData().getDouble("jslop_rika_base_ce_max");
            double lastSetMax = player.getPersistentData().getDouble("jslop_rika_last_set_max");

            if (rikaPresent) {
               boolean hasPureLove = hasAdvancement(player, "jujutsucraft:skill_curseis_lifted");
               double multiplier = hasPureLove ? 5.0D : 2.0D;

               if (!currentlyBoosted || storedBaseMax <= 0.0D) {
                  player.getPersistentData().putDouble("jslop_rika_base_ce_max", currentMax);
                  player.getPersistentData().putBoolean("jslop_rika_ce_boosted", true);

                  double fillMax = currentMax * multiplier;
                  ceMaxField.set(vars, fillMax);
                  ceField.set(vars, fillMax);
                  vars.getClass().getMethod("syncPlayerVariables", Entity.class).invoke(vars, player);

                  player.getPersistentData().putDouble("jslop_rika_last_set_max", fillMax);
                  return;
               }

               if (lastSetMax > 0.0D && Math.abs(currentMax - lastSetMax) > 1.0D) {
                  storedBaseMax = currentMax;
                  player.getPersistentData().putDouble("jslop_rika_base_ce_max", currentMax);
               }

               double newMax = storedBaseMax * multiplier;
               double baseRegen = 1.0D + ((2.0D + playerLevel) / 1.1D) * 0.2D;

               if (player.getHealth() >= player.getMaxHealth()) {
                  baseRegen *= 2.0D;
               }

               MobEffect zoneFx = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "zone"));
               if (zoneFx != null && player.hasEffect(zoneFx)) {
                  baseRegen *= 1.2D;
               }

               double bonusRegen = (double) Math.round(baseRegen) * (multiplier - 1.0D);
               double newCE = Math.min(currentCE + bonusRegen, newMax);
               boolean changed = false;

               if (currentMax != newMax) {
                  ceMaxField.set(vars, newMax);
                  changed = true;
               }

               if (newCE != currentCE) {
                  ceField.set(vars, newCE);
                  changed = true;
               }

               if (changed) {
                  vars.getClass().getMethod("syncPlayerVariables", Entity.class).invoke(vars, player);
               }

               player.getPersistentData().putDouble("jslop_rika_last_set_max", newMax);
            } else if (currentlyBoosted && storedBaseMax > 0.0D) {
               if (lastSetMax <= 0.0D || Math.abs(currentMax - lastSetMax) < 2.0D) {
                  ceMaxField.set(vars, storedBaseMax);
                  ceField.set(vars, Math.min(currentCE, storedBaseMax));
                  vars.getClass().getMethod("syncPlayerVariables", Entity.class).invoke(vars, player);
               }

               player.getPersistentData().remove("jslop_rika_ce_boosted");
               player.getPersistentData().remove("jslop_rika_base_ce_max");
               player.getPersistentData().remove("jslop_rika_last_set_max");
            }
         } catch (Exception ignored) {
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onCsmSpiritRightClickRecall(PlayerInteractEvent.EntityInteract event) {
      // 1. Only run on the main hand to prevent double execution
      if (event.getHand() != InteractionHand.MAIN_HAND) {
         return;
      }

      if (event.getEntity() instanceof ServerPlayer player && isCsmPlayer(player)) {
         Entity target = event.getTarget();
         if (target != null && target.isAlive() && !(target instanceof Player)) {
            if (target.getPersistentData().getBoolean("CursedSpirit")) {
               String spiritMarker = target.getPersistentData().getString("CSM_SPIRIT_UUID");

               if (!spiritMarker.isEmpty() && target.getPersistentData().getString("OWNER_UUID").equals(player.getStringUUID())) {
                  event.setCanceled(true);

                  // 2. Safely strip the "DEPLOYED:" prefix instead of setting it to ""
                  String resolvedUuid = spiritMarker.startsWith("DEPLOYED:")
                          ? spiritMarker.substring(9)
                          : spiritMarker;

                  String csmRosterType = target.getPersistentData().getString("CSM_ROSTER_TYPE");
                  ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
                  String entityType = !csmRosterType.isEmpty() ? csmRosterType : (rl != null ? rl.toString() : "unknown");

                  String csmDisplayName = target.getPersistentData().getString("CSM_DISPLAY_NAME");
                  String displayName = !csmDisplayName.isEmpty() ? csmDisplayName : target.getDisplayName().getString();

                  CsmRoster.restoreSpirit(player, entityType, resolvedUuid, displayName);

                  target.remove(RemovalReason.DISCARDED);
                  CsmSync.sendRoster(player);
               }
            }
         }
      }
   }

   private static boolean isCsmPlayer(ServerPlayer player) {
      try {
         Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
         Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
         capField.setAccessible(true);
         @SuppressWarnings("unchecked")
         Capability<Object> cap = (Capability<Object>) capField.get(null);
         Object vars = player.getCapability(cap, null).orElse(null);

         if (vars == null) {
            return false;
         }

         Field ct1 = vars.getClass().getDeclaredField("PlayerCurseTechnique");
         Field ct2 = vars.getClass().getDeclaredField("PlayerCurseTechnique2");
         ct1.setAccessible(true);
         ct2.setAccessible(true);

         return (Double) ct1.get(vars) == 18.0D || (Double) ct2.get(vars) == 18.0D;
      } catch (Exception e) {
         return false;
      }
   }

   @SubscribeEvent
   public static void onSpiritDeath(LivingDeathEvent event) {
      Entity entity = event.getEntity();
      if (entity.level().isClientSide()) return;

      CompoundTag tag = entity.getPersistentData();
      if (tag.getBoolean("CursedSpirit")) {
         String ownerUuidStr = tag.getString("OWNER_UUID");
         if (!ownerUuidStr.isEmpty()) {
            try {
               UUID ownerUuid = UUID.fromString(ownerUuidStr);
               ServerPlayer player = entity.getServer().getPlayerList().getPlayer(ownerUuid);

               if (player != null) {
                  String spiritUuid = tag.getString("CSM_SPIRIT_UUID");
                  ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                  if (key == null) return;
                  String entityType = key.toString();

                  // Remove or decrement the dead spirit from player roster
                  CsmRoster.removeDeployedSpirit(player, entityType, spiritUuid);

                  // Sync updated roster to client
                  CsmSync.sendRoster(player);
               }
            } catch (IllegalArgumentException ignored) {}
         }
      }
   }
   @SubscribeEvent
   public static void onSpiritUnload(EntityLeaveLevelEvent event) {
      if (event.getLevel().isClientSide()) return;

      Entity entity = event.getEntity();
      if (entity.isRemoved()) return;

      if (event.getEntity().getRemovalReason() == Entity.RemovalReason.UNLOADED_TO_CHUNK) {
         CompoundTag tag = entity.getPersistentData();

         if (tag.getBoolean("CursedSpirit")) {
            if (tag.getBoolean("CSM_RESTORED")) return;
            tag.putBoolean("CSM_RESTORED", true);

            String ownerUuidStr = tag.getString("OWNER_UUID");
            if (!ownerUuidStr.isEmpty()) {
               try {
                  UUID ownerUuid = UUID.fromString(ownerUuidStr);
                  ServerPlayer player = entity.getServer().getPlayerList().getPlayer(ownerUuid);

                  String spiritUuid = tag.getString("CSM_SPIRIT_UUID");
                  String entityType = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
                  String displayName = entity.getDisplayName().getString();

                  if (player != null) {
                     // Player is online
                     CsmRoster.restoreSpirit(player, entityType, spiritUuid, displayName);
                     CsmSync.sendRoster(player);
                  } else if (entity.getServer() != null) {
                     // Player is offline: Directly update their <UUID>.dat file in world/playerdata
                     File playerdataDir = entity.getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
                     File playerFile = new File(playerdataDir, ownerUuidStr + ".dat");

                     if (playerFile.exists()) {
                        CompoundTag offlineNbt = NbtIo.readCompressed(playerFile);
                        CompoundTag forgeData = offlineNbt.getCompound("ForgeData");
                        CompoundTag rosterRoot = forgeData.getCompound("csm_roster");
                        ListTag spiritsList = rosterRoot.getList("spirits", 10); // 10 = CompoundTag

                        // Search and restore spirit in offline spirits ListTag
                        boolean matchesFound = false;
                        for (int i = 0; i < spiritsList.size(); i++) {
                           CompoundTag spiritTag = spiritsList.getCompound(i);
                           boolean isUnique = spiritTag.getBoolean("unique");
                           String uType = spiritTag.getString("type");
                           String uUuid = spiritTag.getString("uuid");

                           if ((isUnique && uUuid.equals(spiritUuid)) || (!isUnique && uType.equals(entityType))) {
                              spiritTag.putInt("count", spiritTag.getInt("count") + 1);
                              matchesFound = true;
                              break;
                           }
                        }

                        // Save updated NBT back to player's .dat file
                        if (matchesFound) {
                           rosterRoot.put("spirits", spiritsList);
                           forgeData.put("csm_roster", rosterRoot);
                           offlineNbt.put("ForgeData", forgeData);
                           NbtIo.writeCompressed(offlineNbt, playerFile);
                        }
                     }
                  }
               } catch (Exception ignored) {}
            }
         }
      }
   }

   @SubscribeEvent
   public static void onRikaInteract(PlayerInteractEvent.EntityInteract event) {
      Entity target = event.getTarget();
      if (target != null) {
         ResourceLocation entityType = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
         if (RIKA_RL.equals(entityType) || RIKA_2_RL.equals(entityType)) {
            String ownerUUID = target.getPersistentData().getString("OWNER_UUID");
            if (!ownerUUID.isEmpty()) {
               Player player = event.getEntity();
               if (player.getStringUUID().equals(ownerUUID) && player instanceof ServerPlayer serverPlayer) {
                  event.setCanceled(true);
                  RikaInventory rikaInv = RikaInventory.load(serverPlayer);
                  NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                          (id, playerInv, p) -> new RikaInventoryMenu(id, playerInv, rikaInv),
                          Component.translatable("container.abyss.rika_inventory")
                  ));
               }
            }
         }
      }
   }
}