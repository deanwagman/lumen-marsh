package com.deanwagman.lumenmarsh.venueops.security;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({
        SecurityConfiguration.class,
        ActorResolver.class,
        CommandAuthorization.class,
        SecurityExceptionHandler.class
})
public @interface ImportVenueOpsSecurity {
}
