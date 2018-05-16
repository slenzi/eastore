package org.eamrf.eastore.core.service.file.task;

import java.nio.file.Path;

import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.service.file.ErrorHandler;
import org.eamrf.eastore.core.service.file.FileService;
import org.eamrf.eastore.core.service.file.PermissionError;
import org.eamrf.eastore.core.service.tree.file.PathResourceUtil;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.DirectoryResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.FileMetaResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task for copying a file
 * 
 * @author slenzi
 */
public class CopyFileTask extends FileServiceTask<Void> {

	private Logger logger = LoggerFactory.getLogger(CopyFileTask.class);
	
	private FileMetaResource fileToCopy;
	private DirectoryResource toDir;
	private boolean replaceExisting;
	private String userId;
	private FileService fileService;
	private ErrorHandler errorHandler;
	
	private int jobCount = 0;
	
	public CopyFileTask(
			FileMetaResource fileToCopy, DirectoryResource toDir, boolean replaceExisting, String userId,
			FileService fileService, ErrorHandler errorHandler) {
		
		this.fileToCopy = fileToCopy;
		this.toDir = toDir;
		this.replaceExisting = replaceExisting;
		this.userId = userId;
		this.fileService = fileService;
		this.errorHandler = errorHandler;
		
		//notifyChange();
		
	}
	
	private void calculateJobCount() {
		
		jobCount = 3;
		
		notifyChange();
		
	}

	@Override
	public Void doWork() throws ServiceException {

		calculateJobCount();
		
		// user must have read on parent directory
		// file resource inherits permission from parent directory, so this works.
		if(!fileToCopy.getCanRead()) {
			errorHandler.handlePermissionDenied(PermissionError.READ, fileToCopy, userId);
		}
		
		Store soureStore = fileService.getStore(fileToCopy, userId);
		Path sourceFilePath = PathResourceUtil.buildPath(soureStore, fileToCopy);
		
		// can't copy a file to the directory it's already in
		if(fileToCopy.getParentNodeId().equals(toDir.getNodeId())){
			throw new ServiceException("You cannot copy a file to the directory that it's already in. "
					+ "fileNodeId => " + fileToCopy.getNodeId() + ", dirNodeId => " + toDir.getNodeId());
		}
		
		fileService.addFile(toDir, sourceFilePath, replaceExisting, userId, task -> {
			setCompletedJobCount(task, task.getCompletedJobCount());
		});
		
		// TODO - consider the idea of adding a new field to eas_path_resource called "is_locked" which can be set to Y/N.
		// If the path resource is locked then no update operations (delete, move, update, copy, etc) can be performed.
		// we can lock a file meta resource right when we add it, then unlock it after we refresh the binary data.
		
		// TODO - do we want to block for updating binary data in the database?  Uhg!
		// If we don't block then it's possible for one of those update tasks to fail (someone else might
		// delete a file before the update process runs.)  We should make those tasks fail gracefully		
		
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
			return "Copy file task pending...";
		}else{
			return "Copy file task is " + Math.round(getProgress()) + "% complete (job " + this.getCompletedJobCount() + " of " + this.getJobCount() + " processed)";
		}
		
	}
	
	@Override
	public String getUserId() {
		return userId;
	}	

}
