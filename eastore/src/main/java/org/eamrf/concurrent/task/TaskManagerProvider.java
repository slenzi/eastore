/**
 * 
 */
package org.eamrf.concurrent.task;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
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
	@Scope(value = "prototype")
	public QueuedTaskManager createQueuedTaskManager(){
		logger.info("Creating new " + QueuedTaskManager.class.getName());
		return new QueuedTaskManager();
	}

}
