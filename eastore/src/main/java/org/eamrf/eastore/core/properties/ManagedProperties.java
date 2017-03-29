package org.eamrf.eastore.core.properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Global spring managed application properties for our application
 * 
 * @author slenzi
 */
@Component(value="appProperties")
@Scope("singleton")
public class ManagedProperties {

	@Autowired
	Environment env;
	
	@Value( "${application.title}" )
	private String appTitle = null;

	/**
	 * @return the env
	 */
	public Environment getEnv() {
		return env;
	}
	
	/**
	 * Get a specific property
	 */
	public String getProperty(String name) {
		return env.getProperty(name);
	}
	
	/**
	 * @return the appTitle
	 */
	public String getAppTitle() {
		return appTitle;
	}
	
	/**
	 * Check if the spring profile is active.
	 * 
	 * @param profileName - spring profile name
	 * @return True if the profile is active, false otherwise
	 */
	public boolean isProfileActive(String profileName){
		
		return env.acceptsProfiles(profileName);
		
	}

}
