package org.eamrf.eastore.core.aop.profiler;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Methods annotated with this annotation will will have their
 * runtime logged via our MethodTimerAspect.
 * 
 * @author slenzi
 */
@Documented
@Retention(RUNTIME)
@Target({FIELD, PARAMETER, METHOD})
public @interface MethodTimer {

}
