package com.wikijump.gui;

import com.wikijump.config.WikiJumpConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * In-game settings screen, opened with {@code /wikijump}.
 *
 * Edits are written straight into the shared {@link WikiJumpConfig} instance as
 * the player makes them, and flushed to disk when the screen closes — so every
 * change takes effect immediately and there is no separate save step to forget.
 *
 * The screen is plain vanilla {@code Screen} code with no third-party config
 * library, keeping the mod's zero-dependency promise intact.
 */
public class WikiJumpConfigScreen extends Screen {

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
    private static final int ROW_GAP = 4;

    private final Screen parent;

    private int left;
    private int titleY;
    private int hintY;

    private EditBox customUrl;
    private EditBox moddedChineseUrl;
    private EditBox moddedForeignUrl;

    public WikiJumpConfigScreen(Screen parent) {
        super(Component.translatable("wikijump.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        WikiJumpConfig cfg = WikiJumpConfig.get();
        left = (width - PANEL_WIDTH) / 2;
        int controlX = left + LABEL_WIDTH;
        int y = Math.max(34, (height - 176) / 2);
        titleY = y - 24;

        // 1. Which wiki site vanilla content goes to.
        String initialSite = SITES.contains(cfg.wikiSite) ? cfg.wikiSite : "auto";
        addRenderableWidget(CycleButton.<String>builder(WikiJumpConfigScreen::siteLabel)
                .withValues(SITES)
                .withInitialValue(initialSite)
                .create(controlX, y, CONTROL_WIDTH, ROW_HEIGHT,
                        Component.translatable("wikijump.config.site"),
                        (button, value) -> {
                            cfg.wikiSite = value;
                            refreshCustomUrlState();
                        }));
        y += ROW_HEIGHT + ROW_GAP;

        // 2-4. The three URL templates. {name} is substituted on lookup.
        customUrl = templateBox(cfg.customUrl, controlX, y, "wikijump.config.custom_url",
                value -> cfg.customUrl = value);
        y += ROW_HEIGHT + ROW_GAP;

        moddedChineseUrl = templateBox(cfg.moddedChineseUrl, controlX, y,
                "wikijump.config.modded_chinese_url", value -> cfg.moddedChineseUrl = value);
        y += ROW_HEIGHT + ROW_GAP;

        moddedForeignUrl = templateBox(cfg.moddedForeignUrl, controlX, y,
                "wikijump.config.modded_foreign_url", value -> cfg.moddedForeignUrl = value);
        y += ROW_HEIGHT + ROW_GAP;

        // 5-6. Behaviour switches.
        addRenderableWidget(Checkbox.builder(Component.translatable("wikijump.config.fallback_main_hand"), font)
                .pos(controlX, y)
                .maxWidth(CONTROL_WIDTH)
                .selected(cfg.fallbackToMainHand)
                .onValueChange((box, value) -> cfg.fallbackToMainHand = value)
                .build());
        y += ROW_HEIGHT + ROW_GAP;

        addRenderableWidget(Checkbox.builder(Component.translatable("wikijump.config.show_message"), font)
                .pos(controlX, y)
                .maxWidth(CONTROL_WIDTH)
                .selected(cfg.showOpenMessage)
                .onValueChange((box, value) -> cfg.showOpenMessage = value)
                .build());
        y += ROW_HEIGHT + 14;

        addRenderableWidget(Button.builder(Component.translatable("wikijump.config.done"), button -> onClose())
                .bounds(width / 2 - 60, y, 120, ROW_HEIGHT)
                .build());
        hintY = y + ROW_HEIGHT + 6;

        refreshCustomUrlState();
    }

    /** Creates a URL-template text box wired to the given config setter. */
    private EditBox templateBox(String initial, int x, int y, String labelKey,
                                java.util.function.Consumer<String> setter) {
        EditBox box = addRenderableWidget(new EditBox(font, x, y, CONTROL_WIDTH, ROW_HEIGHT,
                Component.translatable(labelKey)));
        box.setMaxLength(512);
        box.setValue(initial == null ? "" : initial);
        box.setResponder(setter);
        return box;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(font, title, width / 2, titleY, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("wikijump.config.custom_url"),
                left + 4, customUrl.getY() + 6, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("wikijump.config.modded_chinese_url"),
                left + 4, moddedChineseUrl.getY() + 6, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("wikijump.config.modded_foreign_url"),
                left + 4, moddedForeignUrl.getY() + 6, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("wikijump.config.hint"),
                width / 2, hintY, 0xA0A0A0);
    }

    /** The custom URL box is only usable while the custom site is selected. */
    private void refreshCustomUrlState() {
        boolean custom = WikiJumpConfig.get().isCustom();
        customUrl.setEditable(custom);
        customUrl.active = custom;
        customUrl.setTextColor(custom ? EditBox.DEFAULT_TEXT_COLOR : 0x707070);
    }

    @Override
    public void onClose() {
        WikiJumpConfig cfg = WikiJumpConfig.get();
        cfg.normalize();
        cfg.save();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    /** Display label for one value of the site cycle button. */
    private static Component siteLabel(String id) {
        if (CUSTOM.equals(id)) {
            return Component.translatable("wikijump.config.site.custom");
        }
        if ("auto".equals(id)) {
            return Component.translatable("wikijump.config.site.auto");
        }
        return Component.literal(id);
    }
}
