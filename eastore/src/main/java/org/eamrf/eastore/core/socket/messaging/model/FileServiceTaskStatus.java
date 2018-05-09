/**
 * 
 */
package org.eamrf.eastore.core.socket.messaging.model;

import java.io.Serializable;

/**
 * Message object used to notify clients about that status of ongoing file service tasks
 * 
 * @author slenzi
 */
public class FileServiceTaskStatus implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7245524797729683558L;

	private String id = null;
	private String progress = null;
	private Integer jobCount = 0;
	private Integer jobCompletedCount = 0;
	private String message = null;
	
	/**
	 * 
	 */
	public FileServiceTaskStatus() {
		
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the progress
	 */
	public String getProgress() {
		return progress;
	}

	/**
	 * @param progress the progress to set
	 */
	public void setProgress(String progress) {
		this.progress = progress;
	}

	/**
	 * @return the jobCount
	 */
	public Integer getJobCount() {
		return jobCount;
	}

	/**
	 * @param jobCount the jobCount to set
	 */
	public void setJobCount(Integer jobCount) {
		this.jobCount = jobCount;
	}

	/**
	 * @return the jobCompletedCount
	 */
	public Integer getJobCompletedCount() {
		return jobCompletedCount;
	}

	/**
	 * @param jobCompletedCount the jobCompletedCount to set
	 */
	public void setJobCompletedCount(Integer jobCompletedCount) {
		this.jobCompletedCount = jobCompletedCount;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return FileServiceTaskStatus.class.getName() + " [id=" + id + ", progress=" + progress + ", jobCount=" + jobCount
				+ ", jobCompletedCount=" + jobCompletedCount + ", message=" + message + "]";
	}

	
	
}
