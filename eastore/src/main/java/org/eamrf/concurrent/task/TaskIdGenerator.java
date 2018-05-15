/**
 * 
 */
package org.eamrf.concurrent.task;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates unique task IDs for all tasks. Task IDs MUST increment sequentially,
 * and MUST be unique even across different task managers.
 * 
 * @author slenzi
 */
public abstract class TaskIdGenerator {

	private static AtomicLong taskId = new AtomicLong(0);
	
	/**
	 * 
	 */
	public TaskIdGenerator() {
		
	}
	
	public static synchronized long getNextTaskId(){
		
		return taskId.incrementAndGet();
		
	}

}
