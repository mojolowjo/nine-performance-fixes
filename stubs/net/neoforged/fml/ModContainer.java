// Compile-time stub (see stubs/README.md). The real class is provided at runtime.
package net.neoforged.fml;
public abstract class ModContainer {
    public final String getModId() { throw new UnsupportedOperationException("stub"); }
    public <T extends IExtensionPoint> void registerExtensionPoint(Class<T> point, T extension) { throw new UnsupportedOperationException("stub"); }
    public <T extends IExtensionPoint> void registerExtensionPoint(Class<T> point, java.util.function.Supplier<T> extension) { throw new UnsupportedOperationException("stub"); }
    public void registerConfig(net.neoforged.fml.config.ModConfig.Type type, net.neoforged.fml.config.IConfigSpec configSpec) { throw new UnsupportedOperationException("stub"); }
}
