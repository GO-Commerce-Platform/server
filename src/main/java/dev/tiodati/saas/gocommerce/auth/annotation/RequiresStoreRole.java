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
 * Annotation for store-specific role-based access control.
 * Methods or classes annotated with @RequiresStoreRole will require the user to have at least one
 * of the specified roles AND be associated with the store context of the current request.
 */
@Inherited
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface RequiresStoreRole {
    /**
     * The roles required for access.
     * If multiple roles are specified, the user must have at least one of them.
     */
    @Nonbinding Roles[] value();

    /**
     * Indicates whether the user must have access to the store associated with the current request.
     * If true, the user must be associated with the store context of the current request.
     * If false, the user can have any store context.
     */
    @Nonbinding boolean requireStoreAccess() default true;
}
