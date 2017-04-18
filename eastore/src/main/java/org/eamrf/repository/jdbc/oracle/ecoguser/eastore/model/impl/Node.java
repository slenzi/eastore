package org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl;

import java.io.Serializable;
import java.sql.Timestamp;

public class Node implements Serializable {

	private static final long serialVersionUID = 648966529174347555L;
	
	private Long nodeId = -1L;
	private Long parentNodeId = -1L;
	private Long childNodeId = -1L;
	private String nodeName = null;
	private Timestamp dateCreated = null;
	private Timestamp dateUpdated = null;
	
	public Node() {
		
	}

	public Long getNodeId() {
		return nodeId;
	}

	public Long getParentNodeId() {
		return parentNodeId;
	}

	public String getNodeName() {
		return nodeName;
	}

	public Timestamp getDateCreated() {
		return dateCreated;
	}

	public Timestamp getDateUpdated() {
		return dateUpdated;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public void setParentNodeId(Long parentNodeId) {
		this.parentNodeId = parentNodeId;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public void setDateCreated(Timestamp dateCreated) {
		this.dateCreated = dateCreated;
	}

	public void setDateUpdated(Timestamp dateUpdated) {
		this.dateUpdated = dateUpdated;
	}

	public Long getChildNodeId() {
		return childNodeId;
	}

	public void setChildNodeId(Long childNodeId) {
		this.childNodeId = childNodeId;
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
		Node other = (Node) obj;
		if (nodeId == null) {
			if (other.nodeId != null)
				return false;
		} else if (!nodeId.equals(other.nodeId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return Node.class.getSimpleName() + " [nodeId=" + nodeId + ", parentNodeId=" + parentNodeId + ", childNodeId=" + childNodeId
				+ ", nodeName=" + nodeName + ", dateCreated=" + dateCreated + ", dateUpdated=" + dateUpdated + "]";
	}

}
