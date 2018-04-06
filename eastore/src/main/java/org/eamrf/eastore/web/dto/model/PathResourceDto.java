/**
 * 
 */
package org.eamrf.eastore.web.dto.model;

import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.ResourceType;

/**
 * @author slenzi
 *
 */
public class PathResourceDto extends NodeDto {

	private Long storeId = -1L;
	private ResourceType resourceType = null;
	private String relativePath = null;
	private StoreDto store = null;
	private String desc = null;
	
	private String readGroup1 = null;
	private String writeGroup1 = null;
	private String executeGroup1 = null;
	
	private Boolean canRead = false;
	private Boolean canWrite = false;
	private Boolean canExecute = false;
		
	public PathResourceDto() {
		
	}

	/**
	 * @return the storeId
	 */
	public Long getStoreId() {
		return storeId;
	}

	/**
	 * @param storeId the storeId to set
	 */
	public void setStoreId(Long storeId) {
		this.storeId = storeId;
	}

	/**
	 * @return the resourceType
	 */
	public ResourceType getResourceType() {
		return resourceType;
	}

	/**
	 * @param resourceType the resourceType to set
	 */
	public void setResourceType(ResourceType resourceType) {
		this.resourceType = resourceType;
	}

	/**
	 * @return the relativePath
	 */
	public String getRelativePath() {
		return relativePath;
	}

	/**
	 * @param relativePath the relativePath to set
	 */
	public void setRelativePath(String relativePath) {
		this.relativePath = relativePath;
	}

	/**
	 * @return the store
	 */
	public StoreDto getStore() {
		return store;
	}

	/**
	 * @param store the store to set
	 */
	public void setStore(StoreDto store) {
		this.store = store;
	}

	/**
	 * @return the desc
	 */
	public String getDesc() {
		return desc;
	}

	/**
	 * @param desc the desc to set
	 */
	public void setDesc(String desc) {
		this.desc = desc;
	}

	/**
	 * @return the readGroup1
	 */
	public String getReadGroup1() {
		return readGroup1;
	}

	/**
	 * @param readGroup1 the readGroup1 to set
	 */
	public void setReadGroup1(String readGroup1) {
		this.readGroup1 = readGroup1;
	}

	/**
	 * @return the writeGroup1
	 */
	public String getWriteGroup1() {
		return writeGroup1;
	}

	/**
	 * @param writeGroup1 the writeGroup1 to set
	 */
	public void setWriteGroup1(String writeGroup1) {
		this.writeGroup1 = writeGroup1;
	}

	/**
	 * @return the executeGroup1
	 */
	public String getExecuteGroup1() {
		return executeGroup1;
	}

	/**
	 * @param executeGroup1 the executeGroup1 to set
	 */
	public void setExecuteGroup1(String executeGroup1) {
		this.executeGroup1 = executeGroup1;
	}

	/**
	 * @return the canRead
	 */
	public Boolean getCanRead() {
		return canRead;
	}

	/**
	 * @param canRead the canRead to set
	 */
	public void setCanRead(Boolean canRead) {
		this.canRead = canRead;
	}

	/**
	 * @return the canWrite
	 */
	public Boolean getCanWrite() {
		return canWrite;
	}

	/**
	 * @param canWrite the canWrite to set
	 */
	public void setCanWrite(Boolean canWrite) {
		this.canWrite = canWrite;
	}

	/**
	 * @return the canExecute
	 */
	public Boolean getCanExecute() {
		return canExecute;
	}

	/**
	 * @param canExecute the canExecute to set
	 */
	public void setCanExecute(Boolean canExecute) {
		this.canExecute = canExecute;
	}



}
