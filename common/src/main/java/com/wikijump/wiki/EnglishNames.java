package com.wikijump.wiki;

import com.wikijump.WikiJump;
import com.wikijump.compat.Generation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.util.List;
import java.util.function.Consumer;

/**
 * Resolves the in-game English (en_us) name of a translation key, caching the
 * parsed {@code en_us} language.
 *
 * <p>{@link ClientLanguage#loadFrom} parses the en_us file of <em>every</em>
 * installed mod, so calling it on each lookup costs tens to hundreds of
 * milliseconds in a large modpack. The result is therefore built lazily on
 * first use and kept until the resource packs change.</p>
 *
 * <p>Cache invalidation needs a resource-reload listener, and that is the one
 * part of this class that is <em>not</em> portable: 1.21.1 and 26.1 reshaped
 * {@code PreparableReloadListener#reload} and the nested state types it passes
 * around, so a listener written by hand for one does not compile for the other.
 * The registration therefore goes through {@link Generation#registerReloadHook},
 * which each generation implements with its own listener — and which lets both
 * of them use the far simpler {@code ResourceManagerReloadListener} callback
 * instead of the barrier dance.</p>
 *
 * <p>The hook is installed at the same moment the cache is first built, rather
 * than from a loader client-setup event, so every loader and every generation
 * takes exactly the same code path. Pressing F3+T (or changing resource packs)
 * drops the cache and the next lookup rebuilds it from the new packs.</p>
 */
public final class EnglishNames {

    private static final List<String> EN_US = List.of("en_us");

    /** Parsed en_us language, or {@code null} when not built / invalidated. */
    private static volatile ClientLanguage cached;

    /** Resource manager {@link #cached} was built from. */
    private static volatile ResourceManager cachedFrom;

    /** True once the reload listener has been registered. */
    private static volatile boolean listenerRegistered;

    private EnglishNames() {
    }

    /**
     * Returns the target's in-game English name, falling back to
     * {@code fallback} (the localized display name) when no English
     * translation exists or the language files cannot be read.
     */
    public static String resolve(String translationKey, String fallback) {
        if (translationKey == null || translationKey.isEmpty()) {
            return fallback;
        }
        ClientLanguage english = language();
        if (english != null && english.has(translationKey)) {
            String name = english.getOrDefault(translationKey, translationKey);
            if (!name.isEmpty() && !name.equals(translationKey)) {
                return name;
            }
        }
        return fallback;
    }

    /** Drops the cache; the next lookup rebuilds it from the current packs. */
    public static void invalidate() {
        cached = null;
        cachedFrom = null;
    }

    private static ClientLanguage language() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return null;
        }
        ResourceManager rm = mc.getResourceManager();
        if (rm == null) {
            return null;
        }
        ClientLanguage current = cached;
        if (current != null && cachedFrom == rm) {
            return current;
        }
        try {
            ClientLanguage loaded = ClientLanguage.loadFrom(rm, EN_US, false);
            cached = loaded;
            cachedFrom = rm;
            ensureListenerRegistered();
            return loaded;
        } catch (Exception e) {
            WikiJump.LOGGER.error("Failed to load en_us translations", e);
            return null;
        }
    }

    /**
     * Installs the reload hook through a loader-supplied registrar.
     *
     * <p>For loaders on which {@link Generation#registerReloadHook} cannot work
     * — see {@link #ensureListenerRegistered} — the adapter registers the
     * listener itself, during client setup, using whatever hook that loader
     * provides. This method exists so the listener and the bookkeeping stay
     * here: the adapter only has to say <em>where</em> to put it.</p>
     *
     * @param registrar receives the listener; the caller attaches whatever id or
     *                  key its platform requires
     * @return true when the registration succeeded
     */
    public static boolean installReloadHook(Consumer<PreparableReloadListener> registrar) {
        if (listenerRegistered) {
            return true;
        }
        try {
            registrar.accept(reloadListener());
            listenerRegistered = true;
            return true;
        } catch (Exception e) {
            WikiJump.LOGGER.warn("Could not register the resource reload listener", e);
            return false;
        }
    }

    /**
     * Registers the cache invalidator once, through the vanilla resource manager.
     *
     * <p>A failure is not sticky: the generation reports false when the resource
     * manager is not ready, and the next lookup tries again.</p>
     *
     * <p>Works on every loader except NeoForge 26.1, which replaced the vanilla
     * listener list with an immutable one once mod loading finished — there
     * {@code registerReloadListener} throws from the moment the game starts, so
     * its adapter calls {@link #installReloadHook} during client setup instead
     * and {@link #listenerRegistered} is already true by the time we get here.</p>
     */
    private static void ensureListenerRegistered() {
        if (listenerRegistered) {
            return;
        }
        try {
            if (Generation.get().registerReloadHook(EnglishNames::invalidate)) {
                listenerRegistered = true;
            }
        } catch (Exception e) {
            // Worst case the cache lives on until the next game restart.
            WikiJump.LOGGER.warn("Could not register the resource reload listener", e);
        }
    }

    /** Drops the cache when the platform reports a resource reload. */
    private static ResourceManagerReloadListener reloadListener() {
        return new ResourceManagerReloadListener() {
            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                invalidate();
            }
        };
    }
}
