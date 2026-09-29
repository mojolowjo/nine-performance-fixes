package io.github.mojolowjo.ninefix;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only setup. Only loaded on the client (NineFix checks the side first). */
final class NineFixClient {
    static void init(ModContainer container) {
        // NeoForge's built-in config screen: Mods -> NINE Performance Fixes -> Config
        IConfigScreenFactory factory = (mod, parent) -> new ConfigurationScreen(mod, parent);
        container.registerExtensionPoint(IConfigScreenFactory.class, factory);
    }

    private NineFixClient() {}
}
