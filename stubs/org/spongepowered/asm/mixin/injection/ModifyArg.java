// Compile-time stub (see stubs/README.md). Real class is provided at runtime.
package org.spongepowered.asm.mixin.injection;
import java.lang.annotation.*;
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ModifyArg {
    String[] method() default {};
    At[] at();
    int index() default -1;
    boolean remap() default true;
    int require() default -1;
}
