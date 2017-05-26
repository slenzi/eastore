package org.eamrf.eastore.core.config;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Configure Cross-Origin Resource Sharing (CORS)
 * 
 * We use this class so our eastore-ui (angular UI running from different host during development) can access
 * eastore resources when deployed on ecog2, neptune, triton, etc.
 * 
 * @author slenzi
 *
 */
@Configuration
public class CorsConfig {

	@InjectLogger
	private Logger logger;	
	
	public CorsConfig() {
		
	}
	
    @Bean
    public WebMvcConfigurer corsConfigurer() {
    	
    	logger.info(CorsConfig.class.getSimpleName() + ".corsConfigurer() called");
    	
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                
            	logger.info(WebMvcConfigurerAdapter.class.getSimpleName() + ".addCorsMappings(...) called");
            	
            	registry
                	.addMapping("/**")
                	.allowCredentials(false)
                	.allowedOrigins("*");
            }
        };
    }

}
