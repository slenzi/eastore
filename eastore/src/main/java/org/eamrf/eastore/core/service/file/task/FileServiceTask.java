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
	
	public void registerProgressListener(FileServiceTaskListener listener) {
		listeners.add(listener);
	}
	
	protected void notifyProgressListeners() {
		listeners.forEach(listener -> listener.onProgressChange(this) );
	}
	
	/**
	 * Increment the counter that tracks the number of jobs completed, and notify all
	 * observers that the state of the task has changed. 
	 */
	protected void incrementJobsCompleted() {
		
		jobCompletedCount++;
		
		updateProgress();
		
		//setChanged();
		//notifyObservers();
		
		notifyProgressListeners();
		
	}
	
	protected void updateProgress() {
		
		if(getJobCount() > 0 && jobCompletedCount > 0) {
			setProgress( (Double.valueOf(jobCompletedCount) / Double.valueOf(getJobCount())) * 100.0);
		}else {
			setProgress(0.0);
		}
		
	}
	
	public abstract int getJobCount();

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