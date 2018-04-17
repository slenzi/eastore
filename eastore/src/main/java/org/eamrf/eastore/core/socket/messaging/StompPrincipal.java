/**
 * 
 */
package org.eamrf.eastore.core.socket.messaging;

import java.security.Principal;

/**
 * @author slenzi
 *
 */
public class StompPrincipal implements Principal {

	private final String name;
	
	/**
	 * 
	 */
	public StompPrincipal(String name) {
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see java.security.Principal#getName()
	 */
	@Override
	public String getName() {
		return name;
	}
	

}
