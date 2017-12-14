/**
 * 
 */
package org.eamrf.eastore.core.service.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.CollectionUtil;
import org.eamrf.eastore.core.aop.profiler.MethodTimer;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.gatekeeper.web.service.jaxws.model.Group;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for gatekeeper related actions
 * 
 * @author slenzi
 */
@Service
public class GatekeeperService {

    @InjectLogger
    private Logger logger;
    
    @Autowired
    private GatekeeperUserGroupCache gatekeeperCache;	
	
	/**
	 * 
	 */
	public GatekeeperService() {
		
	}
	
	/**
	 * Fetch users group codes
	 * 
	 * @param userId - user id (ctep id)
	 * @return A set of group codes
	 */
	@MethodTimer
	public Set<String> getUserGroupCodes(String userId) throws ServiceException {
		
		Collection<Group> groups = getGatekeeperGroups(userId);
		
		Set<String> groupSet = new HashSet<String>();
		if(!CollectionUtil.isEmpty(groups)) {
			for(Group group : groups) {
				groupSet.add(group.getGroupCode());
			}
		}
		
		return groupSet;
	}
	
	/**
	 * Fetch users gatekeeper groups
	 * 
	 * @param userId
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public Collection<Group> getGatekeeperGroups(String userId) throws ServiceException {
		
		Map<String, Group> groupMap;
		try {
			groupMap = gatekeeperCache.getGroupsForUser(userId);
		} catch (ExecutionException e) {
			throw new ServiceException("Error fetching gatekeeper groups from cache for user " + userId, e);
		}
		
		return groupMap.values();
		
	}	

}
