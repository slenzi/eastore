/**
 * 
 */
package org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl;

import java.io.Serializable;
import java.util.HashSet;

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
	
	
	// ---------------------------------------------
	// Begin permissions
	// ---------------------------------------------
	
	// group for the 'canRead' access control bit
	private String readGroup1 = null;
	
	// group for the 'canWrite' access control bit
	private String writeGroup1 = null;
	
	// group for the 'canExecute' access control bit
	private String executeGroup1 = null;
	
	// Access control bits. These will be set when user access is evaluated based on the read/write/execute groups.
	private Boolean canRead = false;	// read file, or directory contents
	private Boolean canWrite = false;	// write, update, delete file, add files to directory
	private Boolean canExecute = false;	// administer file and directory properties, including access groups
	
	// ---------------------------------------------
	// End permissions
	// ---------------------------------------------
	
	
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
	
	public String getExecuteGroup1() {
		return executeGroup1;
	}

	public void setExecuteGroup1(String executeGroup1) {
		this.executeGroup1 = executeGroup1;
	}

	public Boolean getCanRead() {
		return canRead;
	}

	public void setCanRead(Boolean canRead) {
		this.canRead = canRead;
	}

	public Boolean getCanWrite() {
		return canWrite;
	}

	public void setCanWrite(Boolean canWrite) {
		this.canWrite = canWrite;
	}

	public Boolean getCanExecute() {
		return canExecute;
	}

	public void setCanExecute(Boolean canExecute) {
		this.canExecute = canExecute;
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
	
	/**
	 * Convenience method to return all execute groups for the resource. This will
	 * be useful down the road if we add more than one execute group to the resource. 
	 * 
	 * @return
	 */	
	public HashSet<String> getExecuteGroups(){
		HashSet<String> executeGroups = new HashSet<String>();
		if(executeGroup1 != null)
			executeGroups.add(executeGroup1);
		return executeGroups;
	}
	
	/**
	 * Same as toString but with less attributes printed
	 * 
	 * @return
	 */
	public String simpleToString() {
		return PathResource.class.getSimpleName() + " [id=" + getNodeId() + ", parentId=" + getParentNodeId() + ", type=" + getResourceType().getTypeString() + 
				", relPath=" + getRelativePath() + ", canRead=" + canRead + ", canWrite=" + canWrite + ", canExecute=" + canExecute + "]";
	}

	/**
	 * 
	 */
	@Override
	public String toString() {
		return PathResource.class.getSimpleName() + " [id=" + getNodeId() + ", parentId=" + getParentNodeId() + ", childId=" + getChildNodeId()
				+ ", name=" + getNodeName() + ", dtCreated=" + getDateCreated() + ", dtUpdated=" + getDateUpdated()
				+ ", pathName=" + getPathName() + ", relPath=" + getRelativePath() + ", type=" + getResourceType().getTypeString()
				+ ", storeId=" + getStoreId() + ", haveStoreObj=" + ((store != null) ? true : false)
				+ ", haveDesc=" + ((desc != null) ? true : false) + ", readGroups=" + getReadGroups() + ", writeGroups=" + getWriteGroups()
				+ ", executeGroups=" + getExecuteGroups() + ", canRead=" + canRead + ", canWrite=" + canWrite + ", canExecute=" + canExecute + "]";
	}	
}
