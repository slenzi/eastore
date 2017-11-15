/**
 * 
 */
package org.eamrf.cmdline.common.config;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.slf4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;
import org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * Configure websocket support
 * 
 * @author slenzi
 */
@Configuration
@EnableWebSocketMessageBroker
public class CmdLineWebSocketConfig extends AbstractWebSocketMessageBrokerConfigurer {

	@InjectLogger
	private Logger logger;	
	
	public CmdLineWebSocketConfig() {
		
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		
		logger.info(CmdLineWebSocketConfig.class.getSimpleName() + ".configureMessageBroker(...) called");
		
		registry.enableSimpleBroker("/topic");
		registry.setApplicationDestinationPrefixes("/app");
		
		logger.info("Message broker registery = " + registry.toString());		
		
	}

	/* (non-Javadoc)
	 * @see org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer#registerStompEndpoints(org.springframework.web.socket.config.annotation.StompEndpointRegistry)
	 */
	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		
		logger.info(CmdLineWebSocketConfig.class.getSimpleName() + ".registerStompEndpoints(...) called");
		
		TomcatRequestUpgradeStrategy tomcatStrategy = new TomcatRequestUpgradeStrategy();
		DefaultHandshakeHandler handshakeHandler = new DefaultHandshakeHandler(tomcatStrategy);		
		
		// URL will be, http://localhost:45001/eastore/stomp-service/info
		// (replace 45001 with whatever 'server.port' value you specified in the build properties file)
		StompWebSocketEndpointRegistration endpoint = registry.addEndpoint("/stomp-service");
		
		endpoint
			.setAllowedOrigins("*") // allow access from all origins for now
			.setHandshakeHandler(handshakeHandler);
		
		SockJsServiceRegistration sockReg = endpoint.withSockJS();
		
		//This option can be used to disable automatic addition of CORS headers for SockJS requests.
		sockReg.setSupressCors(true);
		
		// optionally specify the SockJS client library to fall back on (give URL to script)
		//.setClientLibraryUrl(sockjsClientUrl);
		
	}

}
