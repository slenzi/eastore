package org.eamrf.eastore.core.config;

import org.eamrf.eastore.core.constants.AppConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * Load properties from our src/main/resources/application.properties file.
 * 
 * @author sal
 */
@Configuration
@PropertySource("classpath:" + AppConstants.APP_PROPERTIES_FILE)
public class PropertyConfig {

	/**
	 * To resolve ${} in @Value annotations.
	 * 
	 * @return
	 */
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

}
