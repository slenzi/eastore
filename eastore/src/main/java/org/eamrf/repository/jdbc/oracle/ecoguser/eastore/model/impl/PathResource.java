/**
 * 
 */
package org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl;

import java.io.Serializable;

/**
 * Model which contains common data elements for any resource in our file system.
 * 
 * @author slenzi
 */
public class PathResource extends Node implements Serializable {

	private static final long serialVersionUID = -9025005355249576232L;
	
	// the id of the store that this resource exists under
	private Long storeId = -1L;
	
	// the type of resource (file, directory, etc...)
	private ResourceType resourceType = null;
	
	// path to resource relative to the store directory.
	private String relativePath = null;
	
	// the store that this resource exists under
	private Store store = null;
	
	// optional description field...
	private String desc = null;
	
	//
	// TODO - add fields for access control
	//
	// read & write access
	//
	// WRITE_GROUPS - comma delimited field of gatekeeper groups
	// READ_GROUPS - comma delimited field of gatekeeper groups
	//
	// Directory Resource:
	// if user is a member of any WRITE_GROUP then they can upload & overwrite files in the directory
	// if user is a member of any READ_GROUP then they view/download files in the directory
	//
	// File Resources:
	// 
	
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

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	@Override
	public String toString() {
		return PathResource.class.getSimpleName() + " [id=" + getNodeId() + ", parentId=" + getParentNodeId() + ", childId=" + getChildNodeId()
				+ ", name=" + getNodeName() + ", dtCreated=" + getDateCreated() + ", dtUpdated=" + getDateUpdated()
				+ ", pathName=" + getPathName() + ", relPath=" + getRelativePath() + ", type=" + getResourceType().getTypeString()
				+ ", storeId=" + getStoreId() + ", haveStoreObj=" + ((store != null) ? true : false)
				+ ", haveDesc=" + ((desc != null) ? true : false) + "]";
	}	
}
