package org.eamrf.concurrent.task;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.DateUtil;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Queues tasks in a priority blocking queue for execution.
 * 
 * prototype scope - new instance each time
 * 
 * @author sal
 */
@Component
@Scope(value = "prototype") // new instance every time object is wired
public class QueuedTaskManager implements TaskManager {

	@InjectLogger
	private Logger logger;
	
	private BlockingQueue<QueuedTask<?>> queue = new PriorityBlockingQueue<QueuedTask<?>>();

	private boolean isRunning = false;
	
	private ExecutorService executorService = null;
	
	private long taskId = 0L;
	
	private String managerName = QueuedTaskManager.class.getName();
	
	// amount of time to wait in milliseconds before consuming next item in queue
	private long consumeFrequency = 0L;
	
	/**
	 * Create a queued task manager that will consume tasks one a time, where the
	 * next task will be consume and process immediately after the preceding task
	 * is completed (zero wait time.)
	 */
	public QueuedTaskManager() {
		
	}
	
	/**
	 * Create a queued task manager that will consume tasks one a time, where the next
	 * task will be consumed and processed at a predefined time after the preceding task
	 * is completed.  
	 * 
	 * @param consumeFrequency - how long to wait in milliseconds before consuming the next task
	 */
	public QueuedTaskManager(long consumeFrequency) {
		this.consumeFrequency = consumeFrequency;
	}
	
	/**
	 * Set a logger
	 * 
	 * @param logger
	 */
	public void setLogger(Logger logger){
		this.logger = logger;
	}
	
	/**
	 * @return the managerName
	 */
	public String getManagerName() {
		return managerName;
	}

	/**
	 * @param managerName the managerName to set
	 */
	public void setManagerName(String managerName) {
		this.managerName = managerName;
	}

	/**
	 * Starts the task manager by adding it to the executor service.
	 */
	@Override
	public void startTaskManager(ExecutorService executorService) {
		
		this.executorService = executorService;
		
		logger.debug("Submitting queued task manager '" + managerName + "' to executor service.");
		
		//this.executorService.submit(this);
		this.executorService.execute(this);

	}

	/**
	 * get the ExecutorService for this task manager
	 * 
	 * @return
	 */
	public ExecutorService getExecutorService(){
		return executorService;
	}
	
	/**
	 * Stops the task manager by shutting down the executor service. 
	 */
	@Override
	public void stopTaskManager() {
		
		logger.debug("Stop '" + managerName + "' task manager called");

		queue.clear();

		logger.debug("Shuttin down executor service for '" + managerName + "' task manager...");

		executorService.shutdown(); // Disable new tasks from being submitted to executorService

		logger.debug("Call to executor shutdown complete...");

		try {

			logger.debug("Awaiting termination for 10 seconds...");

			if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {

				logger.debug("Calling shutdownNow() on executor service...");

				executorService.shutdownNow();

				logger.debug("Awaiting termination for additional 10 seconds...");

				if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
					logger.error("Executor service did not terminate");
				}
			}

		} catch (InterruptedException e) {
			// (Re-)Cancel if current thread also interrupted
			executorService.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();   
		}

		logger.debug("Stop '" + managerName + "' task manager call complete");
		
	}
	
	/**
	 * Get the number of tasks currently in the queue
	 */
	@Override
	public int taskCount() {
		
		return queue.size();
		
	}

	/**
	 * Check if the task manager is already running
	 * 
	 * @return
	 */
	public boolean isRunning() {
		return isRunning;
	}
	
	/**
	 * Check if the task manager already contains a task ( comparing tasks using equals() )
	 * 
	 * @param task
	 * @return
	 */
	public boolean contains(QueuedTask<?> task) {
		logger.debug("Checking if queue contains existing task. Current queue size = " + queue.size());
		return queue.contains(task);
	}
	
	/**
	 * Run this task manager
	 */
	@Override
	public void run() {
		
		isRunning = true;
		
		logger.debug(QueuedTaskManager.class.getName() + " running!");
		
		while(true){
			
			logger.debug("Polling queued task manager, name => '" + managerName + "', queue size => " + taskCount() + ", hash code => " + hashCode());
			
			if(Thread.currentThread().isInterrupted()){
				break;
			}
			
			try {
				
				// wait 5 seconds for next item in queue
				consume(queue.poll(5000, TimeUnit.MILLISECONDS));
				
				if(consumeFrequency > 0L) {
					Thread.sleep(consumeFrequency);
				}
				
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				logger.warn("Interrupt exception thrown while taking next element from task queue for '" + managerName + "'.", e);
			}
			
		}		
	
		isRunning = false;
		
		logger.debug(QueuedTaskManager.class.getName() + " run has ended for '" + managerName + "'!");
		
	}

	/**
	 * Add a task to the queue for processing
	 */
	@Override
	public synchronized void addTask(QueuedTask<?> task) {
		
		task.setTaskId(getNextTaskId());
		task.setQueuedTime(DateUtil.getCurrentTime());
		
		try {
		
			//logger.info(this.getClass().getName() + ".addTask(...) called");
			
			queue.put(task);

			logger.debug("Task was queued [id => " + task.getTaskId() + ", name => " + task.getName() + 
					", time => " + DateUtil.defaultFormat(task.getQueuedTime()) + ", size => " + queue.size() + "]");
			
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}		
			
	}
	
	private synchronized long getNextTaskId(){
		return ++taskId;
	}
	
	/**
	 * Run the task
	 * 
	 * @param task
	 */
	private void consume(QueuedTask<?> task){
		
		if(task != null){
			
			logger.debug("Task consumed (for run), [id => " + task.getTaskId() + ", name => " + task.getName() + 
					", time => " + DateUtil.defaultFormat(task.getQueuedTime()) + ", size => " + queue.size() + "]");			
			
			task.run();
			
		}
		
	}

}
