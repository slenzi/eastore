package org.eamrf.concurrent.task;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

import org.eamrf.eastore.core.exception.ServiceException;

/**
 * Defines a basic task with a completable future
 * 
 * @author sal
 *
 * @param <T>
 */
public interface QueuedTask<T> extends Runnable, Comparable<QueuedTask<T>> {

	public long getTaskId();
	
	public void setTaskId(long taskId);
	
	public void setName(String name);
	
	public String getName();
	
	/**
	 * Date & time task was added to queue for processing.
	 * 
	 * @return
	 */
	public Date getQueuedTime();
	
	/**
	 * Set queued data & time. This will be set when the task is added to the queue for processing.
	 * 
	 * @param d
	 */
	public void setQueuedTime(Date d);
	
	/**
	 * The time the task started execution. This will be set when the task is started.
	 * 
	 * @return
	 */
	public Date getRunStartTime();
	
	/**
	 * The time the task ended execution. This will be set when the task has ended.
	 * 
	 * @return
	 */
	public Date getRunEndTime();
	
	/**
	 * The future to complete once the task had ended. This will notify the calling thread that the task
	 * has ended and that the resulting value T is ready ( see get() method) )
	 * 
	 * @return
	 */
	public CompletableFuture<T> getCompletableFuture();
	
	/**
	 * Calls get on the task's completable future. Will block until the task has completed, and it's future has completed.
	 * 
	 * @return
	 * @throws ServiceException
	 */
	public T get() throws ServiceException;
	
	/**
	 * Same as calling get, but for task that don't return vaues.
	 * 
	 * @throws ServiceException
	 */
	public void waitComplete() throws ServiceException;
	
	/**
	 * Override to complete work for the task.
	 * 
	 * @return
	 * @throws ServiceException
	 */
	public T doWork() throws ServiceException;
	
	
	/**
	 * Return the percentage of the task that is complete
	 * 
	 * @return A value from 0 to 100
	 */
	//public Double getProgress();  
	
}
