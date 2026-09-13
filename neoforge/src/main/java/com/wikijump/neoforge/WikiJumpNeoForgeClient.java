package com.wikijump.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import com.wikijump.HoverTracker;
import com.wikijump.TooltipHint;
import com.wikijump.WikiJump;
import com.wikijump.WikiJumpCommands;
import com.wikijump.WikiJumpLogic;
import com.wikijump.WikiKey;
import com.wikijump.compat.KeyPress;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
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

    /**
     * The {@link ModContainer} parameter is injected by FML and is what makes
     * the mod list's "Config" button work; see {@link ConfigScreenHook}.
     */
    public WikiJumpNeoForgeClient(ModContainer container) {
        ConfigScreenHook.register(container);
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
        // Also feeds the hover tracker, which is what lets the key work on the
        // item lists of JEI/EMI/REI: they draw their own tooltips.
        HoverTracker.capture(event.getItemStack());
        TooltipHint.append(event.getToolTip());
    }

    @SubscribeEvent
    public void onScreenKey(ScreenEvent.KeyPressed.Pre event) {
        if (WikiJumpLogic.onScreenKey(event.getScreen(),
                new KeyPress(event.getKeyCode(), event.getScanCode(), event.getModifiers()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        // A click moves the cursor on to something else (a viewer's search box,
        // for instance) without a tooltip being drawn for it.
        HoverTracker.clear();
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
