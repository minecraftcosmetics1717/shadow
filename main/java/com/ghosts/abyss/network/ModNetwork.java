package com.ghosts.abyss.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

// Abyss packets (keep these imports until you move the packet classes)
import com.ghosts.abyss.network.OpenShadowInventoryPacket;
import com.ghosts.abyss.network.OpenLifeCharmsPacket;
import com.ghosts.abyss.network.PacketSyncShadowDive;
import com.ghosts.abyss.network.RequestTeleportPacket;
import com.ghosts.abyss.network.TeleportRequestNotifyPacket;
import com.ghosts.abyss.network.RespondTeleportPacket;

public class ModNetwork {
   private static final String PROTOCOL_VERSION = "1";
   public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
           new ResourceLocation("abyss", "main"),
           () -> "1",
           "1"::equals,
           "1"::equals
   );

   private static int id = 0;

   public static void register() {
      // ===== jslops packets =====
      CHANNEL.registerMessage(id++, PacketSpendRefinementPoint.class, PacketSpendRefinementPoint::encode, PacketSpendRefinementPoint::new, PacketSpendRefinementPoint::handle);
      CHANNEL.registerMessage(id++, PacketSyncRefinementData.class, PacketSyncRefinementData::encode, PacketSyncRefinementData::new, PacketSyncRefinementData::handle);
      CHANNEL.registerMessage(id++, PacketSyncAdvTechData.class, PacketSyncAdvTechData::encode, PacketSyncAdvTechData::new, PacketSyncAdvTechData::handle);
      CHANNEL.registerMessage(id++, PacketActivateHWB.class, PacketActivateHWB::encode, PacketActivateHWB::new, PacketActivateHWB::handle);
      CHANNEL.registerMessage(id++, PacketRequestUnlockHWB.class, PacketRequestUnlockHWB::encode, PacketRequestUnlockHWB::new, PacketRequestUnlockHWB::handle);
      CHANNEL.registerMessage(id++, PacketOpenPactDialogue.class, PacketOpenPactDialogue::encode, PacketOpenPactDialogue::new, PacketOpenPactDialogue::handle);
      CHANNEL.registerMessage(id++, PacketPactResponse.class, PacketPactResponse::encode, PacketPactResponse::new, PacketPactResponse::handle);
      CHANNEL.registerMessage(id++, PacketPurchaseBindingVow.class, PacketPurchaseBindingVow::encode, PacketPurchaseBindingVow::new, PacketPurchaseBindingVow::handle);
      CHANNEL.registerMessage(id++, PacketToggleOpenDomain.class, PacketToggleOpenDomain::encode, PacketToggleOpenDomain::new, PacketToggleOpenDomain::handle);
      CHANNEL.registerMessage(id++, PacketSyncOpenDomainData.class, PacketSyncOpenDomainData::encode, PacketSyncOpenDomainData::new, PacketSyncOpenDomainData::handle);
      CHANNEL.registerMessage(id++, PacketRequestUnlockOpenDomain.class, PacketRequestUnlockOpenDomain::encode, PacketRequestUnlockOpenDomain::new, PacketRequestUnlockOpenDomain::handle);
      CHANNEL.registerMessage(id++, PacketTransformToCursedObject.class, PacketTransformToCursedObject::encode, PacketTransformToCursedObject::new, PacketTransformToCursedObject::handle);
      CHANNEL.registerMessage(id++, PacketToggleAbsorbedTechnique.class, PacketToggleAbsorbedTechnique::encode, PacketToggleAbsorbedTechnique::new, PacketToggleAbsorbedTechnique::handle);
      CHANNEL.registerMessage(id++, PacketUnlockCursedObjectTransformation.class, PacketUnlockCursedObjectTransformation::encode, PacketUnlockCursedObjectTransformation::new, PacketUnlockCursedObjectTransformation::handle);
      CHANNEL.registerMessage(id++, PacketRecoverBindingVow.class, PacketRecoverBindingVow::encode, PacketRecoverBindingVow::new, PacketRecoverBindingVow::handle);
      CHANNEL.registerMessage(id++, PacketGiveCursedTechniqueStarter.class, PacketGiveCursedTechniqueStarter::encode, PacketGiveCursedTechniqueStarter::new, PacketGiveCursedTechniqueStarter::handle);
      CHANNEL.registerMessage(id++, PacketActivateAbilitySlot.class, PacketActivateAbilitySlot::encode, PacketActivateAbilitySlot::new, PacketActivateAbilitySlot::handle);
      CHANNEL.registerMessage(id++, PacketSyncAbilities.class, PacketSyncAbilities::encode, PacketSyncAbilities::decode, PacketSyncAbilities::handle);
      CHANNEL.registerMessage(id++, PacketRequestAbilityScan.class, PacketRequestAbilityScan::encode, PacketRequestAbilityScan::decode, PacketRequestAbilityScan::handle);
      CHANNEL.registerMessage(id++, PacketSaveKeybinds.class, PacketSaveKeybinds::encode, PacketSaveKeybinds::decode, PacketSaveKeybinds::handle);
      CHANNEL.registerMessage(id++, PacketSyncKeybinds.class, PacketSyncKeybinds::encode, PacketSyncKeybinds::decode, PacketSyncKeybinds::handle);
      CHANNEL.registerMessage(id++, PacketSyncRctData.class, PacketSyncRctData::encode, PacketSyncRctData::decode, PacketSyncRctData::handle);
      CHANNEL.registerMessage(id++, PacketPurchaseRctPerk.class, PacketPurchaseRctPerk::encode, PacketPurchaseRctPerk::decode, PacketPurchaseRctPerk::handle);
      CHANNEL.registerMessage(id++, PacketSyncBlackFlashData.class, PacketSyncBlackFlashData::encode, PacketSyncBlackFlashData::decode, PacketSyncBlackFlashData::handle);
      CHANNEL.registerMessage(id++, PacketPurchaseBlackFlashPerk.class, PacketPurchaseBlackFlashPerk::encode, PacketPurchaseBlackFlashPerk::decode, PacketPurchaseBlackFlashPerk::handle);
      CHANNEL.registerMessage(id++, PacketActivateAtWillBlackFlash.class, PacketActivateAtWillBlackFlash::encode, PacketActivateAtWillBlackFlash::decode, PacketActivateAtWillBlackFlash::handle);
      CHANNEL.registerMessage(id++, PacketSyncPerfectBodyVisible.class, PacketSyncPerfectBodyVisible::encode, PacketSyncPerfectBodyVisible::decode, PacketSyncPerfectBodyVisible::handle);
      CHANNEL.registerMessage(id++, PacketSyncGamerules.class, PacketSyncGamerules::encode, PacketSyncGamerules::decode, PacketSyncGamerules::handle);
      CHANNEL.registerMessage(id++, PacketRegisterYutaTechnique.class, PacketRegisterYutaTechnique::encode, PacketRegisterYutaTechnique::new, PacketRegisterYutaTechnique::handle);
      CHANNEL.registerMessage(id++, PacketSyncYutaTechniques.class, PacketSyncYutaTechniques::encode, PacketSyncYutaTechniques::decode, PacketSyncYutaTechniques::handle);
      CHANNEL.registerMessage(id++, PacketYutaDomainSelect.class, PacketYutaDomainSelect::encode, PacketYutaDomainSelect::new, PacketYutaDomainSelect::handle);
      CHANNEL.registerMessage(id++, PacketRemoveYutaTechnique.class, PacketRemoveYutaTechnique::encode, PacketRemoveYutaTechnique::new, PacketRemoveYutaTechnique::handle);
      CHANNEL.registerMessage(id++, PacketToggleFourArms.class, PacketToggleFourArms::encode, PacketToggleFourArms::new, PacketToggleFourArms::handle);
      CHANNEL.registerMessage(id++, PacketToggleCopyOnHit.class, PacketToggleCopyOnHit::encode, PacketToggleCopyOnHit::new, PacketToggleCopyOnHit::handle);
      CHANNEL.registerMessage(id++, PacketSyncCopyToggle.class, PacketSyncCopyToggle::encode, PacketSyncCopyToggle::decode, PacketSyncCopyToggle::handle);
      CHANNEL.registerMessage(id++, PacketRenameYutaTechnique.class, PacketRenameYutaTechnique::encode, PacketRenameYutaTechnique::new, PacketRenameYutaTechnique::handle);
      CHANNEL.registerMessage(id++, PacketSyncCsmRoster.class, PacketSyncCsmRoster::encode, PacketSyncCsmRoster::decode, PacketSyncCsmRoster::handle);
      CHANNEL.registerMessage(id++, PacketCsmMarkUnique.class, PacketCsmMarkUnique::encode, PacketCsmMarkUnique::new, PacketCsmMarkUnique::handle);
      CHANNEL.registerMessage(id++, PacketCsmRenameSpirit.class, PacketCsmRenameSpirit::encode, PacketCsmRenameSpirit::new, PacketCsmRenameSpirit::handle);
      CHANNEL.registerMessage(id++, PacketCsmToggleSpared.class, PacketCsmToggleSpared::encode, PacketCsmToggleSpared::new, PacketCsmToggleSpared::handle);
      CHANNEL.registerMessage(id++, PacketCsmFeedFinger.class, PacketCsmFeedFinger::encode, PacketCsmFeedFinger::new, PacketCsmFeedFinger::handle);
      CHANNEL.registerMessage(id++, PacketCsmSelectSpirit.class, PacketCsmSelectSpirit::encode, PacketCsmSelectSpirit::new, PacketCsmSelectSpirit::handle);
      CHANNEL.registerMessage(id++, PacketCsmSetRosterIdx.class, PacketCsmSetRosterIdx::encode, PacketCsmSetRosterIdx::new, PacketCsmSetRosterIdx::handle);
      CHANNEL.registerMessage(id++, PacketSyncCsmCooldown.class, PacketSyncCsmCooldown::encode, PacketSyncCsmCooldown::new, PacketSyncCsmCooldown::handle);
      CHANNEL.registerMessage(id++, PacketRctOutputHold.class, PacketRctOutputHold::encode, PacketRctOutputHold::decode, PacketRctOutputHold::handle);

      // ===== Abyss packets =====
      CHANNEL.registerMessage(id++, OpenShadowInventoryPacket.class,
              OpenShadowInventoryPacket::encode,
              OpenShadowInventoryPacket::new,
              OpenShadowInventoryPacket::handle);
      CHANNEL.registerMessage(id++, OpenLifeCharmsPacket.class,
              OpenLifeCharmsPacket::encode,
              OpenLifeCharmsPacket::new,
              OpenLifeCharmsPacket::handle);
      CHANNEL.registerMessage(id++, PacketSyncShadowDive.class,
              PacketSyncShadowDive::encode,
              PacketSyncShadowDive::new,
              PacketSyncShadowDive::handle);
      CHANNEL.registerMessage(id++, RequestTeleportPacket.class,
              RequestTeleportPacket::encode,
              RequestTeleportPacket::new,
              RequestTeleportPacket::handle);
      CHANNEL.registerMessage(id++, TeleportRequestNotifyPacket.class,
              TeleportRequestNotifyPacket::encode,
              TeleportRequestNotifyPacket::new,
              TeleportRequestNotifyPacket::handle);
      CHANNEL.registerMessage(id++, RespondTeleportPacket.class,
              RespondTeleportPacket::encode,
              RespondTeleportPacket::new,
              RespondTeleportPacket::handle);
      CHANNEL.registerMessage(id++, PacketUnlockMinimap.class,
              PacketUnlockMinimap::encode, PacketUnlockMinimap::new, PacketUnlockMinimap::handle);
      CHANNEL.registerMessage(id++, PacketToggleMinimap.class,
              PacketToggleMinimap::encode, PacketToggleMinimap::new, PacketToggleMinimap::handle);
      CHANNEL.registerMessage(id++, PacketSyncMinimapState.class,
              PacketSyncMinimapState::encode, PacketSyncMinimapState::new, PacketSyncMinimapState::handle);
   }
}