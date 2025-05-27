package dev.tiodati.saas.gocommerce.auth.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import dev.tiodati.saas.gocommerce.auth.model.Roles;
import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

/**
 * Annotation for role-based access control.
 * Methods or classes annotated with @RequiresRole will require the user to have at least one of the specified roles.
 */
@Inherited
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface RequiresRole {
    @Nonbinding Roles[] value();
}
