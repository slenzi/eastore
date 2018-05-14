/**
 * 
 */
package org.eamrf.eastore.core.service.file.task;

import java.nio.file.Path;

import org.eamrf.concurrent.task.QueuedTaskManager;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.search.service.StoreIndexerService;
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
 * Task for adding a file
 * 
 * @author slenzi
 */
public class AddFileTask extends FileServiceTask<FileMetaResource> {

	private Logger logger = LoggerFactory.getLogger(AddFileTask.class);
	
	private Path filePath;
	private boolean replaceExisting;
	private DirectoryResource toDir;
	private String userId;
	
	private FileSystemRepository fileSystemRepository;
	private ResourceChangeService resChangeService;
	private StoreIndexerService indexerService;
	private QueuedTaskManager binaryTaskManager;
	private QueuedTaskManager indexWriterTaskManager;
	private FileService fileService;
	private ErrorHandler errorHandler;
	
	private int jobCount = -1;
	
	public AddFileTask(
			Path filePath,
			boolean replaceExisting,
			DirectoryResource toDir,
			String userId,
			FileSystemRepository fileSystemRepository,
			ResourceChangeService resChangeService,
			StoreIndexerService indexerService,
			QueuedTaskManager binaryTaskManager,
			QueuedTaskManager indexWriterTaskManager,
			FileService fileService,
			ErrorHandler errorHandler) {
		
		this.filePath = filePath;
		this.replaceExisting = replaceExisting;
		this.toDir = toDir;
		this.userId = userId;
		this.fileSystemRepository = fileSystemRepository;
		this.resChangeService = resChangeService;
		this.indexerService = indexerService;
		this.binaryTaskManager = binaryTaskManager;
		this.indexWriterTaskManager = indexWriterTaskManager;
		this.fileService = fileService;
		this.errorHandler = errorHandler;
		
		notifyProgressChange();
		
	}
	
	private void calculateJobCount() {
		
		// 1 = add the file
		// 2 = update lucene
		// 3 = update binary data
		jobCount = 3;
	
		notifyProgressChange();
		
	}
	
	@Override
	public FileMetaResource doWork() throws ServiceException {

		calculateJobCount();
		
		logger.info("HERE 01 => " + this.getCompletedJobCount());
		
		// user must have write permission on destination directory
		if(!toDir.getCanWrite()){
			errorHandler.handlePermissionDenied(PermissionError.WRITE, toDir, userId);
		}		
		
		String fileName = filePath.getFileName().toString();
		FileMetaResource existingResource = fileService.getChildFileMetaResource(toDir.getNodeId(), fileName, userId);
		final boolean haveExisting = existingResource != null ? true : false;
		FileMetaResource newOrUpdatedFileResource = null;
		
		if(haveExisting && !replaceExisting) {
			throw new ServiceException(" Directory [id=" + toDir.getNodeId() + ", relPath=" + toDir.getRelativePath() + "] "
					+ "already contains a file with the name '" + fileName + "', and 'replaceExisting' param is set to false.");					
		}else if(haveExisting) {
			// update existing file (remove current binary data, and update existing file meta data)
			try {
				newOrUpdatedFileResource = fileSystemRepository._updateFileDiscardOldBinary(toDir, filePath, existingResource);
			} catch (Exception e) {
				throw new ServiceException("Error updating existing file [id=" + existingResource.getNodeId() + ", relPath=" + existingResource.getRelativePath() + "] in "
						+ "directory [id=" + toDir.getNodeId() + ", relPath=" + toDir.getRelativePath() + "], " + e.getMessage());
			}					
		}else {
			try {
				newOrUpdatedFileResource = fileSystemRepository._addNewFileWithoutBinary(toDir.getStore(), toDir, filePath);
			} catch (Exception e) {
				throw new ServiceException("Error adding new file " + filePath.toString() + " to "
						+ "directory [id=" + toDir.getNodeId() + ", relPath=" + toDir.getRelativePath() + "], " + e.getMessage());
			}					
		}
		
		logger.info("HERE 02 => " + this.getCompletedJobCount());
		
		// set the directory so we can store that information in the lucene index
		newOrUpdatedFileResource.setDirectory(toDir);
		newOrUpdatedFileResource.setStore(toDir.getStore());
		
		// job 1 of 3 complete
		setCompletedJobCount(1);
		
		logger.info("HERE 03 => " + this.getCompletedJobCount());
		
		// broadcast directory contents changed event
		resChangeService.directoryContentsChanged(toDir.getNodeId(), userId);
		
		logger.info("HERE 04 => " + this.getCompletedJobCount());

		// Child task for adding file to lucene index
		AddFileToSearchIndexTask indexTask = new AddFileToSearchIndexTask.Builder()
				.withUserId(userId)
				.withResource(newOrUpdatedFileResource)
				.withIndexer(indexerService)
				.withHaveExisting(haveExisting)
				.withTaskName("Index Writer Task [" + newOrUpdatedFileResource.toString() + "]")
				.build();
		indexTask.registerProgressListener(task -> {
			// job 2 of 3 complete
			logger.info("HERE 05 => " + this.getCompletedJobCount());
			setCompletedJobCount(getCompletedJobCount() + task.getCompletedJobCount());
		});
		indexWriterTaskManager.addTask(indexTask);
		
		// Child task refreshes the binary data in the database.
		RefreshFileBinaryTask refreshTask = new RefreshFileBinaryTask(
				newOrUpdatedFileResource.getNodeId(), userId, fileSystemRepository);
		refreshTask.setName("Refresh binary data in DB [" + newOrUpdatedFileResource.toString() + "]");
		refreshTask.registerProgressListener(task -> {
			// job 3 of 3 complete
			logger.info("HERE 06 => " + this.getCompletedJobCount());
			setCompletedJobCount(getCompletedJobCount() + task.getCompletedJobCount());
		});		
		binaryTaskManager.addTask(refreshTask);		
		
		logger.info("HERE 07 => " + this.getCompletedJobCount());
		
		return newOrUpdatedFileResource;		
		
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

		if(getJobCount() < 0) {
			return "Add file task pending...";
		}else{
			return "Add file task is " + Math.round(getProgress()) + "% complete (job " + this.getCompletedJobCount() + " of " + this.getJobCount() + " processed)";
		}
		
	}
	
	@Override
	public String getUserId() {
		return userId;
	}	

}
