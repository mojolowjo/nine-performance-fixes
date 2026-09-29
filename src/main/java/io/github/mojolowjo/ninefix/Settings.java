package io.github.mojolowjo.ninefix;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The list of fixes and which ones are switched on.
 *
 * <p>The switches live in {@code config/ninefix-startup.toml}, which NeoForge's config screen edits
 * (Mods -> NINE Performance Fixes -> Config). They are read once, at startup, straight from that file:
 * Mixin needs them before NeoForge's config system exists, and a snapshot also makes "restart to apply"
 * true for every fix. Uses only java.* so it's safe to load from the Mixin plugin.
 */
public final class Settings {

    /** One switchable fix. {@code mixins} are the simple names of the mixin classes it owns. */
    public record Fix(String key, boolean defaultOn, String label, String description, List<String> mixins) {}

    public static final String FILE_NAME = "ninefix-startup.toml";

    public static final List<Fix> FIXES = List.of(
            new Fix("geckolibAnimationFix", true,
                    "GeckoLib animations",
                    "Makes GeckoLib skip resource folders it can't read (e.g. Mining & Placing Animations), so "
                            + "GeckoLib mobs animate again and stop writing an error to the log every frame.",
                    List.of()),
            new Fix("arphexSphereCache", true,
                    "ArPhEx: build its sphere once",
                    "ArPhEx rebuilds the same sphere mesh twice every frame. Build it once and reuse it.",
                    List.of("ArphexSphereMixin")),
            new Fix("arphexTrophyScan", true,
                    "ArPhEx: slower trophy search",
                    "ArPhEx searches every block entity in every loaded chunk every frame for its mob trophies. "
                            + "Re-check each chunk about once a second instead.",
                    List.of("ArphexTrophyScanMixin")),
            new Fix("fancymenuScreenCache", true,
                    "FancyMenu / SpiffyHUD: remember screen lookups",
                    "FancyMenu works out the current screen's name thousands of times per frame. Remember the "
                            + "answer until the screen changes.",
                    List.of("FancyMenuScreenIdMixin", "FancyMenuBlacklistMixin", "FancyMenuRegistryMixin")),
            new Fix("skillExpConfigCache", true,
                    "SkillExpNotifier: keep config in memory",
                    "SkillExpNotifier re-reads its config file from disk every frame. Keep it in memory "
                            + "(re-read at most every 2 seconds).",
                    List.of("SkillExpToastConfigMixin", "SkillExpWatcherMixin")),
            new Fix("kubejsHighlightBuffers", true,
                    "KubeJS: skip idle highlight buffers",
                    "KubeJS clears and copies its highlight buffers every frame even when nothing is "
                            + "highlighted. Skip that work while nothing is highlighted.",
                    List.of("KubeJSHighlightMixin")),
            new Fix("kubejsWebServerOff", true,
                    "KubeJS: turn off the web server",
                    "KubeJS runs a small web server for script developers, and its loop keeps about 3/4 of a "
                            + "CPU core busy nonstop. Players don't need it.",
                    List.of("KubeJSWebServerMixin")),
            new Fix("vanillaBackportLeafTint", true,
                    "VanillaBackport: no throwaway colour caches",
                    "VanillaBackport builds a brand-new colour cache for every leaf block Distant Horizons draws "
                            + "and throws it away. Work the colour out directly instead (same colour).",
                    List.of("VanillaBackportLeafTintMixin")),
            new Fix("distantHorizonsThreadCap", true,
                    "Distant Horizons: use at most half the CPU",
                    "The pack lets Distant Horizons use every CPU core, which starves the game while it loads "
                            + "distant terrain. Cap it at half your CPU threads (its own default). Doesn't change "
                            + "its config file.",
                    List.of()),
            new Fix("obsessedNetworkFix", true,
                    "The Obsessed: stop the network flood (server)",
                    "The Obsessed resends each player's data dozens of times per tick, flooding the connection. "
                            + "Send it once per tick, with the latest values. Only does something on the server "
                            + "(or in single player).",
                    List.of("TheObsessedSyncMixin", "ServerTickFlushMixin")));

    private static final Pattern LINE = Pattern.compile("^\\s*\"?([A-Za-z0-9_]+)\"?\\s*=\\s*(true|false)\\s*(?:#.*)?$");

    private static final Map<String, Boolean> ON;
    private static final String SOURCE;

    static {
        Map<String, Boolean> on = parse(List.of());
        String source;
        Path file = configDir().resolve(FILE_NAME);
        try {
            if (Files.isRegularFile(file)) {
                on = parse(Files.readAllLines(file, StandardCharsets.UTF_8));
                source = file.toString();
            } else {
                source = "defaults (" + file + " not created yet)";
            }
        } catch (IOException | RuntimeException e) {
            source = "defaults (couldn't read " + file + ": " + e + ")";
        }
        ON = Collections.unmodifiableMap(on);
        SOURCE = source;
    }

    /** Switches from the lines of a settings file: defaults, overridden by any "key = true/false" line. */
    static Map<String, Boolean> parse(List<String> lines) {
        Map<String, Boolean> on = new LinkedHashMap<>();
        for (Fix f : FIXES) on.put(f.key(), f.defaultOn());
        for (String line : lines) {
            Matcher m = LINE.matcher(line);
            if (m.matches() && on.containsKey(m.group(1))) on.put(m.group(1), Boolean.parseBoolean(m.group(2)));
        }
        return on;
    }

    /** Is this fix switched on for this run of the game? Unknown keys count as off. */
    public static boolean on(String key) {
        return ON.getOrDefault(key, false);
    }

    /** The fix that owns this mixin class (simple name), or null if none does. */
    public static Fix fixForMixin(String mixinSimpleName) {
        for (Fix f : FIXES) if (f.mixins().contains(mixinSimpleName)) return f;
        return null;
    }

    public static Map<String, Boolean> snapshot() {
        return ON;
    }

    public static String source() {
        return SOURCE;
    }

    static Path configDir() {
        String override = System.getProperty("ninefix.configDir"); // used by the tests
        if (override != null && !override.isEmpty()) return Paths.get(override);
        try {
            Class<?> paths = Class.forName("net.neoforged.fml.loading.FMLPaths");
            Object dir = paths.getField("CONFIGDIR").get(null);
            Object path = dir.getClass().getMethod("get").invoke(dir);
            if (path instanceof Path p) return p;
        } catch (Throwable ignored) {
            // not running under FML (tests) or not set up yet
        }
        return Paths.get("config").toAbsolutePath();
    }

    private Settings() {}
}
