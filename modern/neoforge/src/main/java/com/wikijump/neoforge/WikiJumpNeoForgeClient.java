package com.wikijump.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import com.wikijump.HoverTracker;
import com.wikijump.TooltipHint;
import com.wikijump.WikiJump;
import com.wikijump.WikiJumpCommands;
import com.wikijump.WikiJumpLogic;
import com.wikijump.WikiKey;
import com.wikijump.compat.KeyPress;
import com.wikijump.wiki.EnglishNames;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only NeoForge entrypoint.
 *
 * <p><b>26.1 generation variant</b> (lives in {@code modern/neoforge/src/main/java}).
 * The mod's behaviour is unchanged; 26.1 moved three things this file touches:</p>
 * <ul>
 *   <li>a {@link KeyMapping} now takes a {@link KeyMapping.Category} object
 *       instead of a free-form category string — and the canonical way to add
 *       a category is {@link RegisterKeyMappingsEvent#registerCategory(Category)},
 *       not the deprecated {@link KeyMapping.Category#register(Identifier)}
 *       which throws on duplicates;</li>
 *   <li>screen input events deliver a {@code KeyEvent} record (key/scancode/modifiers)
 *       and a {@code MouseButtonEvent} record instead of loose ints, and the key
 *       handler therefore receives one argument instead of three;</li>
 *   <li>{@code ClientTickEvent.Post} no longer takes a phase argument.</li>
 * </ul>
 *
 * <p>The one thing that did <em>not</em> move is the Config button of the mod
 * list, so that registration is not duplicated here — it is
 * {@link ConfigScreenHook}, compiled into this generation straight from the
 * root {@code neoforge} directory.</p>
 */
@Mod(value = WikiJump.MOD_ID, dist = Dist.CLIENT)
public class WikiJumpNeoForgeClient {

    /**
     * The {@link ModContainer} parameter is injected by FML and is what lets
     * {@link ConfigScreenHook} light up the mod list's Config button.
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
            // Direct canonical ctor — not the deprecated static register, which
            // throws on duplicates and is rejected by the NeoForge event anyway.
            KeyMapping.Category category = new KeyMapping.Category(
                    Identifier.fromNamespaceAndPath(WikiJump.MOD_ID, "main"));
            event.registerCategory(category);
            WikiKey.openWiki = new KeyMapping(
                    "key.wikijump.open_wiki",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_K,
                    category
            );
            event.register(WikiKey.openWiki);
        }

        /**
         * Registers the English-name cache's reload listener.
         *
         * <p>NeoForge 26.1 deprecates
         * {@code ReloadableResourceManager#registerReloadListener} and freezes
         * the listener list the moment mod loading ends, so the shared code's
         * lazy registration — which runs on the first lookup, i.e. well after
         * startup — would throw. This event is the supported replacement, and it
         * fires exactly once, while {@code Minecraft} is being constructed.</p>
         *
         * <p>Without this the Shift-lookup would keep serving stale English
         * names after a resource pack change. It is the one hook that cannot be
         * shared between loaders, because the restriction is NeoForge's alone:
         * Fabric and Forge still accept late registration, and NeoForge 21.1
         * did too.</p>
         */
        @SubscribeEvent
        public static void onAddClientReloadListeners(AddClientReloadListenersEvent event) {
            EnglishNames.installReloadHook(listener -> event.addListener(
                    Identifier.fromNamespaceAndPath(WikiJump.MOD_ID, "english_names"), listener));
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
        // 26.1 wraps the key data in a KeyEvent record; unpack it into the shape
        // the shared logic works with.
        var key = event.getKeyEvent();
        if (WikiJumpLogic.onScreenKey(event.getScreen(),
                new KeyPress(key.key(), key.scancode(), key.modifiers()))) {
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
