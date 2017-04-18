package org.eamrf.repository.jdbc.oracle.ecoguser.ws.model.impl;

/**
 * Taken from MatchInformaticsLayer project
 * 
 * @author sal
 */
public enum Role {
	
	RAVE("RAVE"),
	ADMIN("MATCH_ADMIN"), // MATCH_ADMIN needs to Match AuthWorld role
	NCI("NCI");
	
	String groupCode;
	
	private Role(String groupCode) {
		this.groupCode = groupCode;
	}

	public String getGroupCode() {
		return groupCode;
	}
}