package com.ghosts.abyss.gui;

import com.ghosts.abyss.ClientAbilityCache;
import com.ghosts.abyss.KeyBindings;
import com.ghosts.abyss.KeybindConfig;
import com.ghosts.abyss.KeybindConfig.Ability;
import com.ghosts.abyss.ScannedAbility;
import com.ghosts.abyss.csm.ClientCsmCache;
import com.ghosts.abyss.csm.CsmSpiritData;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketRequestAbilityScan;
import com.ghosts.abyss.network.PacketSaveKeybinds;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;

public class KeybindScreen extends Screen {
   private static final int COL_BG = -1157627904;
   private static final int COL_PANEL = -15592932;
   private static final int COL_BORDER = -9814112;
   private static final int COL_HEADER = -5537836;
   private static final int COL_GOLD = -10496;
   private static final int COL_GREY = -7829368;
   private static final int COL_LABEL = -3355444;
   private static final int COL_SELECTED_BG = 1435020031;
   private static final int COL_SLOT_BG = 581610239;
   private static final int COL_ASSIGNED_BG = 1145372552;
   private static final int COL_DIVIDER = 1147813792;
   private static final int COL_NO_CT = 1149798536;

   private static final int PANEL_W = 400;
   private static final int PANEL_H = 310;
   private static final int TITLE_H = 24;
   private static final int BORDER = 1;
   private static final int PAD = 10;
   private static final int ROW_H = 20;
   private static final int COL_GAP = 10;
   private static final int COL_W = 185;
   private static final int SLOT_COL_W = 185;
   private static final int ACTION_COL_W = 185;

   private int selectedSlot = -1;
   private int slotScroll = 0;
   private int actionScroll = 0;
   private int currentCT = Integer.MIN_VALUE;
   private boolean showSavedOverlay = false;
   private final KeybindConfig.Ability[] workingSlots = new KeybindConfig.Ability[8];

   private int px;
   private int py;
   private int slotColX;
   private int actionColX;
   private int contentY;
   private int contentH;

   private Button[] slotButtons;
   private Button[] actionButtons;
   private JslopButton saveButton;
   private JslopButton closeButton;
   private JslopButton confirmSaveButton;
   private int lastCacheVersion = -1;

   public KeybindScreen() {
      super(Component.translatable("screen.abyss.keybind_menu"));
   }

   @Override
   protected void init() {
      super.init();
      this.px = (this.width - PANEL_W) / 2;
      this.py = (this.height - PANEL_H) / 2;
      this.slotColX = this.px + PAD;
      this.actionColX = this.px + PANEL_W - PAD - ACTION_COL_W;
      this.contentY = this.py + TITLE_H + 34;
      this.contentH = 222;
      this.currentCT = this.readCurrentCT();
      this.lastCacheVersion = ClientAbilityCache.getVersion();

      ModNetwork.CHANNEL.sendToServer(new PacketRequestAbilityScan());

      for (int i = 0; i < 8; ++i) {
         this.workingSlots[i] = KeybindConfig.getSlot(i);
      }

      this.buildWidgets();
   }

   private int readCurrentCT() {
      Player player = Minecraft.getInstance().player;
      if (player == null) {
         return Integer.MIN_VALUE;
      }

      try {
         Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
         Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
         capField.setAccessible(true);

         @SuppressWarnings("unchecked")
         Capability<Object> cap = (Capability<Object>) capField.get(null);
         Object vars = player.getCapability(cap, (Direction) null).orElse(null);

         if (vars == null) {
            return Integer.MIN_VALUE;
         }

         Field secondField = vars.getClass().getDeclaredField("SecondTechnique");
         secondField.setAccessible(true);
         boolean second = (Boolean) secondField.get(vars);

         String ctFieldName = second ? "PlayerCurseTechnique2" : "PlayerCurseTechnique";
         Field ctField = vars.getClass().getDeclaredField(ctFieldName);
         ctField.setAccessible(true);
         return (int) (double) (Double) ctField.get(vars);
      } catch (Exception e) {
         return Integer.MIN_VALUE;
      }
   }

   private List<ScannedAbility> getAbilityList() {
      List<ScannedAbility> base = new ArrayList<>();
      if (this.currentCT == 18) {
         for (ScannedAbility a : ClientAbilityCache.getForCT(this.currentCT)) {
            if (a.select() != 12) {
               base.add(a);
            }
         }

         List<CsmSpiritData> allSpirits = ClientCsmCache.getAll();
         for (int i = 0; i < allSpirits.size(); ++i) {
            CsmSpiritData sd = allSpirits.get(i);
            if (sd.count > 0) {
               base.add(new ScannedAbility(18, 1000 + i, sd.displayName + " ×" + sd.count, 0.0D, false, false, true));
            }
         }
      } else {
         base.addAll(ClientAbilityCache.getForCT(this.currentCT));
      }

      return base;
   }

   private void buildWidgets() {
      this.clearWidgets();
      this.slotButtons = new Button[8];

      for (int i = 0; i < 8; ++i) {
         int slotIndex = i;
         this.slotButtons[i] = JslopButton.jslopBuilder(this.slotLabel(i), btn -> {
            this.selectedSlot = this.selectedSlot == slotIndex ? -1 : slotIndex;
            this.refreshSlotLabels();
         }).bounds(this.slotColX, this.contentY + i * ROW_H, 183, 18).build();
         this.addRenderableWidget(this.slotButtons[i]);
      }

      List<ScannedAbility> abilities = this.getAbilityList();
      this.actionButtons = new Button[abilities.size() + 1];

      this.actionButtons[0] = JslopButton.jslopBuilder(Component.literal("— Unbound —"), btn -> {
         if (this.selectedSlot >= 0) {
            this.workingSlots[this.selectedSlot] = Ability.UNBOUND;
            this.refreshSlotLabels();
         }
      }).bounds(this.actionColX, this.contentY, 183, 18).build();
      this.addRenderableWidget(this.actionButtons[0]);

      for (int i = 0; i < abilities.size(); ++i) {
         ScannedAbility scanned = abilities.get(i);
         String label = scanned.available() ? scanned.name() : "⚠ " + scanned.name();
         KeybindConfig.Ability ability = scanned.toSlotAbility();

         this.actionButtons[i + 1] = JslopButton.jslopBuilder(Component.literal(label), btn -> {
            if (this.selectedSlot >= 0) {
               this.workingSlots[this.selectedSlot] = ability;
               this.refreshSlotLabels();
            }
         }).bounds(this.actionColX, this.contentY + (i + 1) * ROW_H, 183, 18).build();
         this.addRenderableWidget(this.actionButtons[i + 1]);
      }

      int btnY = this.py + PANEL_H - 22;
      this.saveButton = JslopButton.jslopBuilder(Component.translatable("button.abyss.keybind_save"), btn -> {
         for (int i = 0; i < 8; ++i) {
            KeybindConfig.setSlot(i, this.workingSlots[i]);
         }

         ModNetwork.CHANNEL.sendToServer(new PacketSaveKeybinds(KeybindConfig.getSlots()));
         this.showSavedOverlay = true;
      }).bounds(this.px + PAD, btnY, 130, 16).build();
      this.addRenderableWidget(this.saveButton);

      this.closeButton = JslopButton.jslopBuilder(Component.translatable("gui.done"), btn -> this.onClose())
              .bounds(this.px + PANEL_W - PAD - 80, btnY, 80, 16).build();
      this.addRenderableWidget(this.closeButton);

      int overlayBtnX = this.width / 2 - 40;
      int overlayBtnY = this.height / 2 + 6;
      this.confirmSaveButton = JslopButton.jslopBuilder(Component.literal("OK"), btn -> this.showSavedOverlay = false)
              .bounds(overlayBtnX, overlayBtnY, 80, 16).build();
      this.confirmSaveButton.visible = false;
      this.addRenderableWidget(this.confirmSaveButton);
   }

   @Override
   public void render(GuiGraphics g, int mx, int my, float pt) {
      g.fill(0, 0, this.width, this.height, COL_BG);
      g.fill(this.px, this.py, this.px + PANEL_W, this.py + PANEL_H, COL_PANEL);
      this.drawOutline(g, this.px - 1, this.py - 1, this.px + PANEL_W + 1, this.py + PANEL_H + 1, COL_BORDER);

      g.drawCenteredString(this.font, Component.translatable("screen.abyss.keybind_menu"), this.px + 200, this.py + 6, COL_HEADER);
      g.fill(this.px + 16, this.py + TITLE_H, this.px + PANEL_W - 16, this.py + TITLE_H + 1, COL_DIVIDER);

      Component hint = Component.translatable("label.abyss.keybind_hint");
      g.drawCenteredString(this.font, hint, this.px + 200, this.py + TITLE_H + 8, COL_GREY);

      int headerY = this.contentY - 9;
      g.drawString(this.font, Component.literal("Slot  [Key]  →  Ability"), this.slotColX, headerY, COL_LABEL, false);

      Component actionHeader = this.selectedSlot >= 0
              ? Component.literal("Assign to Slot " + (this.selectedSlot + 1) + ":")
              : Component.literal("← Select a slot first");
      g.drawString(this.font, actionHeader, this.actionColX, headerY, this.selectedSlot >= 0 ? COL_GOLD : COL_GREY, false);

      int divX = this.slotColX + SLOT_COL_W + 5;
      g.fill(divX, this.contentY - 4, divX + 1, this.contentY + this.contentH + 4, COL_DIVIDER);

      // --- Render Slots Left Column ---
      g.enableScissor(this.slotColX, this.contentY, this.slotColX + SLOT_COL_W, this.contentY + this.contentH);

      for (int i = 0; i < 8; ++i) {
         int rowY = this.contentY + i * ROW_H - this.slotScroll;
         boolean visible = rowY >= this.contentY && rowY + ROW_H <= this.contentY + this.contentH;

         if (this.slotButtons[i] != null) {
            this.slotButtons[i].setY(rowY);
            this.slotButtons[i].visible = visible;
         }

         if (visible) {
            int bg = i == this.selectedSlot ? COL_SELECTED_BG : COL_SLOT_BG;
            g.fill(this.slotColX, rowY, this.slotColX + SLOT_COL_W - 2, rowY + ROW_H - 2, bg);
         }
      }

      g.disableScissor();

      // --- Render Actions Right Column ---
      List<ScannedAbility> abilities = this.getAbilityList();
      g.enableScissor(this.actionColX, this.contentY, this.actionColX + ACTION_COL_W, this.contentY + this.contentH);

      if (this.currentCT != Integer.MIN_VALUE && !abilities.isEmpty()) {
         int totalRows = abilities.size() + 1;
         KeybindConfig.Ability currentAssigned = this.selectedSlot >= 0 ? this.workingSlots[this.selectedSlot] : null;

         for (int i = 0; i < totalRows; ++i) {
            int rowY = this.contentY + i * ROW_H - this.actionScroll;
            boolean visible = rowY >= this.contentY && rowY + ROW_H <= this.contentY + this.contentH;

            if (this.actionButtons != null && i < this.actionButtons.length && this.actionButtons[i] != null) {
               this.actionButtons[i].setY(rowY);
               this.actionButtons[i].active = this.selectedSlot >= 0;
               this.actionButtons[i].visible = visible;
            }

            if (visible && this.selectedSlot >= 0 && currentAssigned != null) {
               KeybindConfig.Ability thisAbility = i == 0 ? Ability.UNBOUND : abilities.get(i - 1).toSlotAbility();
               boolean isAssigned = (currentAssigned.isUnbound() && thisAbility.isUnbound()) ||
                       (!currentAssigned.isUnbound() && !thisAbility.isUnbound() && currentAssigned.selectValue() == thisAbility.selectValue());

               if (isAssigned) {
                  g.fill(this.actionColX, rowY, this.actionColX + ACTION_COL_W - 2, rowY + ROW_H - 2, COL_ASSIGNED_BG);
               }

               if (!currentAssigned.isUnbound() && i > 0 && !abilities.get(i - 1).available()) {
                  g.fill(this.actionColX, rowY, this.actionColX + ACTION_COL_W - 2, rowY + ROW_H - 2, 1426063360);
               }
            }
         }
      } else {
         if (this.actionButtons != null) {
            for (Button b : this.actionButtons) {
               if (b != null) {
                  b.visible = false;
               }
            }
         }

         g.fill(this.actionColX, this.contentY, this.actionColX + ACTION_COL_W, this.contentY + this.contentH, COL_NO_CT);
         g.drawCenteredString(this.font, Component.literal("No technique data"), this.actionColX + 92, this.contentY + this.contentH / 2 - 4, COL_GREY);
      }

      g.disableScissor();

      if (this.confirmSaveButton != null) {
         this.confirmSaveButton.visible = false;
      }

      super.render(g, mx, my, pt);

      // --- Render Confirmation Overlay ---
      if (this.showSavedOverlay) {
         int ow = 160;
         int oh = 50;
         int ox = this.width / 2 - ow / 2;
         int oy = this.height / 2 - oh / 2;

         g.fill(0, 0, this.width, this.height, -2013265920);
         g.fill(ox, oy, ox + ow, oy + oh, COL_PANEL);
         this.drawOutline(g, ox - 1, oy - 1, ox + ow + 1, oy + oh + 1, COL_BORDER);

         g.drawCenteredString(this.font, Component.literal("Bindings Saved!"), this.width / 2, oy + 10, COL_HEADER);

         if (this.confirmSaveButton != null) {
            this.confirmSaveButton.setX(this.width / 2 - 40);
            this.confirmSaveButton.setY(oy + oh - 22);
            this.confirmSaveButton.visible = true;
            this.confirmSaveButton.render(g, mx, my, pt);
         }
      }
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      if (this.showSavedOverlay) {
         return this.confirmSaveButton != null && this.confirmSaveButton.visible && this.confirmSaveButton.mouseClicked(mx, my, button);
      }
      return super.mouseClicked(mx, my, button);
   }

   @Override
   public boolean mouseScrolled(double mx, double my, double delta) {
      int slotMax = Math.max(0, 160 - this.contentH);
      int actionMax = Math.max(0, (this.getAbilityList().size() + 1) * ROW_H - this.contentH);

      if (mx >= (double) this.slotColX && mx < (double) (this.slotColX + SLOT_COL_W)) {
         this.slotScroll = Math.max(0, Math.min(slotMax, this.slotScroll - (int) (delta * 20.0F)));
         return true;
      } else if (mx >= (double) this.actionColX && mx < (double) (this.actionColX + ACTION_COL_W)) {
         this.actionScroll = Math.max(0, Math.min(actionMax, this.actionScroll - (int) (delta * 20.0F)));
         return true;
      }

      return super.mouseScrolled(mx, my, delta);
   }

   @Override
   public void tick() {
      super.tick();
      int currentVersion = ClientAbilityCache.getVersion();
      if (currentVersion != this.lastCacheVersion) {
         this.lastCacheVersion = currentVersion;
         this.buildWidgets();
      }

      if (this.slotButtons != null) {
         for (AbstractWidget b : this.slotButtons) {
            if (b instanceof JslopButton jb) {
               jb.tick();
            }
         }
      }

      if (this.actionButtons != null) {
         for (AbstractWidget b : this.actionButtons) {
            if (b instanceof JslopButton jb) {
               jb.tick();
            }
         }
      }
   }

   @Override
   public void onClose() {
      for (int i = 0; i < 8; ++i) {
         KeybindConfig.setSlot(i, this.workingSlots[i]);
      }

      ModNetwork.CHANNEL.sendToServer(new PacketSaveKeybinds(KeybindConfig.getSlots()));
      super.onClose();
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public void renderBackground(GuiGraphics g) {
      // Overridden to avoid rendering default background
   }

   private Component slotLabel(int i) {
      String keyName = KeyBindings.ABILITY_SLOTS[i].getTranslatedKeyMessage().getString();
      String abilityName = this.workingSlots[i].displayName();
      String prefix = i == this.selectedSlot ? "▶ " : "   ";
      return Component.literal(prefix + "[" + keyName + "]  →  " + abilityName);
   }

   private void refreshSlotLabels() {
      for (int i = 0; i < 8; ++i) {
         if (this.slotButtons[i] != null) {
            this.slotButtons[i].setMessage(this.slotLabel(i));
         }
      }
   }

   private void drawOutline(GuiGraphics g, int x1, int y1, int x2, int y2, int col) {
      g.fill(x1, y1, x2, y1 + 1, col);
      g.fill(x1, y2 - 1, x2, y2, col);
      g.fill(x1, y1, x1 + 1, y2, col);
      g.fill(x2 - 1, y1, x2, y2, col);
   }
}