package org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl;

/**
 * Model for file metadata. All data for a file except for the binary data.
 * 
 * @author slenzi
 */
public class FileMetaResource extends PathResource {

	private static final long serialVersionUID = 5490626976580639467L;
	
	// file size in bytes
	private Long fileSize = 0L;
	
	// file mime type
	private String mimeType = null;
	
	// will be true if the binary data for the file is in the database (blob), false otherwise
	private Boolean isBinaryInDatabase = false;
	
	// the binary data for the file
	private BinaryResource binaryResource = null;
	
	// optional reference to the directory that the file is in
	private DirectoryResource directory = null;
	
	public FileMetaResource() { }

	public Long getFileSize() {
		return fileSize;
	}

	public String getMimeType() {
		return mimeType;
	}

	public Boolean getIsBinaryInDatabase() {
		return isBinaryInDatabase;
	}

	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public void setIsBinaryInDatabase(Boolean isBinaryInDatabase) {
		this.isBinaryInDatabase = isBinaryInDatabase;
	}

	public BinaryResource getBinaryResource() {
		return binaryResource;
	}

	public void setBinaryResource(BinaryResource binaryResource) {
		this.binaryResource = binaryResource;
	}
	
	/**
	 * @return the directory that the file is in
	 */
	public DirectoryResource getDirectory() {
		return directory;
	}

	/**
	 * @param directory - the directory that the file is in
	 */
	public void setDirectory(DirectoryResource directory) {
		this.directory = directory;
	}

	@Override
	public String toString() {
		return FileMetaResource.class.getSimpleName() + " [id=" + getNodeId() + ", parentId=" + getParentNodeId()
				+ ", childId=" + getChildNodeId() + ", name=" + getNodeName() + ", dtCreated="
				+ getDateCreated() + ", dtUpdated=" + getDateUpdated() + ", pathName=" + getPathName()
				+ ", relPath=" + getRelativePath() + ", type=" + getResourceType().getTypeString()
				+ ", storeId=" + getStoreId() + ", size=" + getFileSize() + ", mime=" + getMimeType() 
				+ ", isBinInDb=" + isBinaryInDatabase + ", canRead=" + getCanRead() + ", canWrite=" + getCanWrite() + ", canExecute=" + getCanExecute() + "]";
	}	
	
}
