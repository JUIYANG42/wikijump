package com.wikijump.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wikijump.WikiJump;
import com.wikijump.wiki.WikiSite;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple JSON config stored at {@code config/wikijump.json}. Values here mirror
 * what the loader adapters need; the file is created with defaults on first
 * launch and re-read whenever the game world is joined.
 */
public class WikiJumpConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String DEFAULT_CUSTOM_URL = "https://minecraft.wiki/w/{name}";
    private static final String DEFAULT_MODDED_CHINESE_URL = "https://search.mcmod.cn/s?key={name}";
    private static final String DEFAULT_MODDED_FOREIGN_URL = "https://ftb.fandom.com/wiki/Special:Search?query={name}";

    /** Which wiki site to open; "auto" follows the game language. */
    public String wikiSite = "auto";

    /** Custom URL template used when wikiSite starts with "custom:", containing {name}. */
    public String customUrl = DEFAULT_CUSTOM_URL;

    /**
     * URL template for modded content whose display name is Chinese (i.e. the
     * mod provides a Chinese translation). Defaults to the mcmod.cn search.
     * Set to an empty string to disable and fall back to the regular wiki.
     */
    public String moddedChineseUrl = DEFAULT_MODDED_CHINESE_URL;

    /**
     * URL template for modded content without a Chinese display name.
     * Defaults to the FTB Wiki search. Set to an empty string to disable and
     * fall back to the regular wiki.
     */
    public String moddedForeignUrl = DEFAULT_MODDED_FOREIGN_URL;

    /** When no crosshair target exists, fall back to the main-hand item. */
    public boolean fallbackToMainHand = true;

    /** Show an action-bar message when the wiki page is opened. */
    public boolean showOpenMessage = true;

    private static WikiJumpConfig instance;

    public static WikiJumpConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static Path configFile() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve(WikiJump.MOD_ID + ".json");
    }

    public static WikiJumpConfig load() {
        Path path = configFile();
        if (Files.exists(path)) {
            try {
                WikiJumpConfig cfg = GSON.fromJson(Files.readString(path), WikiJumpConfig.class);
                if (cfg != null) {
                    cfg.normalize();
                    return cfg;
                }
            } catch (Exception e) {
                WikiJump.LOGGER.error("Failed to read config, using defaults", e);
            }
        }
        WikiJumpConfig cfg = new WikiJumpConfig();
        cfg.save();
        return cfg;
    }

    public void save() {
        try {
            Files.createDirectories(configFile().getParent());
            Files.writeString(configFile(), GSON.toJson(this));
        } catch (IOException e) {
            WikiJump.LOGGER.error("Failed to save config", e);
        }
    }

    /** Drops invalid values back to defaults. */
    private void normalize() {
        if (WikiSite.byId(wikiSite) == null && !isCustom()) {
            wikiSite = "auto";
        }
        if (isCustom() && (customUrl == null || !customUrl.contains("{name}"))) {
            customUrl = DEFAULT_CUSTOM_URL;
        }
        // A null template means the user disabled that lookup path.
        if (moddedChineseUrl == null) {
            moddedChineseUrl = "";
        }
        if (moddedForeignUrl == null) {
            moddedForeignUrl = "";
        }
    }

    public boolean isCustom() {
        return wikiSite != null && wikiSite.startsWith(WikiSite.CUSTOM_PREFIX);
    }

    /** Builds the final URL for a page title according to this config. */
    public String urlFor(String pageTitle) {
        if (isCustom()) {
            String encoded = pageTitle.replace(' ', '_');
            return customUrl.replace("{name}", encoded);
        }
        WikiSite site = WikiSite.byId(wikiSite);
        if (site == null) {
            site = WikiSite.AUTO;
        }
        return site.urlFor(pageTitle);
    }
}
