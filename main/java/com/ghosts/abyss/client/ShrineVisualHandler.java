package com.ghosts.abyss.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import com.ghosts.abyss.AbyssGamerules;
import com.ghosts.abyss.Abyss;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;

@EventBusSubscriber(
        modid = "abyss",
        bus = Bus.FORGE,
        value = {Dist.CLIENT}
)
public class ShrineVisualHandler {
   private static final ResourceLocation SLASH_1 = new ResourceLocation("abyss", "textures/slash1.png");
   private static final ResourceLocation SLASH_2 = new ResourceLocation("abyss", "textures/slash2.png");
   private static final List<SlashInstance> slashes = new ArrayList<>();
   private static final Random RNG = new Random();
   private static long tick = 0L;
   private static boolean inShrine = false;

   private static boolean computeInShrine() {
      if (!AbyssGamerules.isClientShrineVisualEnabled()) {
         return false;
      } else {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null) {
            Player player = mc.player;
            MobEffect domainFx = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "domain_expansion"));
            if (domainFx != null && player.hasEffect(domainFx)) {
               try {
                  Object vars = player.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, (Direction) null).orElse(null);
                  if (vars != null) {
                     double ct = (Double) vars.getClass().getField("PlayerCurseTechnique").get(vars);
                     if (ct == 1.0D) {
                        return true;
                     }

                     double ct2 = (Double) vars.getClass().getField("PlayerCurseTechnique2").get(vars);
                     if (ct2 == 1.0D) {
                        return true;
                     }
                  }
               } catch (Exception e) {
                  Abyss.LOGGER.warn("[JSlop] ShrineVisuals caster check failed: {}", e.getMessage());
               }

               for (Entity e : mc.level.entitiesForRendering()) {
                  if (e != player && e instanceof LivingEntity le) {
                     if (le.hasEffect(domainFx)) {
                        try {
                           Object vars = le.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, (Direction) null).orElse(null);
                           if (vars != null) {
                              double ct = (Double) vars.getClass().getField("PlayerCurseTechnique").get(vars);
                              if (ct == 1.0D) {
                                 return true;
                              }

                              double ct2 = (Double) vars.getClass().getField("PlayerCurseTechnique2").get(vars);
                              if (ct2 == 1.0D) {
                                 return true;
                              }
                           }
                        } catch (Exception ignored) {
                        }

                        ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(le.getType());
                        if (type != null) {
                           String t = type.toString();
                           if (t.equals("jujutsucraft:sukuna") || t.equals("jujutsucraft:sukuna_fushiguro") || t.equals("jujutsucraft:sukuna_perfect")) {
                              return true;
                           }
                        }
                     }
                  }
               }

               return false;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   @SubscribeEvent
   public static void onClientTick(TickEvent.ClientTickEvent event) {
      if (event.phase == Phase.END) {
         Minecraft mc = Minecraft.getInstance();
         if (!mc.isPaused() && mc.player != null) {
            boolean was = inShrine;
            inShrine = computeInShrine();
            if (inShrine != was) {
               Player p = mc.player;
               MobEffect se = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "sukuna_effect"));
               MobEffect de = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "domain_expansion"));
               Abyss.LOGGER.info("[JSlop] ShrineVisuals inShrine={} sukuna_effect={} domain_expansion={}", inShrine, se != null && p.hasEffect(se), de != null && p.hasEffect(de));
            }

            if (!inShrine) {
               slashes.clear();
               tick = 0L;
            } else {
               ++tick;
               Iterator<SlashInstance> it = slashes.iterator();

               while (it.hasNext()) {
                  if (it.next().tick()) {
                     it.remove();
                  }
               }

               if (slashes.size() < 750 && mc.player != null) {
                  Player p = mc.player;
                  int n = 55 + RNG.nextInt(56);

                  for (int i = 0; i < n && slashes.size() < 360; ++i) {
                     SlashInstance s = new SlashInstance();
                     float dist = 3.0F + RNG.nextFloat() * 16.0F;
                     double yaw = RNG.nextDouble() * Math.PI * 2.0D;
                     double pitch = (RNG.nextDouble() - 0.5D) * Math.PI;
                     s.x = p.getX() + Math.cos(pitch) * Math.sin(yaw) * (double) dist;
                     s.y = p.getY() + 1.0D + Math.sin(pitch) * (double) dist * 0.6D;
                     s.z = p.getZ() + Math.cos(pitch) * Math.cos(yaw) * (double) dist;
                     s.rotX = RNG.nextFloat() * 360.0F;
                     s.rotY = RNG.nextFloat() * 360.0F;
                     s.rotZ = RNG.nextFloat() * 360.0F;
                     s.scale = (1.5F + RNG.nextFloat() * 3.5F) * 2.0F;
                     s.scaleV = s.scale * 0.1F;
                     s.textureIdx = RNG.nextInt(2);
                     s.maxAge = 1 + RNG.nextInt(2);
                     slashes.add(s);
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_PARTICLES) {
         if (inShrine && !slashes.isEmpty()) {
            Camera camera = event.getCamera();
            Vec3 cam = camera.getPosition();
            PoseStack ps = event.getPoseStack();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);

            for (int texIdx = 0; texIdx < 2; ++texIdx) {
               RenderSystem.setShaderTexture(0, texIdx == 0 ? SLASH_1 : SLASH_2);
               BufferBuilder buf = Tesselator.getInstance().getBuilder();
               buf.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
               boolean any = false;

               for (SlashInstance s : slashes) {
                  if (s.textureIdx == texIdx) {
                     any = true;
                     ps.pushPose();
                     ps.translate(s.x - cam.x, s.y - cam.y, s.z - cam.z);
                     ps.mulPose(Axis.YP.rotationDegrees(s.rotY));
                     ps.mulPose(Axis.XP.rotationDegrees(s.rotX));
                     ps.mulPose(Axis.ZP.rotationDegrees(s.rotZ));
                     Matrix4f m = ps.last().pose();
                     float hw = s.scale;
                     float hh = s.scaleV;
                     float a = s.getAlpha();
                     buf.vertex(m, -hw, -hh, 0.0F).uv(0.0F, 1.0F).color(1.0F, 1.0F, 1.0F, a).endVertex();
                     buf.vertex(m, hw, -hh, 0.0F).uv(1.0F, 1.0F).color(1.0F, 1.0F, 1.0F, a).endVertex();
                     buf.vertex(m, hw, hh, 0.0F).uv(1.0F, 0.0F).color(1.0F, 1.0F, 1.0F, a).endVertex();
                     buf.vertex(m, -hw, hh, 0.0F).uv(0.0F, 0.0F).color(1.0F, 1.0F, 1.0F, a).endVertex();
                     ps.popPose();
                  }
               }

               if (any) {
                  Tesselator.getInstance().end();
               } else {
                  buf.discard();
               }
            }

            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }
      }
   }

   @SubscribeEvent
   public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
      if (inShrine) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null) {
            float t = ((float) mc.level.getGameTime() + (float) event.getPartialTick()) * 1.05F;
            float amp = 0.9F;
            double dy = Math.sin((double) t * 1.1D) * (double) amp + Math.sin((double) t * 2.3D) * (double) amp * 0.4D;
            double dp = Math.cos((double) t * 0.9D) * (double) amp + Math.cos((double) t * 1.7D) * (double) amp * 0.35D;
            event.setYaw((float) ((double) event.getYaw() + dy));
            event.setPitch((float) ((double) event.getPitch() + dp));
         }
      }
   }

   private static class SlashInstance {
      double x;
      double y;
      double z;
      float rotX;
      float rotY;
      float rotZ;
      float scale;
      float scaleV;
      int textureIdx;
      int age;
      int maxAge;

      boolean tick() {
         return ++this.age >= this.maxAge;
      }

      float getAlpha() {
         float p = (float) this.age / (float) this.maxAge;
         return p > 0.75F ? (1.0F - p) / 0.25F : 1.0F;
      }
   }
}