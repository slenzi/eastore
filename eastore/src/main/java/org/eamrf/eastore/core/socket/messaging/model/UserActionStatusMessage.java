/**
 * 
 */
package org.eamrf.eastore.core.socket.messaging.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Message object used to notify clients of ongoing status of various user initiated actions.
 * 
 * e.g., running a zip-download process
 * 
 * @author slenzi
 */
public class UserActionStatusMessage implements Serializable {

	private static final long serialVersionUID = -7910473201791885244L;
	
	public enum UserAction {
		
		ZIP("ZIP");
		
		private final String type;
		
		UserAction(final String type){
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
	
	private String userId = null;
	private UserAction taskType;
	private Map<String, String> attributes;

	/**
	 * 
	 */
	public UserActionStatusMessage() {
		
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
	 * @return the taskType
	 */
	public UserAction getTaskType() {
		return taskType;
	}

	/**
	 * @param taskType the taskType to set
	 */
	public void setTaskType(UserAction taskType) {
		this.taskType = taskType;
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
	
	/**
	 * Set message attribute
	 * 
	 * @param key
	 * @param value
	 */
	public void setAttribute(String key, String value) {
		if(attributes == null) {
			attributes = new HashMap<String,String>();
		}
		attributes.put(key, value);
	}
	
	/**
	 * Get message attribute
	 * 
	 * @param key
	 * @return
	 */
	public String getAttribute(String key) {
		if(attributes != null) {
			return attributes.get(key);
		}
		return null;
	}
	
}
