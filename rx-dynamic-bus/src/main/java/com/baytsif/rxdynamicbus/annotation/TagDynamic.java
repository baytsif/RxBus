package com.baytsif.rxdynamicbus.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TagDynamic {
    static final String DEFAULT = "rxbus_default_tag_dynamic";

    String value() default DEFAULT;
}
