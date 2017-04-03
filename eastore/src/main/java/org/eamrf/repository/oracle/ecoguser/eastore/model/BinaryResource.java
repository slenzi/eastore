/**
 * 
 */
package org.eamrf.repository.oracle.ecoguser.eastore.model;

import java.io.Serializable;

/**
 * Model for binary data
 * 
 * @author slenzi
 */
public class BinaryResource implements Serializable {

	private static final long serialVersionUID = -3547703469344969123L;
	
	private Long nodeId = -1L;
	private byte[] fileData;
	
	public BinaryResource() {
		
	}

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public byte[] getFileData() {
		return fileData;
	}

	public void setFileData(byte[] fileData) {
		this.fileData = fileData;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
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
		BinaryResource other = (BinaryResource) obj;
		if (nodeId == null) {
			if (other.nodeId != null)
				return false;
		} else if (!nodeId.equals(other.nodeId))
			return false;
		return true;
	}

}
