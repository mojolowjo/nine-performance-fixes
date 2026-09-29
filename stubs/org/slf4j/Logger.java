// Compile-time stub (see stubs/README.md). Real class is provided at runtime.
// Signatures match org.slf4j.Logger (SLF4J 2.x, which NeoForge 21.1 ships).
package org.slf4j;
public interface Logger {
    void info(String msg);
    void info(String format, Object arg);
    void info(String format, Object arg1, Object arg2);
    void info(String format, Object... arguments);
    void warn(String msg);
    void warn(String format, Object arg);
    void warn(String format, Object arg1, Object arg2);
    void warn(String msg, Throwable t);
}
