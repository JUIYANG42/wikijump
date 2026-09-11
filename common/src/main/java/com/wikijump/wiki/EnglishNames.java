package com.wikijump.wiki;

import com.wikijump.WikiJump;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Resolves the in-game English (en_us) name of a translation key, caching the
 * parsed {@code en_us} language.
 *
 * <p>{@link ClientLanguage#loadFrom} parses the en_us file of <em>every</em>
 * installed mod, so calling it on each lookup costs tens to hundreds of
 * milliseconds in a large modpack. The result is therefore built lazily on
 * first use and kept until the resource packs change.</p>
 *
 * <p>Invalidation uses the vanilla
 * {@link ReloadableResourceManager#registerReloadListener} hook, which is
 * loader-independent — the listener is registered lazily together with the
 * cache, on the first lookup, so it needs no client-setup event and carries no
 * init-order risk. Pressing F3+T (or changing resource packs) drops the cache
 * and the next lookup rebuilds it from the new packs.</p>
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
            ensureListenerRegistered(rm);
            return loaded;
        } catch (Exception e) {
            WikiJump.LOGGER.error("Failed to load en_us translations", e);
            return null;
        }
    }

    /**
     * Registers the cache invalidator once. Done lazily — together with the
     * first cache build — instead of from a loader client-setup hook, so all
     * three loaders share exactly the same code path.
     */
    private static void ensureListenerRegistered(ResourceManager rm) {
        if (listenerRegistered || !(rm instanceof ReloadableResourceManager reloadable)) {
            return;
        }
        try {
            reloadable.registerReloadListener(new CacheInvalidator());
            listenerRegistered = true;
        } catch (Exception e) {
            // Worst case the cache lives on until the next game restart.
            WikiJump.LOGGER.warn("Could not register the resource reload listener", e);
        }
    }

    /** Clears the cache whenever resources are reloaded (F3+T, pack change, ...). */
    private static final class CacheInvalidator implements PreparableReloadListener {

        @Override
        public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resourceManager,
                                              ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler,
                                              Executor backgroundExecutor, Executor gameExecutor) {
            invalidate();
            // Every reload listener has to pass through the barrier or the
            // whole reload pipeline stalls.
            return barrier.wait(Unit.INSTANCE).thenAccept(unit -> {
            });
        }

        @Override
        public String getName() {
            return "WikiJump English name cache";
        }
    }
}
