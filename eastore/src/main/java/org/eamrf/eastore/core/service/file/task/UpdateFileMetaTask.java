package org.eamrf.eastore.core.service.file.task;

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
 * A task for updating attributes of a file, e.g., name, description, tags, ect. Not for updating the file
 * binary data.
 * 
 * @author slenzi
 *
 */
public class UpdateFileMetaTask extends FileServiceTask<Void> {

	private Logger logger = LoggerFactory.getLogger(UpdateFileMetaTask.class);
	
	private FileMetaResource file;
	private String newName = null;
	private String newDesc = null;
	private String userId = null;
	
	private StoreIndexerService indexerService = null;
	private FileSystemRepository fileSystemRepository;
	private ResourceChangeService resChangeService;
	private QueuedTaskManager indexWriterTaskManager;
	private FileService fileService;
	private ErrorHandler errorHandler;
	
	private int jobCount = -1;
	
	public static class Builder {
	
		private FileMetaResource file;
		private String newName = null;
		private String newDesc = null;
		private String userId;
		
		private StoreIndexerService indexerService = null;
		private FileSystemRepository fileSystemRepository;
		private ResourceChangeService resChangeService;
		private QueuedTaskManager indexWriterTaskManager;
		private FileService fileService;
		private ErrorHandler errorHandler;		
		
		public Builder(FileMetaResource file) {
			this.file = file;
			this.newName = file.getPathName();
			this.newDesc = file.getDesc();
		}
		
		public Builder withNewName(String newName) {
			this.newName = newName;
			return this;
		}
		
		public Builder withNewDesc(String newDesc) {
			this.newDesc = newDesc;
			return this;
		}
		
		public Builder withUserId(String userId) {
			this.userId = userId;
			return this;
		}
		
		public Builder withIndexer(StoreIndexerService indexerService) {
			this.indexerService = indexerService;
			return this;
		}
		
		public Builder withFileRepository(FileSystemRepository fileSystemRepository) {
			this.fileSystemRepository = fileSystemRepository;
			return this;
		}
		
		public Builder withResourceChangeService(ResourceChangeService resChangeService) {
			this.resChangeService = resChangeService;
			return this;
		}
		
		public Builder withIndexWriterTaskManager(QueuedTaskManager indexWriterTaskManager) {
			this.indexWriterTaskManager = indexWriterTaskManager;
			return this;
		}
		
		public Builder withFileService(FileService fileService) {
			this.fileService = fileService;
			return this;
		}
		
		public Builder withErrorHandler(ErrorHandler errorHandler) {
			this.errorHandler = errorHandler;
			return this;
		}
		
		public UpdateFileMetaTask build() {
			return new UpdateFileMetaTask(this);
		}
	}
	
	private UpdateFileMetaTask(Builder builder) {
		
		this.file = builder.file;
		this.newName = builder.newName;
		this.newDesc = builder.newDesc;
		this.userId = builder.userId;
		this.indexerService = builder.indexerService;
		this.fileSystemRepository = builder.fileSystemRepository;
		this.resChangeService = builder.resChangeService;
		this.indexWriterTaskManager = builder.indexWriterTaskManager;
		this.fileService = builder.fileService;
		this.errorHandler = builder.errorHandler;
		
		//notifyChange();
		
	}
	
	private void calculateJobCount() {
		
		// 1 = update file
		// 2 = update lucene		
		jobCount = 2;
		
		notifyChange();
		
	}	

	@Override
	public Void doWork() throws ServiceException {

		calculateJobCount();
		
		// need execute & write permission on file
		if(!file.getCanExecute()) {
			logger.info("No execute permission");
			errorHandler.handlePermissionDenied(PermissionError.EXECUTE, file, userId);
		}
		if(!file.getCanWrite()) {
			logger.info("No write permission");
			errorHandler.handlePermissionDenied(PermissionError.WRITE, file, userId);
		}		
		
		// also need read & write permission on parent directory, if one exists
		DirectoryResource parentDir = fileService.getParentDirectory(file.getNodeId(), userId);
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
			throw new ServiceException("Error fetching parent directory file file resource with node id = " + file.getNodeId());		
		}
		
		// set directory so we can store directory related meta data in lucene
		file.setDirectory(parentDir);		
		
		// update file
		try {
			fileSystemRepository.updateFile(file, newName, newDesc);
		} catch (Exception e) {
			throw new ServiceException("Error updating file with node id => " + file.getNodeId() + ". " + e.getMessage(), e);
		}
		
		setCompletedJobCount(getTaskId(), 1);
		
		// Child task for adding file to lucene index
		AddFileToSearchIndexTask indexTask = new AddFileToSearchIndexTask.Builder()
				.withUserId(userId)
				.withResource(file)
				.withIndexer(indexerService)
				.withHaveExisting(true)
				.withTaskName("Index Writer Task [" + file.toString() + "]")
				.build();
		indexTask.registerProgressListener(task -> {
			setCompletedJobCount(task.getTaskId(), task.getCompletedJobCount());
		});
		
		indexWriterTaskManager.addTask(indexTask);
		
		// broadcast resource change message
		resChangeService.directoryContentsChanged(file.getDirectory().getNodeId(), userId);		
		
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
		
		if(getJobCount() < 0) {
			return "Update file task pending...";
		}else{
			return "Update file task is " + Math.round(getProgress()) + "% complete (job " + this.getCompletedJobCount() + " of " + this.getJobCount() + " processed)";
		}		

	}		

	@Override
	public String getUserId() {
		return userId;
	}
	
}
