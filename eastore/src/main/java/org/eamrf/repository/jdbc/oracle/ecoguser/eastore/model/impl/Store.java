/**
 * 
 */
package org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl;

import java.io.Serializable;
import java.nio.file.Path;
import java.sql.Timestamp;

/**
 * Model for a data store
 * 
 * @author slenzi
 */
public class Store implements Serializable {

	private static final long serialVersionUID = 2510915862464361107L;

	private Long id = -1L;
	private String name = null;
	private String description = null;
	private Path path = null;
	private Long nodeId = -1L;
	private Long maxFileSizeBytes = 52428800L; // default to 50 megabytes
	private Timestamp dateCreated = null;
	private Timestamp dateUpdated = null;
	
	private DirectoryResource rootDir = null;

	public Store() {
		
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Path getPath() {
		return path;
	}

	public Long getNodeId() {
		return nodeId;
	}

	public Long getMaxFileSizeBytes() {
		return maxFileSizeBytes;
	}

	public Timestamp getDateCreated() {
		return dateCreated;
	}

	public Timestamp getDateUpdated() {
		return dateUpdated;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setPath(Path path) {
		this.path = path;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public void setMaxFileSizeBytes(Long maxFileSizeBytes) {
		this.maxFileSizeBytes = maxFileSizeBytes;
	}

	public void setDateCreated(Timestamp dateCreated) {
		this.dateCreated = dateCreated;
	}

	public void setDateUpdated(Timestamp dateUpdated) {
		this.dateUpdated = dateUpdated;
	}

	public DirectoryResource getRootDir() {
		return rootDir;
	}

	public void setRootDir(DirectoryResource rootDir) {
		this.rootDir = rootDir;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Store other = (Store) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return Store.class.getSimpleName() + " [id=" + id + ", name=" + name + ", description=" + description + ", path=" + path + ", nodeId="
				+ nodeId + ", maxFileSizeBytes=" + maxFileSizeBytes + ", dateCreated=" + dateCreated + ", dateUpdated="
				+ dateUpdated + "]";
	}

	
	
}
