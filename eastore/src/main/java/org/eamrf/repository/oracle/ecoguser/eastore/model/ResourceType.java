/**
 * 
 */
package org.eamrf.repository.oracle.ecoguser.eastore.model;

/**
 * @author slenzi
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
	
	public static ResourceType getFromString(String type){
		if(type.equals(FILE.getTypeString())){
			return ResourceType.FILE;
		}else if(type.equals(DIRECTORY.getTypeString())){
			return ResourceType.DIRECTORY;
		}else{
			return null;
		}
	}

	@Override
	public String toString() {
		return type;
	}
	
}
