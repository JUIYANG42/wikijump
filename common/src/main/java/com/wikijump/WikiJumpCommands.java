package com.wikijump;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.wikijump.gui.WikiJumpConfigScreen;
import net.minecraft.client.Minecraft;

/**
 * Client command tree, shared by all three loaders. The loader layer only has
 * to hand the built tree to its own dispatcher, which keeps the per-loader
 * command code down to a couple of lines each.
 *
 * <pre>
 *   /wikijump            open the settings screen
 *   /wikijump config     same as above
 *   /wikijump &lt;name&gt;     look up any name, without needing a target
 * </pre>
 */
public final class WikiJumpCommands {

    public static final String ROOT = "wikijump";

    private WikiJumpCommands() {
    }

    /**
     * Builds the whole command tree.
     *
     * The source type is left generic on purpose: NeoForge and Forge dispatch
     * {@code CommandSourceStack} while Fabric uses
     * {@code FabricClientCommandSource}, and this tree has no need to touch the
     * source at all, so one signature serves all three.
     */
    public static <S> LiteralArgumentBuilder<S> build() {
        return LiteralArgumentBuilder.<S>literal(ROOT)
                .executes(context -> {
                    openConfigScreen();
                    return 1;
                })
                .then(LiteralArgumentBuilder.<S>literal("config")
                        .executes(context -> {
                            openConfigScreen();
                            return 1;
                        }))
                .then(RequiredArgumentBuilder.<S, String>argument("name", StringArgumentType.greedyString())
                        .executes(context -> {
                            WikiJumpLogic.openByName(StringArgumentType.getString(context, "name"));
                            return 1;
                        }));
    }

    /** Opens the settings screen on top of whatever is showing right now. */
    public static void openConfigScreen() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new WikiJumpConfigScreen(mc.screen));
    }
}
