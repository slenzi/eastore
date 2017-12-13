package org.eamrf.concurrent.task;

import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eamrf.core.util.CodeTimer;
import org.eamrf.core.util.DateUtil;
import org.eamrf.core.util.StringUtil;
import org.eamrf.eastore.core.exception.ServiceException;
import org.slf4j.Logger;

/**
 * Abstract class which encapsulates general logic for a file store task/operation
 * 
 * @author sal
 *
 * @param <T>
 */
public abstract class AbstractQueuedTask<T> implements QueuedTask<T>, Comparable<QueuedTask<T>> {

	private long taskId = 0L;
	
	private Date queuedTime = null;
	private Date runStartTime = null;
	private Date runEndTime = null;
	private String name = null;
	private CodeTimer timer = null;
	
	private CompletableFuture<T> completableFuture = new CompletableFuture<T>();
	
	public AbstractQueuedTask() {
		
	}

	/**
	 * @return the taskId
	 */
	public long getTaskId() {
		return taskId;
	}

	/**
	 * @param taskId the taskId to set
	 */
	public void setTaskId(long taskId) {
		this.taskId = taskId;
	}

	/**
	 * Return task name.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set optional task name
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	private boolean hasName(){
		return !StringUtil.isNullEmpty(this.name);
	}

	/**
	 * Compares tasks on the date and time they were queued for execution in the task manager. 
	 */
	@Override
	public int compareTo(QueuedTask<T> otherTask) {
		
		if(otherTask == null){
			
			// run this task first
			return -1;
			
		}else{
			
			if(queuedTime == null && otherTask.getQueuedTime() == null){
				
				return Long.valueOf(taskId).compareTo(Long.valueOf(otherTask.getTaskId()));
				
			}else if(queuedTime != null && otherTask.getQueuedTime() == null){
				
				// run this task first
				return -1;
				
			}else if(queuedTime == null && otherTask.getQueuedTime() != null){
				
				// run other task first
				return 1;
				
			}else{
				
				// run whichever task was queued first
				
				if(queuedTime.equals(otherTask.getQueuedTime())){
					
					return Long.valueOf(taskId).compareTo(Long.valueOf(otherTask.getTaskId()));
					
				}else{
					
					return queuedTime.compareTo(otherTask.getQueuedTime());
					
				}
				
			}
			
		}
		
	}

	/**
	 * @return the queuedTime
	 */
	public Date getQueuedTime() {
		return queuedTime;
	}

	/**
	 * @param queuedTime the queuedTime to set
	 */
	public void setQueuedTime(Date queuedTime) {
		this.queuedTime = queuedTime;
	}

	/**
	 * @return the runStartTime
	 */
	public Date getRunStartTime() {
		return runStartTime;
	}

	/**
	 * @param runStartTime the runStartTime to set
	 */
	public void setRunStartTime(Date runStartTime) {
		this.runStartTime = runStartTime;
	}

	/**
	 * @return the runEndTime
	 */
	public Date getRunEndTime() {
		return runEndTime;
	}

	/**
	 * @param runEndTime the runEndTime to set
	 */
	public void setRunEndTime(Date runEndTime) {
		this.runEndTime = runEndTime;
	}
	
	/**
	 * @return the future
	 */
	public CompletableFuture<T> getCompletableFuture() {
		return completableFuture;
	}

	/**
	 * @param completableFuture the completableFuture to set
	 */
	public void setCompletableFuture(CompletableFuture<T> completableFuture) {
		this.completableFuture = completableFuture;
	}

	/**
	 * Calls get on completable future
	 */
	@Override
	public T get() throws ServiceException {
		try {
			return completableFuture.get();
		} catch (InterruptedException e) {
			throw new ServiceException("InterruptedException thrown for task " + 
					"[id = " + this.getTaskId() + ((hasName() ? ", name = " + getName() : "")) + "]. " + 
					e.getMessage(), e);
		} catch (ExecutionException e) {
			throw new ServiceException("ExecutionException thrown for task " +
					"[id = " + this.getTaskId() + ((hasName() ? ", name = " + getName() : "")) + "]. " + 
					e.getMessage(), e);
		}
	}
	
	/**
	 * same as calling get, but for tasks with futures that return void
	 */
	@Override
	public void waitComplete() throws ServiceException {
		
		get();
		
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		
		runStartTime = DateUtil.getCurrentTime();
		timer = new CodeTimer();	
		timer.start();
		
		getLogger().info("Task is running, [id => " + getTaskId() + ", name => " + getClass().getName() + 
				", queued at => " + DateUtil.defaultFormat(this.getQueuedTime()) + "]");
		
		T value = null;
		try {
			
			value = doWork();
			
			timer.stop();
			
		} catch (ServiceException e) {
			
			timer.stop();
			
			// pass exception to CompletableFuture.get()
			getCompletableFuture().completeExceptionally(e);
			
		}
		
		runEndTime = DateUtil.getCurrentTime();
		
		getLogger().info("Task completed run in " + timer.getElapsedTime() + ", id => " + getTaskId() + ", name => " + this.getName() + 
				", queued at => " + DateUtil.defaultFormat(this.getQueuedTime()) + "]");
		
		// at this point, any potential client thread that's blocking on CompletableFuture.get() will wake up and receive the value
		getCompletableFuture().complete(value);
		
	}

	public abstract T doWork() throws ServiceException;
	
	public abstract Logger getLogger();

}
