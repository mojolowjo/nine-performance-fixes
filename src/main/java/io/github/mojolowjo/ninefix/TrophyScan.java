package io.github.mojolowjo.ninefix;

import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * ArPhEx's WorldRenderTestProcedure walks every block entity in every loaded chunk, every frame,
 * looking for its mob-trophy blocks. Here each chunk is only re-checked about once per second
 * (staggered); chunks without a trophy are skipped in between (ArPhEx already skips a chunk when
 * getChunk returns null). Chunks with a trophy are returned every frame, so trophies render as
 * before; a newly placed trophy shows up within a second.
 */
public final class TrophyScan {
    private static final long RECHECK_NANOS = 1_000_000_000L;
    private static final int MAX_ENTRIES = 1 << 15;

    private static java.lang.ref.WeakReference<Object> currentLevel = new java.lang.ref.WeakReference<>(null);
    private static final LongMap CACHE = new LongMap();
    private static Class<?> trophyClass;
    private static boolean trophyResolved;

    public static LevelChunk chunkIfItMayHaveTrophies(ClientLevel level, int chunkX, int chunkZ) {
        if (level == null) return null;
        Class<?> trophy = trophyClass();
        if (trophy == null) return level.getChunk(chunkX, chunkZ); // can't tell -> original behaviour

        if (level != currentLevel.get() || CACHE.size() > MAX_ENTRIES) {
            CACHE.clear();
            currentLevel = new java.lang.ref.WeakReference<>(level);
        }
        long key = ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
        long now = System.nanoTime();
        long entry = CACHE.get(key); // (nextCheckNanos << 1) | hasTrophy, or Long.MIN_VALUE
        if (entry != LongMap.MISSING && now - (entry >> 1) < 0) {
            return (entry & 1L) != 0 ? level.getChunk(chunkX, chunkZ) : null;
        }
        LevelChunk chunk = level.getChunk(chunkX, chunkZ);
        boolean has = chunk != null && containsTrophy(chunk, trophy);
        long stagger = ((key * 0x9E3779B97F4A7C15L) >>> 33) % 250_000_000L; // spread re-checks over 250 ms
        CACHE.put(key, ((now + RECHECK_NANOS + stagger) << 1) | (has ? 1L : 0L));
        return has ? chunk : null;
    }

    private static boolean containsTrophy(LevelChunk chunk, Class<?> trophy) {
        Map<?, ?> blockEntities = chunk.getBlockEntities();
        if (blockEntities == null || blockEntities.isEmpty()) return false;
        for (Object be : blockEntities.values()) {
            if (trophy.isInstance(be)) return true;
        }
        return false;
    }

    private static Class<?> trophyClass() {
        if (!trophyResolved) {
            try {
                trophyClass = Class.forName("net.arphex.block.entity.MobTrophyBlockEntity", false, TrophyScan.class.getClassLoader());
            } catch (Throwable t) {
                trophyClass = null;
            }
            trophyResolved = true;
        }
        return trophyClass;
    }

    /** Tiny open-addressing long->long map (no boxing on the per-frame path). */
    static final class LongMap {
        static final long MISSING = Long.MIN_VALUE;
        private static final long EMPTY_KEY = Long.MIN_VALUE + 1;
        private long[] keys = new long[1024];
        private long[] vals = new long[1024];
        private int size;
        { java.util.Arrays.fill(keys, EMPTY_KEY); }

        int size() { return size; }

        void clear() {
            java.util.Arrays.fill(keys, EMPTY_KEY);
            size = 0;
        }

        long get(long k) {
            if (k == EMPTY_KEY) k = EMPTY_KEY + 1;
            int mask = keys.length - 1;
            for (int i = mix(k) & mask; ; i = (i + 1) & mask) {
                long cur = keys[i];
                if (cur == EMPTY_KEY) return MISSING;
                if (cur == k) return vals[i];
            }
        }

        void put(long k, long v) {
            if (k == EMPTY_KEY) k = EMPTY_KEY + 1;
            if ((size + 1) * 2 > keys.length) grow();
            int mask = keys.length - 1;
            for (int i = mix(k) & mask; ; i = (i + 1) & mask) {
                long cur = keys[i];
                if (cur == EMPTY_KEY) { keys[i] = k; vals[i] = v; size++; return; }
                if (cur == k) { vals[i] = v; return; }
            }
        }

        private void grow() {
            long[] ok = keys, ov = vals;
            keys = new long[ok.length * 2];
            vals = new long[ok.length * 2];
            java.util.Arrays.fill(keys, EMPTY_KEY);
            size = 0;
            for (int i = 0; i < ok.length; i++) if (ok[i] != EMPTY_KEY) put(ok[i], ov[i]);
        }

        private static int mix(long k) {
            long h = k * 0x9E3779B97F4A7C15L;
            return (int) (h ^ (h >>> 32));
        }
    }

    private TrophyScan() {}
}
