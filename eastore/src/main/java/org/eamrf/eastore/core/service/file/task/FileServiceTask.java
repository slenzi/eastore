/**
 * 
 */
package org.eamrf.eastore.core.service.file.task;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

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
	// keys = task ids, values = completed job count for task
	private TreeMap<Long,TaskCompleted> taskCompletedJobMap = new TreeMap<Long,TaskCompleted>();
	
	// helper class which maps a task to its number of completed jobs
	private class TaskCompleted {
		
		private FileServiceTask<?> task;
		private int completedJobs = 0;
		
		public TaskCompleted(FileServiceTask<?> task, int completedJobs) {
			this.task = task;
			this.completedJobs = completedJobs;
		}

		public FileServiceTask<?> getTask() {
			return task;
		}

		public int getCompletedJobs() {
			return completedJobs;
		}

	}
	
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
	
	protected void setCompletedJobCount(FileServiceTask<?> task, int count) {
		if(task.getTaskId() <= 0) {
			logger.warn("Cannot track completed job count for task, ID must be >= 1 [id=" + task.getTaskId() + ", name=" + task.getName() + "].");
			return;
		}
		taskCompletedJobMap.put(task.getTaskId(), new TaskCompleted(task, count));
		//logCompletedMap();
		updateProgress();
		notifyChange();
	}
	
	// debug method which can be deleted later...
	protected void logCompletedMap(){
		
		SortedSet<Long> keys = new TreeSet<>(taskCompletedJobMap.keySet());
		StringBuffer buf = new StringBuffer();
		for(Long id : keys){
			buf.append("\t[id=" + id + ", completedJobs=" + taskCompletedJobMap.get(id).getCompletedJobs() + 
					", totalJobs=" + this.getJobCount() + ", name=" + taskCompletedJobMap.get(id).getTask().getName() + "]\n");
		}		
		logger.info("Jobs completed for task [id=" + getTaskId() + ", name=" + getName() + ", jobCount=" + getJobCount() + "]\n" + buf.toString());
	
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
		for(TaskCompleted tk : CollectionUtil.emptyIfNull(taskCompletedJobMap.values())){
			completedJobCount += tk.getCompletedJobs();
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