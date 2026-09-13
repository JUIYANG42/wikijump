package com.wikijump.gui;

import com.wikijump.config.WikiJumpConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Everything the settings screen does, minus the parts that cannot be shared.
 *
 * <p>The screen is a grid of widgets plus five lines of text, and every one of
 * those pieces — the widget classes, their constructors, their builder methods,
 * the config fields they write — is identical in 1.21.1 and 26.1. Only two
 * things moved, and this class takes each as a parameter instead of duplicating
 * the whole screen:</p>
 * <ul>
 *   <li>{@link SiteButton} — {@code CycleButton.builder} grew the initial value
 *       as an argument in 26.1, so {@code withInitialValue} no longer exists;</li>
 *   <li>{@link TextSink} — the render entry point moved
 *       ({@code render(GuiGraphics, ...)} became
 *       {@code extractRenderState(GuiGraphicsExtractor, ...)}) and
 *       {@code drawString} / {@code drawCenteredString} became
 *       {@code text} / {@code centeredText}.</li>
 * </ul>
 *
 * <p>{@link WidgetHost} exists for a Java reason rather than a Minecraft one:
 * {@code Screen#addRenderableWidget} is {@code protected}, so a helper in
 * another package cannot call it. The screen passes
 * {@code this::addRenderableWidget} through.</p>
 */
public final class ConfigLayout {

    /** Sentinel value the site button uses for a user-supplied URL template. */
    private static final String CUSTOM = "custom:";

    private static final List<String> SITES = List.of(
            "auto",
            "minecraft.wiki",
            "zh.minecraft.wiki",
            "minecraft.fandom.com",
            "minecraft.fandom.com/zh",
            CUSTOM);

    private static final int PANEL_WIDTH = 330;
    private static final int LABEL_WIDTH = 132;
    private static final int CONTROL_WIDTH = PANEL_WIDTH - LABEL_WIDTH;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 2;
    private static final int ROW_STEP = ROW_HEIGHT + ROW_GAP;

    /** Number of stacked control rows, used to centre the whole panel. */
    private static final int ROWS = 8;

    /** Adds a widget to the screen (see the class comment for why this exists). */
    @FunctionalInterface
    public interface WidgetHost {
        <T extends GuiEventListener & Renderable & NarratableEntry> T add(T widget);
    }

    /** Draws one line of text. The only thing a generation supplies to render. */
    public interface TextSink {
        void text(Component line, int x, int y, int color);

        void centeredText(Component line, int centerX, int y, int color);
    }

    /** Builds the wiki-site cycle button. */
    @FunctionalInterface
    public interface SiteButton {
        CycleButton<String> create(int x, int y, int width, int height,
                                   List<String> values, String initialValue,
                                   Component label,
                                   BiConsumer<CycleButton<String>, String> onChanged);
    }

    private final Screen screen;
    private final Screen parent;

    private int left;
    private int titleY;
    private int hintY;

    private EditBox customUrl;
    private EditBox moddedChineseUrl;
    private EditBox moddedForeignUrl;

    public ConfigLayout(Screen screen, Screen parent) {
        this.screen = screen;
        this.parent = parent;
    }

    /** Assembles every widget. Called from the screen's {@code init()}. */
    public void build(Font font, WidgetHost host, SiteButton siteButton) {
        WikiJumpConfig cfg = WikiJumpConfig.get();

        left = (screen.width - PANEL_WIDTH) / 2;
        int controlX = left + LABEL_WIDTH;
        int panelHeight = ROWS * ROW_STEP + ROW_HEIGHT;
        // 24 rather than 30 so the hint line still fits a 320x240 GUI scale.
        int y = Math.max(24, (screen.height - panelHeight) / 2);
        titleY = y - 20;

        // 1. Which wiki site vanilla content goes to.
        String initialSite = SITES.contains(cfg.wikiSite) ? cfg.wikiSite : "auto";
        host.add(siteButton.create(controlX, y, CONTROL_WIDTH, ROW_HEIGHT, SITES, initialSite,
                Component.translatable("wikijump.config.site"),
                (button, value) -> {
                    cfg.wikiSite = value;
                    refreshCustomUrlState();
                }));
        y += ROW_STEP;

        // 2-4. The three URL templates. {name} is substituted on lookup.
        customUrl = templateBox(font, host, cfg.customUrl, controlX, y, "wikijump.config.custom_url",
                value -> cfg.customUrl = value);
        y += ROW_STEP;

        moddedChineseUrl = templateBox(font, host, cfg.moddedChineseUrl, controlX, y,
                "wikijump.config.modded_chinese_url", value -> cfg.moddedChineseUrl = value);
        y += ROW_STEP;

        moddedForeignUrl = templateBox(font, host, cfg.moddedForeignUrl, controlX, y,
                "wikijump.config.modded_foreign_url", value -> cfg.moddedForeignUrl = value);
        y += ROW_STEP;

        // 5-8. Behaviour switches.
        y = checkbox(font, host, controlX, y, "wikijump.config.fallback_main_hand",
                cfg.fallbackToMainHand, value -> cfg.fallbackToMainHand = value, ROW_STEP);
        y = checkbox(font, host, controlX, y, "wikijump.config.overlay_items",
                cfg.overlayItemLookup, value -> cfg.overlayItemLookup = value, ROW_STEP);
        y = checkbox(font, host, controlX, y, "wikijump.config.show_message",
                cfg.showOpenMessage, value -> cfg.showOpenMessage = value, ROW_STEP);
        y = checkbox(font, host, controlX, y, "wikijump.config.show_tooltip_hint",
                cfg.showTooltipHint, value -> cfg.showTooltipHint = value, ROW_HEIGHT + 6);

        host.add(Button.builder(Component.translatable("wikijump.config.done"), button -> onClose())
                .bounds(screen.width / 2 - 60, y, 120, ROW_HEIGHT)
                .build());
        hintY = y + ROW_HEIGHT + 6;

        refreshCustomUrlState();
    }

    /** Draws the labels the widgets do not carry. Called from the screen's render. */
    public void draw(TextSink sink) {
        sink.centeredText(screen.getTitle(), screen.width / 2, titleY, 0xFFFFFF);
        sink.text(Component.translatable("wikijump.config.custom_url"),
                left + 4, customUrl.getY() + 6, 0xFFFFFF);
        sink.text(Component.translatable("wikijump.config.modded_chinese_url"),
                left + 4, moddedChineseUrl.getY() + 6, 0xFFFFFF);
        sink.text(Component.translatable("wikijump.config.modded_foreign_url"),
                left + 4, moddedForeignUrl.getY() + 6, 0xFFFFFF);
        sink.centeredText(Component.translatable("wikijump.config.hint"),
                screen.width / 2, hintY, 0xA0A0A0);
    }

    /** Flushes the edits and returns to the parent screen (or closes if none). */
    public void onClose() {
        WikiJumpConfig cfg = WikiJumpConfig.get();
        cfg.normalize();
        cfg.save();
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.setScreen(parent);
        }
    }

    /** Creates a URL-template text box wired to the given config setter. */
    private static EditBox templateBox(Font font, WidgetHost host, String initial,
                                       int x, int y, String labelKey, Consumer<String> setter) {
        EditBox box = host.add(new EditBox(font, x, y, CONTROL_WIDTH, ROW_HEIGHT,
                Component.translatable(labelKey)));
        box.setMaxLength(512);
        box.setValue(initial == null ? "" : initial);
        box.setResponder(setter);
        return box;
    }

    /** Adds one behaviour checkbox and returns the next row's y. */
    private static int checkbox(Font font, WidgetHost host, int x, int y, String labelKey,
                                boolean selected, Consumer<Boolean> setter, int advance) {
        host.add(Checkbox.builder(Component.translatable(labelKey), font)
                .pos(x, y)
                .maxWidth(CONTROL_WIDTH)
                .selected(selected)
                .onValueChange((box, value) -> setter.accept(value))
                .build());
        return y + advance;
    }

    /** The custom URL box is only usable while the custom site is selected. */
    private void refreshCustomUrlState() {
        boolean custom = WikiJumpConfig.get().isCustom();
        customUrl.setEditable(custom);
        customUrl.active = custom;
        customUrl.setTextColor(custom ? EditBox.DEFAULT_TEXT_COLOR : 0x707070);
    }

    /** Display label for one value of the site cycle button. */
    public static Component siteLabel(String id) {
        if (CUSTOM.equals(id)) {
            return Component.translatable("wikijump.config.site.custom");
        }
        if ("auto".equals(id)) {
            return Component.translatable("wikijump.config.site.auto");
        }
        return Component.literal(id);
    }
}
