package io.windfall.anticheat.core.check;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// RUNTIME retention required — Check constructor reads this via getAnnotation() at startup
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CheckData {
    String name();
    String stableKey();
    double decay() default 0.02;
    int setbackVl() default 20;
    int minVersion() default 4;
    int maxVersion() default 99999;

    CompatFlag[] compat() default {};
    double relaxMultiplier() default 1.0;
    boolean disableOnFolia() default false;
    boolean disableOnPurpur() default false;
}
