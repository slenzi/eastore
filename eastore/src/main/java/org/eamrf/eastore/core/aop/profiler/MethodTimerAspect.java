package org.eamrf.eastore.core.aop.profiler;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.eamrf.core.util.CodeTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * An aspect for profiling method run times.
 * 
 * Simply annotate a method with @MethodTimer and the method runtime will be logged.
 * 
 * Of course, this only works on spring proxied method calls (i.e., no private methods)
 * 
 * @author slenzi
 */
@Component
@Aspect
public class MethodTimerAspect {
	
	private Logger logger = LoggerFactory.getLogger(MethodTimerAspect.class);

	/**
	 * This pointcut is a catch all pointcut with the scope of execution.
	 * 
	 * Basically all method calls
	 */
	@Pointcut("execution(* *(..))")
    public void allMethodsPointcut(){}	
	
    @Around("allMethodsPointcut() && @annotation(MethodTimer)")
    public Object logMethodExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
    	
        String packageName = pjp.getSignature().getDeclaringTypeName();
        String methodName = pjp.getSignature().getName();
        
        CodeTimer timer = new CodeTimer();
    	
        // start stopwatch
    	timer.start();
        
    	Object retVal = pjp.proceed();
        
    	// stop stopwatch
    	timer.stop();
    	
    	logger.info(">> Profiler: " + packageName + " " + methodName + " completed in " + timer.getElapsedTime());
        
    	return retVal;
        
    }

}
