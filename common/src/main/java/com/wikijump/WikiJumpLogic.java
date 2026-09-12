package com.wikijump;

import com.wikijump.config.WikiJumpConfig;
import com.wikijump.wiki.EnglishNames;
import com.wikijump.wiki.WikiSite;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Loader-independent core: resolves what the player is looking at (hovered
 * item, crosshair block/entity, or main-hand fallback) and opens the matching
 * wiki page in the system browser.
 *
 * Lookup rules:
 * - vanilla content follows the configured wiki site ("auto" = game language);
 * - modded content with a Chinese display name searches mcmod.cn (MC百科);
 * - other modded content searches the FTB Wiki;
 * - a "custom:" wikiSite template overrides everything;
 * - holding Shift while pressing the key (Shift+K by default) looks the target
 *   up on the foreign wiki using its in-game English (en_us) name instead.
 *
 * What is under the cursor is taken from {@link HoverTracker} — which covers
 * the item lists of JEI, EMI and REI, plus any other overlay that draws its own
 * item tooltips — and from the vanilla hovered slot as a fallback.
 */
public final class WikiJumpLogic {

    private WikiJumpLogic() {
    }

    /**
     * Handles a key press while a screen is open. Returns true if the key was
     * consumed (the caller should then cancel the underlying event).
     *
     * Any screen is accepted, not just container screens: with JEI, EMI or REI
     * installed, their item lists and bookmarks are drawn on every screen, and
     * those are precisely the places where a lookup is most useful.
     */
    public static boolean onScreenKey(Screen screen, int keyCode, int scanCode) {
        if (screen == null || WikiKey.openWiki == null
                || !WikiKey.openWiki.matches(keyCode, scanCode)) {
            return false;
        }
        // Don't hijack typing in search boxes and other text fields.
        if (isTyping(screen, keyCode, scanCode)) {
            return false;
        }
        ItemStack stack = hoveredItem(screen);
        if (stack.isEmpty()) {
            return false;
        }
        String title = stack.getHoverName().getString();
        if (title.isEmpty()) {
            return false;
        }
        openWiki(title, stack.getDescriptionId(),
                namespaceOf(BuiltInRegistries.ITEM.getKey(stack.getItem())),
                Screen.hasShiftDown());
        return true;
    }

    /**
     * The item under the cursor: an overlay capture first (JEI/EMI/REI item
     * lists and anything else drawing its own tooltips), a vanilla slot second.
     */
    private static ItemStack hoveredItem(Screen screen) {
        if (WikiJumpConfig.get().overlayItemLookup) {
            ItemStack tracked = HoverTracker.current();
            if (!tracked.isEmpty()) {
                return tracked;
            }
        }
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            Slot slot = containerScreen.hoveredSlot;
            if (slot != null && slot.hasItem()) {
                return slot.getItem();
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * True when the key belongs to the focused text field instead of to us.
     * Recipe viewers focus their search box through the regular widget system,
     * so an {@link EditBox} check covers them without knowing which viewer it
     * is; the {@code keyPressed} probe catches the odd custom field that does
     * not extend it.
     */
    private static boolean isTyping(Screen screen, int keyCode, int scanCode) {
        GuiEventListener focused = screen.getFocused();
        if (focused == null) {
            return false;
        }
        if (focused instanceof EditBox box && box.canConsumeInput()) {
            return true;
        }
        return focused.keyPressed(keyCode, scanCode, 0);
    }

    /**
     * Handles the key press in-world (no screen open). Crosshair target first,
     * main-hand item as fallback, "no target" message otherwise.
     */
    public static void onWorldKey() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        boolean english = Screen.hasShiftDown();
        HitResult hit = mc.hitResult;
        if (hit != null) {
            String title = null;
            String translationKey = null;
            String namespace = "minecraft";
            if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
                BlockState state = mc.level.getBlockState(blockHit.getBlockPos());
                title = state.getBlock().getName().getString();
                translationKey = state.getBlock().getDescriptionId();
                namespace = namespaceOf(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
            } else if (hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult entityHit) {
                Entity entity = entityHit.getEntity();
                title = entity.getType().getDescription().getString();
                translationKey = entity.getType().getDescriptionId();
                namespace = namespaceOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
            }
            if (title != null && !title.isEmpty()) {
                openWiki(title, translationKey, namespace, english);
                return;
            }
        }

        WikiJumpConfig cfg = WikiJumpConfig.get();
        if (cfg.fallbackToMainHand) {
            ItemStack mainHand = mc.player.getMainHandItem();
            if (!mainHand.isEmpty()) {
                openWiki(mainHand.getHoverName().getString(),
                        mainHand.getDescriptionId(),
                        namespaceOf(BuiltInRegistries.ITEM.getKey(mainHand.getItem())),
                        english);
                return;
            }
        }
        actionBar("message.wikijump.no_target");
    }

    /**
     * Opens the best lookup URL for the given target. With {@code forceEnglish}
     * (Shift held while pressing the key) the lookup instead uses the target's
     * in-game English (en_us) name and goes to the foreign wiki, regardless of
     * the localized display name.
     */
    private static void openWiki(String pageTitle, String translationKey,
                                 String namespace, boolean forceEnglish) {
        WikiJumpConfig cfg = WikiJumpConfig.get();
        if (forceEnglish) {
            openWikiInEnglish(pageTitle, translationKey, namespace, cfg);
            return;
        }
        if (!"minecraft".equals(namespace) && !cfg.isCustom()) {
            boolean chinese = containsChinese(pageTitle);
            if (chinese && !cfg.moddedChineseUrl.isEmpty()) {
                openUrl(applyTemplate(cfg.moddedChineseUrl, pageTitle), pageTitle,
                        "message.wikijump.searching");
                return;
            }
            if (!chinese && !cfg.moddedForeignUrl.isEmpty()) {
                openUrl(applyTemplate(cfg.moddedForeignUrl, pageTitle), pageTitle,
                        "message.wikijump.searching");
                return;
            }
        }
        openUrl(cfg.urlFor(pageTitle), pageTitle, "message.wikijump.opening");
    }

    /**
     * Forced English lookup: translates the target to its in-game en_us name
     * and searches the foreign wiki — the FTB Wiki for modded content, the
     * English counterpart of the configured wiki site for vanilla content.
     */
    private static void openWikiInEnglish(String pageTitle, String translationKey,
                                          String namespace, WikiJumpConfig cfg) {
        String englishName = EnglishNames.resolve(translationKey, pageTitle);
        if (!"minecraft".equals(namespace) && !cfg.isCustom() && !cfg.moddedForeignUrl.isEmpty()) {
            openUrl(applyTemplate(cfg.moddedForeignUrl, englishName), englishName,
                    "message.wikijump.searching");
            return;
        }
        if (cfg.isCustom()) {
            openUrl(applyTemplate(cfg.customUrl, englishName), englishName,
                    "message.wikijump.searching");
            return;
        }
        WikiSite site = WikiSite.byId(cfg.wikiSite);
        if (site == null) {
            site = WikiSite.AUTO;
        }
        openUrl(site.englishCounterpart().urlFor(englishName), englishName,
                "message.wikijump.opening");
    }

    /**
     * Opens the configured wiki page for an arbitrary search term. Used by the
     * {@code /wikijump <name>} command, where there is no registry target to
     * take a translation key from.
     */
    public static void openByName(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        String trimmed = name.trim();
        openUrl(WikiJumpConfig.get().urlFor(trimmed), trimmed, "message.wikijump.opening");
    }

    /** Opens the URL in the system browser, with an action-bar message on success. */
    private static void openUrl(String url, String pageTitle, String messageKey) {
        try {
            Util.getPlatform().openUri(URI.create(url));
        } catch (Exception e) {
            WikiJump.LOGGER.error("Failed to open wiki URL: {}", url, e);
            actionBar("message.wikijump.open_failed");
            return;
        }
        if (WikiJumpConfig.get().showOpenMessage) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        Component.translatable(messageKey, pageTitle).withStyle(ChatFormatting.GRAY), true);
            }
        }
    }

    private static void actionBar(String key) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    Component.translatable(key).withStyle(ChatFormatting.GRAY), true);
        }
    }

    /** Registry namespace of the content ("minecraft" for vanilla, mod id otherwise). */
    private static String namespaceOf(ResourceLocation id) {
        return id != null ? id.getNamespace() : "minecraft";
    }

    /** True when the text contains CJK unified ideographs, i.e. it is a Chinese name. */
    private static boolean containsChinese(String text) {
        return text.codePoints().anyMatch(cp -> cp >= 0x4E00 && cp <= 0x9FFF);
    }

    /** Replaces {name} in a URL template with the URL-encoded page title. */
    private static String applyTemplate(String template, String pageTitle) {
        return template.replace("{name}", URLEncoder.encode(pageTitle, StandardCharsets.UTF_8)
                .replace("+", "%20"));
    }
}
