package ht.treechop.client;

import com.mojang.blaze3d.platform.InputConstants;
import ht.treechop.TreeChop;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(value = Dist.CLIENT, modid = TreeChop.MOD_ID)
public class NeoForgeClient extends Client {
    static {
        Client.instance = new NeoForgeClient();
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.register(EventHandler.class);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        KeyBindings.registerKeyMappings(event::register);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }

    static class EventHandler {
        @SubscribeEvent
        public static void onConnect(ClientPlayerNetworkEvent.LoggingIn event) {
            syncOnJoin();
        }

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (event.getKey() != InputConstants.UNKNOWN.getValue()) {
                for (KeyBindings.ActionableKeyBinding keyBinding : KeyBindings.allKeyBindings) {
                    if (event.getKey() == keyBinding.getKey().getValue() && event.getAction() == InputConstants.PRESS) {
                        keyBinding.onPress();
                        return;
                    }
                }
            }
        }
    }
}
