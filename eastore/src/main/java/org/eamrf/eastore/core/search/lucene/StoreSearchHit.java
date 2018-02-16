package org.eamrf.eastore.core.search.lucene;

import java.nio.file.Path;

/**
 * Encapsulates one search result "hit" from a lucene search.
 * 
 * @author slenzi
 *
 */
public class StoreSearchHit {

	// lucene specific fields
	private int luceneDocId = 0;
	private String[] fragments = null;
	
	// eastore fields
	private Long resourceId = 0L;
	private String resourceName = null;
	private String resourceDesc = null;
	private Path resourcePath = null;
	private String resourceRelativePath = null;
	private Long storeId = 0L;
	private String storeName = null;
	
	public StoreSearchHit() {
		
	}

	/**
	 * @return the luceneDocId
	 */
	public int getLuceneDocId() {
		return luceneDocId;
	}

	/**
	 * @param luceneDocId the luceneDocId to set
	 */
	public void setLuceneDocId(int luceneDocId) {
		this.luceneDocId = luceneDocId;
	}

	/**
	 * @return the fragments
	 */
	public String[] getFragments() {
		return fragments;
	}

	/**
	 * @param fragments the fragments to set
	 */
	public void setFragments(String[] fragments) {
		this.fragments = fragments;
	}

	/**
	 * @return the resourceId
	 */
	public Long getResourceId() {
		return resourceId;
	}

	/**
	 * @param resourceId the resourceId to set
	 */
	public void setResourceId(Long resourceId) {
		this.resourceId = resourceId;
	}

	/**
	 * @return the resourceName
	 */
	public String getResourceName() {
		return resourceName;
	}

	/**
	 * @param resourceName the resourceName to set
	 */
	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

	/**
	 * @return the resourceDesc
	 */
	public String getResourceDesc() {
		return resourceDesc;
	}

	/**
	 * @param resourceDesc the resourceDesc to set
	 */
	public void setResourceDesc(String resourceDesc) {
		this.resourceDesc = resourceDesc;
	}

	/**
	 * @return the resourcePath
	 */
	public Path getResourcePath() {
		return resourcePath;
	}

	/**
	 * @param resourcePath the resourcePath to set
	 */
	public void setResourcePath(Path resourcePath) {
		this.resourcePath = resourcePath;
	}

	/**
	 * @return the resourceRelativePath
	 */
	public String getResourceRelativePath() {
		return resourceRelativePath;
	}

	/**
	 * @param resourceRelativePath the resourceRelativePath to set
	 */
	public void setResourceRelativePath(String resourceRelativePath) {
		this.resourceRelativePath = resourceRelativePath;
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
	 * @return the storeName
	 */
	public String getStoreName() {
		return storeName;
	}

	/**
	 * @param storeName the storeName to set
	 */
	public void setStoreName(String storeName) {
		this.storeName = storeName;
	}


	
}
