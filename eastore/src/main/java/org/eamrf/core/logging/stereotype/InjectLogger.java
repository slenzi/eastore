package org.eamrf.core.logging.stereotype;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
* Indicates InjectLogger of appropriate type to be supplied at runtime to the annotated field.
*
* The injected logger is an appropriate implementation of org.slf4j.Logger.
*/
@Retention(RUNTIME)
@Target(FIELD)
@Documented
public @interface InjectLogger {

}