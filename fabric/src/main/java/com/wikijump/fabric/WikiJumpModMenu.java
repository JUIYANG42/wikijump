package com.wikijump.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.wikijump.gui.WikiJumpConfigScreen;

/**
 * Mod Menu integration: makes the "Configure" button on WikiJump's row in Mod
 * Menu's mod list open its settings screen.
 *
 * <p>Fabric has no built-in mod list, so there is no loader-side config-screen
 * registry to hook the way {@code ConfigScreenHook} does on NeoForge and
 * {@code WikiJumpForgeClient} does on Forge. Mod Menu provides one instead, via
 * the {@code modmenu} entrypoint.
 *
 * <p>This class sits in the shared loader layer — layer 2 of the four-layer
 * merge — on purpose: {@code ModMenuApi} and {@code ConfigScreenFactory} are
 * byte-identical in Mod Menu 11.x (MC 1.21.1) and 18.x (MC 26.1), so one file
 * covers both generations. Only the compile dependency differs, and that is
 * declared per build.
 *
 * <p>Mod Menu is an optional dependency. The class is compiled against it but
 * never bundled: Fabric resolves an entrypoint only when something asks for its
 * key, and only Mod Menu asks for {@code modmenu}. With Mod Menu absent this
 * class is therefore never loaded and the mod behaves exactly as before.
 */
public class WikiJumpModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // The settings screen already takes its parent and returns to it on
        // close, so the back button leads to Mod Menu's mod list.
        return WikiJumpConfigScreen::new;
    }
}
