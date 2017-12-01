package org.eamrf.eastore.core.service.security;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.gatekeeper.web.service.jaxws.model.Group;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;

/**
 * 
 * A cache which stores gatekeeper group data for users.
 * 
 * @author slenzi
 */
@Component
@Scope(value = "singleton")
public class GatekeeperUserGroupCache {

	@InjectLogger
	private Logger logger;
	
	@Autowired
	GatekeeperUserGroupCacheLoader userGroupCacheLoader;
	
	// cache will be refreshed 3 minutes after last write (on a per key basis)
	private final long CACHE_REFRESH_MINUTES = 5L;
	
	// keys => user ctep id
	// values => a map where keys are group codes and values are groups.
	// The map will contain all the groups for the user.
	private LoadingCache<String, Map<String,Group>> userGroupCache = null;
	
	private ScheduledExecutorService scheduleExecutorService = null;
	
	public GatekeeperUserGroupCache() {
		
	}
	
	/**
	 * Initialize the cache
	 */
	@PostConstruct
	public void init() {
		
		//
		// Initialize cache. values will be refreshed after write.
		//
		userGroupCache = CacheBuilder.newBuilder()
				.maximumSize(100)
				.refreshAfterWrite(CACHE_REFRESH_MINUTES, TimeUnit.MINUTES)
				.build(
						userGroupCacheLoader
				);
		
		//
		// service for forcibly refreshing cache values, start with 2 minute delay
		//
		scheduleExecutorService = Executors.newSingleThreadScheduledExecutor();
		scheduleExecutorService.scheduleWithFixedDelay(new Runnable(){

			@Override
			public void run() {
				
				for(String ctepId : userGroupCache.asMap().keySet()){
					userGroupCache.refresh(ctepId);
				}
			}
			
		}, 2, CACHE_REFRESH_MINUTES, TimeUnit.MINUTES);
		
	}
	
	/**
	 * Get all groups for user from cache.
	 * 
	 * If the cache does not contain an entry for the key then it will go to the
	 * database in attempt to fetch the data.
	 * 
	 * @param userId - users CTEP ID
	 * @return a map where keys are group codes and values are group objects
	 */
	public Map<String, Group> getGroupsForUser(String userId) throws ExecutionException {
		return userGroupCache.get(userId);
	}
	
	/**
	 * Get all groups for user from cache, if present in the cache.
	 * 
	 * If the cache does not contain an entry for the key then it will *NOT* go to the
	 * database to fetch the data.
	 * 
	 * @param userId - users CTEP ID
	 * @return
	 * @throws ExecutionException
	 */
	public Map<String, Group> getGroupsForUserIfPresent(String userId) {
		return userGroupCache.getIfPresent(userId);
	}
	
	/**
	 * Manually refresh list of groups in the cache for the user
	 * 
	 * @param userId
	 */
	public void refresh(String userId) {
		userGroupCache.refresh(userId);
	}
	
	/**
	 * Manually update the cache for the user
	 * 
	 * @param userId - the users CTEP ID
	 * @param groupMap - The groups to add to the user. The current cached values will be overwritten.
	 */
	public void updateCachedGroupsForUser(String userId, Map<String, Group> groupMap) {
		userGroupCache.put(userId, groupMap);
	}	

}
