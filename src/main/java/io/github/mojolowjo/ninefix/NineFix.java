package io.github.mojolowjo.ninefix;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NINE Performance Fixes (client only).
 *
 * Fix 1 - GeckoLib animations never loading:
 *   GeckoLib reads EVERY .json inside ANY "assets/<namespace>/animations/" (and "geo/")
 *   folder from every mod and resource pack. One file in the wrong format (e.g. the
 *   block animations of "Mining & Placing Animations") makes GeckoLib throw away its
 *   whole cache, so no GeckoLib mob animates and each one logs an error + stack trace
 *   every frame. We tell GeckoLib to skip namespaces that only contain non-GeckoLib
 *   files, before the first resource load.
 *
 * Fix 2 - ArPhEx rebuilding a sphere twice per frame: see mixin/ArphexSphereMixin.
 */
@Mod(value = NineFix.MOD_ID, dist = Dist.CLIENT)
public final class NineFix {
    public static final String MOD_ID = "ninefix";
    static final Logger LOG = LoggerFactory.getLogger("NINE Fix");

    /** Known offender, always skipped: its own block-animation format lives in an animations/ folder. */
    private static final String[] ALWAYS_SKIP = { "mining_and_placing_animations" };

    // assets/<ns>/<animations|geo>/...json, optionally inside a built-in pack folder
    private static final Pattern GECKO_PATH =
            Pattern.compile("^(?:.*/)?assets/([a-z0-9_.\\-]+)/(animations|geo)/.+\\.json$");
    private static final long MAX_JSON_BYTES = 16L * 1024 * 1024;
    private static final long MAX_NESTED_JAR_BYTES = 64L * 1024 * 1024;

    public NineFix() {
        try {
            fixGeckoLibNamespaces();
        } catch (Throwable t) {
            LOG.warn("GeckoLib folder check failed; nothing changed", t);
        }
    }

    // ------------------------------------------------------------------
    private static final class Tally {
        int gecko;           // files GeckoLib can read
        int foreign;         // files that would crash GeckoLib's loader
        String example;      // one offending file, for the log
    }

    private static void fixGeckoLibNamespaces() throws Exception {
        Class<?> cache;
        try {
            cache = Class.forName("software.bernie.geckolib.cache.GeckoLibCache");
        } catch (ClassNotFoundException e) {
            LOG.info("GeckoLib not installed - nothing to fix");
            return;
        }
        java.lang.reflect.Method exclude = cache.getMethod("registerNamespaceExclusion", String.class);

        Map<String, Tally> byNamespace = new TreeMap<>();
        Path gameDir = gameDir();
        long t0 = System.nanoTime();
        scanFolder(gameDir.resolve("mods"), ".jar", byNamespace);
        scanFolder(gameDir.resolve("resourcepacks"), ".zip", byNamespace);

        for (String ns : ALWAYS_SKIP) {
            Tally t = byNamespace.get(ns);
            if (t == null || t.gecko == 0) {
                exclude.invoke(null, ns);
                LOG.info("Told GeckoLib to skip '{}' (not GeckoLib files)", ns);
            }
        }
        for (Map.Entry<String, Tally> e : byNamespace.entrySet()) {
            Tally t = e.getValue();
            if (t.foreign == 0) continue;
            boolean known = false;
            for (String ns : ALWAYS_SKIP) known |= ns.equals(e.getKey());
            if (known) continue;
            if (t.gecko == 0) {
                exclude.invoke(null, e.getKey());
                LOG.info("Told GeckoLib to skip '{}' - it would break GeckoLib loading (e.g. {})", e.getKey(), t.example);
            } else {
                LOG.warn("Namespace '{}' mixes GeckoLib and non-GeckoLib files ({}); can't skip it safely",
                        e.getKey(), t.example);
            }
        }
        LOG.info("GeckoLib folder check done in {} ms", (System.nanoTime() - t0) / 1_000_000);
    }

    private static Path gameDir() {
        String override = System.getProperty("ninefix.gameDir"); // used by the tests
        if (override != null && !override.isEmpty()) return Paths.get(override);
        try {
            Class<?> paths = Class.forName("net.neoforged.fml.loading.FMLPaths");
            Object gameDirConst = paths.getField("GAMEDIR").get(null);
            return (Path) gameDirConst.getClass().getMethod("get").invoke(gameDirConst);
        } catch (Throwable t) {
            return Paths.get("").toAbsolutePath();
        }
    }

    private static void scanFolder(Path dir, String ext, Map<String, Tally> out) {
        if (!Files.isDirectory(dir)) return;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir)) {
            for (Path f : files) {
                String name = f.getFileName().toString().toLowerCase();
                if (!name.endsWith(ext) || !Files.isRegularFile(f)) continue;
                try (ZipFile zip = new ZipFile(f.toFile())) {
                    scanZip(zip, f.getFileName().toString(), out);
                } catch (IOException | RuntimeException ex) {
                    // unreadable archive: ignore, GeckoLib/Minecraft will report it themselves
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static void scanZip(ZipFile zip, String label, Map<String, Tally> out) throws IOException {
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) continue;
            String name = entry.getName();
            if (name.startsWith("META-INF/jarjar/") && name.endsWith(".jar")) {
                if (entry.getSize() >= 0 && entry.getSize() <= MAX_NESTED_JAR_BYTES) {
                    try (InputStream in = zip.getInputStream(entry)) {
                        scanStream(new ZipInputStream(in), label + "!" + name, out);
                    } catch (IOException | RuntimeException ignored) {
                    }
                }
                continue;
            }
            Matcher m = GECKO_PATH.matcher(name);
            if (!m.matches()) continue;
            if (entry.getSize() > MAX_JSON_BYTES) continue;
            try (InputStream in = zip.getInputStream(entry)) {
                classify(m.group(1), m.group(2), name, readAll(in), label, out);
            }
        }
    }

    private static void scanStream(ZipInputStream zin, String label, Map<String, Tally> out) throws IOException {
        ZipEntry entry;
        while ((entry = zin.getNextEntry()) != null) {
            if (entry.isDirectory()) continue;
            Matcher m = GECKO_PATH.matcher(entry.getName());
            if (!m.matches()) continue;
            byte[] data = readAll(zin);
            if (data.length > MAX_JSON_BYTES) continue;
            classify(m.group(1), m.group(2), entry.getName(), data, label, out);
        }
    }

    private static void classify(String ns, String folder, String path, byte[] data, String label,
                                 Map<String, Tally> out) {
        boolean gecko;
        try {
            String text = new String(data, StandardCharsets.UTF_8);
            if (!text.isEmpty() && text.charAt(0) == '﻿') text = text.substring(1);
            JsonElement root = JsonParser.parseString(text);
            if (!root.isJsonObject()) {
                gecko = false;
            } else if (folder.equals("animations")) {
                JsonObject obj = root.getAsJsonObject();
                // GeckoLib: "Geo model file found in animations folder!" / needs an "animations" object
                gecko = !path.endsWith(".geo.json") && obj.has("animations") && obj.get("animations").isJsonObject();
            } else {
                JsonObject obj = root.getAsJsonObject();
                gecko = obj.has("minecraft:geometry") && obj.get("minecraft:geometry").isJsonArray();
            }
        } catch (Throwable parseError) {
            gecko = false;
        }
        Tally t = out.computeIfAbsent(ns, k -> new Tally());
        if (gecko) {
            t.gecko++;
        } else {
            t.foreign++;
            if (t.example == null) t.example = label + " -> " + path;
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        long total = 0;
        while ((n = in.read(buf)) > 0) {
            total += n;
            if (total > MAX_JSON_BYTES) break;
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }
}
