/**
 * 
 */
package org.eamrf.repository.jdbc.oracle.ecoguser.ws.model.impl;

/**
 * @author sal
 *
 */
public enum WSService {

	RAVE_WS("RAVE_WS"),
	MATCH_WS("MATCH_WS"),
	WS_STS("WS_STS"),
	MDA_WS("MDA_WS"),
	EARS_EMMES_WS("EARS_EMMES_WS"),
	EARS_PRS_WS("EARS_PRS_WS"), // DEPRECATED - Use EARS_IN_HOUSE instead
	EARS_IN_HOUSE("EARS_IN_HOUSE"),
	EALS_IN_HOUSE("EALS_IN_HOUSE"),
	EA_FEED_PROD_WS("EA_FEED_PROD_WS"),
	EA_FEED_TEST_WS("EA_FEED_TEST_WS"),
	EA_STORE("EA_STORE");
	
	String serviceName = null;

	private WSService(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getServiceName() {
		return serviceName;
	}	
	
}
