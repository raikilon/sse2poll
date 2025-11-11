package ch.sse2poll.core.framework.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Minimal annotation for polled GET endpoints.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PolledGet {
    int ttl() default 300;        // READY retention (seconds)
    int pendingTtl() default 30;  // PENDING retention (seconds)
}

