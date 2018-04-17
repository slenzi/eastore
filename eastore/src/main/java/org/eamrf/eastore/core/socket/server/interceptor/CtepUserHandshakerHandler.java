/**
 * 
 */
package org.eamrf.eastore.core.socket.server.interceptor;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.eamrf.core.util.CollectionUtil;
import org.eamrf.core.util.StringUtil;
import org.eamrf.eastore.core.socket.messaging.StompPrincipal;
import org.springframework.http.HttpHeaders;
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
		
		Principal principal = request.getPrincipal();
		if(principal != null) {
			String principalName = StringUtil.changeNull(principal.getName());
			logger.info("Principal Name = " + principalName);
		}
		
		logRequestHeaders(request);
		
        // Generate principal with UUID as name
        return new StompPrincipal(UUID.randomUUID().toString());	
		
		//return super.determineUser(request, wsHandler, attributes);
        		
	}
	
	private void logRequestHeaders(ServerHttpRequest request) {
		
		HttpHeaders headers = request.getHeaders();
		Set<String> headerNames = headers.keySet();
		if(!CollectionUtil.isEmpty(headerNames)) {
			logger.info("Http Headers:");
			for(String headerName : headerNames) {
				logger.info(headerName + " = " + headers.get(headerName));
			}			
		}		
		
	}
	


}
