package com.sparkmvc.ann;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by Nurmuhammad on 29-Dec-15.
 */

@Retention(RUNTIME)
@Target({TYPE, METHOD})
public @interface Page {
    String layout() default "";
}