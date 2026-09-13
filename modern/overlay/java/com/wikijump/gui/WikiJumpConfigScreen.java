package com.wikijump.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * In-game settings screen, opened with {@code /wikijump}.
 *
 * <p><b>26.1 generation variant</b> (lives in {@code modern/overlay/java}). This
 * file is deliberately thin: the layout, the widget assembly and the config
 * plumbing all live in {@link ConfigLayout}, which both generations share. Only
 * the two things that genuinely moved in 26.1 are left here, and each is a
 * single method:</p>
 * <ul>
 *   <li>{@code Screen#render(GuiGraphics, ...)} became
 *       {@code Screen#extractRenderState(GuiGraphicsExtractor, ...)}, together
 *       with {@code drawString}/{@code drawCenteredString} becoming
 *       {@code text}/{@code centeredText};</li>
 *   <li>{@code CycleButton.builder} folded {@code withInitialValue} into the
 *       builder call, so it takes the initial value directly.</li>
 * </ul>
 *
 * <p>Edits are written straight into the shared
 * {@link com.wikijump.config.WikiJumpConfig} instance as the player makes them,
 * and flushed to disk when the screen closes — so every change takes effect
 * immediately and there is no separate save step to forget. It is plain vanilla
 * {@code Screen} code with no third-party config library, keeping the mod's
 * zero-dependency promise intact.</p>
 */
public class WikiJumpConfigScreen extends Screen {

    private final ConfigLayout layout;

    public WikiJumpConfigScreen(Screen parent) {
        super(Component.translatable("wikijump.config.title"));
        this.layout = new ConfigLayout(this, parent);
    }

    @Override
    protected void init() {
        layout.build(font, this::addRenderableWidget, WikiJumpConfigScreen::siteButton);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        layout.draw(new ConfigLayout.TextSink() {
            @Override
            public void text(Component line, int x, int y, int color) {
                graphics.text(font, line, x, y, color);
            }

            @Override
            public void centeredText(Component line, int centerX, int y, int color) {
                graphics.centeredText(font, line, centerX, y, color);
            }
        });
    }

    @Override
    public void onClose() {
        layout.onClose();
    }

    /** 26.1's cycle-button builder takes the initial value directly. */
    private static CycleButton<String> siteButton(int x, int y, int width, int height,
                                                 List<String> values, String initialValue,
                                                 Component label,
                                                 BiConsumer<CycleButton<String>, String> onChanged) {
        return CycleButton.<String>builder(ConfigLayout::siteLabel, initialValue)
                .withValues(values)
                .create(x, y, width, height, label, onChanged::accept);
    }
}
