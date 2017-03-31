/**
 * 
 */
package org.eamrf.repository.oracle.ecoguser.eastore.model;

/**
 * @author slenzi
 *
 */
public enum ResourceType {

	FILE("File"),
	DIRECTORY("Directory");
	
	private final String type;
	
	private ResourceType(final String type){
		this.type = type;
	}
	
	public String getTypeString(){
		return type;
	}

	@Override
	public String toString() {
		return type;
	}
	
}
