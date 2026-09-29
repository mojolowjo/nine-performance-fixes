// Compile-time stub (see stubs/README.md). The real class is provided at runtime.
package net.neoforged.neoforge.common;
public class ModConfigSpec implements net.neoforged.fml.config.IConfigSpec {
    public static class Builder {
        public Builder() {}
        public BooleanValue define(String path, boolean defaultValue) { throw new UnsupportedOperationException("stub"); }
        public Builder comment(String comment) { throw new UnsupportedOperationException("stub"); }
        public Builder comment(String... comment) { throw new UnsupportedOperationException("stub"); }
        public Builder translation(String translationKey) { throw new UnsupportedOperationException("stub"); }
        public Builder gameRestart() { throw new UnsupportedOperationException("stub"); }
        public ModConfigSpec build() { throw new UnsupportedOperationException("stub"); }
    }
    public static class ConfigValue<T> implements java.util.function.Supplier<T> {
        public T get() { throw new UnsupportedOperationException("stub"); }
    }
    public static class BooleanValue extends ConfigValue<Boolean> implements java.util.function.BooleanSupplier {
        public boolean getAsBoolean() { throw new UnsupportedOperationException("stub"); }
    }
}
