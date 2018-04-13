/**
 * 
 */
package org.eamrf.eastore.core.socket.server.interceptor;

import java.security.Principal;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.RequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * @author slenzi
 *
 */
public class CtepUserHandshakerHandler extends DefaultHandshakeHandler {

	private Logger logger = Logger.getLogger(this.getClass().getName()); 
	
	/**
	 * 
	 */
	public CtepUserHandshakerHandler() {
		super();
	}

	/**
	 * @param requestUpgradeStrategy
	 */
	public CtepUserHandshakerHandler(RequestUpgradeStrategy requestUpgradeStrategy) {
		super(requestUpgradeStrategy);
	}

	/* (non-Javadoc)
	 * @see org.springframework.web.socket.server.support.AbstractHandshakeHandler#determineUser(org.springframework.http.server.ServerHttpRequest, org.springframework.web.socket.WebSocketHandler, java.util.Map)
	 */
	@Override
	protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
		
		logger.info("determineUser called");
		
		return super.determineUser(request, wsHandler, attributes);
	}
	


}
