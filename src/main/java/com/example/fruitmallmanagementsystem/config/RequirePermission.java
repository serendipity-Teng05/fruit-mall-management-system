package com.example.fruitmallmanagementsystem.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the backend permission or role required by an API.
 * Frontend menu visibility is only presentation; this annotation is the real
 * authorization boundary.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String[] value() default {};

    String[] roles() default {};
}
