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
		
		incrementJobsCompleted(1);
		
	}
	
	/**
	 * Increment the jobs completed counter, by the amount specified, and notify all
	 * observers that the state of the task has changed. 
	 * 
	 * @param increment
	 */
	protected void incrementJobsCompleted(int increment) {
		
		jobCompletedCount += increment;
		
		updateProgress();
		
		//setChanged();
		//notifyObservers();
		
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
	
	public abstract String getStatusMessage();
	
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
		//getLogger().info("Set progress to " + progress);
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