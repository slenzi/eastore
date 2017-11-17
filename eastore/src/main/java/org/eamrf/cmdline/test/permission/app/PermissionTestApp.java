/**
 * 
 */
package org.eamrf.cmdline.test.permission.app;

import org.eamrf.cmdline.common.config.CmdLineWebSocketConfig;
import org.eamrf.eastore.core.constants.ApplicationConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Controller;

/**
 * Command line based spring application for testing eastore/gatekeeper permissions
 * 
 * @author slenzi
 */
//@Configuration
@SpringBootApplication
@EnableAspectJAutoProxy
@PropertySource("classpath:" + ApplicationConstants.APP_PROPERTIES_FILE)
@ComponentScan(
	basePackages = {
		"org.eamrf.concurrent",
		"org.eamrf.core.logging",
		"org.eamrf.repository",
		"org.eamrf.eastore.core"
		//,"org.eamrf.eastore.web"
		
		// command line packages needed just for this app
		,"org.eamrf.cmdline.common"
		,"org.eamrf.cmdline.test.permission"
		
	},
	excludeFilters = {
		@ComponentScan.Filter(type = FilterType.ANNOTATION, value = Controller.class),
		@ComponentScan.Filter(type = FilterType.ANNOTATION, value = Configuration.class)
	}
)
@Import({
	
	// We use @PropertySource annotation above to setup properties
	//CmdLinePropertyConfig.class,
	
	// We use Spring Boot which sets up a datasource for us (see property files for connection details)
	//CmdLineDataSourceConfig.class
	
	// spring messaging (for org.eamrf.eastore.core.messaging.*)
	CmdLineWebSocketConfig.class
	
})
public class PermissionTestApp {
	
	@Autowired
	private PermissionTestCmdLineRunner permissionTest;

	public static void main(String[] args) {
		
		SpringApplication app = new SpringApplication(PermissionTestApp.class);
		
		// Specify not to load embedded web container. All we want is a console application.
		// we won't be using web services either
		app.setWebEnvironment(false);
		
		ApplicationContext ctx = app.run(args);
		
		// TODO - optionally load spring profiles
		
	}
	
}
