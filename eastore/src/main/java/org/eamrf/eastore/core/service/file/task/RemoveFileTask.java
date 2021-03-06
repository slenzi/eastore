package org.eamrf.eastore.core.service.file.task;

import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.service.file.ErrorHandler;
import org.eamrf.eastore.core.service.file.FileService;
import org.eamrf.eastore.core.service.file.PermissionError;
import org.eamrf.eastore.core.socket.messaging.ResourceChangeMessageService;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.FileSystemRepository;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.DirectoryResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.FileMetaResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task for removing a file
 * 
 * @author slenzi
 *
 */
public class RemoveFileTask extends FileServiceTask<Void> {

	private Logger logger = LoggerFactory.getLogger(RemoveFileTask.class);
	
	private FileMetaResource file;
	private String userId;
	private FileSystemRepository fileSystemRepository;
	private ResourceChangeMessageService resChangeService;
	private FileService fileService;
	private ErrorHandler errorHandler;
	
	private int jobCount = 0;
	
	public RemoveFileTask(
			FileMetaResource file,
			String userId,
			FileSystemRepository fileSystemRepository,
			ResourceChangeMessageService resChangeService,
			FileService fileService,
			ErrorHandler errorHandler) {
		
		this.file = file;
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
	public Void doWork() throws ServiceException {
		
		calculateJobCount();
		
		DirectoryResource parentDir = fileService.getParentDirectory(file.getNodeId(), userId);
		file.setDirectory(parentDir);
		
		if(!parentDir.getCanRead()) {
			errorHandler.handlePermissionDenied(PermissionError.READ, parentDir, userId);
		}		
		if(!parentDir.getCanWrite()) {
			errorHandler.handlePermissionDenied(PermissionError.WRITE, parentDir, userId);
		}		
		
		try {
			fileSystemRepository.removeFile(file);
		} catch (Exception e) {
			throw new ServiceException("Error removing file with node id => " + file.getNodeId() + ". " + e.getMessage(), e);
		}
		
		setCompletedJobCount(this, 1);
		
		resChangeService.directoryContentsChanged(file.getDirectory().getNodeId(), userId);
		
		// TODO - remove from lucene index!
		
		return null;
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
			return "Remove file task pending...";
		}else{
			return "Remove file task is " + Math.round(getProgress()) + "% complete (job " + this.getCompletedJobCount() + " of " + this.getJobCount() + " processed)";
		}		

	}	

	@Override
	public String getUserId() {
		return userId;
	}
	
}
