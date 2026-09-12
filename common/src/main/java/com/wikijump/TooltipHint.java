package com.wikijump;

import com.wikijump.config.WikiJumpConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Appends a one-line key reminder to item tooltips.
 *
 * The mod is invisible until you know the key exists, and a player who never
 * reads the README has no way of finding out — which is exactly the problem
 * the tooltip hint solves. It is appended as the last line so it stays out of
 * the way of the information the player is actually reading.
 *
 * The hint names the key the player has actually bound instead of a hard-coded
 * "K", so it stays truthful after a rebind. Turn the whole line off with
 * {@code showTooltipHint} in the config, or from the in-game settings screen.
 */
public final class TooltipHint {

    /** Translation key of the hint line; the bound key is its only argument. */
    private static final String HINT_KEY = "wikijump.tooltip.hint";

    /** Used before the loaders have registered the keybinding. */
    private static final String FALLBACK_KEY = "K";

    private TooltipHint() {
    }

    /**
     * Appends the hint to the given tooltip lines, if the hint is enabled.
     *
     * @return {@code true} when a line was added
     */
    public static boolean append(List<Component> lines) {
        if (lines == null || lines.isEmpty()) {
            return false;
        }
        try {
            if (!WikiJumpConfig.get().showTooltipHint) {
                return false;
            }
            lines.add(Component.translatable(HINT_KEY, boundKey(), boundKey())
                    .withStyle(ChatFormatting.DARK_GRAY));
            return true;
        } catch (Exception e) {
            // A tooltip can be built while the client is still coming up, when
            // the config file and the keybinding may not exist yet.
            WikiJump.LOGGER.error("Failed to add the wiki key hint to a tooltip", e);
            return false;
        }
    }

    /**
     * The player's own keybinding, so a rebind is reflected in the hint.
     * Falls back to the default key when the binding is not registered yet.
     */
    private static Component boundKey() {
        KeyMapping mapping = WikiKey.openWiki;
        if (mapping == null) {
            return Component.literal(FALLBACK_KEY);
        }
        Component name = mapping.getTranslatedKeyMessage();
        return name == null ? Component.literal(FALLBACK_KEY) : name;
    }
}
