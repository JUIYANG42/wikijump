package com.wikijump;

import net.minecraft.client.KeyMapping;

/**
 * Holder for the key binding instance. Each loader adapter registers its own
 * {@link KeyMapping} during client init and stores it here so the shared
 * screen-input logic can test key presses against it.
 */
public final class WikiKey {
    public static KeyMapping openWiki = null;

    private WikiKey() {
    }
}
