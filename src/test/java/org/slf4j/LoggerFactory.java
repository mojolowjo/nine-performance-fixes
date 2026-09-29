// Test double: prints log lines to stdout instead of needing a real SLF4J binding.
package org.slf4j;

public final class LoggerFactory {
    public static Logger getLogger(String name) {
        return new Logger() {
            private String fmt(String s, Object... args) {
                for (Object a : args) s = s.replaceFirst("\\{\\}", java.util.regex.Matcher.quoteReplacement(String.valueOf(a)));
                return s;
            }
            public void info(String m) { System.out.println("  [INFO] " + m); }
            public void info(String m, Object a) { System.out.println("  [INFO] " + fmt(m, a)); }
            public void info(String m, Object a, Object b) { System.out.println("  [INFO] " + fmt(m, a, b)); }
            public void info(String m, Object... a) { System.out.println("  [INFO] " + fmt(m, a)); }
            public void warn(String m) { System.out.println("  [WARN] " + m); }
            public void warn(String m, Object a) { System.out.println("  [WARN] " + fmt(m, a)); }
            public void warn(String m, Object a, Object b) { System.out.println("  [WARN] " + fmt(m, a, b)); }
            public void warn(String m, Throwable t) { System.out.println("  [WARN] " + m + " " + t); }
        };
    }
}
