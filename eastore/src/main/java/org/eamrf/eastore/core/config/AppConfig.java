/**
 * 
 */
package org.eamrf.eastore.core.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Main spring configuration file for application.
 * 
 * @author slenzi
 */
@Configuration
@ComponentScan(
		basePackages = {
			"org.eamrf.concurrent",
			"org.eamrf.core.logging",
			"org.eamrf.repository",
			"org.eamrf.eastore.core",
			"org.eamrf.eastore.web"
		}
	)
	@Import({
		PropertyConfig.class,
		CorsConfig.class,
		TomcatConfig.class,
		CxfConfig.class,
		WebSocketConfig.class
	})
public class AppConfig {

	// PropertyConfig - configure managed properties
	
	// CorsConfig - configure cross-origin resource sharing permissions
	
	// TomcatConfig - configure embedded tomcat container
	
	// CxfConfig - configure our jax-rs RESTful services
	
	// WebSocketConfig - configure websocket stomp message with sockjs
	
}
