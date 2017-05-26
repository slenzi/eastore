package org.eamrf.eastore.core.config;

import org.apache.catalina.connector.Connector;
import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.StringUtil;
import org.eamrf.eastore.core.properties.ManagedProperties;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Perform custom initialization for the servlet container
 * 
 * @author slenzi
 */
@Configuration
public class TomcatConfig {

	@InjectLogger
	private Logger logger;	
	
    @Autowired
    private ManagedProperties appProps;	
	
	public TomcatConfig() {
		
	}
	
	/**
	 * Configure embedded tomcat container with AJP/1.3 connector on some port (set in properties file).
	 * This will allow us to proxy connections via Apache & mod proxy.
	 * 
	 * @return
	 */
	@Bean
	public EmbeddedServletContainerFactory servletContainer() {
	    
		TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();
		
		Connector ajpConnector = configureAjpConnector();
		if(ajpConnector != null){
			tomcat.addAdditionalTomcatConnectors(ajpConnector);
		}
		
	    return tomcat;
	    
	}
	
	private Connector configureAjpConnector(){
		
	    Connector ajpConnector = new Connector("AJP/1.3");
	    
	    String ajpPort = appProps.getProperty("ajp.port");
	    if(StringUtil.isNullEmpty(ajpPort)){
	    	logger.error("Unable to configure AJP/1.3 connector. Missing ajp.port property.");
	    	return null;
	    }
	    Integer iAjpPort = null;
	    try {
			iAjpPort = Integer.parseInt(ajpPort);
		} catch (NumberFormatException e) {
	    	logger.error("Unable to configure AJP/1.3 connector, " + e.getMessage() + 
	    			". ajp.port property contains invalid integer value => " + ajpPort);
	    	return null;
		}
	    
	    ajpConnector.setPort(iAjpPort.intValue());
	    ajpConnector.setSecure(false);
	    ajpConnector.setAllowTrace(false);
	    ajpConnector.setScheme("http");
	    
	    return ajpConnector;
		
	}

}
