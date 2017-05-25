package org.eamrf.eastore.core.config;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

/**
 * Spring MVC configuration.
 * 
 * Component scanning notes:
 * -- org.eamrf.eastore.web.controller : base controllers package
 * -- org.eamrf.core.logging : custom LoggerBeanPostProccessor which enables us to inject a logger using @InjectLogger annotation.
 * 
 * @author slenzi
 */
@Configuration
@EnableWebMvc
@ComponentScan(
	basePackages = {
		"org.eamrf.eastore.web.controller",
		"org.eamrf.core.logging"
		}
)
public class WebMvcConfig extends WebMvcConfigurerAdapter {
	
	@InjectLogger
	private Logger logger;	
	
	/* (non-Javadoc)
	 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter#configureDefaultServletHandling(org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer)
	 */
	@Override
	public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
		
		logger.info(WebMvcConfig.class.getSimpleName() + ".configureDefaultServletHandling(...) called");
		
		configurer.enable();
	}

	/* (non-Javadoc)
	 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter#addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry)
	 */
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		
		logger.info(WebMvcConfig.class.getSimpleName() + ".addResourceHandlers(...) called");
		
		registry.addResourceHandler("/public/**").addResourceLocations("classpath:/public/");
	}
	
    @Bean
    public InternalResourceViewResolver viewResolver() {
        
    	logger.info(WebMvcConfig.class.getSimpleName() + ".InternalResourceViewResolver() called");
    	
    	InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setViewClass(JstlView.class);
    	resolver.setPrefix("/WEB-INF/views/jsp/");
        resolver.setSuffix(".jsp");
        return resolver;
        
    }

}
