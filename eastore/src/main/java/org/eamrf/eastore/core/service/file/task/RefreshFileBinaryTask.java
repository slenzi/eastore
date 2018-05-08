/**
 * 
 */
package org.eamrf.eastore.core.service.file.task;

import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.FileSystemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A task which refreshes the binary data in the database for the file.
 * 
 * @author slenzi
 */
public class RefreshFileBinaryTask extends FileServiceTask<Void> {

	private Logger logger = LoggerFactory.getLogger(RefreshFileBinaryTask.class);
	
	private Long fileNodeId;
	private FileSystemRepository fileSystemRepository;
	
	private int jobCount = 1;
	
	/**
	 * 
	 */
	public RefreshFileBinaryTask(Long fileNodeId, FileSystemRepository fileSystemRepository) {
		this.fileNodeId = fileNodeId;
		this.fileSystemRepository = fileSystemRepository;
	}

	/* (non-Javadoc)
	 * @see org.eamrf.concurrent.task.AbstractQueuedTask#doWork()
	 */
	@Override
	public Void doWork() throws ServiceException {
		try {
			fileSystemRepository.refreshBinaryDataInDatabase(fileNodeId);
		} catch (Exception e) {
			throw new ServiceException("Error refreshing (or adding) binary data in database (eas_binary_resource) "
					+ "for file resource node => " + fileNodeId, e);
		}
		incrementJobsCompleted();
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eamrf.concurrent.task.AbstractQueuedTask#getLogger()
	 */
	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public int getJobCount() {
		return jobCount;
	}

}
