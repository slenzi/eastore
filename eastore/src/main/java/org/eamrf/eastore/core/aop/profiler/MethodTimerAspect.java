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
 * @author slenzi
 */
@Component
@Aspect
public class MethodTimerAspect {
	
	private Logger logger = LoggerFactory.getLogger(MethodTimerAspect.class);

	private CodeTimer timer = new CodeTimer();
	
	/**
	 * This pointcut is a catch all pointcut with the scope of execution.
	 * 
	 * Basically all method calls
	 */
	@Pointcut("execution(* *(..))")
    public void allMethodsPointcut(){}	
	
    @Around("allMethodsPointcut() && @annotation(MethodTimer)")
    public Object doBasicProfiling(ProceedingJoinPoint pjp) throws Throwable {
    	
        String packageName = pjp.getSignature().getDeclaringTypeName();
        String methodName = pjp.getSignature().getName();    	
    	
        // start stopwatch
    	timer.start();
        
    	Object retVal = pjp.proceed();
        
    	// stop stopwatch
    	timer.stop();
    	
    	logger.info(">> Profiler: " + packageName + " " + methodName + " completed in " + timer.getElapsedTime());
        
    	return retVal;
        
    }

}
