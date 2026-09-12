package com.wikijump;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

/**
 * Remembers which item the cursor is resting on while a screen is open, so a
 * lookup can also be started from an overlay that has no vanilla slot.
 *
 * Recipe viewers (JEI, EMI, REI) draw their own item lists, which means there
 * is no {@code hoveredSlot} to ask for the stack under the cursor. Depending
 * on all three APIs — and shipping a variant of that for each of the three
 * loaders — would dwarf the rest of this mod, so instead we listen on the one
 * pipeline every viewer shares: all of them build an item's tooltip through
 * {@code ItemStack#getTooltipLines}, which is exactly the method the loaders'
 * tooltip events already hook for the key hint. Whatever was tooltipped last
 * is what the cursor is on — for a viewer's item list just as for a vanilla
 * slot, and for a bookmark overlay, a recipe preview or any other overlay that
 * draws item tooltips.
 *
 * Two boundaries keep the remembered item honest:
 * <ul>
 *   <li>a capture is dropped as soon as a different screen is open, so a hover
 *       can never leak from one GUI into the next;</li>
 *   <li>a capture requires a real screen <em>and</em> a loaded player, which
 *       keeps the item search trees built at startup with a null player out of
 *       the picture.</li>
 * </ul>
 */
public final class HoverTracker {

    private static ItemStack hovered = ItemStack.EMPTY;
    private static Screen owner;

    private HoverTracker() {
    }

    /**
     * Records the item whose tooltip was just built. Called from the loaders'
     * item-tooltip hooks; anything outside a screen is ignored.
     */
    public static void capture(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        Screen screen = mc.screen;
        if (screen == null || mc.player == null) {
            return;
        }
        // Copy: the slot a stack came from may keep mutating after the capture.
        hovered = stack.copy();
        owner = screen;
    }

    /**
     * The item the cursor was on during the last drawn frame, or an empty stack
     * when nothing was hovered yet or the screen has changed since.
     */
    public static ItemStack current() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.screen == null || mc.screen != owner) {
            return ItemStack.EMPTY;
        }
        return hovered;
    }

    /**
     * Forgets the remembered item. The loaders call this when the player clicks,
     * which is how a hover stops counting once the cursor has moved on to a
     * viewer's search box.
     */
    public static void clear() {
        hovered = ItemStack.EMPTY;
        owner = null;
    }
}
