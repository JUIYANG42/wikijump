package com.wikijump.fabric;

import com.wikijump.HoverTracker;
import com.wikijump.TooltipHint;
import com.wikijump.WikiJump;
import com.wikijump.WikiJumpCommands;
import com.wikijump.WikiJumpLogic;
import com.wikijump.WikiKey;
import com.wikijump.compat.KeyPress;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only Fabric entrypoint.
 *
 * <p><b>26.1 generation variant</b> (lives in {@code modern/fabric/src/main/java}).
 * The mod's behaviour is unchanged; 26.1 moved three things this file touches:</p>
 * <ul>
 *   <li>the keybinding module was renamed to match Mojang's names, so
 *       {@code fabric-key-binding-api-v1} became {@code fabric-key-mapping-api-v1}
 *       and {@code KeyBindingHelper.registerKeyBinding} became
 *       {@code KeyMappingHelper.registerKeyMapping};</li>
 *   <li>a {@link KeyMapping} now takes a {@link KeyMapping.Category} object
 *       rather than a free-form category string — the label for it comes from
 *       the category id, i.e. the lang key {@code key.category.wikijump.main};</li>
 *   <li>screen input events deliver input records, so the key handler receives
 *       a {@code KeyEvent} and the click handler a {@code MouseButtonEvent}
 *       instead of loose ints.</li>
 * </ul>
 */
public class WikiJumpFabricClient implements ClientModInitializer {

    /**
     * Cached because {@link KeyMapping.Category#register} throws when the same
     * category is registered twice.
     */
    private static KeyMapping.Category category;

    @Override
    public void onInitializeClient() {
        WikiKey.openWiki = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.wikijump.open_wiki",
                GLFW.GLFW_KEY_K,
                category()
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
                    .register((scr, click) -> HoverTracker.clear());
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (WikiKey.openWiki.consumeClick()) {
                WikiJumpLogic.onWorldKey();
            }
        });

        WikiJump.LOGGER.info("WikiJump (Fabric) initialized");
    }

    private static void registerScreenKeyHandler(Screen screen) {
        ScreenKeyboardEvents.beforeKeyPress(screen).register((scr, keyEvent) -> {
            // Returning without opening means the press falls through to the screen.
            WikiJumpLogic.onScreenKey(scr,
                    new KeyPress(keyEvent.key(), keyEvent.scancode(), keyEvent.modifiers()));
        });
    }

    /** The mod's own controls category, registered once on first use. */
    private static KeyMapping.Category category() {
        if (category == null) {
            category = KeyMapping.Category.register(
                    Identifier.fromNamespaceAndPath(WikiJump.MOD_ID, "main"));
        }
        return category;
    }
}
