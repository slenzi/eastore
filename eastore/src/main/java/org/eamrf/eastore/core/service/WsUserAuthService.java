package org.eamrf.eastore.core.service;

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
		
		WSUser user = null;
		try {
			user = wsUserRepository.getUser(login, service);
		} catch (Exception e) {
			throw new ServiceException(
					"Error fetching ws user " + login + ". " + e.getMessage(), e);
		}
		return user;
		
	}

}
