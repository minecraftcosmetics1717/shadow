package com.ghosts.abyss.network;

import com.ghosts.abyss.Abyss;
import java.lang.reflect.Field;
import java.util.function.Supplier;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class PacketActivateAbilitySlot {
   private final int selectValue;
   private final int targetCT;
   private final boolean press;

   public PacketActivateAbilitySlot(int selectValue, int targetCT, boolean press) {
      this.selectValue = selectValue;
      this.targetCT = targetCT;
      this.press = press;
   }

   public PacketActivateAbilitySlot(FriendlyByteBuf buf) {
      this.selectValue = buf.readInt();
      this.targetCT = buf.readInt();
      this.press = buf.readBoolean();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(this.selectValue);
      buf.writeInt(this.targetCT);
      buf.writeBoolean(this.press);
   }

   public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context ctx = contextSupplier.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null) {
            if (this.selectValue >= 0) {
               try {
                  Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
                  Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
                  capField.setAccessible(true);
                  @SuppressWarnings("unchecked")
                  Capability<Object> cap = (Capability<Object>) capField.get(null);
                  Object vars = player.getCapability(cap, (Direction) null).orElse(null);
                  if (vars == null) {
                     return;
                  }

                  Field secondField = vars.getClass().getDeclaredField("SecondTechnique");
                  secondField.setAccessible(true);
                  boolean jccSecondActive = (Boolean) secondField.get(vars);

                  Field ct1Field = vars.getClass().getDeclaredField("PlayerCurseTechnique");
                  ct1Field.setAccessible(true);
                  int primaryCT = (int) (double) (Double) ct1Field.get(vars);

                  Field ct2Field = vars.getClass().getDeclaredField("PlayerCurseTechnique2");
                  ct2Field.setAccessible(true);
                  int jccSecondaryCT = (int) (double) (Double) ct2Field.get(vars);

                  CompoundTag data = player.getPersistentData();
                  boolean cotActive = data.getBoolean("jujutsuslop_absorbed_active");
                  int cotSecondaryCT = data.contains("jujutsuslop_secondary_ct") ? (int) data.getDouble("jujutsuslop_secondary_ct") : -1;
                  int activeCT = jccSecondActive ? jccSecondaryCT : primaryCT;

                  if (activeCT != this.targetCT) {
                     int jccInactiveCT = jccSecondActive ? primaryCT : jccSecondaryCT;
                     if (jccInactiveCT == this.targetCT) {
                        Class<?> switchClass = Class.forName("net.mcreator.jujutsucraft.procedures.KeySwitchCursedTechniqueOnKeyPressedProcedure");
                        switchClass.getMethod("execute", LevelAccessor.class, double.class, double.class, double.class, Entity.class)
                                .invoke(null, player.level(), player.getX(), player.getY(), player.getZ(), player);
                     } else if (cotSecondaryCT == this.targetCT && !cotActive) {
                        PacketToggleAbsorbedTechnique.toggle(player);
                     } else {
                        if (!cotActive) {
                           return;
                        }

                        int originalCT = data.contains("jujutsuslop_original_ct") ? (int) data.getDouble("jujutsuslop_original_ct") : -1;
                        if (originalCT != this.targetCT) {
                           return;
                        }

                        if (jccSecondActive) {
                           Class<?> switchClass = Class.forName("net.mcreator.jujutsucraft.procedures.KeySwitchCursedTechniqueOnKeyPressedProcedure");
                           switchClass.getMethod("execute", LevelAccessor.class, double.class, double.class, double.class, Entity.class)
                                   .invoke(null, player.level(), player.getX(), player.getY(), player.getZ(), player);
                        }

                        PacketToggleAbsorbedTechnique.toggle(player);
                     }
                  }

                  // Handle Yuta Custom Skill Execution (Select Index >= 100)
                  if (this.targetCT == 5 && this.selectValue >= 100) {
                     int regIdx = this.selectValue - 100;
                     ListTag regList = player.getPersistentData().getList("jslop_yuta_registered", Tag.TAG_COMPOUND);

                     if (!this.press) {
                        player.getPersistentData().putBoolean("jslop_yuta_keybind_active", false);
                        player.getPersistentData().putBoolean("PRESS_Z", false);
                        return;
                     }

                     if (regIdx >= regList.size()) {
                        return;
                     }

                     CompoundTag tech = regList.getCompound(regIdx);
                     double regSkill = tech.getDouble("skill");
                     double regCooldown = tech.getDouble("cooldown_ticks");
                     boolean rikaPresent = false;

                     try {
                        rikaPresent = (Boolean) Class.forName("net.mcreator.jujutsucraft.procedures.LocateRikaProcedure")
                                .getMethod("execute", LevelAccessor.class, Entity.class)
                                .invoke(null, player.level(), player);
                     } catch (Exception ignored) {
                     }

                     MobEffect domainFx = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "domain_expansion"));
                     boolean domainActive = domainFx != null && player.hasEffect(domainFx);

                     if (!rikaPresent && !domainActive) {
                        player.sendSystemMessage(Component.translatable("message.abyss.yuta_rika_required"), true);
                        return;
                     }

                     MobEffect cooldownFx = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "cooldown_time"));
                     if (!player.isCreative() && cooldownFx != null && player.hasEffect(cooldownFx)) {
                        return;
                     }

                     double baseCeCost = tech.getDouble("ce_cost");
                     String techName = tech.getString("name");

                     try {
                        Class.forName("net.mcreator.jujutsucraft.procedures.TechniqueDecideProcedure")
                                .getMethod("execute", Entity.class, boolean.class, boolean.class, double.class, double.class, double.class, String.class)
                                .invoke(null, player, false, false, baseCeCost, 5.0D, (double) (100 + regIdx), techName);
                     } catch (Exception ignored) {
                     }

                     try {
                        Field ceField = vars.getClass().getDeclaredField("PlayerCursePower");
                        Field ceCostField = vars.getClass().getDeclaredField("PlayerSelectCurseTechniqueCost");
                        ceField.setAccessible(true);
                        ceCostField.setAccessible(true);

                        double effectiveCost = (Double) ceCostField.get(vars);
                        if (!player.isCreative() && effectiveCost > 0.0D) {
                           double currentCE = (Double) ceField.get(vars);
                           if (currentCE < effectiveCost) {
                              player.sendSystemMessage(Component.literal("Not enough cursed energy."), true);
                              return;
                           }

                           ceField.set(vars, currentCE - effectiveCost);
                        }

                        vars.getClass().getDeclaredMethod("syncPlayerVariables", Entity.class).invoke(vars, player);
                     } catch (Exception ceEx) {
                        Abyss.LOGGER.warn("[JSlop] CE cost field setup failed: {}", ceEx.getMessage());
                     }

                     try {
                        Class.forName("net.mcreator.jujutsucraft.procedures.ResetCounterProcedure")
                                .getMethod("execute", Entity.class)
                                .invoke(null, player);
                     } catch (Exception ignored) {
                     }

                     player.getPersistentData().putDouble("skill", regSkill);
                     player.getPersistentData().putDouble("COOLDOWN_TICKS", regCooldown);
                     player.getPersistentData().putBoolean("PRESS_Z", true);
                     player.getPersistentData().putBoolean("jslop_yuta_keybind_active", true);

                     MobEffect cursedTechFx = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "cursed_technique"));
                     if (cursedTechFx != null) {
                        player.removeEffect(cursedTechFx);
                        player.addEffect(new MobEffectInstance(cursedTechFx, Integer.MAX_VALUE, 0, false, false));
                     }

                     if (!player.isCreative() && cooldownFx != null) {
                        player.removeEffect(cooldownFx);
                        player.addEffect(new MobEffectInstance(cooldownFx, (int) regCooldown, 0, false, false));
                     }

                     return;
                  }

                  int effectiveSelect = this.selectValue;
                  if (this.targetCT == 18 && this.selectValue >= 1000) {
                     int rosterIdx = this.selectValue - 1000;
                     player.getPersistentData().putInt("jslop_csm_target_idx", rosterIdx);
                     effectiveSelect = 12;
                  }

                  boolean needsForward = false;
                  boolean needsSpace = false;

                  if (this.press) {
                     if (this.targetCT == -1) {
                        if (effectiveSelect == 5 || effectiveSelect == 7) {
                           needsForward = true;
                        }

                        if (effectiveSelect == 6) {
                           needsSpace = true;
                        }
                     } else {
                        if (effectiveSelect == 3 || effectiveSelect == 4) {
                           needsSpace = true;
                        }

                        if (effectiveSelect == 5 || effectiveSelect == 6 || effectiveSelect == 7) {
                           needsForward = true;
                        }
                     }
                  }

                  player.getPersistentData().putBoolean("PRESS_W", needsForward);
                  player.getPersistentData().putBoolean("PRESS_SPACE", needsSpace);

                  if (this.press && needsForward) {
                     player.getPersistentData().putBoolean("jslop_fwd_inject", true);
                  }

                  Field selectField = vars.getClass().getDeclaredField("PlayerSelectCurseTechnique");
                  selectField.setAccessible(true);
                  selectField.set(vars, (double) effectiveSelect);

                  Field noChangeField = vars.getClass().getDeclaredField("noChangeTechnique");
                  noChangeField.setAccessible(true);
                  noChangeField.set(vars, true);

                  vars.getClass().getDeclaredMethod("syncPlayerVariables", Entity.class).invoke(vars, player);

                  Level world = player.level();
                  double x = player.getX();
                  double y = player.getY();
                  double z = player.getZ();

                  if (!this.press) {
                     boolean releasedForward = this.targetCT == -1 ? effectiveSelect == 5 || effectiveSelect == 7 : effectiveSelect == 5 || effectiveSelect == 6 || effectiveSelect == 7;
                     if (releasedForward) {
                        player.getPersistentData().putBoolean("jslop_fwd_inject", true);
                     }

                     player.getPersistentData().putBoolean("PRESS_W", false);
                     player.getPersistentData().putBoolean("PRESS_SPACE", false);

                     Class<?> releaseClass = Class.forName("net.mcreator.jujutsucraft.procedures.KeyStartTechniqueOnKeyReleasedProcedure");
                     releaseClass.getMethod("execute", Entity.class).invoke(null, player);

                     if (this.selectValue == 9) {
                        MobEffect cooldownFx = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "cooldown_time"));
                        if (cooldownFx != null && player.hasEffect(cooldownFx)) {
                           player.removeEffect(cooldownFx);
                           player.addEffect(new MobEffectInstance(cooldownFx, 30, 0, false, false));
                        }
                     }
                  } else {
                     if (this.targetCT == 5 && this.selectValue == 20) {
                        Advancement adv = player.getServer().getAdvancements().getAdvancement(new ResourceLocation("jujutsucraft", "skill_curseis_lifted"));
                        if (adv == null || !player.getAdvancements().getOrStartProgress(adv).isDone()) {
                           player.sendSystemMessage(Component.literal("Authentic Mutual Love requires Pure Love to have been used first."), true);
                           return;
                        }
                     }

                     if (this.selectValue == 9) {
                        player.getPersistentData().putBoolean("jslop_teleport_auth", true);
                     }

                     Class<?> changeClass = Class.forName("net.mcreator.jujutsucraft.procedures.KeyChangeTechniqueOnKeyPressedProcedure");
                     changeClass.getMethod("execute", LevelAccessor.class, double.class, double.class, double.class, Entity.class)
                             .invoke(null, world, x, y, z, player);

                     Class<?> startClass = Class.forName("net.mcreator.jujutsucraft.procedures.KeyStartTechniqueOnKeyPressedProcedure");
                     startClass.getMethod("execute", LevelAccessor.class, double.class, double.class, double.class, Entity.class)
                             .invoke(null, world, x, y, z, player);
                  }
               } catch (Exception e) {
                  Abyss.LOGGER.warn("Could not activate ability slot: {}", e.getMessage());
               }
            }
         }
      });
      ctx.setPacketHandled(true);
   }
}