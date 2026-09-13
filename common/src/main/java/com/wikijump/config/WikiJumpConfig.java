package com.wikijump.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wikijump.WikiJump;
import com.wikijump.wiki.WikiSite;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Simple JSON config stored at {@code config/wikijump.json}. Values here mirror
 * what the loader adapters need; the file is created with defaults on first
 * launch, and re-read every time the settings screen is opened so a hand edit
 * takes effect without restarting the game.
 *
 * <p>Beyond the URL templates, the file accepts a {@code sites} array for wiki
 * sites this mod does not ship. Everything listed there shows up in the
 * settings screen's site dropdown and can be selected like a built-in one,
 * which is the place for a regional mirror, a non-Minecraft wiki, or a
 * modpack's own wiki.</p>
 */
public class WikiJumpConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String DEFAULT_CUSTOM_URL = "https://minecraft.wiki/w/{name}";
    private static final String DEFAULT_MODDED_CHINESE_URL = "https://search.mcmod.cn/s?key={name}";
    private static final String DEFAULT_MODDED_FOREIGN_URL = "https://ftb.fandom.com/wiki/Special:Search?query={name}";

    /** Placeholder inside every template, replaced by the name being looked up. */
    public static final String NAME_PLACEHOLDER = "{name}";

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

    /**
     * Wiki sites added by the user, on top of the five built into
     * {@link WikiSite}. Each entry's {@code name} becomes an option in the
     * settings screen's site dropdown, and selecting it sends every lookup —
     * vanilla and modded alike — to that site.
     */
    public List<Site> sites = new ArrayList<>();

    /** When no crosshair target exists, fall back to the main-hand item. */
    public boolean fallbackToMainHand = true;

    /** Show an action-bar message when the wiki page is opened. */
    public boolean showOpenMessage = true;

    /**
     * Append a "press K to open the wiki" line to item tooltips. This is what
     * makes the mod discoverable, but it does add a line to every tooltip, so
     * it can be switched off here or from the in-game settings screen. The
     * line names the player's own keybinding.
     */
    public boolean showTooltipHint = true;

    /**
     * Let the key work on the item lists of JEI, EMI and REI as well, instead
     * of only on the slots of the vanilla screen. The viewer panels draw their
     * own items, so the hovered stack is taken from the tooltip pipeline they
     * all share (see {@code HoverTracker}); switching this off restores the
     * plain hovered-slot behaviour.
     */
    public boolean overlayItemLookup = true;

    /**
     * One user-defined wiki site, written by hand into the {@code sites} array.
     *
     * <p>{@code name} is both the dropdown label and the value stored in
     * {@code wikiSite}, so it has to be unique and must not collide with a
     * built-in site id. {@code englishUrl} is optional: it is used by the
     * Shift lookup so a Chinese site can hand English queries to a different
     * site, and when left empty that lookup simply reuses {@code url}.</p>
     */
    public static class Site {
        public String name = "";
        public String url = "";
        public String englishUrl = "";

        /** True when this entry is complete enough to open a URL. */
        public boolean isUsable() {
            return name != null && !name.isBlank() && hasPlaceholder(url);
        }
    }

    private static WikiJumpConfig instance;

    public static WikiJumpConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    /**
     * Reads the file again, replacing the in-memory copy. Called when the
     * settings screen opens, which is what lets a hand-edited {@code sites}
     * array appear in the dropdown straight away.
     */
    public static WikiJumpConfig reload() {
        instance = load();
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
                WikiJump.LOGGER.error("Failed to read config, keeping a copy and using defaults", e);
                backup(path);
            }
        }
        WikiJumpConfig cfg = new WikiJumpConfig();
        cfg.save();
        return cfg;
    }

    /**
     * Moves an unreadable file aside before the defaults are written back, so a
     * typo in a hand-edited config costs a file rename instead of the whole
     * site list.
     */
    private static void backup(Path path) {
        try {
            Files.move(path, path.resolveSibling(path.getFileName() + ".broken"),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            WikiJump.LOGGER.error("Failed to back up the unreadable config", e);
        }
    }

    public void save() {
        try {
            Files.createDirectories(configFile().getParent());
            Files.writeString(configFile(), GSON.toJson(this));
        } catch (IOException e) {
            WikiJump.LOGGER.error("Failed to save config", e);
        }
    }

    /** Drops invalid values back to defaults. Called on load and by the settings screen. */
    public void normalize() {
        sites = validSites(sites);
        if (!isKnownSite(wikiSite)) {
            wikiSite = "auto";
        }
        if (isCustom() && !hasPlaceholder(customUrl)) {
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

    /**
     * Keeps the usable entries of the {@code sites} array, in order, and drops
     * the rest with a note in the log: each site needs a non-blank name and a
     * template containing {@code {name}}, and the name must not collide with
     * another entry or with a built-in site.
     */
    private static List<Site> validSites(List<Site> configured) {
        List<Site> kept = new ArrayList<>();
        if (configured == null) {
            return kept;
        }
        Set<String> names = new LinkedHashSet<>();
        for (Site site : configured) {
            if (site == null) {
                continue;
            }
            site.name = site.name == null ? "" : site.name.trim();
            site.url = site.url == null ? "" : site.url.trim();
            if (!site.isUsable()) {
                WikiJump.LOGGER.warn("Ignoring wiki site \"{}\": name and a URL containing {} are required",
                        site.name, NAME_PLACEHOLDER);
                continue;
            }
            if (WikiSite.byId(site.name) != null || site.name.startsWith(WikiSite.CUSTOM_PREFIX)
                    || !names.add(site.name)) {
                WikiJump.LOGGER.warn("Ignoring wiki site \"{}\": the name is already taken", site.name);
                continue;
            }
            // An English template is optional, but a malformed one is worse
            // than none: fall back to the main template instead of failing.
            site.englishUrl = hasPlaceholder(site.englishUrl) ? site.englishUrl.trim() : "";
            kept.add(site);
        }
        return kept;
    }

    /** True when the template is present and carries the {name} placeholder. */
    private static boolean hasPlaceholder(String template) {
        return template != null && !template.isBlank() && template.contains(NAME_PLACEHOLDER);
    }

    /** The user-defined site named {@code id}, or null if there is none. */
    public Site site(String id) {
        if (id == null || sites == null) {
            return null;
        }
        for (Site site : sites) {
            if (id.equals(site.name)) {
                return site;
            }
        }
        return null;
    }

    /** True when the id names a site this config can actually open. */
    public boolean isKnownSite(String id) {
        return WikiSite.byId(id) != null
                || (id != null && id.startsWith(WikiSite.CUSTOM_PREFIX))
                || site(id) != null;
    }

    public boolean isCustom() {
        return wikiSite != null && wikiSite.startsWith(WikiSite.CUSTOM_PREFIX);
    }

    /** True when the selected site is one of the user-defined ones. */
    public boolean isUserSite() {
        return site(wikiSite) != null;
    }

    /**
     * True when the selection is an explicit site — the custom template or a
     * user-defined site — rather than one of the built-in wikis. Explicit sites
     * take every lookup, modded content included, because picking a site by
     * name can only mean "send me there".
     */
    public boolean isExplicitSite() {
        return isCustom() || isUserSite();
    }

    /** Builds the final URL for a page title according to this config. */
    public String urlFor(String pageTitle) {
        return urlFor(pageTitle, false);
    }

    /**
     * Builds the final URL for a page title. With {@code english} the
     * user-defined site's optional {@code englishUrl} is preferred, so a
     * Chinese site can pass the Shift lookup to its English counterpart.
     */
    public String urlFor(String pageTitle, boolean english) {
        if (isCustom()) {
            return fillTemplate(customUrl, pageTitle);
        }
        Site site = site(wikiSite);
        if (site != null) {
            String template = english && !site.englishUrl.isEmpty() ? site.englishUrl : site.url;
            return fillTemplate(template, pageTitle);
        }
        WikiSite builtin = WikiSite.byId(wikiSite);
        if (builtin == null) {
            builtin = WikiSite.AUTO;
        }
        return builtin.urlFor(pageTitle);
    }

    /**
     * Substitutes the URL-encoded lookup name into a template. Every
     * user-editable template in this file goes through here, so {@code {name}}
     * means the same thing in all of them — and non-ASCII names always produce
     * a URI that {@code java.net.URI} accepts.
     */
    public static String fillTemplate(String template, String name) {
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        return template.replace(NAME_PLACEHOLDER, encoded);
    }
}
