package com.wikijump;

import com.wikijump.compat.Generation;
import com.wikijump.compat.KeyPress;
import com.wikijump.config.WikiJumpConfig;
import com.wikijump.wiki.EnglishNames;
import com.wikijump.wiki.WikiSite;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
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
 * <p>Also generation-independent, despite 1.21.1 and 26.1 having renamed a good
 * deal of what this class touches: {@code Util} moved package,
 * {@code ResourceLocation} became {@code Identifier}, key input became a record,
 * {@code ItemStack} stopped answering {@code getDescriptionId()},
 * {@code Screen#hasShiftDown()} disappeared, and the action bar changed method.
 * Every one of those is a call site rather than a piece of logic, so they all go
 * through {@link Generation} and this file compiles unchanged for both
 * generations.</p>
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
     * <p>Any screen is accepted, not just container screens: with JEI, EMI or REI
     * installed, their item lists and bookmarks are drawn on every screen, and
     * those are precisely the places where a lookup is most useful.</p>
     */
    public static boolean onScreenKey(Screen screen, KeyPress press) {
        if (screen == null || WikiKey.openWiki == null || press == null
                || !Generation.get().keyMatches(WikiKey.openWiki, press)) {
            return false;
        }
        // Don't hijack typing in search boxes and other text fields.
        if (isTyping(screen, press)) {
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
        openWiki(title, Generation.get().descriptionId(stack),
                Generation.get().namespaceOfItem(stack.getItem()),
                Generation.get().shiftDown());
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
    private static boolean isTyping(Screen screen, KeyPress press) {
        GuiEventListener focused = screen.getFocused();
        if (focused == null) {
            return false;
        }
        if (focused instanceof EditBox box && box.canConsumeInput()) {
            return true;
        }
        return Generation.get().keyPressed(focused, press);
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
        boolean english = Generation.get().shiftDown();
        HitResult hit = mc.hitResult;
        if (hit != null) {
            String title = null;
            String translationKey = null;
            String namespace = "minecraft";
            if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
                BlockState state = mc.level.getBlockState(blockHit.getBlockPos());
                title = state.getBlock().getName().getString();
                translationKey = state.getBlock().getDescriptionId();
                namespace = Generation.get().namespaceOfBlock(state.getBlock());
            } else if (hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult entityHit) {
                Entity entity = entityHit.getEntity();
                title = entity.getType().getDescription().getString();
                translationKey = entity.getType().getDescriptionId();
                namespace = Generation.get().namespaceOfEntityType(entity.getType());
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
                        Generation.get().descriptionId(mainHand),
                        Generation.get().namespaceOfItem(mainHand.getItem()),
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
            Generation.get().openUri(URI.create(url));
        } catch (Exception e) {
            WikiJump.LOGGER.error("Failed to open wiki URL: {}", url, e);
            actionBar("message.wikijump.open_failed");
            return;
        }
        if (WikiJumpConfig.get().showOpenMessage) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                actionBar(messageKey, pageTitle);
            }
        }
    }

    private static void actionBar(String key, Object... args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            Generation.get().actionBar(
                    Component.translatable(key, args).withStyle(ChatFormatting.GRAY));
        }
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
