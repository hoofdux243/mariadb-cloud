package com.cloud_computing.mariadb.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME) // annotation chỉ tồn tại khi chương trình đang chạy
public @interface AuditLog {
    String action();
    String description() default "";
    boolean includeParams() default true;
}
