package com.ghosts.abyss.gui;

import com.ghosts.abyss.AdvTechPoints;
import com.ghosts.abyss.AbyssGamerules;
import com.ghosts.abyss.KeyBindings;
import com.ghosts.abyss.ModItems;
import com.ghosts.abyss.PerfectBodySync;
import com.ghosts.abyss.RefinementPoints;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketPurchaseBindingVow;
import com.ghosts.abyss.network.PacketRecoverBindingVow;
import com.ghosts.abyss.network.PacketRequestUnlockOpenDomain;
import com.ghosts.abyss.network.PacketToggleFourArms;
import com.ghosts.abyss.network.PacketUnlockCursedObjectTransformation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;

public class AscendantTab implements IMenuTab {
   private static final int COL_HEADER = -5537836;
   private static final int COL_LABEL = -3355444;
   private static final int COL_GOLD = -10496;
   private static final int COL_GREEN = -12255352;
   private static final int COL_BORDER = -9814112;
   private static final int COL_GREY = -7829368;
   private static final int COL_DIVINE_BG = -452657136;
   private static final int COL_DIVINE_BORDER = -4684277;
   private static final int COL_DIVINE_GOLD = -10496;
   private static final int COL_DIVINE_STAR = -1519361;
   private static final int COL_DIVINE_AMBER = -19641;
   private static final int COL_REQ_MET = -12255352;
   private static final int COL_REQ_UNMET = -10066330;
   private static final int COL_COT_BG = -451936251;
   private static final int COL_COT_BORDER = -7667712;
   private static final int COL_COT_INNER = 1149763584;
   private static final int COL_COT_TEXT_LOCKED = -4491401;
   private static final int COL_COT_TEXT_UNLOCKED = -17477;
   private static final int COL_PB_BG = -452657136;
   private static final int COL_PB_BORDER = -9814112;
   private static final int COL_PB_INNER = 1147813792;
   private static final int PURCHASE_BTN_Y_OFFSET = 103;
   private static final int PB_SECTION_Y = 5;
   private static final int PB_SECTION_H = 122;
   private static final int COT_SECTION_Y = 135;
   private static final int COT_SECTION_H_LOCKED = 132;
   private static final int COT_SECTION_H_UNLOCKED = 84;
   private static final int COT_OD_GAP = 8;
   private static final int OD_SECTION_H_LOCKED = 160;
   private static final int OD_SECTION_H_UNLOCKED = 130;
   private JslopButton purchaseButton;
   private JslopButton recoverVowButton;
   private JslopButton toggleFourArmsButton;
   private JslopButton transformUnlockButton;
   private JslopButton unlockOpenDomainButton;
   private int scrollOffset = 0;
   private int storedPanelH = 230;
   private int contentHeight = 0;

   private static int odSectionY(boolean cotUnlocked) {
      return 135 + (cotUnlocked ? 84 : 132) + 8;
   }

   private static int contentH(boolean cotUnlocked, boolean odUnlocked) {
      return odSectionY(cotUnlocked) + (odUnlocked ? 130 : 160) + 10;
   }

   @Override
   public int getScrollOffset() {
      return this.scrollOffset;
   }

   @Override
   public int getContentHeight() {
      return this.contentHeight;
   }

   @Override
   public Component getTitle() {
      return Component.translatable("tab.abyss.ascendant");
   }

   @Override
   public List<AbstractWidget> buildWidgets(int px, int py, int pw, int ph) {
      this.storedPanelH = ph;
      this.scrollOffset = 0;
      List<AbstractWidget> widgets = new ArrayList<>();
      Player player = Minecraft.getInstance().player;
      boolean cotUnlocked = player != null && player.getPersistentData().getBoolean("cursed_object_transform_unlocked");
      int btnW = 180;
      int btnH = 16;

      this.purchaseButton = JslopButton.jslopBuilder(Component.translatable("button.abyss.purchase_binding_vow"), (btn) -> ModNetwork.CHANNEL.sendToServer(new PacketPurchaseBindingVow()))
              .bounds(px + (pw - btnW) / 2, py + 103, btnW, btnH)
              .colors(-9814112, -6656048, -872087536, -871561179, 711606176)
              .build();
      widgets.add(this.purchaseButton);

      this.recoverVowButton = JslopButton.jslopBuilder(Component.translatable("button.abyss.recover_binding_vow"), (btn) -> ModNetwork.CHANNEL.sendToServer(new PacketRecoverBindingVow()))
              .bounds(px + (pw - 180) / 2, py + 103, 180, 18)
              .colors(-9814112, -6656048, -872087536, -871561179, 711606176)
              .build();
      widgets.add(this.recoverVowButton);

      this.toggleFourArmsButton = JslopButton.jslopBuilder(Component.translatable("button.abyss.toggle_four_arms_hide"), (btn) -> ModNetwork.CHANNEL.sendToServer(new PacketToggleFourArms()))
              .bounds(px + (pw - btnW) / 2, py + 103, btnW, btnH)
              .colors(-9814112, -6656048, -872087536, -871561179, 711606176)
              .build();
      widgets.add(this.toggleFourArmsButton);

      int cotX = px + 8;
      int cotW = pw - 16;
      int cotY = py + 135;
      if (!cotUnlocked) {
         this.transformUnlockButton = JslopButton.jslopBuilder(Component.translatable("button.abyss.unlock_cot"), (btn) -> ModNetwork.CHANNEL.sendToServer(new PacketUnlockCursedObjectTransformation()))
                 .bounds(cotX + (cotW - 180) / 2, cotY + 132 - 26, 180, 18)
                 .colors(-7667712, -4513760, -871366651, -870318064, 1149763584)
                 .build();
         widgets.add(this.transformUnlockButton);
      } else {
         this.transformUnlockButton = null;
      }

      boolean restrictedCT = isRestrictedCT(player);
      int odY = py + odSectionY(cotUnlocked);
      int odX = px + 8;
      int odW = pw - 16;
      this.unlockOpenDomainButton = JslopButton.jslopBuilder(Component.translatable("button.abyss.unlock_open_domain"), (btn) -> ModNetwork.CHANNEL.sendToServer(new PacketRequestUnlockOpenDomain()))
              .bounds(odX + (odW - 180) / 2, odY + 160 - 26, 180, 18)
              .colors(-4684277, -1527760, -872087536, -871757792, 1152943627)
              .build();
      if (restrictedCT) {
         this.unlockOpenDomainButton.visible = false;
      }

      widgets.add(this.unlockOpenDomainButton);
      return widgets;
   }

   public boolean onMouseScrolled(double mx, double my, double delta) {
      Player player = Minecraft.getInstance().player;
      boolean cotUnlocked = player != null && player.getPersistentData().getBoolean("cursed_object_transform_unlocked");
      boolean odUnlocked = player != null && player.getPersistentData().getBoolean("open_domain_unlocked");
      int maxScroll = Math.max(0, contentH(cotUnlocked, odUnlocked) - this.storedPanelH);
      if (maxScroll == 0) {
         return false;
      } else {
         this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollOffset - (int)(delta * 10.0F)));
         return true;
      }
   }

   public void onClose() {
      this.scrollOffset = 0;
   }

   @Override
   public void render(GuiGraphics g, int px, int py, int pw, int ph, int mx, int my, float pt) {
      this.storedPanelH = ph;
      Player player = Minecraft.getInstance().player;
      int atAvailable = player != null ? AdvTechPoints.getAvailable(player) : 0;
      int rpAvailable = player != null ? RefinementPoints.getAvailable(player) : 0;
      int rpRefinement = player != null ? RefinementPoints.getRefinement(player) : 0;
      boolean perfectBody = player != null && player.getPersistentData().getBoolean("perfect_body");
      boolean hasVow = player != null && player.getInventory().items.stream().anyMatch((s) -> s.getItem() == ModItems.BINDING_VOW.get());
      boolean hasGrade1 = this.hasGrade1Advancement(player);
      boolean cotUnlocked = player != null && player.getPersistentData().getBoolean("cursed_object_transform_unlocked");
      boolean odUnlocked = player != null && player.getPersistentData().getBoolean("open_domain_unlocked");
      boolean odToggled = player != null && player.getPersistentData().getBoolean("open_domain_toggled");
      boolean restrictedCT = isRestrictedCT(player);
      boolean odWitnessed = player != null && !restrictedCT && player.getPersistentData().getBoolean("witnessed_open_domain");
      boolean hasSpecial = this.hasSpecialGradeAdvancement(player);
      boolean canUnlock = !restrictedCT && odWitnessed && hasSpecial && rpRefinement >= 50 && rpAvailable >= 20 && atAvailable >= 20;

      this.contentHeight = contentH(cotUnlocked, odUnlocked);
      int maxScroll = Math.max(0, this.contentHeight - ph);
      this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollOffset));

      boolean pbGamerule = AbyssGamerules.isClientPerfectBodyEnabled();
      boolean cotGamerule = AbyssGamerules.isClientCursedObjectTransformEnabled();
      boolean odGamerule = AbyssGamerules.isClientOpenDomainEnabled();
      boolean vowPurchased = player != null && player.getPersistentData().getBoolean("binding_vow_purchased");

      int purchaseY = py + 103 - this.scrollOffset;
      if (this.purchaseButton != null) {
         this.purchaseButton.setY(purchaseY);
         this.purchaseButton.active = pbGamerule && !perfectBody && !hasVow && !vowPurchased && atAvailable >= 10 && hasGrade1;
         this.purchaseButton.visible = !vowPurchased && !perfectBody && purchaseY + 18 > py && purchaseY < py + ph;
      }

      if (this.recoverVowButton != null) {
         this.recoverVowButton.setY(purchaseY);
         this.recoverVowButton.active = pbGamerule && vowPurchased && !perfectBody && !hasVow;
         this.recoverVowButton.visible = vowPurchased && !perfectBody && purchaseY + 18 > py && purchaseY < py + ph;
      }

      if (this.toggleFourArmsButton != null) {
         boolean armsVisible = player != null && Boolean.TRUE.equals(PerfectBodySync.MAP.get(player.getUUID()));
         this.toggleFourArmsButton.setY(purchaseY);
         this.toggleFourArmsButton.setMessage(Component.translatable(armsVisible ? "button.abyss.toggle_four_arms_hide" : "button.abyss.toggle_four_arms_show"));
         this.toggleFourArmsButton.active = true;
         this.toggleFourArmsButton.visible = perfectBody && purchaseY + 18 > py && purchaseY < py + ph;
      }

      int cotY = py + 135 - this.scrollOffset;
      int cotBtnY = cotY + 132 - 26;
      if (this.transformUnlockButton != null) {
         this.transformUnlockButton.setY(cotBtnY);
         this.transformUnlockButton.visible = !cotUnlocked && cotBtnY + 18 > py && cotBtnY < py + ph;
         this.transformUnlockButton.active = cotGamerule && atAvailable >= 20;
      }

      int odY = py + odSectionY(cotUnlocked) - this.scrollOffset;
      int unlockBtnY = odY + 160 - 26;
      if (this.unlockOpenDomainButton != null) {
         this.unlockOpenDomainButton.setY(unlockBtnY);
         this.unlockOpenDomainButton.visible = odGamerule && !restrictedCT && !odUnlocked && unlockBtnY + 18 > py && unlockBtnY < py + ph;
         this.unlockOpenDomainButton.active = odGamerule && canUnlock;
      }

      Font font = Minecraft.getInstance().font;
      g.enableScissor(px, py, px + pw, py + ph);

      int cy = py + 10 - this.scrollOffset;
      int pbX = px + 8;
      int pbW = pw - 16;
      int pbY = py + 5 - this.scrollOffset;

      g.fill(pbX, pbY, pbX + pbW, pbY + 122, COL_PB_BG);
      g.fill(pbX, pbY, pbX + pbW, pbY + 1, COL_PB_BORDER);
      g.fill(pbX, pbY + 122 - 1, pbX + pbW, pbY + 122, COL_PB_BORDER);
      g.fill(pbX, pbY, pbX + 1, pbY + 122, COL_PB_BORDER);
      g.fill(pbX + pbW - 1, pbY, pbX + pbW, pbY + 122, COL_PB_BORDER);
      g.fill(pbX + 2, pbY + 2, pbX + pbW - 2, pbY + 3, COL_PB_INNER);
      g.fill(pbX + 2, pbY + 122 - 3, pbX + pbW - 2, pbY + 122 - 2, COL_PB_INNER);

      Component pbHeader = Component.translatable("label.abyss.ascendant_header");
      g.drawString(font, pbHeader, pbX + (pbW - font.width(pbHeader)) / 2, cy, COL_HEADER, false);
      g.fill(pbX + 16, cy + 10, pbX + pbW - 16, cy + 11, COL_PB_INNER);
      cy += 18;

      Component statusText;
      int statusColor;
      if (perfectBody) {
         statusText = Component.translatable("label.abyss.perfect_body_active");
         statusColor = COL_GREEN;
      } else if (hasVow) {
         statusText = Component.translatable("label.abyss.binding_vow_held");
         statusColor = COL_GOLD;
      } else if (!hasGrade1) {
         statusText = Component.translatable("label.abyss.grade_required");
         statusColor = COL_GREY;
      } else {
         statusText = Component.translatable("label.abyss.perfect_body_locked");
         statusColor = COL_GREY;
      }

      g.drawString(font, statusText, pbX + (pbW - font.width(statusText)) / 2, cy, statusColor, false);
      cy += 16;

      Component pbdesc_1 = Component.translatable("label.abyss.perfect_body_desc_1");
      g.drawString(font, pbdesc_1, pbX + (pbW - font.width(pbdesc_1)) / 2, cy, COL_LABEL, false);
      cy += 10;
      Component pbdesc_2 = Component.translatable("label.abyss.perfect_body_desc_2");
      g.drawString(font, pbdesc_2, pbX + (pbW - font.width(pbdesc_2)) / 2, cy, COL_LABEL, false);
      cy += 10;
      Component pbdesc_3 = Component.translatable("label.abyss.perfect_body_desc_3");
      g.drawString(font, pbdesc_3, pbX + (pbW - font.width(pbdesc_3)) / 2, cy, COL_LABEL, false);
      cy += 14;

      Component pbCostLabel = Component.translatable("label.abyss.cost_advtech");
      String pbCostVal = atAvailable + " / 20";
      int pbCostTotalW = font.width(pbCostLabel) + 4 + font.width(pbCostVal);
      int pbCostStartX = pbX + (pbW - pbCostTotalW) / 2;
      g.drawString(font, pbCostLabel, pbCostStartX, cy, COL_LABEL, false);
      g.drawString(font, Component.literal(pbCostVal), pbCostStartX + font.width(pbCostLabel) + 4, cy, atAvailable >= 10 ? COL_GOLD : COL_GREY, false);

      int cotSectionH = cotUnlocked ? 84 : 132;
      int cotX = px + 8;
      int cotW = pw - 16;
      g.fill(cotX, cotY, cotX + cotW, cotY + cotSectionH, COL_COT_BG);
      g.fill(cotX, cotY, cotX + cotW, cotY + 1, COL_COT_BORDER);
      g.fill(cotX, cotY + cotSectionH - 1, cotX + cotW, cotY + cotSectionH, COL_COT_BORDER);
      g.fill(cotX, cotY, cotX + 1, cotY + cotSectionH, COL_COT_BORDER);
      g.fill(cotX + cotW - 1, cotY, cotX + cotW, cotY + cotSectionH, COL_COT_BORDER);
      g.fill(cotX + 2, cotY + 2, cotX + cotW - 2, cotY + 3, COL_COT_INNER);
      g.fill(cotX + 2, cotY + cotSectionH - 3, cotX + cotW - 2, cotY + cotSectionH - 2, COL_COT_INNER);

      int icy = cotY + 7;
      Component cotHeader = Component.translatable("label.abyss.cot_section");
      g.drawString(font, cotHeader, cotX + (cotW - font.width(cotHeader)) / 2, icy, -3390396, false);
      icy += 11;
      g.fill(cotX + 16, icy, cotX + cotW - 16, icy + 1, COL_COT_INNER);
      icy += 6;

      if (cotUnlocked) {
         Component u1 = Component.translatable("label.abyss.cot_unlocked_1");
         g.drawString(font, u1, cotX + (cotW - font.width(u1)) / 2, icy, COL_COT_TEXT_UNLOCKED, false);
         icy += 10;
         Component uConfirm = Component.literal("✔ Transformation Unlocked");
         g.drawString(font, uConfirm, cotX + (cotW - font.width(uConfirm)) / 2, icy, COL_GREEN, false);
         icy += 12;
         Component keyName = KeyBindings.TRANSFORM_TO_CURSED_OBJECT.getTranslatedKeyMessage();
         Component u2 = Component.literal("Press [").append(keyName).append("] to shed your form");
         g.drawString(font, u2, cotX + (cotW - font.width(u2)) / 2, icy, COL_COT_TEXT_UNLOCKED, false);
         icy += 10;
         Component u3 = Component.translatable("label.abyss.cot_unlocked_3");
         g.drawString(font, u3, cotX + (cotW - font.width(u3)) / 2, icy, COL_COT_TEXT_UNLOCKED, false);
         icy += 10;
         Component u4 = Component.translatable("label.abyss.cot_unlocked_4");
         g.drawString(font, u4, cotX + (cotW - font.width(u4)) / 2, icy, COL_COT_TEXT_UNLOCKED, false);
      } else {
         Component f1 = Component.translatable("label.abyss.cot_flavor_1");
         g.drawString(font, f1, cotX + (cotW - font.width(f1)) / 2, icy, COL_COT_TEXT_LOCKED, false);
         icy += 10;
         Component f2 = Component.translatable("label.abyss.cot_flavor_2");
         g.drawString(font, f2, cotX + (cotW - font.width(f2)) / 2, icy, COL_COT_TEXT_LOCKED, false);
         icy += 10;
         Component f3 = Component.translatable("label.abyss.cot_flavor_3");
         g.drawString(font, f3, cotX + (cotW - font.width(f3)) / 2, icy, COL_COT_TEXT_LOCKED, false);
         icy += 10;
         Component f4 = Component.translatable("label.abyss.cot_flavor_4");
         g.drawString(font, f4, cotX + (cotW - font.width(f4)) / 2, icy, COL_COT_TEXT_LOCKED, false);
         icy += 10;
         Component f5 = Component.translatable("label.abyss.cot_flavor_5");
         g.drawString(font, f5, cotX + (cotW - font.width(f5)) / 2, icy, COL_COT_TEXT_LOCKED, false);
         icy += 14;
         boolean atpMet = atAvailable >= 20;
         Component atpLine = Component.literal(atpMet ? "✔ " : "✘ ").append(Component.literal("Adv. Tech Points: " + atAvailable + "/20"));
         g.drawString(font, atpLine, cotX + (cotW - font.width(atpLine)) / 2, icy, atpMet ? COL_REQ_MET : COL_REQ_UNMET, false);
      }

      int odSectionH = odUnlocked ? 130 : 160;
      int odX = px + 8;
      int odW = pw - 16;
      g.fill(odX, odY, odX + odW, odY + odSectionH, COL_DIVINE_BG);
      g.fill(odX, odY, odX + odW, odY + 1, COL_DIVINE_BORDER);
      g.fill(odX, odY + odSectionH - 1, odX + odW, odY + odSectionH, COL_DIVINE_BORDER);
      g.fill(odX, odY, odX + 1, odY + odSectionH, COL_DIVINE_BORDER);
      g.fill(odX + odW - 1, odY, odX + odW, odY + odSectionH, COL_DIVINE_BORDER);
      g.fill(odX + 2, odY + 2, odX + odW - 2, odY + 3, 1152943627);
      g.fill(odX + 2, odY + odSectionH - 3, odX + odW - 2, odY + odSectionH - 2, 1152943627);

      icy = odY + 7;
      Component odHeader = Component.translatable("label.abyss.open_domain_section");
      g.drawString(font, odHeader, odX + (odW - font.width(odHeader)) / 2, icy, COL_DIVINE_GOLD, false);
      icy += 11;
      g.fill(odX + 16, icy, odX + odW - 16, icy + 1, -2001172981);
      icy += 6;

      if (odUnlocked) {
         Component toggleStatus = odToggled ? Component.translatable("label.abyss.open_domain_status_active") : Component.translatable("label.abyss.open_domain_status_inactive");
         int toggleColor = odToggled ? COL_DIVINE_GOLD : COL_GREY;
         g.drawString(font, toggleStatus, odX + (odW - font.width(toggleStatus)) / 2, icy, toggleColor, false);
         icy += 14;
         Component f1 = Component.translatable("label.abyss.open_domain_flavor_1");
         g.drawString(font, f1, odX + (odW - font.width(f1)) / 2, icy, COL_DIVINE_STAR, false);
         icy += 10;
         Component f2 = Component.translatable("label.abyss.open_domain_flavor_2");
         g.drawString(font, f2, odX + (odW - font.width(f2)) / 2, icy, COL_DIVINE_STAR, false);
         icy += 14;
         Component u1 = Component.translatable("label.abyss.open_domain_unlocked_1");
         g.drawString(font, u1, odX + (odW - font.width(u1)) / 2, icy, COL_DIVINE_STAR, false);
         icy += 10;
         Component u2 = Component.translatable("label.abyss.open_domain_unlocked_2");
         g.drawString(font, u2, odX + (odW - font.width(u2)) / 2, icy, COL_DIVINE_STAR, false);
         icy += 10;
         Component u3 = Component.translatable("label.abyss.open_domain_unlocked_3");
         g.drawString(font, u3, odX + (odW - font.width(u3)) / 2, icy, COL_DIVINE_STAR, false);
         icy += 14;
         Component u4 = Component.translatable("label.abyss.open_domain_unlocked_4");
         g.drawString(font, u4, odX + (odW - font.width(u4)) / 2, icy, COL_DIVINE_STAR, false);
         icy += 10;
         Component u5 = Component.translatable("label.abyss.open_domain_unlocked_5");
         g.drawString(font, u5, odX + (odW - font.width(u5)) / 2, icy, COL_DIVINE_STAR, false);
         icy += 10;
         Component u6 = Component.translatable("label.abyss.open_domain_unlocked_6");
         g.drawString(font, u6, odX + (odW - font.width(u6)) / 2, icy, COL_DIVINE_STAR, false);
      } else {
         Component f1 = Component.translatable("label.abyss.open_domain_flavor_1");
         g.drawString(font, f1, odX + (odW - font.width(f1)) / 2, icy, COL_DIVINE_AMBER, false);
         icy += 10;
         Component f2 = Component.translatable("label.abyss.open_domain_flavor_2");
         g.drawString(font, f2, odX + (odW - font.width(f2)) / 2, icy, COL_DIVINE_AMBER, false);
         icy += 10;
         Component f3 = Component.translatable("label.abyss.open_domain_flavor_3");
         g.drawString(font, f3, odX + (odW - font.width(f3)) / 2, icy, COL_DIVINE_AMBER, false);
         icy += 10;
         Component f4 = Component.translatable("label.abyss.open_domain_flavor_4");
         g.drawString(font, f4, odX + (odW - font.width(f4)) / 2, icy, COL_DIVINE_AMBER, false);
         icy += 14;

         int icx = odX + 10;
         this.renderReq(g, font, icx, icy, "label.abyss.open_domain_req_grade", hasSpecial);
         icy += 10;
         this.renderReq(g, font, icx, icy, "label.abyss.open_domain_req_witnessed", odWitnessed);
         icy += 10;

         boolean refMet = rpRefinement >= 50;
         Component refLine = Component.literal(refMet ? "✔ " : "✘ ").append(Component.literal("Max Domain Refinement (" + rpRefinement + "/50)"));
         g.drawString(font, refLine, icx, icy, refMet ? COL_REQ_MET : COL_REQ_UNMET, false);
         icy += 10;

         boolean rpMet = rpAvailable >= 20;
         Component rpLine = Component.literal(rpMet ? "✔ " : "✘ ").append(Component.literal("Refinement Points: " + rpAvailable + "/20"));
         g.drawString(font, rpLine, icx, icy, rpMet ? COL_REQ_MET : COL_REQ_UNMET, false);
         icy += 10;

         boolean atpMet = atAvailable >= 20;
         Component atpLine = Component.literal(atpMet ? "✔ " : "✘ ").append(Component.literal("Adv. Tech Points: " + atAvailable + "/20"));
         g.drawString(font, atpLine, icx, icy, atpMet ? COL_REQ_MET : COL_REQ_UNMET, false);
      }

      if (!pbGamerule) {
         drawGameruleOverlay(g, font, pbX, pbY, pbW, 122);
      }

      if (!cotGamerule) {
         drawGameruleOverlay(g, font, cotX, cotY, cotW, cotSectionH);
      }

      if (!odGamerule) {
         drawGameruleOverlay(g, font, odX, odY, odW, odSectionH);
      } else if (restrictedCT) {
         g.fill(odX, odY, odX + odW, odY + odSectionH, -653192943);
         Component disabledMsg = Component.literal("Feature disabled for your active technique.");
         int msgX = odX + (odW - font.width(disabledMsg)) / 2;
         int msgY = odY + (odSectionH - 8) / 2;
         g.drawString(font, disabledMsg, msgX, msgY, COL_GREY, false);
      }

      g.disableScissor();
   }

   private static void drawGameruleOverlay(GuiGraphics g, Font font, int x, int y, int w, int h) {
      g.fill(x, y, x + w, y + h, -653192943);
      Component msg = Component.literal("Disabled by server rule.");
      g.drawString(font, msg, x + (w - font.width(msg)) / 2, y + (h - 8) / 2, COL_GREY, false);
   }

   private void renderReq(GuiGraphics g, Font font, int x, int y, String key, boolean met) {
      Component line = Component.literal(met ? "✔ " : "✘ ").append(Component.translatable(key));
      g.drawString(font, line, x, y, met ? COL_REQ_MET : COL_REQ_UNMET, false);
   }

   @Override
   public void renderTooltips(GuiGraphics g, int px, int py, int pw, int ph, int mx, int my) {
      Player player = Minecraft.getInstance().player;
      Font font = Minecraft.getInstance().font;

      if (this.purchaseButton != null && this.purchaseButton.visible && !this.purchaseButton.active && this.isHovering(this.purchaseButton, mx, my)) {
         boolean perfectBody = player != null && player.getPersistentData().getBoolean("perfect_body");
         boolean hasVow = player != null && player.getInventory().items.stream().anyMatch((s) -> s.getItem() == ModItems.BINDING_VOW.get());
         boolean hasGrade1 = this.hasGrade1Advancement(player);
         String tipKey;
         if (perfectBody) {
            tipKey = "tooltip.abyss.perfect_body_already";
         } else if (hasVow) {
            tipKey = "tooltip.abyss.binding_vow_already_owned";
         } else if (!hasGrade1) {
            tipKey = "tooltip.abyss.grade_required_tip";
         } else {
            tipKey = "tooltip.abyss.no_advtech_points";
         }

         g.renderComponentTooltip(font, List.of(Component.translatable(tipKey)), mx, my);
      }

      if (this.transformUnlockButton != null && this.transformUnlockButton.visible && !this.transformUnlockButton.active && this.isHovering(this.transformUnlockButton, mx, my)) {
         g.renderComponentTooltip(font, List.of(Component.translatable("tooltip.abyss.cot_no_atp")), mx, my);
      }

      if (this.unlockOpenDomainButton != null && this.unlockOpenDomainButton.visible && !this.unlockOpenDomainButton.active && this.isHovering(this.unlockOpenDomainButton, mx, my)) {
         boolean odWitnessed = player != null && player.getPersistentData().getBoolean("witnessed_open_domain");
         boolean hasSpecial = this.hasSpecialGradeAdvancement(player);
         int rpRefinement = player != null ? RefinementPoints.getRefinement(player) : 0;
         int rpAvailable = player != null ? RefinementPoints.getAvailable(player) : 0;

         String tipKey;
         if (!odWitnessed) {
            tipKey = "tooltip.abyss.open_domain_not_witnessed";
         } else if (!hasSpecial) {
            tipKey = "tooltip.abyss.open_domain_no_grade";
         } else if (rpRefinement < 50) {
            tipKey = "tooltip.abyss.open_domain_no_refinement";
         } else if (rpAvailable < 20) {
            tipKey = "tooltip.abyss.open_domain_no_rp";
         } else {
            tipKey = "tooltip.abyss.open_domain_no_atp";
         }

         g.renderComponentTooltip(font, List.of(Component.translatable(tipKey)), mx, my);
      }
   }

   private boolean hasGrade1Advancement(Player player) {
      if (player instanceof ServerPlayer sp) {
         Advancement adv = sp.getServer().getAdvancements().getAdvancement(new ResourceLocation("jujutsucraft:sorcerer_grade_1"));
         return adv != null && sp.getAdvancements().getOrStartProgress(adv).isDone();
      } else {
         return true;
      }
   }

   private boolean hasSpecialGradeAdvancement(Player player) {
      if (player instanceof ServerPlayer sp) {
         Advancement adv = sp.getServer().getAdvancements().getAdvancement(new ResourceLocation("jujutsucraft:sorcerer_grade_special"));
         return adv != null && sp.getAdvancements().getOrStartProgress(adv).isDone();
      } else {
         return true;
      }
   }

   private static boolean isRestrictedCT(Player player) {
      if (player == null) {
         return false;
      } else {
         try {
            Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
            Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
            capField.setAccessible(true);
            Capability<Object> cap = (Capability) capField.get(null);
            Object vars = player.getCapability(cap, null).orElse(null);
            if (vars == null) {
               return false;
            } else {
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
            }
         } catch (Exception var13) {
            return false;
         }
      }
   }

   private boolean isHovering(AbstractWidget widget, int mx, int my) {
      return mx >= widget.getX() && mx <= widget.getX() + widget.getWidth() && my >= widget.getY() && my <= widget.getY() + widget.getHeight();
   }
}