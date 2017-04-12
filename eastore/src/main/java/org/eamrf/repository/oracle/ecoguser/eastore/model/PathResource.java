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
	private Store store = null;
	
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

	public Store getStore() {
		return store;
	}

	public void setStore(Store store) {
		this.store = store;
	}

	@Override
	public String toString() {
		return PathResource.class.getSimpleName() + " [id=" + getNodeId() + ", parentId=" + getParentNodeId() + ", childId=" + getChildNodeId()
				+ ", name=" + getNodeName() + ", dtCreated=" + getDateCreated() + ", dtUpdated=" + getDateUpdated()
				+ ", pathName=" + getPathName() + ", relPath=" + getRelativePath() + ", type=" + getResourceType().getTypeString()
				+ ", storeId=" + getStoreId() + ", haveStoreObj=" + ((store != null) ? true : false) + "]";
	}	
}
