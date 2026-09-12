package com.wikijump.fabric;

import com.wikijump.HoverTracker;
import com.wikijump.TooltipHint;
import com.wikijump.WikiJump;
import com.wikijump.WikiJumpCommands;
import com.wikijump.WikiJumpLogic;
import com.wikijump.WikiKey;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

public class WikiJumpFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        WikiKey.openWiki = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.wikijump.open_wiki",
                GLFW.GLFW_KEY_K,
                "key.categories.wikijump"
        ));

        // Same command tree as the other loaders; only the source type differs.
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(WikiJumpCommands.build()));

        // Key reminder in item tooltips; the shared code decides whether it is on.
        // The same hook feeds the hover tracker, which is what lets the key work
        // on the item lists of JEI/EMI/REI: they draw their own tooltips.
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            HoverTracker.capture(stack);
            TooltipHint.append(lines);
        });

        // Key events are per-screen: hook every screen as it initializes.
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            registerScreenKeyHandler(screen);
            ScreenMouseEvents.beforeMouseClick(screen)
                    // A click moves the cursor on to something else (a viewer's
                    // search box, for instance) without a tooltip drawn for it.
                    .register((scr, mouseX, mouseY, button) -> HoverTracker.clear());
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (WikiKey.openWiki.consumeClick()) {
                WikiJumpLogic.onWorldKey();
            }
        });

        WikiJump.LOGGER.info("WikiJump (Fabric) initialized");
    }

    private static void registerScreenKeyHandler(Screen screen) {
        ScreenKeyboardEvents.beforeKeyPress(screen).register((scr, keyCode, scanCode, modifiers) -> {
            // Returning without opening means the press falls through to the screen.
            WikiJumpLogic.onScreenKey(scr, keyCode, scanCode);
        });
    }
}
