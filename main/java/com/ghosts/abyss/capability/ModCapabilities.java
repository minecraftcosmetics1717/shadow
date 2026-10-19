package com.ghosts.abyss.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.ghosts.abyss.Abyss;

// Abyss capability classes (make sure these are copied into this package or imported correctly)
import com.ghosts.abyss.capability.ShadowInventoryCapability;
import com.ghosts.abyss.capability.LifeCharmsCapability;
import com.ghosts.abyss.capability.InnerselfInventoryCapability;

@Mod.EventBusSubscriber(modid = "abyss", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCapabilities {

    // From Abyss
    public static final Capability<ShadowInventoryCapability> SHADOW_INVENTORY =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static final Capability<LifeCharmsCapability> LIFE_CHARMS =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static final Capability<InnerselfInventoryCapability> INNERSELF_INVENTORY =
            CapabilityManager.get(new CapabilityToken<>() {});

    // From jslops
    public static final Capability<IPlayerBodyData> PLAYER_BODY_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        // Abyss
        event.register(ShadowInventoryCapability.class);
        event.register(LifeCharmsCapability.class);
        event.register(InnerselfInventoryCapability.class);

        // jslops
        event.register(IPlayerBodyData.class);
    }
}