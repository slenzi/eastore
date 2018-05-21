/**
 * 
 */
package org.eamrf.eastore.core.socket.messaging.model;

import java.io.Serializable;
import java.util.Map;

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
	
	public enum FileServiceTaskType {
		
		ZIP("ZIP");
		
		private final String type;
		
		FileServiceTaskType(final String type){
			this.type = type;
		}

		/* (non-Javadoc)
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return type;
		}
	
	}

	private String id = null;
	private String userId = null;
	private String progress = null;
	private Integer jobCount = 0;
	private Integer jobCompletedCount = 0;
	private String message = null;
	
	private Map<String, String> attributes;
	
	private FileServiceTaskType taskType;
	
	/**
	 * 
	 */
	public FileServiceTaskStatus() {
		
	}

	/**
	 * @return the taskType
	 */
	public FileServiceTaskType getTaskType() {
		return taskType;
	}

	/**
	 * @param taskType the taskType to set
	 */
	public void setTaskType(FileServiceTaskType taskType) {
		this.taskType = taskType;
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
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
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

	/**
	 * @return the attributes
	 */
	public Map<String, String> getAttributes() {
		return attributes;
	}

	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return FileServiceTaskStatus.class.getName() + " [id=" + id + ", userId=" + userId + ", progress=" + progress + ", jobCount="
				+ jobCount + ", jobCompletedCount=" + jobCompletedCount + ", message=" + message + ", attributes="
				+ attributes + ", taskType=" + taskType + "]";
	}

	
	
}
