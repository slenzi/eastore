/**
 * 
 */
package org.eamrf.repository.oracle.ecoguser.eastore.model;

/**
 * @author slenzi
 */
public class DirectoryResource extends PathResource {

	private static final long serialVersionUID = -417293061097239790L;

	public DirectoryResource() {
		
	}

	//
	// Just a stub for now, but may contain data elements in the future.
	// For now all elements needed for a directory are in the parent
	// PathResource class.
	//
	
	@Override
	public String toString() {
		return "DirectoryResource [nodeId=" + getNodeId() + ", parentNodeId=" + getParentNodeId() + ", childNodeId=" + getChildNodeId()
				+ ", nodeName=" + getNodeName() + ", dateCreated=" + getDateCreated() + ", dateUpdated=" + getDateUpdated()
				+ ", pathName=" + getPathName() + ", relativePath=" + getRelativePath() + ", resourceType=" + getResourceType().getTypeString()
				+ ", storeId=" + getStoreId() + "]";
	}	

}
