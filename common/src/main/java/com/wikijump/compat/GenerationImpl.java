package com.wikijump.compat;

import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.net.URI;

/**
 * {@link Generation} for Minecraft 1.21.1 — the {@code ResourceLocation} era.
 *
 * <p>This file is <b>replaced wholesale</b> for the 26.x generation: the build
 * merges {@code modern/overlay/java} over the shared tree, so a jar built for
 * 26.1 carries that generation's implementation instead of this one. Nothing
 * else in the mod needs to know which one it got.</p>
 *
 * <p>1.21.1 names: {@code net.minecraft.Util},
 * {@code KeyMapping#matches(int, int)}, {@code GuiEventListener#keyPressed(int, int, int)},
 * {@code ItemStack#getDescriptionId()}, {@code Screen#hasShiftDown()}, and the
 * action bar via {@code LocalPlayer#displayClientMessage(Component, boolean)}.</p>
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
            mc.player.displayClientMessage(message, true);
        }
    }

    @Override
    public boolean shiftDown() {
        return Screen.hasShiftDown();
    }

    @Override
    public boolean keyMatches(KeyMapping binding, KeyPress press) {
        return binding.matches(press.keyCode(), press.scanCode());
    }

    @Override
    public boolean keyPressed(GuiEventListener focused, KeyPress press) {
        return focused.keyPressed(press.keyCode(), press.scanCode(), press.modifiers());
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
        return stack.getDescriptionId();
    }
}
