package com.wikijump.compat;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.net.URI;

/**
 * {@link Generation} for the Minecraft 26.x generation — the
 * {@code Identifier} era.
 *
 * <p>Merged over {@code common}'s copy (see {@code modern/buildSrc}'s
 * wikijump-loader plugin), so a jar built for 26.1 carries this implementation
 * instead of the 1.21.1 one. It is the <em>only</em> file in the mod that has to
 * know 26.1 renamed things; everything else compiles from the same source for
 * both generations.</p>
 *
 * <p>26.1 names: {@code net.minecraft.util.Util} (was {@code net.minecraft.Util}),
 * key input as a {@code KeyEvent} record, {@code ItemStack} no longer answering
 * {@code getDescriptionId()}, and the action bar via
 * {@code LocalPlayer#sendOverlayMessage(Component)}.</p>
 *
 * <p>{@code registerReloadHook} is not overridden: both generations ship
 * {@code ResourceManagerReloadListener} unchanged, so the interface supplies it.</p>
 */
public final class GenerationImpl implements Generation {

    public static final GenerationImpl INSTANCE = new GenerationImpl();

    private GenerationImpl() {
    }

    @Override
    public void openUri(URI uri) throws Exception {
        Util.getPlatform().openUri(uri);
    }

    @Override
    public void actionBar(Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendOverlayMessage(message);
        }
    }

    @Override
    public boolean shiftDown() {
        // Screen#hasShiftDown() was dropped in 26.1; the equivalent lives on
        // Minecraft now.
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.hasShiftDown();
    }

    @Override
    public boolean keyMatches(KeyMapping binding, KeyPress press) {
        return binding.matches(event(press));
    }

    @Override
    public boolean keyPressed(GuiEventListener focused, KeyPress press) {
        return focused.keyPressed(event(press));
    }

    /**
     * Rebuilds 26.1's key-input record from the three ints. The record's
     * components are exactly {@code (key, scancode, modifiers)}, so this is a
     * lossless round-trip — no information is dropped on the way in or out.
     */
    private static KeyEvent event(KeyPress press) {
        return new KeyEvent(press.keyCode(), press.scanCode(), press.modifiers());
    }

    @Override
    public String namespaceOfItem(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getNamespace();
    }

    @Override
    public String namespaceOfBlock(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getNamespace();
    }

    @Override
    public String namespaceOfEntityType(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace();
    }

    @Override
    public String descriptionId(ItemStack stack) {
        // ItemStack stopped forwarding this in 26.1, so it is asked of the item.
        return stack.getItem().getDescriptionId();
    }
}
