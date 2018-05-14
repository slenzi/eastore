/**
 * 
 */
package org.eamrf.eastore.core.service.file.task;

import java.util.ArrayList;
import java.util.List;

import org.eamrf.concurrent.task.AbstractQueuedTask;

/**
 * Base task class for all file service tasks
 * 
 * @author slenzi
 */
public abstract class FileServiceTask<T> extends AbstractQueuedTask<T> {

	private int jobCompletedCount = 0;
	private Double progress = 0.0;
	
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
	
	/**
	 * Increment the jobs completed counter, by 1, and notify all
	 * observers that the state of the task has changed. 
	 */
	protected void incrementJobsCompleted() {
		jobCompletedCount++;
		updateProgress();
		notifyProgressChange();		
	}
	
	//protected void addToCompletedJobCount(int additional) {
	//	jobCompletedCount += additional;
	//	updateProgress();
	//	notifyProgressChange();			
	//}
	
	//protected void setJobsCompleted(int completed) {
	//	jobCompletedCount = completed;
	//	updateProgress();
	//	notifyProgressChange();		
	//}
	
	protected void setCompletedJobCount(int count) {
		jobCompletedCount = count;
		updateProgress();
		notifyProgressChange();
	}
	
	protected void notifyProgressChange() {
		notifyProgressListeners();
	}
	
	/**
	 * Recalculate the progress based on the number of jobs completed
	 */
	protected void updateProgress() {
		if(getJobCount() > 0 && jobCompletedCount > 0) {
			setProgress( (Double.valueOf(jobCompletedCount) / Double.valueOf(getJobCount())) * 100.0);
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
		return jobCompletedCount;
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