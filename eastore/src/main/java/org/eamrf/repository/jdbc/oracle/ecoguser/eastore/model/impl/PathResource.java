/**
 * 
 */
package org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

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
	
	private String readGroup1 = null;
	
	private String writeGroup1 = null;
	
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

	public String getReadGroup1() {
		return readGroup1;
	}

	public void setReadGroup1(String readGroup1) {
		this.readGroup1 = readGroup1;
	}

	public String getWriteGroup1() {
		return writeGroup1;
	}

	public void setWriteGroup1(String writeGroup1) {
		this.writeGroup1 = writeGroup1;
	}
	
	/**
	 * Convenience method to return all read groups for the resource. This will
	 * be useful down the road if we add more than one read group to the resource. 
	 * 
	 * @return
	 */
	public HashSet<String> getReadGroups(){
		HashSet<String> readGroups = new HashSet<String>();
		if(readGroup1 != null)
			readGroups.add(readGroup1);
		return readGroups;
	}
	
	/**
	 * Convenience method to return all write groups for the resource. This will
	 * be useful down the road if we add more than one write group to the resource. 
	 * 
	 * @return
	 */	
	public HashSet<String> getWriteGroups(){
		HashSet<String> writeGroups = new HashSet<String>();
		if(writeGroup1 != null)
			writeGroups.add(writeGroup1);
		return writeGroups;
	}	

	@Override
	public String toString() {
		return PathResource.class.getSimpleName() + " [id=" + getNodeId() + ", parentId=" + getParentNodeId() + ", childId=" + getChildNodeId()
				+ ", name=" + getNodeName() + ", dtCreated=" + getDateCreated() + ", dtUpdated=" + getDateUpdated()
				+ ", pathName=" + getPathName() + ", relPath=" + getRelativePath() + ", type=" + getResourceType().getTypeString()
				+ ", storeId=" + getStoreId() + ", haveStoreObj=" + ((store != null) ? true : false)
				+ ", haveDesc=" + ((desc != null) ? true : false) + ", readGroups=" + getReadGroups() + ", writeGroups=" + getWriteGroups() + "]";
	}	
}
