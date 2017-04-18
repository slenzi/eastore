/**
 * 
 */
package org.eamrf.eastore.web.jaxrs.core.security;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.service.WsUserAuthService;
import org.eamrf.repository.jdbc.oracle.ecoguser.ws.model.impl.WSService;
import org.eamrf.repository.jdbc.oracle.ecoguser.ws.model.impl.WSUser;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Authorizes access to EA Store JAX-RS Services
 * 
 * See following jax-rs resource classes as some examples (might not be all...)
 * 
 * @see org.eamrf.eastore.web.jaxrs.core.rs.ClosureResource
 * @see org.eamrf.eastore.web.jaxrs.core.rs.FileSystemActionResource
 * @see org.eamrf.eastore.web.jaxrs.core.rs.FileSystemJsonResource
 * @see org.eamrf.eastore.web.jaxrs.core.rs.TestResource
 * @see org.eamrf.eastore.web.jaxrs.core.rs.TreeResource
 * 
 * See following config setup class.
 * 
 * @see org.eamrf.eastore.core.config.CXFConfig
 * 
 * @author sal
 */
@Service
public class EAAuthRequestHandler implements ContainerRequestFilter {

    @InjectLogger
    private Logger logger;
    
    @Autowired
    private WsUserAuthService wsUserAuthService;
    
    private final WSService EA_STORE_SERVICE = WSService.EA_STORE;
	
	/**
	 * 
	 */
	public EAAuthRequestHandler() {
		
	}

	/* (non-Javadoc)
	 * @see javax.ws.rs.container.ContainerRequestFilter#filter(javax.ws.rs.container.ContainerRequestContext)
	 */
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		
		Response failedRS = Response.status(401).header("WWW-Authenticate", "Basic").build();
		Message message = JAXRSUtils.getCurrentMessage();
		AuthorizationPolicy policy = message.get(AuthorizationPolicy.class);

		if (policy == null) {
			requestContext.abortWith(failedRS);
		} else {

			String username = policy.getUserName();
			String password = policy.getPassword();
			
			WSUser wsUser = null;
			try {
				wsUser = wsUserAuthService.getUser(username, EA_STORE_SERVICE);
			} catch (ServiceException e) {
				logger.error("Failed to lookup user for login => " + username + ". " + e.getMessage(), e);
				requestContext.abortWith(failedRS);
			}
			if(wsUser == null){
				logger.error("Error, no account found for service " + EA_STORE_SERVICE + ". Provided login = '" + username + "'");
				requestContext.abortWith(failedRS);
				return;
			}
			
			//Otherwise, check credentials
			String expectedHashedPassword = wsUser.getHashedPassword();
			
			// Compute the SHA-1 Message Digest of the given password.
			// We will use this to compare against the expected password.
			MessageDigest md = null;
			try {
				md = MessageDigest.getInstance("SHA-1");
			} catch (NoSuchAlgorithmException e) {
				logger.error("No SHA-1 message digest available. Cannot compute SHA-1 message digest for password "
						+ "for comparison in the EA database. " + e.getMessage(), e);
				requestContext.abortWith(failedRS);
				return;
			}
			md.update(password.getBytes());
			byte[] hashedPassword = md.digest();
			
			// Convert the given hashed password to base 64 to do the comparison.  This is because
			// the hashed password in the database will be stored in base 64.
			String base64HashedPassword = Base64.encodeBase64String(hashedPassword);
			
			logger.info("username => " + username + ", expected hashed pwd => " + expectedHashedPassword + 
					", base64 computed hash pwd => " + base64HashedPassword);
			
			if(base64HashedPassword.equals(expectedHashedPassword)){
				// good password
				logger.info("User '" + username + "' is allowed access! Yay!");
			}else{
				logger.error("user '" + username + "' provided bad password. Not allowed to access " + 
						EA_STORE_SERVICE + " services.");
				requestContext.abortWith(failedRS);
				return;				
			}
			
		}		 
		
	}

}
