// Compile-time stub (see stubs/README.md). The real class is provided at runtime.
package com.seibel.distanthorizons.api.interfaces.config;
public interface IDhApiConfigValue<T> {
    T getValue();
    T getTrueValue();
    boolean setValue(T value, String apiUserDisplayName);
}
