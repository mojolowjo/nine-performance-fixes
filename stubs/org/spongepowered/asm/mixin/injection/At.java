// Compile-time stub (see stubs/README.md). Real class is provided at runtime.
package org.spongepowered.asm.mixin.injection;
import java.lang.annotation.*;
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface At {
    String value();
    String target() default "";
    int ordinal() default -1;
    boolean remap() default true;
}
