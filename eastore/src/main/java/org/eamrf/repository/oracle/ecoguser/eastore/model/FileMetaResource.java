package org.eamrf.repository.oracle.ecoguser.eastore.model;

/**
 * Model for file metadata. All data for a file except for the binary data.
 * 
 * @author slenzi
 */
public class FileMetaResource extends PathResource {

	private static final long serialVersionUID = 5490626976580639467L;
	
	private Long fileSize = 0L;
	private String mimeType = null;
	private Boolean isBinaryInDatabase = false;
	private BinaryResource binaryResource = null;
	
	public FileMetaResource() {
		
	}

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

}
