package com.wikijump.fabric;

import com.wikijump.WikiJump;
import com.wikijump.WikiJumpCommands;
import com.wikijump.WikiJumpLogic;
import com.wikijump.WikiKey;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
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

        // Key events are per-screen: hook every screen as it initializes.
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) ->
                registerScreenKeyHandler(screen));

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
