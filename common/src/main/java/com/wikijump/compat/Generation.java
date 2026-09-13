package com.wikijump.compat;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.net.URI;

/**
 * The complete set of Minecraft APIs whose names or shapes differ between the
 * 1.21.1 and 26.x generations, expressed as methods that are identical in both.
 *
 * <p>Everything else in the mod — the lookup rules, the hover tracking, the
 * tooltip hint, the settings layout — is written against this interface and
 * therefore compiles once, unchanged, for every loader and every generation.
 * Only {@link GenerationImpl} differs, and there is exactly one of those per
 * generation.</p>
 *
 * <p><b>Why an interface instead of a handful of {@code if} branches:</b> the
 * differences are compile-time ones (a class that moved, a method that grew an
 * argument, a type that no longer exists under that name). No runtime check can
 * paper over them, so the choice has to be made by having the compiler see a
 * different {@code GenerationImpl} — which the build does by merging each
 * generation's own copy over the shared one.</p>
 *
 * <p>Deliberately absent: the {@link KeyMapping} constructor and its category.
 * Those differ per <em>loader</em> as well as per generation (NeoForge registers
 * categories through an event, Fabric through a static call), so they stay in the
 * loader adapters, which are per-generation files anyway.</p>
 */
public interface Generation {

    // --- browser -----------------------------------------------------------------

    /** Opens a URL in the system browser. */
    void openUri(URI uri) throws Exception;

    // --- chat --------------------------------------------------------------------

    /** Shows a message above the hotbar. */
    void actionBar(Component message);

    // --- input -------------------------------------------------------------------

    /** Whether either Shift key is physically held. */
    boolean shiftDown();

    /** Whether a press is the given binding. */
    boolean keyMatches(KeyMapping binding, KeyPress press);

    /**
     * Offers a press to the focused widget, so text fields can claim it before
     * the mod does.
     */
    boolean keyPressed(GuiEventListener focused, KeyPress press);

    // --- registry keys -----------------------------------------------------------
    // BuiltInRegistries#getKey returns ResourceLocation in 1.21.1 but Identifier
    // in 26.1. Both answer getNamespace(), but the return type of getKey is what
    // the shared code would have to name, so the call is made here instead.

    String namespaceOfItem(Item item);

    String namespaceOfBlock(Block block);

    String namespaceOfEntityType(EntityType<?> type);

    // --- naming ------------------------------------------------------------------

    /**
     * The item's translation key. 1.21.1 answers this on {@link ItemStack}
     * itself; 26.1 dropped that method and asks the item.
     */
    String descriptionId(ItemStack stack);

    // --- resource reload ---------------------------------------------------------

    /**
     * Registers a one-shot listener that runs {@code invalidator} whenever
     * resources are reloaded (F3+T, resource pack change, ...).
     *
     * <p>Implemented here rather than per generation: both generations ship
     * {@code ResourceManagerReloadListener} with the same
     * {@code onResourceManagerReload(ResourceManager)} callback, and that
     * interface is the whole reason this hook does not have to be written
     * against {@code PreparableReloadListener} — whose {@code reload} signature
     * <em>did</em> change, and whose barrier protocol is more machinery than a
     * cache reset needs.</p>
     *
     * <p>Only useful on platforms that accept a late registration. NeoForge 26.1
     * swaps the vanilla listener list for an immutable one once mod loading
     * ends, so its adapter registers rather during client setup — see
     * {@code EnglishNames#installReloadHook}.</p>
     *
     * @return true when the hook was installed; false if the resource manager
     *         was not available yet, in which case the caller may try later
     */
    // Suppressed rather than avoided: registering through the vanilla resource
    // manager is the right call on Fabric, on Forge, and on NeoForge 21.x, and
    // it is the reason this hook needs no loader code on any of them. Only
    // NeoForge 26.1 marks the method deprecated, and on that platform the
    // adapter installs the hook first, so this body never runs there.
    @SuppressWarnings("deprecation")
    default boolean registerReloadHook(Runnable invalidator) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return false;
        }
        ResourceManager rm = mc.getResourceManager();
        if (!(rm instanceof ReloadableResourceManager reloadable)) {
            return false;
        }
        reloadable.registerReloadListener(new ResourceManagerReloadListener() {
            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                invalidator.run();
            }
        });
        return true;
    }

    // --- lookup ------------------------------------------------------------------

    /** The implementation for the generation this jar was built for. */
    static Generation get() {
        return GenerationImpl.INSTANCE;
    }
}
