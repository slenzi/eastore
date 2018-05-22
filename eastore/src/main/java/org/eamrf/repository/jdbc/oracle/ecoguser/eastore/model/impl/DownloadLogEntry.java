/**
 * 
 */
package org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl;

import java.io.Serializable;
import java.nio.file.Path;
import java.sql.Timestamp;

/**
 * @author slenzi
 *
 */
public class DownloadLogEntry implements Serializable {

	private static final long serialVersionUID = -3063506652744501466L;

	private Long downloadId = null;
	private Path filePath = null;
	private String userId = null;
	private Timestamp downloadDate = null;
	
	/**
	 * 
	 */
	public DownloadLogEntry() {
		
	}

	/**
	 * @return the downloadId
	 */
	public Long getDownloadId() {
		return downloadId;
	}

	/**
	 * @param downloadId the downloadId to set
	 */
	public void setDownloadId(Long downloadId) {
		this.downloadId = downloadId;
	}

	/**
	 * @return the filePath
	 */
	public Path getFilePath() {
		return filePath;
	}

	/**
	 * @param filePath the filePath to set
	 */
	public void setFilePath(Path filePath) {
		this.filePath = filePath;
	}

	/**
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * @return the downloadDate
	 */
	public Timestamp getDownloadDate() {
		return downloadDate;
	}

	/**
	 * @param downloadDate the downloadDate to set
	 */
	public void setDownloadDate(Timestamp downloadDate) {
		this.downloadDate = downloadDate;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "DownloadLogEntry [downloadId=" + downloadId + ", filePath=" + filePath + ", userId=" + userId
				+ ", downloadDate=" + downloadDate + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((downloadId == null) ? 0 : downloadId.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DownloadLogEntry other = (DownloadLogEntry) obj;
		if (downloadId == null) {
			if (other.downloadId != null)
				return false;
		} else if (!downloadId.equals(other.downloadId))
			return false;
		return true;
	}


	
}
