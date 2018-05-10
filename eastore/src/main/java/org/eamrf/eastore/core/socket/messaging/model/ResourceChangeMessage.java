package org.eamrf.eastore.core.socket.messaging.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Message object used to notify clients that a resource had changed on the server.
 * 
 * @author slenzi
 *
 */
public class ResourceChangeMessage implements Serializable {

	private static final long serialVersionUID = -3076398022440403293L;
	
	private String code = null;
	private String message = null;
	private Long nodeId = null;
	private String userId = null;
	private LocalDate date = null;
	private LocalTime time = null;

	public ResourceChangeMessage() {

	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	public Long getNodeId() {
		return nodeId;
	}

	public LocalDate getDate() {
		return date;
	}

	public LocalTime getTime() {
		return time;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public void setTime(LocalTime time) {
		this.time = time;
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

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ResourceChangeMessage [code=" + code + ", message=" + message + ", nodeId=" + nodeId + ", userId="
				+ userId + ", date=" + date + ", time=" + time + "]";
	}

	
}
