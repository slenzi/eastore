/**
 * 
 */
package org.eamrf.eastore.core.service.file.task;

import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.service.file.ErrorHandler;
import org.eamrf.eastore.core.service.file.FileService;
import org.eamrf.eastore.core.service.file.PermissionError;
import org.eamrf.eastore.core.socket.messaging.ResourceChangeService;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.FileSystemRepository;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.DirectoryResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.FileMetaResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author slenzi
 *
 */
public class MoveFileTask extends FileServiceTask<Void> {

	private Logger logger = LoggerFactory.getLogger(MoveFileTask.class);
	
	private String userId;
	private FileMetaResource fileToMove;
	private DirectoryResource destDir;
	private boolean replaceExisting;
	private FileSystemRepository fileSystemRepository;
	private ResourceChangeService resChangeService;
	private ErrorHandler errorHandler;
	
	private FileService fileService;
	
	private int jobCount = -1;
	
	/**
	 * 
	 */
	public MoveFileTask(
			String userId,
			FileMetaResource fileToMove,
			DirectoryResource destDir,
			boolean replaceExisting,
			FileSystemRepository fileSystemRepository,
			ResourceChangeService resChangeService,
			ErrorHandler errorHandler,
			FileService fileService) {
		
		this.userId = userId;
		this.fileToMove = fileToMove;
		this.destDir = destDir;
		this.replaceExisting = replaceExisting;
		this.fileSystemRepository = fileSystemRepository;
		this.resChangeService = resChangeService;
		this.errorHandler = errorHandler;
		this.fileService = fileService;
		
		notifyProgressChange();
		
	}
	
	private void calculateJobCount() {
		
		jobCount = 1;
		
		notifyProgressChange();
		
	}

	/* (non-Javadoc)
	 * @see org.eamrf.concurrent.task.AbstractQueuedTask#doWork()
	 */
	@Override
	public Void doWork() throws ServiceException {

		calculateJobCount();
		
		// user must have write access on destination directory
		if(!destDir.getCanWrite()){
			errorHandler.handlePermissionDenied(PermissionError.WRITE, destDir, userId);
		}
		
		// user must have read and write on parent directory of file being moved
		DirectoryResource sourceDir = fileService.getParentDirectory(fileToMove.getChildNodeId(), userId);	
		if(!sourceDir.getCanRead()) {
			errorHandler.handlePermissionDenied(PermissionError.READ, sourceDir, userId);
		}
		if(!sourceDir.getCanWrite()) {
			errorHandler.handlePermissionDenied(PermissionError.WRITE, sourceDir, userId);
		}
		
		try {
			fileSystemRepository.moveFile(fileToMove, destDir, replaceExisting);
		} catch (Exception e) {
			throw new ServiceException("Error moving file " + fileToMove.getNodeId() + " to directory " + 
					destDir.getNodeId() + ", replaceExisting = " + replaceExisting + ". " + e.getMessage(), e);
		}
		
		setCompletedJobCount(1);
		
		// TODO - do we need to update the lucene search index?
		
		// TODO - if you add a child task for updating lucene, don't forget to increment the job count (and for other parent tasks like MoveDirectoryTask)
		
		// broadcast resource change message
		resChangeService.directoryContentsChanged(sourceDir.getNodeId(), userId);
		resChangeService.directoryContentsChanged(destDir.getNodeId(), userId);
		
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
			return "Move file task pending...";
		}else{
			return "Move file task is " + Math.round(getProgress()) + "% complete (job " + this.getCompletedJobCount() + " of " + this.getJobCount() + " processed)";
		}		
	
	}	

	@Override
	public String getUserId() {
		return userId;
	}
	
}
