package io.github.mojolowjo.ninefix;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import software.bernie.geckolib.cache.GeckoLibCache;

/** Builds a fake game folder full of tricky mods/resource packs and checks what gets skipped. */
public final class ScanTest {
    private static final String GECKO_ANIM = "{\"format_version\":\"1.8.0\",\"animations\":{\"walk\":{\"loop\":true}}}";
    private static final String GECKO_GEO = "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"bones\":[]}]}";
    private static final String BLOCK_ANIM = "{\"duration\":0.4167,\"pivot_point\":[0.5,0.5,0.5],\"keyframes\":[]}";

    public static void main(String[] args) throws Exception {
        Path game = Files.createTempDirectory("ninefix-test");
        Path mods = Files.createDirectories(game.resolve("mods"));
        Path packs = Files.createDirectories(game.resolve("resourcepacks"));

        // 1. Mining & Placing Animations style: built-in pack with its own block-animation format.
        zip(mods.resolve("mining_and_placing_animations.jar"), Map.of(
                "resourcepacks/default_animations/assets/mining_and_placing_animations/animations/default_mining_animation.json", BLOCK_ANIM));
        // 2. A normal GeckoLib mod: must be left alone.
        zip(mods.resolve("gecko-mob-mod.jar"), Map.of(
                "assets/geckomod/animations/mob.animation.json", GECKO_ANIM,
                "assets/geckomod/geo/mob.geo.json", GECKO_GEO));
        // 3. A bad file hidden inside a bundled (jar-in-jar) library: must be found.
        byte[] inner = zipBytes(Map.of("assets/sneaky/animations/emote.json", "{\"name\":\"wave\",\"frames\":[1,2]}"));
        zip(mods.resolve("outer-mod.jar"), Map.of("META-INF/jarjar/inner-lib.jar", inner));
        // 4. A namespace with both valid and broken files: can't be skipped safely, only warned about.
        zip(mods.resolve("mixed.jar"), Map.of(
                "assets/mixedmod/animations/good.animation.json", GECKO_ANIM,
                "assets/mixedmod/geo/broken.json", "{ this is not json"));
        // 5. A disabled mod: ignored completely.
        zip(mods.resolve("old-mod.jar.disabled"), Map.of("assets/disabledmod/animations/x.json", "{}"));
        // 6. A resource pack zip with a foreign animations file: must be found.
        zip(packs.resolve("some-pack.zip"), Map.of("assets/rpbad/animations/foo.json", "{\"foo\":1}"));
        // 7. Optional: a real mod jar that must be left alone.
        String arphex = System.getenv("ARPHEX_JAR");
        if (arphex != null && !arphex.isEmpty()) Files.copy(Paths.get(arphex), mods.resolve("arphex.jar"));

        System.setProperty("ninefix.gameDir", game.toString());
        new NineFix();

        Set<String> expected = new TreeSet<>(Set.of("mining_and_placing_animations", "sneaky", "rpbad"));
        System.out.println("Skipped namespaces: " + GeckoLibCache.EXCLUDED);
        if (!GeckoLibCache.EXCLUDED.equals(expected)) {
            System.out.println("FAIL: expected " + expected);
            System.exit(1);
        }
        System.out.println("PASS");
    }

    private static void zip(Path file, Map<String, ?> entries) throws IOException {
        try (OutputStream out = Files.newOutputStream(file)) {
            out.write(zipBytes(entries));
        }
    }

    private static byte[] zipBytes(Map<String, ?> entries) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(bos)) {
            for (Map.Entry<String, ?> e : entries.entrySet()) {
                z.putNextEntry(new ZipEntry(e.getKey()));
                Object v = e.getValue();
                z.write(v instanceof byte[] b ? b : v.toString().getBytes(StandardCharsets.UTF_8));
                z.closeEntry();
            }
        }
        return bos.toByteArray();
    }
}
