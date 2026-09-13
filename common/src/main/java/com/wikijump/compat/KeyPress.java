package com.wikijump.compat;

/**
 * One key press, reduced to the three ints both Minecraft generations can
 * produce and consume.
 *
 * <p>1.21.1 delivers key input as loose {@code int}s
 * ({@code keyPressed(keyCode, scanCode, modifiers)}), while 26.1 wraps them in a
 * {@code KeyEvent} record — whose components are exactly these three values, in
 * this order. Passing one {@code KeyPress} around therefore lets the shared
 * logic stay unaware of which shape the running game uses, without losing any
 * information: a 26.1 {@code KeyEvent} round-trips through it unchanged.</p>
 *
 * @param keyCode   GLFW key code
 * @param scanCode  platform scan code
 * @param modifiers modifier bitmask (shift/ctrl/alt/super)
 */
public record KeyPress(int keyCode, int scanCode, int modifiers) {
}
