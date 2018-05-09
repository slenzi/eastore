/**
 * 
 */
package org.eamrf.concurrent.task;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/**
 * Factory for task managers
 * 
 * @author slenzi
 */
@Service
public class TaskManagerProvider {

    @InjectLogger
    private Logger logger;
	
	public TaskManagerProvider() {
	
	}
	
	/**
	 * Create a new singleton instance of QueuedTaskManager 
	 * 
	 * @return
	 */
	@Bean
	public QueuedTaskManager createQueuedTaskManager(){
		
		QueuedTaskManager manager = new QueuedTaskManager();
		
		// for some reason our BeanPostProcessor is not running, so we manually wire the logger dependency.
		// bad hack... figure this out later.
		manager.setLogger(LoggerFactory.getLogger(QueuedTaskManager.class));
		
		return manager;
		
	}
	
	/**
	 * Create a new singleton instance of QueuedTaskManager 
	 * 
	 * @param consumeFrequency - how long to wait in milliseconds between consuming each task in the queue
	 * @return
	 */
	@Bean
	public QueuedTaskManager createQueuedTaskManager(long consumeFrequency){
		
		QueuedTaskManager manager = new QueuedTaskManager(consumeFrequency);
		
		// for some reason our BeanPostProcessor is not running, so we manually wire the logger dependency.
		// bad hack... figure this out later.
		manager.setLogger(LoggerFactory.getLogger(QueuedTaskManager.class));
		
		return manager;
		
	}	

}
