package org.eamrf.eastore.web.dto.model;


public class FileMetaResourceDto extends PathResourceDto {

	private Long fileSize = 0L;
	private String mimeType = null;
	private Boolean isBinaryInDatabase = false;
	
	// no need to marshal the binary data into a dto 
	// private BinaryResource binaryResource = null;
	
	private DirectoryResourceDto directory = null;
	
	public FileMetaResourceDto() {
		
	}

	/**
	 * @return the fileSize
	 */
	public Long getFileSize() {
		return fileSize;
	}

	/**
	 * @param fileSize the fileSize to set
	 */
	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}

	/**
	 * @return the mimeType
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * @param mimeType the mimeType to set
	 */
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	/**
	 * @return the isBinaryInDatabase
	 */
	public Boolean getIsBinaryInDatabase() {
		return isBinaryInDatabase;
	}

	/**
	 * @param isBinaryInDatabase the isBinaryInDatabase to set
	 */
	public void setIsBinaryInDatabase(Boolean isBinaryInDatabase) {
		this.isBinaryInDatabase = isBinaryInDatabase;
	}

	/**
	 * @return the directory
	 */
	public DirectoryResourceDto getDirectory() {
		return directory;
	}

	/**
	 * @param directory the directory to set
	 */
	public void setDirectory(DirectoryResourceDto directory) {
		this.directory = directory;
	}


	
}
