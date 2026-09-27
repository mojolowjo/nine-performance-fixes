// Compile-time stub. The real annotation ships with NeoForge (sponge-mixin); only names/types/retention must match.
package org.spongepowered.asm.mixin;
import java.lang.annotation.*;
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Mixin {
    Class<?>[] value() default {};
    String[] targets() default {};
    int priority() default 1000;
    boolean remap() default true;
}
