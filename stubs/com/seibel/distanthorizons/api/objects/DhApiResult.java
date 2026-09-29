// Compile-time stub (see stubs/README.md). The real class is provided at runtime.
package com.seibel.distanthorizons.api.objects;
public class DhApiResult<T> {
    // not initialised inline on purpose: a constant here would be copied into our code by javac
    public final boolean success;
    public final String message;
    private DhApiResult() { success = false; message = null; }
}
