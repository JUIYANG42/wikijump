package com.wikijump.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import com.wikijump.TooltipHint;
import com.wikijump.WikiJump;
import com.wikijump.WikiJumpCommands;
import com.wikijump.WikiJumpLogic;
import com.wikijump.WikiKey;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

@Mod(value = WikiJump.MOD_ID, dist = Dist.CLIENT)
public class WikiJumpNeoForgeClient {

    public WikiJumpNeoForgeClient() {
        NeoForge.EVENT_BUS.register(this);
        WikiJump.LOGGER.info("WikiJump (NeoForge) initialized");
    }

    @EventBusSubscriber(modid = WikiJump.MOD_ID, value = Dist.CLIENT)
    public static class ModEvents {
        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            WikiKey.openWiki = new KeyMapping(
                    "key.wikijump.open_wiki",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_K,
                    "key.categories.wikijump"
            );
            event.register(WikiKey.openWiki);
        }
    }

    @SubscribeEvent
    public void onClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(WikiJumpCommands.build());
    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        TooltipHint.append(event.getToolTip());
    }

    @SubscribeEvent
    public void onScreenKey(ScreenEvent.KeyPressed.Pre event) {
        if (WikiJumpLogic.onScreenKey(event.getScreen(), event.getKeyCode(), event.getScanCode())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        if (WikiKey.openWiki != null) {
            while (WikiKey.openWiki.consumeClick()) {
                WikiJumpLogic.onWorldKey();
            }
        }
    }
}
