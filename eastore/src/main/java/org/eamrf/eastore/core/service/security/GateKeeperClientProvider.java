package org.eamrf.eastore.core.service.security;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.properties.ManagedProperties;
import org.eamrf.gatekeeper.web.service.Gatekeeper;
import org.eamrf.gatekeeper.web.service.jaxrs.client.GatekeeperRestClient;
import org.eamrf.gatekeeper.web.service.jaxws.client.GatekeeperClient;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Provider for Gatekeeper SOAP & RESTFul based client.
 * 
 * @author sal
 *
 */
@Service("gatekeeperClientProvider")
public class GateKeeperClientProvider {
	
	@InjectLogger
	private Logger logger;
	
	@Autowired
	private ManagedProperties appProps;
	
	private Gatekeeper soapClient = null;
	
	public GateKeeperClientProvider() {
		
	}
	
	/**
	 * Get instance of the Gatekeeper SOAP client
	 * 
	 * @return
	 */
	public Gatekeeper getSoapClient(){
		if(soapClient != null){
			return soapClient;
		}
		soapClient = GatekeeperClient.getInstance(
				appProps.getProperty("gatekeeper.jax.ws.namespace"),
				appProps.getProperty("gatekeeper.jax.ws.wsdl"),
				appProps.getProperty("gatekeeper.password")
				);
		return soapClient;
	}
	
	/**
	 * Get instance of the Gatekeeper RESTful client.
	 * 
	 * @return
	 */
	public GatekeeperRestClient getRestClient() {
		
		return new GatekeeperRestClient(
				appProps.getProperty("gatekeeper.jax.rs.service.url"),
				appProps.getProperty("gatekeeper.username"),
				appProps.getProperty("gatekeeper.password")
				);
		
	}

}
