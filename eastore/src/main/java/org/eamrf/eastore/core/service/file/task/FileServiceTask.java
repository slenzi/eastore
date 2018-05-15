/**
 * 
 */
package org.eamrf.eastore.core.service.file.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eamrf.concurrent.task.AbstractQueuedTask;
import org.eamrf.core.util.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base task class for all file service tasks
 * 
 * @author slenzi
 */
public abstract class FileServiceTask<T> extends AbstractQueuedTask<T> {

	Logger logger = LoggerFactory.getLogger(FileServiceTask.class.getName());
	
	//private int jobCompletedCount = 0;
	private Double progress = 0.0;
	
	// track the number of completed jobs for the task, and all child tasks
	// key = task id
	// value = completed job count for task
	private Map<Long,Integer> taskCompletedJobMap = new HashMap<Long,Integer>();
	
	private List<FileServiceTaskListener> listeners = new ArrayList<FileServiceTaskListener>();
	
	/**
	 * Register a listener for the task to monitor progess
	 * 
	 * @param listener
	 */
	public void registerProgressListener(FileServiceTaskListener listener) {
		listeners.add(listener);
	}
	
	/**
	 * Notify all listeners that the progress of the task has changed.
	 */
	protected void notifyProgressListeners() {
		listeners.forEach(listener -> listener.onProgressChange(this) );
	}
	
	protected void setCompletedJobCount(Long taskId, int count) {
		taskCompletedJobMap.put(taskId, count);
		logCompletedMap();
		updateProgress();
		notifyChange();
	}
	
	private void logCompletedMap(){
		Set<Long> keys = taskCompletedJobMap.keySet();
		logger.info("Jobs completed for task " + getTaskId() + ", " + getName() + ":");
		for(Long id : keys){
			logger.info("Task " + id + " = " + taskCompletedJobMap.get(id) + " completed jobs");
		}
	} 
	
	protected void notifyChange() {
		notifyProgressListeners();
	}
	
	/**
	 * Recalculate the progress based on the number of jobs completed
	 */
	protected void updateProgress() {
		
		int completedJobCount = getCompletedJobCount();
		
		if(getJobCount() > 0 && completedJobCount > 0) {
			setProgress( (Double.valueOf(completedJobCount) / Double.valueOf(getJobCount())) * 100.0);
		}else {
			setProgress(0.0);
		}
		
	}
	
	/**
	 * Get the total job count for the task.
	 * 
	 * @return
	 */
	public abstract int getJobCount();
	
	/**
	 * Get the status message for the task. e.g. a short string with the task id, name, progress, user id, etc,
	 * suitable for displaying to end users.
	 * 
	 * @return
	 */
	public abstract String getStatusMessage();
	
	/**
	 * Get the user id of the user that initiated the task.
	 * 
	 * @return
	 */
	public abstract String getUserId();
	
	/**
	 * Get the number of completed jobs for the task.
	 * 
	 * @return
	 */
	public int getCompletedJobCount() {
		int completedJobCount = 0;
		for(Integer jobCount : CollectionUtil.emptyIfNull(taskCompletedJobMap.values())){
			completedJobCount += jobCount;
		}
		return completedJobCount;
	}

	/* (non-Javadoc)
	 * @see org.eamrf.concurrent.task.QueuedTask#setProgress(java.lang.Double)
	 */
	@Override
	public void setProgress(Double progress) {
		this.progress = progress;
	}

	/* (non-Javadoc)
	 * @see org.eamrf.concurrent.task.QueuedTask#getProgress()
	 */
	@Override
	public Double getProgress() {
		return progress;
	}

}