package org.eamrf.eastore.web.dto.model;

import java.sql.Timestamp;

public class NodeDto {

	private Long nodeId = -1L;
	private Long parentNodeId = -1L;
	private Long childNodeId = -1L;
	private String nodeName = null;
	private Timestamp dateCreated = null;
	private Timestamp dateUpdated = null;	
	
	public NodeDto() {
		
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
	 * @return the parentNodeId
	 */
	public Long getParentNodeId() {
		return parentNodeId;
	}

	/**
	 * @param parentNodeId the parentNodeId to set
	 */
	public void setParentNodeId(Long parentNodeId) {
		this.parentNodeId = parentNodeId;
	}

	/**
	 * @return the childNodeId
	 */
	public Long getChildNodeId() {
		return childNodeId;
	}

	/**
	 * @param childNodeId the childNodeId to set
	 */
	public void setChildNodeId(Long childNodeId) {
		this.childNodeId = childNodeId;
	}

	/**
	 * @return the nodeName
	 */
	public String getNodeName() {
		return nodeName;
	}

	/**
	 * @param nodeName the nodeName to set
	 */
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
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


	
}
