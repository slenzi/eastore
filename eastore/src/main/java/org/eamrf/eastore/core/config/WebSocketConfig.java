/**
 * 
 */
package org.eamrf.eastore.core.config;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.slf4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * Configure websocket support
 * 
 * @author slenzi
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig extends AbstractWebSocketMessageBrokerConfigurer {

	@InjectLogger
	private Logger logger;	
	
	public WebSocketConfig() {
		
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		
		logger.info(WebSocketConfig.class.getSimpleName() + ".configureMessageBroker(...) called");
		
		registry.enableSimpleBroker("/topic");
		registry.setApplicationDestinationPrefixes("/app");
		
		logger.info("Message broker registery = " + registry.toString());		
		
	}

	/* (non-Javadoc)
	 * @see org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer#registerStompEndpoints(org.springframework.web.socket.config.annotation.StompEndpointRegistry)
	 */
	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		
		logger.info(WebSocketConfig.class.getSimpleName() + ".registerStompEndpoints(...) called");
		
		TomcatRequestUpgradeStrategy tomcatStrategy = new TomcatRequestUpgradeStrategy();
		DefaultHandshakeHandler handshakeHandler = new DefaultHandshakeHandler(tomcatStrategy);		
		
		
		registry.addEndpoint("/hello")
			.setAllowedOrigins("*") // allow access from all origins for now
			.setHandshakeHandler(handshakeHandler)
			.withSockJS();
		
			// optionally specify the SockJS client library to fall back on (give URL to script)
			//.setClientLibraryUrl(sockjsClientUrl);
		
	}

}
