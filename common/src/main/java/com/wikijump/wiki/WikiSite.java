package com.wikijump.wiki;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Wiki sites that can be opened by WikiJump. The page name is the localized
 * in-game display name of the target, so the wiki language should match the
 * game language to resolve to an existing page.
 */
public enum WikiSite {
    /** Follows the game language: zh* opens the Chinese wiki, everything else the English one. */
    AUTO("auto", "Follow game language"),
    /** minecraft.wiki — the English community wiki (most complete). */
    ENGLISH("minecraft.wiki", "minecraft.wiki"),
    /** zh.minecraft.wiki — the Chinese community wiki. */
    CHINESE("zh.minecraft.wiki", "zh.minecraft.wiki"),
    /** minecraft.fandom.com — the legacy English Fandom wiki. */
    FANDOM_EN("minecraft.fandom.com", "minecraft.fandom.com"),
    /** minecraft.fandom.com/zh — the legacy Chinese Fandom wiki. */
    FANDOM_ZH("minecraft.fandom.com/zh", "minecraft.fandom.com/zh");

    public static final String CUSTOM_PREFIX = "custom:";

    private final String id;
    private final String label;

    WikiSite(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    /** Builds the full wiki URL for the given page title. */
    public String urlFor(String pageTitle) {
        String encoded = URLEncoder.encode(pageTitle.replace(' ', '_'), StandardCharsets.UTF_8)
                .replace("+", "%20");
        String host;
        switch (this) {
            case AUTO -> host = gameLanguageIsChinese() ? "zh.minecraft.wiki" : "minecraft.wiki";
            case ENGLISH -> host = "minecraft.wiki";
            case CHINESE -> host = "zh.minecraft.wiki";
            case FANDOM_EN -> host = "minecraft.fandom.com";
            case FANDOM_ZH -> host = "minecraft.fandom.com/zh";
            default -> host = "minecraft.wiki";
        }
        return "https://" + host + "/w/" + (host.startsWith("minecraft.fandom.com") ? encoded : encoded);
    }

    /** True when the game's current language is a Chinese variant. */
    public static boolean gameLanguageIsChinese() {
        String lang = net.minecraft.client.Minecraft.getInstance().getLanguageManager()
                .getSelected();
        return lang != null && lang.toLowerCase(Locale.ROOT).startsWith("zh");
    }

    /** Resolves a site from its config id, or {@code null} if unknown. */
    public static WikiSite byId(String id) {
        for (WikiSite site : values()) {
            if (site.id.equals(id)) {
                return site;
            }
        }
        return null;
    }

    /**
     * The English-language counterpart of this site, used by the forced
     * English lookup (Shift + key). Chinese sites map to their English
     * counterparts; English sites stay unchanged.
     */
    public WikiSite englishCounterpart() {
        return switch (this) {
            case AUTO, CHINESE -> ENGLISH;
            case FANDOM_ZH -> FANDOM_EN;
            default -> this;
        };
    }
}
