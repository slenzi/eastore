/**
 * 
 */
package org.eamrf.eastore.core.service.file.task;

import org.eamrf.concurrent.task.AbstractQueuedTask;
import org.eamrf.core.util.StringUtil;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.service.file.ErrorHandler;
import org.eamrf.eastore.core.service.file.FileService;
import org.eamrf.eastore.core.service.file.PermissionError;
import org.eamrf.eastore.core.socket.messaging.ResourceChangeService;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.FileSystemRepository;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.DirectoryResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author slenzi
 *
 */
public class UpdateDirectoryTask extends FileServiceTask<Void> {

	private Logger logger = LoggerFactory.getLogger(UpdateDirectoryTask.class);
	
	private DirectoryResource dir;
	private String name;
	private String desc;
	private String readGroup1;
	private String writeGroup1;
	private String executeGroup1;
	private String userId;
	private FileSystemRepository fileSystemRepository;
	private ResourceChangeService resChangeService;
	private FileService fileService;
	private ErrorHandler errorHandler;
	
	private int jobCount = -1;
	
	/**
	 * 
	 */
	public UpdateDirectoryTask(
			DirectoryResource dir,
			String name,
			String desc,
			String readGroup1,
			String writeGroup1,
			String executeGroup1,
			String userId,
			FileSystemRepository fileSystemRepository,
			ResourceChangeService resChangeService,
			FileService fileService,
			ErrorHandler errorHandler) {
		
		this.dir = dir;
		this.name = name;
		this.desc = desc;
		this.readGroup1 = readGroup1;
		this.writeGroup1 = writeGroup1;
		this.executeGroup1 = executeGroup1;
		this.userId = userId;
		this.fileSystemRepository = fileSystemRepository;
		this.resChangeService = resChangeService;
		this.fileService = fileService;
		this.errorHandler = errorHandler;
		
		//notifyChange();
		
	}
	
	private void calculateJobCount() {
		
		jobCount = 1;
		
		notifyChange();
		
	}	

	/* (non-Javadoc)
	 * @see org.eamrf.concurrent.task.AbstractQueuedTask#doWork()
	 */
	@Override
	public Void doWork() throws ServiceException {

		calculateJobCount();
		
		// need execute & write permission on directory
		if(!dir.getCanExecute()) {
			logger.info("No execute permission on directory");
			errorHandler.handlePermissionDenied(PermissionError.EXECUTE, dir, userId);
		}
		if(!dir.getCanWrite()) {
			logger.info("No write permission on directory");
			errorHandler.handlePermissionDenied(PermissionError.WRITE, dir, userId);
		}		
		
		// also need read & write permission on parent directory, if one exists
		DirectoryResource parentDir = fileService.getParentDirectory(dir.getNodeId(), userId);
		if(parentDir != null) {
			if(!parentDir.getCanRead()) {
				logger.info("No read permission on parent");
				errorHandler.handlePermissionDenied(PermissionError.READ, parentDir, userId);
			}
			if(!parentDir.getCanWrite()) {
				logger.info("No write permission on parent");
				errorHandler.handlePermissionDenied(PermissionError.WRITE, parentDir, userId);
			}
		}else {
			// no parent directory, so this must be a root directory for a store.
			// read, write, and execute groups are required for root directories.
			if(StringUtil.isAnyNullEmpty(readGroup1, writeGroup1, executeGroup1)) {
				throw new ServiceException("Read, write, and execute groups are required for root directories (directories that have no parent.) "
						+ "One or all of the values for read, write, and execute groups are null or blank.");
			}			
		}		
		
		// update directory
		try {
			fileSystemRepository.updateDirectory(dir, name, desc, readGroup1, writeGroup1, executeGroup1);
		} catch (Exception e) {
			throw new ServiceException("Error updating directory with node id => " + dir.getNodeId() + ". " + e.getMessage(), e);
		}
		
		setCompletedJobCount(getTaskId(), 1);
		
		// won't have a parent dir if this is a root directory for a store
		if(parentDir != null) {
			resChangeService.directoryContentsChanged(parentDir.getNodeId(), userId);
		}
		
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

	@Override
	public String getStatusMessage() {
		
		if(getJobCount() < 0) {
			return "Update directory task pending...";
		}else{
			return "Update directory task is " + Math.round(getProgress()) + "% complete (job " + this.getCompletedJobCount() + " of " + this.getJobCount() + " processed)";
		}		

	}	
	
	@Override
	public String getUserId() {
		return userId;
	}
	
}
