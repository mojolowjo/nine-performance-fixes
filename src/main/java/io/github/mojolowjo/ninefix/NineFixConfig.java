package io.github.mojolowjo.ninefix;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The NeoForge side of {@link Settings}: describes config/ninefix-startup.toml so NeoForge can create it,
 * keep it tidy and show it in the config screen. It's a STARTUP config, so the screen asks for a game
 * restart after changes. The game itself reads the switches through {@link Settings}.
 */
final class NineFixConfig {
    static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        for (Settings.Fix f : Settings.FIXES) {
            b.comment(f.label() + ": " + f.description(), "Restart the game after changing this.")
                    .translation(NineFix.MOD_ID + ".configuration." + f.key())
                    .gameRestart()
                    .define(f.key(), f.defaultOn());
        }
        SPEC = b.build();
    }

    private NineFixConfig() {}
}
