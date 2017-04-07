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
		PropertyConfig.class
		,TomcatConfig.class
		,CxfConfig.class
	})
public class AppConfig {

}
