/**
 * 
 */
package org.eamrf.eastore.core.messaging.model;

import java.io.Serializable;

import org.springframework.stereotype.Component;

/**
 * @author slenzi
 *
 */
@Component
public class ReplyMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8379398273750497788L;
	
	private String message = null;
	
	public ReplyMessage(){
		message = "Reply not set.";
	}
	
	public ReplyMessage(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
