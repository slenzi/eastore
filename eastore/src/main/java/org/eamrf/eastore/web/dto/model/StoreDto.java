/**
 * 
 */
package org.eamrf.eastore.web.dto.model;

import java.nio.file.Path;
import java.sql.Timestamp;

import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store.AccessRule;

/**
 * @author slenzi
 *
 */
public class StoreDto {

	private Long id = Store.NO_FILE_SIZE_LIMIT;
	
	// name (must be unique)
	private String name = null;
	
	// description
	private String description = null;
	
	// full path of the store
	private Path path = null;
	
	// id of the root node of the store
	private Long nodeId = null;
	
	// max file size in bytes for which files can be store in the database
	private Long maxFileSizeBytes = 52428800L; // default to 50 megabytes
	
	// access rule
	private AccessRule accessRule = AccessRule.DENY;
	
	// date/time store was created
	private Timestamp dateCreated = null;
	
	// date/time store was last updated
	private Timestamp dateUpdated = null;
	
	private DirectoryResourceDto rootDir = null;
	
	public StoreDto() {
		
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the path
	 */
	public Path getPath() {
		return path;
	}

	/**
	 * @param path the path to set
	 */
	public void setPath(Path path) {
		this.path = path;
	}

	/**
	 * @return the nodeId
	 */
	public Long getNodeId() {
		return nodeId;
	}

	/**
	 * @param nodeId the nodeId to set
	 */
	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * @return the maxFileSizeBytes
	 */
	public Long getMaxFileSizeBytes() {
		return maxFileSizeBytes;
	}

	/**
	 * @param maxFileSizeBytes the maxFileSizeBytes to set
	 */
	public void setMaxFileSizeBytes(Long maxFileSizeBytes) {
		this.maxFileSizeBytes = maxFileSizeBytes;
	}

	/**
	 * @return the accessRule
	 */
	public AccessRule getAccessRule() {
		return accessRule;
	}

	/**
	 * @param accessRule the accessRule to set
	 */
	public void setAccessRule(AccessRule accessRule) {
		this.accessRule = accessRule;
	}

	/**
	 * @return the dateCreated
	 */
	public Timestamp getDateCreated() {
		return dateCreated;
	}

	/**
	 * @param dateCreated the dateCreated to set
	 */
	public void setDateCreated(Timestamp dateCreated) {
		this.dateCreated = dateCreated;
	}

	/**
	 * @return the dateUpdated
	 */
	public Timestamp getDateUpdated() {
		return dateUpdated;
	}

	/**
	 * @param dateUpdated the dateUpdated to set
	 */
	public void setDateUpdated(Timestamp dateUpdated) {
		this.dateUpdated = dateUpdated;
	}

	/**
	 * @return the rootDir
	 */
	public DirectoryResourceDto getRootDir() {
		return rootDir;
	}

	/**
	 * @param rootDir the rootDir to set
	 */
	public void setRootDir(DirectoryResourceDto rootDir) {
		this.rootDir = rootDir;
	}
	
}
