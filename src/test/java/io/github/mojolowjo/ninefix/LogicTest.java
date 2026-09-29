package io.github.mojolowjo.ninefix;

import de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen;
import io.github.mojolowjo.ninefix.mixin.NineFixMixinPlugin;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.arphex.block.entity.MobTrophyBlockEntity;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Logic tests with small test doubles for the Minecraft pieces.
 * Usage: LogicTest <build/classes dir>              - default settings (no settings file yet)
 *        LogicTest <build/classes dir> switched-off - run with -Dninefix.configDir pointing at a file
 *                                                     that switches some fixes off
 */
public final class LogicTest {
    static int failures;

    static void check(boolean ok, String what) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + what);
        if (!ok) failures++;
    }

    static final String MIXIN_PKG = "io.github.mojolowjo.ninefix.mixin.";

    public static void main(String[] args) throws Exception {
        Path classes = Paths.get(args[0]);
        if (args.length > 1 && args[1].equals("switched-off")) {
            switchedOff();
        } else {
            defaults(classes);
        }
        System.out.println(failures == 0 ? "\nALL LOGIC TESTS PASSED" : "\n" + failures + " FAILURE(S)");
        System.exit(failures == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------------------------------------
    static void switchedOff() {
        check(Settings.source().endsWith(Settings.FILE_NAME), "settings: read from the file (" + Settings.source() + ")");
        check(!Settings.on("obsessedNetworkFix") && !Settings.on("fancymenuScreenCache"), "settings: switched-off fixes are off");
        check(Settings.on("arphexSphereCache") && Settings.on("kubejsWebServerOff"), "settings: the others stay on");
        NineFixMixinPlugin plugin = new NineFixMixinPlugin();
        check(!plugin.shouldApplyMixin("net.theobsessed.network.TheObsessedModVariables$PlayerVariables", MIXIN_PKG + "TheObsessedSyncMixin")
                        && !plugin.shouldApplyMixin("net.minecraft.server.MinecraftServer", MIXIN_PKG + "ServerTickFlushMixin"),
                "plugin: both mixins of a switched-off fix are not applied");
        check(!plugin.shouldApplyMixin("x", MIXIN_PKG + "FancyMenuScreenIdMixin")
                        && !plugin.shouldApplyMixin("x", MIXIN_PKG + "FancyMenuBlacklistMixin")
                        && !plugin.shouldApplyMixin("x", MIXIN_PKG + "FancyMenuRegistryMixin"),
                "plugin: all three FancyMenu mixins follow their switch");
        check(plugin.shouldApplyMixin("x", MIXIN_PKG + "ArphexSphereMixin"), "plugin: a switched-on fix is applied");
    }

    // ------------------------------------------------------------------------------------------------
    static void defaults(Path classes) throws Exception {
        // --- settings: defaults when the file doesn't exist yet ---
        boolean allOn = true;
        for (Settings.Fix f : Settings.FIXES) allOn &= Settings.on(f.key()) == f.defaultOn();
        check(allOn && Settings.source().startsWith("defaults"), "settings: defaults used before the file exists");
        check(!Settings.on("noSuchFix"), "settings: unknown keys count as off");

        // --- settings: parsing a file the way NeoForge/NightConfig writes it (and hand edits) ---
        Map<String, Boolean> p = Settings.parse(List.of(
                "#GeckoLib animations: blah. Restart the game after changing this.",
                "geckolibAnimationFix = false",
                "\tarphexSphereCache = false   ",
                "\"kubejsWebServerOff\" = false",
                "obsessedNetworkFix = maybe",
                "unknownKey = false",
                "[some.section]",
                "fancymenuScreenCache=false # comment",
                "skillExpConfigCache = FALSE"));
        check(!p.get("geckolibAnimationFix") && !p.get("arphexSphereCache") && !p.get("kubejsWebServerOff") && !p.get("fancymenuScreenCache"),
                "settings: 'key = false' lines are read (spaces, tabs, quotes, sections, trailing comments)");
        check(p.get("obsessedNetworkFix") && p.get("skillExpConfigCache"), "settings: invalid values keep the default");
        check(!p.containsKey("unknownKey") && p.size() == Settings.FIXES.size(), "settings: unknown keys are ignored");

        // --- every mixin belongs to exactly one switch, and every switch is labelled ---
        String json = Files.readString(classes.resolve("ninefix.mixins.json"));
        Set<String> listed = new HashSet<>();
        for (String section : new String[] {"mixins", "client"}) {
            Matcher m = Pattern.compile("\"" + section + "\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(json);
            if (m.find()) for (String s : m.group(1).split(",")) if (!s.isBlank()) listed.add(s.trim().replace("\"", ""));
        }
        boolean mapped = true, exist = true;
        for (String mixin : listed) {
            int owners = 0;
            for (Settings.Fix f : Settings.FIXES) if (f.mixins().contains(mixin)) owners++;
            mapped &= owners == 1;
            exist &= Files.isRegularFile(classes.resolve((MIXIN_PKG + mixin).replace('.', '/') + ".class"));
        }
        check(mapped, "mixins: each of the " + listed.size() + " mixins belongs to exactly one fix");
        check(exist, "mixins: every listed mixin class is in the jar");
        boolean inJson = true;
        Set<String> keys = new HashSet<>();
        for (Settings.Fix f : Settings.FIXES) {
            inJson &= listed.containsAll(f.mixins());
            keys.add(f.key());
        }
        check(inJson, "mixins: every fix's mixins are listed in ninefix.mixins.json");
        check(keys.size() == Settings.FIXES.size(), "settings: keys are unique");
        check(json.contains("\"plugin\": \"io.github.mojolowjo.ninefix.mixin.NineFixMixinPlugin\""), "mixins: the switch plugin is registered");
        String lang = Files.readString(classes.resolve("assets/ninefix/lang/en_us.json"), StandardCharsets.UTF_8);
        boolean labelled = true;
        for (Settings.Fix f : Settings.FIXES) {
            labelled &= lang.contains("\"ninefix.configuration." + f.key() + "\": \"" + f.label() + "\"")
                    && lang.contains("\"ninefix.configuration." + f.key() + ".tooltip\"");
        }
        check(labelled, "settings screen: every switch has a label and a tooltip");
        NineFixMixinPlugin plugin = new NineFixMixinPlugin();
        boolean applied = true;
        for (String mixin : listed) applied &= plugin.shouldApplyMixin("x", MIXIN_PKG + mixin);
        check(applied, "plugin: with default settings every mixin is applied");

        fancyMenu();
        trophies();
        obsessed();

        check(DistantHorizonsFix.cap(6) == 3 && DistantHorizonsFix.cap(12) == 6 && DistantHorizonsFix.cap(7) == 4
                        && DistantHorizonsFix.cap(1) == 1 && DistantHorizonsFix.cap(16) == 8,
                "distant horizons: cap is half the CPU threads, rounded up, at least 1");
    }

    // ------------------------------------------------------------------------------------------------
    static class TitleLike extends Screen {}

    static class OtherScreen extends Screen {}

    static void fancyMenu() {
        Screen a = new TitleLike(), a2 = new TitleLike(), b = new OtherScreen(), custom = new CustomGuiBaseScreen();
        check(FancyMenuCache.cachedScreenId(a) == null, "fancymenu: first lookup is a miss");
        FancyMenuCache.storeScreenId(a, "title_screen");
        check("title_screen".equals(FancyMenuCache.cachedScreenId(a2)), "fancymenu: same screen class -> cached id");
        check(FancyMenuCache.cachedScreenId(b) == null, "fancymenu: different screen class -> miss");
        FancyMenuCache.invalidate();
        check(FancyMenuCache.cachedScreenId(a) == null, "fancymenu: registry change invalidates the memo");
        FancyMenuCache.storeScreenId(custom, "my_custom_gui");
        check(FancyMenuCache.cachedScreenId(custom) == null, "fancymenu: custom GUI screens are never cached");
        FancyMenuCache.storeScreenId(a, "title_screen");
        int misses = 0;
        for (int i = 0; i < 4096; i++) if (FancyMenuCache.cachedScreenId(a) == null) misses++;
        check(misses >= 3 && misses <= 5, "fancymenu: recomputes every 1024th call as a safety net (misses=" + misses + ")");
        String cls = "net.minecraft.client.gui.screens.TitleScreen";
        FancyMenuCache.storeBlacklist(cls, false);
        check(Boolean.FALSE.equals(FancyMenuCache.cachedBlacklist(new String(cls))), "fancymenu: blacklist memo hit (equal string)");
        check(FancyMenuCache.cachedBlacklist("com.simibubi.create.Foo") == null, "fancymenu: blacklist memo miss for other key");
        FancyMenuCache.invalidate();
        check(FancyMenuCache.cachedBlacklist(cls) == null, "fancymenu: new blacklist rule invalidates the memo");
    }

    static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    static void trophies() throws InterruptedException {
        ClientLevel level = new ClientLevel();
        LevelChunk plain = new LevelChunk();
        plain.blockEntities.put("chest", new Object());
        LevelChunk trophy = new LevelChunk();
        trophy.blockEntities.put("t", new MobTrophyBlockEntity());
        level.chunks.put(key(0, 0), plain);
        level.chunks.put(key(1, -1), trophy);
        check(TrophyScan.chunkIfItMayHaveTrophies(level, 0, 0) == null, "arphex: chunk without trophy is skipped");
        check(TrophyScan.chunkIfItMayHaveTrophies(level, 1, -1) == trophy, "arphex: chunk with trophy is handed over");
        int before = level.getChunkCalls;
        for (int i = 0; i < 100; i++) TrophyScan.chunkIfItMayHaveTrophies(level, 0, 0);
        check(level.getChunkCalls == before, "arphex: no-trophy chunk not even fetched again within 1 s");
        boolean always = true;
        for (int i = 0; i < 100; i++) always &= TrophyScan.chunkIfItMayHaveTrophies(level, 1, -1) == trophy;
        check(always, "arphex: trophy chunk returned on every frame");
        plain.blockEntities.put("new", new MobTrophyBlockEntity());
        Thread.sleep(1300);
        check(TrophyScan.chunkIfItMayHaveTrophies(level, 0, 0) == plain, "arphex: newly placed trophy picked up after ~1 s");
        ClientLevel other = new ClientLevel();
        LevelChunk otherTrophy = new LevelChunk();
        otherTrophy.blockEntities.put("t", new MobTrophyBlockEntity());
        other.chunks.put(key(0, 0), otherTrophy);
        check(TrophyScan.chunkIfItMayHaveTrophies(other, 0, 0) == otherTrophy, "arphex: switching world/dimension resets the cache");
        check(TrophyScan.chunkIfItMayHaveTrophies(null, 0, 0) == null, "arphex: no level -> nothing");
        TrophyScan.LongMap lm = new TrophyScan.LongMap();
        Map<Long, Long> ref = new HashMap<>();
        Random r = new Random(1);
        boolean same = true;
        for (int i = 0; i < 200_000; i++) {
            long k = r.nextInt(5000) - 2500L, v = r.nextLong();
            if (r.nextBoolean()) {
                lm.put(k, v);
                ref.put(k, v);
            }
            long got = lm.get(k);
            Long want = ref.get(k);
            if (want == null ? got != TrophyScan.LongMap.MISSING : got != want) {
                same = false;
                break;
            }
        }
        check(same && lm.size() == ref.size(), "arphex: internal long map matches java.util.HashMap on 200k random ops");
    }

    // ------------------------------------------------------------------------------------------------
    /** Stands in for The Obsessed's PlayerVariables with the mixin applied. */
    static final class FakeVars implements ObsessedSync.Vars {
        String content = "mood=0";
        final List<String> sent;
        boolean reentryPassedThrough = true;
        boolean fail;

        FakeVars(List<String> sent) {
            this.sent = sent;
        }

        /** What syncPlayerVariables does once the mixin is in: ask first, then send unless told to wait. */
        void sync(ServerPlayer player) {
            if (!ObsessedSync.intercept(this, player)) sent.add(content);
        }

        @Override
        public void ninefix$sendNow(ServerPlayer player) {
            if (fail) throw new IllegalStateException("boom");
            // the real method runs the injected HEAD check again - it must let our own send through
            boolean held = ObsessedSync.intercept(this, player);
            reentryPassedThrough &= !held;
            if (!held) sent.add(content);
        }
    }

    static void obsessed() {
        List<String> toAlice = new ArrayList<>(), toBob = new ArrayList<>();
        ServerPlayer alice = new ServerPlayer(), bob = new ServerPlayer();
        FakeVars va = new FakeVars(toAlice), vb = new FakeVars(toBob);

        // Before the end-of-tick hook has ever run: nothing is held back.
        va.sync(alice);
        va.sync(alice);
        check(toAlice.size() == 2, "obsessed (no tick hook yet): every sync is sent immediately, as before");

        // The end-of-tick hook runs: from now on, syncs are collected and sent at the end of the tick.
        ObsessedSync.flush();
        toAlice.clear();
        for (int i = 0; i < 50; i++) {
            va.content = "mood=" + i;
            va.sync(alice);
        }
        check(toAlice.isEmpty(), "obsessed: nothing is sent during the tick");
        ObsessedSync.flush();
        check(toAlice.equals(List.of("mood=49")), "obsessed: 50 syncs in one tick -> 1 packet at the end, with the latest values");
        check(va.reentryPassedThrough, "obsessed: our own send isn't held back again");
        ObsessedSync.flush();
        check(toAlice.size() == 1, "obsessed: no request this tick -> nothing sent");

        va.sync(alice);
        ObsessedSync.flush();
        check(toAlice.size() == 2 && toAlice.get(1).equals("mood=49"),
                "obsessed: unchanged data is still sent in every tick that asks (e.g. after a dimension change)");

        vb.content = "bob";
        vb.sync(bob);
        va.sync(alice);
        ObsessedSync.flush();
        check(toBob.equals(List.of("bob")) && toAlice.size() == 3, "obsessed: players are handled independently");

        // Respawn: a new player object (equal id) replaces the old one within the same tick.
        List<String> toClient = new ArrayList<>();
        ServerPlayer before = new ServerPlayer(), after = new ServerPlayer();
        FakeVars oldVars = new FakeVars(toClient), newVars = new FakeVars(toClient);
        oldVars.content = "old";
        newVars.content = "new";
        oldVars.sync(before);
        newVars.sync(after);
        ObsessedSync.flush();
        check(toClient.equals(List.of("old", "new")), "obsessed: respawn in the same tick -> both sent, newest last");

        va.fail = true;
        va.sync(alice);
        vb.sync(bob);
        ObsessedSync.flush();
        check(toBob.size() == 2, "obsessed: one failing send doesn't stop the others");
    }
}
