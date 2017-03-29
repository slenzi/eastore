/**
 * 
 */
package org.eamrf.eastore.core.config;

import java.util.Arrays;

import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.web.jaxrs.prs.rs.EAStoreApplication;
import org.eamrf.eastore.web.jaxrs.prs.rs.EAStoreResource;
import org.slf4j.Logger;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * Web services config using Apache CXF
 * 
 * @author slenzi
 */
@Configuration
public class CxfConfig {

    @InjectLogger
    private Logger logger;	
	
	/**
	 * will call shutdown on SpringBus when bean is destroyed.
	 * 
	 * @return
	 */
	@Bean( destroyMethod = "shutdown" )
	public SpringBus cxf() {
		return new SpringBus();
	}
	
	/**
	 * We don't have our usual Spring MVC config class so we must initialize the CXF Servlet here.
	 * 
	 * @return
	 */
    @Bean
    public ServletRegistrationBean cxfServlet() {
        final ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean(new CXFServlet(), "/services/*");
        servletRegistrationBean.setLoadOnStartup(1);
        return servletRegistrationBean;
    }	
	
	/**
	 * Configure PRS jax-rs service
	 * 
	 * @author slenzi
	 */
    @Configuration
	static class HelloServiceConfig {
	
		@Bean(name="eaStoreJaxRsServer")
		@DependsOn ( "cxf" )    	
		public Server getEAStoreJaxrsServer(){
			
			System.out.println(this.getClass().getName() + ".getEAStoreJaxrsServer() called");
			
			RuntimeDelegate delegate = RuntimeDelegate.getInstance();
			
			JAXRSServerFactoryBean factory = delegate.createEndpoint( 
					getEAStoreApplication(), JAXRSServerFactoryBean.class );
				
			// Add service beans
			factory.setServiceBeans(
					Arrays.<Object> asList( getEAStoreResource() )
					);
			
			factory.setAddress( factory.getAddress() );
					
			// Add providers
			factory.setProviders(
					Arrays.<Object> asList( getJsonProvider() )
					);
			
			return factory.create();
	
		}
		
		@Bean
		public EAStoreApplication getEAStoreApplication(){
			return new EAStoreApplication();
		}
		
		@Bean
		public EAStoreResource getEAStoreResource(){
			return new EAStoreResource();
		}
		
		// marshalling json for our jax-rs services
		@Bean
	    public JacksonJsonProvider getJsonProvider() {
	        return new JacksonJsonProvider();
	    }
	
	};	
	
}
