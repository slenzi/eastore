/**
 * 
 */
package org.eamrf.eastore.core.app;

import org.eamrf.eastore.core.config.AppConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

/**
 * EA Store Services boostrap class.
 * 
 * Enable Spring AOP using AspectJ, use the @EnableAspectJAutoProxy annotation
 * https://docs.spring.io/spring/docs/current/spring-framework-reference/html/aop.html
 * 
 * @author slenzi
 */
@SpringBootApplication
@Import({
	AppConfig.class
})
@EnableAspectJAutoProxy
public class EAStoreServiceApp {

	public EAStoreServiceApp() {}
	
	public static void main(String[] args) {
		
		SpringApplication app = new SpringApplication(EAStoreServiceApp.class);

		ApplicationContext ctx = app.run(args);
		
	}	
	
}
