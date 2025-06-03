package imd.ufrn.br.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a remote object that can be managed and invoked
 * by the middleware platform.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RemoteObject {

    /**
     * If left empty, the middleware might use the class's simple name by default.
     * This name is typically used by the {@code LookupService} to find and register
     * instances of this remote object.
     *
     * @return The name of the remote object.
     */
    String name() default "";
}