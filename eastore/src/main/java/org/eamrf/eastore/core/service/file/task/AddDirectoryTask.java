/**
 * 
 */
package org.eamrf.eastore.core.service.file.task;

import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.service.file.ErrorHandler;
import org.eamrf.eastore.core.service.file.FileService;
import org.eamrf.eastore.core.service.file.PermissionError;
import org.eamrf.eastore.core.socket.messaging.ResourceChangeMessageService;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.FileSystemRepository;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.DirectoryResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task for adding new directory
 * 
 * @author slenzi
 */
public class AddDirectoryTask extends FileServiceTask<DirectoryResource> {

	private Logger logger = LoggerFactory.getLogger(AddDirectoryTask.class);
	
	private DirectoryResource parentDir;
	private String name; 
	private String desc;
	private String readGroup1;
	private String writeGroup1;
	private String executeGroup1;
	private String userId;
	private FileSystemRepository fileSystemRepository;
	private ResourceChangeMessageService resChangeService;
	private FileService fileService;
	private ErrorHandler errorHandler;
	
	private int jobCount = 0;
	
	public AddDirectoryTask(
			DirectoryResource parentDir, 
			String name, 
			String desc, 
			String readGroup1, 
			String writeGroup1, 
			String executeGroup1,
			String userId,
			FileSystemRepository fileSystemRepository,
			ResourceChangeMessageService resChangeService,
			FileService fileService,
			ErrorHandler errorHandler) {
	
		this.parentDir = parentDir;
		this.name = name;
		this.desc = desc;
		this.readGroup1 = readGroup1;
		this.writeGroup1 = writeGroup1;
		this.executeGroup1 =executeGroup1;
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
	
	@Override
	public DirectoryResource doWork() throws ServiceException {

		calculateJobCount();
		
		// user must have write permission on parent directory
		if(!parentDir.getCanWrite()) {
			errorHandler.handlePermissionDenied(PermissionError.WRITE, parentDir, userId);
		}		
		
		DirectoryResource dirResource = null;
		try {
			dirResource = fileSystemRepository.addDirectory(parentDir, name, desc, readGroup1, writeGroup1, executeGroup1);
		} catch (Exception e) {
			throw new ServiceException("Error adding new subdirectory to directory " + parentDir.getNodeId(), e);
		}
		
		// after we create the directory we need to fetch it in order to have the permissions (read, write, & execute bits) properly evaluated.
		DirectoryResource evaluatedDir = fileService.getDirectory(dirResource.getNodeId(), userId);
		
		setCompletedJobCount(this, 1);
		
		// broadcast resource change message
		resChangeService.directoryContentsChanged(parentDir.getNodeId(), userId);
		
		return evaluatedDir;		
		
	}	

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
		
		if(getJobCount() <= 0) {
			return "Add directory task pending...";
		}else{
			return "Add directory task is " + Math.round(getProgress()) + "% complete (job " + this.getCompletedJobCount() + " of " + this.getJobCount() + " processed)";
		}
		
	}

	@Override
	public String getUserId() {
		return userId;
	}

}
