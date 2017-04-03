/**
 * 
 */
package org.eamrf.repository.oracle.ecoguser.eastore.model;

import java.io.Serializable;

/**
 * Model which contains common data elements for any resource in our file system.
 * 
 * @author slenzi
 */
public class PathResource extends Node implements Serializable {

	private static final long serialVersionUID = -9025005355249576232L;
	
	private Long storeId = -1L;
	private ResourceType resourceType = null;
	private String relativePath = null; // path to resource relative to the store directory.
	
	public PathResource() {
		
	}

	public Long getStoreId() {
		return storeId;
	}

	public String getPathName() {
		return getNodeName();
	}

	public String getRelativePath() {
		return relativePath;
	}

	public void setStoreId(Long storeId) {
		this.storeId = storeId;
	}

	public void setPathName(String pathName) {
		setNodeName(pathName);
	}

	public void setRelativePath(String relativePath) {
		this.relativePath = relativePath;
	}

	public ResourceType getResourceType() {
		return resourceType;
	}

	public void setResourceType(ResourceType resourceType) {
		this.resourceType = resourceType;
	}

}
