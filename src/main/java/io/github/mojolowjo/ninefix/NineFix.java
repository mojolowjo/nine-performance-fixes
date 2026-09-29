package io.github.mojolowjo.ninefix;

import java.util.Map;
import java.util.StringJoiner;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NINE Performance Fixes: targeted fixes for the NINE modpack. Every fix can be switched off in
 * Mods -> NINE Performance Fixes -> Config (config/ninefix-startup.toml); changes apply after a restart.
 * See {@link Settings} for the list. Most fixes are Mixins, gated by mixin/NineFixMixinPlugin.
 */
@Mod(NineFix.MOD_ID)
public final class NineFix {
    public static final String MOD_ID = "ninefix";
    public static final Logger LOG = LoggerFactory.getLogger("NINE Fix");

    public NineFix(ModContainer container, Dist dist) {
        try {
            container.registerConfig(ModConfig.Type.STARTUP, NineFixConfig.SPEC);
        } catch (Throwable t) {
            LOG.warn("Couldn't register the settings file; using the settings read at startup", t);
        }
        StringJoiner on = new StringJoiner(", "), off = new StringJoiner(", ");
        for (Map.Entry<String, Boolean> e : Settings.snapshot().entrySet()) {
            (e.getValue() ? on : off).add(e.getKey());
        }
        LOG.info("Fixes on: [{}], off: [{}] (settings from {})", on, off, Settings.source());

        if (dist == Dist.CLIENT) {
            try {
                NineFixClient.init(container);
            } catch (Throwable t) {
                LOG.warn("Couldn't add the settings screen", t);
            }
            if (Settings.on("geckolibAnimationFix")) {
                try {
                    GeckoLibFix.run();
                } catch (Throwable t) {
                    LOG.warn("GeckoLib folder check failed; nothing changed", t);
                }
            }
            if (Settings.on("distantHorizonsThreadCap") && classExists("com.seibel.distanthorizons.api.methods.events.DhApiEventRegister")) {
                try {
                    DistantHorizonsFix.register();
                } catch (Throwable t) {
                    LOG.warn("Couldn't set up the Distant Horizons thread cap", t);
                }
            }
        }
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, NineFix.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
