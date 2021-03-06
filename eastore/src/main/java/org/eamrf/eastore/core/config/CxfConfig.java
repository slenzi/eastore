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
import org.eamrf.eastore.web.jaxrs.core.rs.ClosureResource;
import org.eamrf.eastore.web.jaxrs.core.rs.FileSystemActionResource;
import org.eamrf.eastore.web.jaxrs.core.rs.FileSystemJsonResource;
import org.eamrf.eastore.web.jaxrs.core.rs.FileSystemSearchResource;
import org.eamrf.eastore.web.jaxrs.core.rs.StoreApplication;
import org.eamrf.eastore.web.jaxrs.core.rs.TestResource;
import org.eamrf.eastore.web.jaxrs.core.rs.TreeResource;
import org.eamrf.eastore.web.jaxrs.core.security.EAAuthRequestHandler;
import org.eamrf.web.rs.exception.WebServiceExceptionMapper;
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
					getStoreApplication(), JAXRSServerFactoryBean.class );
				
			// Add service beans
			factory.setServiceBeans(
					Arrays.<Object> asList( 
							getTestResource(), 
							getTreeResource(), 
							getClosureResource(),
							getFileSystemJsonResource(),
							getFileSystemActionResource(),
							getFileSystemSearchResource()
							)
					);
			
			factory.setAddress( factory.getAddress() );
					
			// Add providers
			factory.setProviders(
					Arrays.<Object> asList(
							getJsonProvider(),
							getEAAuthRequestHandler(),
							getExceptionMapper()
							)
					);
			
			return factory.create();
	
		}
		
		@Bean
		public StoreApplication getStoreApplication(){
			return new StoreApplication();
		}
		
		@Bean
		public TestResource getTestResource(){
			return new TestResource();
		}
		
		@Bean
		public ClosureResource getClosureResource(){
			return new ClosureResource();
		}
		
		@Bean
		public TreeResource getTreeResource(){
			return new TreeResource();
		}
		
		@Bean
		public FileSystemJsonResource getFileSystemJsonResource(){
			return new FileSystemJsonResource();
		}
		
		@Bean
		public FileSystemActionResource getFileSystemActionResource(){
			return new FileSystemActionResource();
		}
		
		@Bean
		public FileSystemSearchResource getFileSystemSearchResource() {
			return new FileSystemSearchResource();
		}
		
		// marshalling json for our jax-rs services
		@Bean
	    public JacksonJsonProvider getJsonProvider() {
	        return new JacksonJsonProvider();
	    }
		
		@Bean
		public EAAuthRequestHandler getEAAuthRequestHandler(){
			return new EAAuthRequestHandler();
		}
		
		/**
		 * Maps our WebServiceException to http response codes.
		 * 
		 * @return
		 */
		@Bean
		public WebServiceExceptionMapper getExceptionMapper(){
			return new WebServiceExceptionMapper();
		}		
	
	};	
	
}
