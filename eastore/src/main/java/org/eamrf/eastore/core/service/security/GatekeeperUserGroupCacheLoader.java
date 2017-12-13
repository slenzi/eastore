/**
 * 
 */
package org.eamrf.eastore.core.service.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.gatekeeper.web.service.jaxrs.client.GatekeeperRestClient;
import org.eamrf.gatekeeper.web.service.jaxws.model.Group;
import org.eamrf.web.rs.exception.WebServiceException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.cache.CacheLoader;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

/**
 * Load data into our gatekeeper user group cache. Data provided by Gatekeeper RESTful web service.
 * 
 * @author slenzi
 */
@Component
@Scope(value = "singleton")
public class GatekeeperUserGroupCacheLoader extends CacheLoader<String, Map<String, Group>> {

	@InjectLogger
	private Logger logger;		
	
    @Autowired
    private GateKeeperClientProvider gatekeeperClientProvider;
    
	// 10 threads available for database execution task
	private final ExecutorService executor = Executors.newFixedThreadPool(10);    
	
	@Override
	public Map<String, Group> load(String userId) throws Exception {
		
		return getGroupsForUser(userId);
		
	}

	/**
	 * Asynchronous reload of cache. Old cache value will be returned until reload is complete.
	 */	
	@Override
	public ListenableFuture<Map<String, Group>> reload(final String userId, Map<String,Group> oldMap) throws Exception {

		//logger.info("Reloading groups in user-group cache for user " + userId);
		
		if (neverNeedsRefresh(userId)) {
			return Futures.immediateFuture(oldMap);
		} else {
			// asynchronous!
			ListenableFutureTask<Map<String,Group>> task = ListenableFutureTask.create(new Callable<Map<String,Group>>() {
				public Map<String,Group> call() throws ServiceException {
					//System.out.println("Asynchronous group reload for ctep id => " + userId);
					return getGroupsForUser(userId);
				}
			});
			executor.execute(task);
			return task;
		}		
		
	}
	
	/**
	 * Go to the database to get groups for user. The is the "expensive" call that we want to
	 * speed up with our cache. We'll save a few seconds here or there...
	 * 
	 * @param ctepId users CTEP ID
	 * @return a map where keys are group codes and values are group objects
	 * @throws DatabaseException
	 */
	private Map<String,Group> getGroupsForUser(String userId) throws ServiceException {
		
		//logger.info("RESTful fetch to retrieve Gatekeeper groups for user " + userId);
		
		GatekeeperRestClient client = gatekeeperClientProvider.getRestClient();
		
		List<Group> groupList = null;
		try {
			groupList = client.getGroupsForUser(userId);
		} catch (WebServiceException e) {
			throw new ServiceException("Error fetching Gatekeeper groups for user " + userId + ". " + e.getMessage(), e);
		}
		
		Map<String,Group> userGroupMap = new HashMap<String,Group>();
		
		if(groupList != null && groupList.size() > 0) {
			for(Group group : groupList) {
				userGroupMap.put(group.getGroupCode(), group);
			}
		}
		
		return userGroupMap;
		
	}	
	
	/**
	 * Optionally specify if a specific keys never needs to be refreshed. In our case we need
	 * to periodically refresh group data for every user so this this method always returns false.
	 * 
	 * @param key
	 * @return
	 */
	private boolean neverNeedsRefresh(String key){
		return false;
	}

}
