package com.wikijump.neoforge;

import com.wikijump.gui.WikiJumpConfigScreen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Hooks {@link WikiJumpConfigScreen} up to the <em>Config</em> button of
 * NeoForge's mod list, so the settings are reachable without typing
 * {@code /wikijump}.
 *
 * <p>NeoForge only enables that button for a mod that has registered an
 * {@link IConfigScreenFactory} extension point — its {@code ModListScreen} sets
 * {@code button.active = IConfigScreenFactory.getForMod(info).isPresent()} and
 * dispatches the click to {@code factory.createScreen(container, parent)}.
 * Without the extension point the button is drawn greyed out, which is exactly
 * what it did before this class existed.</p>
 *
 * <p><b>Why this lives in the root {@code neoforge} directory rather than in
 * {@code modern/}:</b> the nested 26.x build merges this directory in as one of
 * its source layers (see
 * {@code modern/buildSrc/src/main/groovy/wikijump-loader.gradle}), so a file
 * that no overlay replaces is compiled into both generations from a single
 * copy. That is only safe because the whole extension-point API is
 * <em>byte-identical</em> in the two generations, which was checked with
 * {@code javap} against neoforge 21.1.249 and 26.1.2.109: the factory
 * interface, {@code ModContainer#registerExtensionPoint} and the mod-list
 * button logic carry the same descriptors in both. Anything that merely
 * <em>looks</em> similar but has moved between generations still belongs in
 * {@code modern/overlay}.</p>
 */
public final class ConfigScreenHook {

    private ConfigScreenHook() {
    }

    /**
     * Hands the settings screen to NeoForge. Call this from the mod
     * constructor, which may declare a {@link ModContainer} parameter — FML
     * injects one, and that is the only way to reach
     * {@code registerExtensionPoint} before the mod list can be opened.
     *
     * <p>The parent screen NeoForge passes in is the mod list itself, so
     * closing the settings returns there instead of dropping the player back
     * into the world.</p>
     */
    public static void register(ModContainer container) {
        // A local variable rather than an inline lambda: it pins down which
        // overload of registerExtensionPoint this is and which interface the
        // lambda implements, at no cost to readability.
        IConfigScreenFactory factory = (modContainer, parent) -> new WikiJumpConfigScreen(parent);
        container.registerExtensionPoint(IConfigScreenFactory.class, factory);
    }
}
