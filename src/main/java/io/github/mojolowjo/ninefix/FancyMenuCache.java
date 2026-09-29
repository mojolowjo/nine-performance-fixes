package io.github.mojolowjo.ninefix;

/**
 * One-entry memos for two FancyMenu lookups that are called thousands of times per frame,
 * almost always with the same input:
 *   ScreenIdentifierHandler.getIdentifierOfScreen(Screen)  - depends only on the screen's class
 *     and FancyMenu's universal-identifier registry (except CustomGuiBaseScreen, never cached);
 *   ScreenCustomization.isScreenBlacklisted(String)        - depends only on the string and the
 *     blacklist rules.
 * Both registries are private and only change through register(...) / addScreenBlacklistRule(...),
 * which bump {@link #generation}. As an extra safety net every 1024th call recomputes anyway.
 */
public final class FancyMenuCache {
    private static volatile int generation;

    private record ScreenId(Class<?> screenClass, String id, int gen) {}
    private record Blacklist(String key, boolean value, int gen) {}

    private static volatile ScreenId lastScreenId;
    private static volatile Blacklist lastBlacklist;
    private static int screenIdCalls, blacklistCalls;

    private static volatile Class<?> customGuiClass;
    private static volatile boolean customGuiResolved;

    public static void invalidate() {
        generation++;
    }

    // ---- getIdentifierOfScreen -------------------------------------------------------------
    public static String cachedScreenId(Object screen) {
        if (screen == null) return null;
        ScreenId s = lastScreenId;
        if (s == null || s.screenClass() != screen.getClass() || s.gen() != generation) return null;
        if ((++screenIdCalls & 1023) == 0) return null;
        return s.id();
    }

    public static void storeScreenId(Object screen, String id) {
        if (screen == null || id == null || isCustomGui(screen)) return;
        lastScreenId = new ScreenId(screen.getClass(), id, generation);
    }

    // ---- isScreenBlacklisted(String) -------------------------------------------------------
    /** @return cached result, or null if it must be computed. */
    public static Boolean cachedBlacklist(String key) {
        if (key == null) return null;
        Blacklist b = lastBlacklist;
        if (b == null || b.gen() != generation || (b.key() != key && !b.key().equals(key))) return null;
        if ((++blacklistCalls & 1023) == 0) return null;
        return b.value();
    }

    public static void storeBlacklist(String key, boolean value) {
        if (key == null) return;
        lastBlacklist = new Blacklist(key, value, generation);
    }

    private static boolean isCustomGui(Object screen) {
        if (!customGuiResolved) {
            try {
                customGuiClass = Class.forName("de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen",
                        false, FancyMenuCache.class.getClassLoader());
            } catch (Throwable t) {
                customGuiClass = null;
            }
            customGuiResolved = true;
        }
        Class<?> c = customGuiClass;
        // If we can't tell, be safe and never cache.
        return c == null || c.isInstance(screen);
    }

    private FancyMenuCache() {}
}
