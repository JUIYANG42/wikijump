package com.wikijump.forge;

import com.mojang.blaze3d.platform.InputConstants;
import com.wikijump.HoverTracker;
import com.wikijump.TooltipHint;
import com.wikijump.WikiJump;
import com.wikijump.WikiJumpCommands;
import com.wikijump.WikiJumpLogic;
import com.wikijump.WikiKey;
import com.wikijump.compat.KeyPress;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only mod entry. The mods.toml marks this mod side=CLIENT, and all
 * referenced classes here are client-only, so the class only loads on clients.
 */
@Mod(value = WikiJump.MOD_ID)
public class WikiJumpForgeClient {

    public WikiJumpForgeClient() {
        MinecraftForge.EVENT_BUS.register(this);
        WikiJump.LOGGER.info("WikiJump (Forge) initialized");
    }

    @Mod.EventBusSubscriber(modid = WikiJump.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
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
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && WikiKey.openWiki != null) {
            while (WikiKey.openWiki.consumeClick()) {
                WikiJumpLogic.onWorldKey();
            }
        }
    }
}
