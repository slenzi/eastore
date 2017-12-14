package org.eamrf.eastore.core.service.security;

import java.util.HashMap;
import java.util.Map;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.repository.jdbc.oracle.ecoguser.ws.WsUserRepository;
import org.eamrf.repository.jdbc.oracle.ecoguser.ws.model.impl.WSService;
import org.eamrf.repository.jdbc.oracle.ecoguser.ws.model.impl.WSUser;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for interacting with WSUser data
 * 
 * @author slenzi
 */
@Service
public class WsUserAuthService {

    @InjectLogger
    private Logger logger;
    
    @Autowired
    private WsUserRepository wsUserRepository;
    
    // a class that's used as a key on our user cache map
    private class UserCacheKey {
    	
    	private String login = null;
    	private WSService service = null;
    	
    	public UserCacheKey(String login, WSService service) {
    		this.login = login;
    		this.service = service;
    	}
		public String getLogin() {
			return login;
		}
		public void setLogin(String login) {
			this.login = login;
		}
		public WSService getService() {
			return service;
		}
		public void setService(WSService service) {
			this.service = service;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((login == null) ? 0 : login.hashCode());
			result = prime * result + ((service == null) ? 0 : service.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			UserCacheKey other = (UserCacheKey) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (login == null) {
				if (other.login != null)
					return false;
			} else if (!login.equals(other.login))
				return false;
			if (service != other.service)
				return false;
			return true;
		}
		private WsUserAuthService getOuterType() {
			return WsUserAuthService.this;
		}
		
    }
    
    // cache users that we've already looked up
    private Map<UserCacheKey, WSUser> cachedUsers = new HashMap<UserCacheKey, WSUser>();
	
	public WsUserAuthService() {
		
	}
	
	/**
	 * Fetch ws user by login, for specific web service
	 * 
	 * @param login - the users login
	 * @param service - the web service they are trying to access
	 * @return
	 * @throws ServiceException
	 */
	public WSUser getUser(String login, WSService service) throws ServiceException {
		
		// first check our cache
		UserCacheKey cacheKey = new UserCacheKey(login, service);
		if(cachedUsers.containsKey(cacheKey)) {
		
			return cachedUsers.get(cacheKey);
		
		// otherwise pull from database
		}else {
			WSUser user = null;
			try {
				user = wsUserRepository.getUser(login, service);
			} catch (Exception e) {
				throw new ServiceException("Error fetching ws user " + login + ". " + e.getMessage(), e);
			}
			return user;
		}
		
	}

}
