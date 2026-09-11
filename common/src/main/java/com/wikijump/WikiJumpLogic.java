package com.wikijump;

import com.wikijump.config.WikiJumpConfig;
import com.wikijump.wiki.WikiSite;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.ClientLanguage;
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
import java.util.List;

/**
 * Loader-independent core: resolves what the player is looking at (hovered
 * slot, crosshair block/entity, or main-hand fallback) and opens the matching
 * wiki page in the system browser.
 *
 * Lookup rules:
 * - vanilla content follows the configured wiki site ("auto" = game language);
 * - modded content with a Chinese display name searches mcmod.cn (MC百科);
 * - other modded content searches the FTB Wiki;
 * - a "custom:" wikiSite template overrides everything;
 * - holding Shift while pressing the key (Shift+K by default) looks the target
 *   up on the foreign wiki using its in-game English (en_us) name instead.
 */
public final class WikiJumpLogic {

    private WikiJumpLogic() {
    }

    /**
     * Handles a key press while a screen is open. Returns true if the key was
     * consumed (the caller should then cancel the underlying event).
     */
    public static boolean onScreenKey(Screen screen, int keyCode, int scanCode) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return false;
        }
        // Don't hijack typing in search boxes and other text fields.
        if (screen.getFocused() != null && screen.getFocused().keyPressed(keyCode, scanCode, 0)) {
            return false;
        }
        if (WikiKey.openWiki == null || !WikiKey.openWiki.matches(keyCode, scanCode)) {
            return false;
        }
        Slot hovered = containerScreen.hoveredSlot;
        if (hovered == null || !hovered.hasItem()) {
            return false;
        }
        ItemStack stack = hovered.getItem();
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
        String englishName = englishName(translationKey, pageTitle);
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
     * Resolves the target's in-game English (en_us) name from its translation
     * key. Loads the en_us language files of vanilla and every installed mod
     * from the resource manager, so the result is exactly the name shown by an
     * English-language game. Falls back to the localized display name when no
     * English translation exists.
     */
    private static String englishName(String translationKey, String fallback) {
        if (translationKey == null || translationKey.isEmpty()) {
            return fallback;
        }
        try {
            ClientLanguage english = ClientLanguage.loadFrom(
                    Minecraft.getInstance().getResourceManager(), List.of("en_us"), false);
            if (english.has(translationKey)) {
                String name = english.getOrDefault(translationKey, translationKey);
                if (!name.isEmpty() && !name.equals(translationKey)) {
                    return name;
                }
            }
        } catch (Exception e) {
            WikiJump.LOGGER.error("Failed to resolve English name for {}", translationKey, e);
        }
        return fallback;
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
