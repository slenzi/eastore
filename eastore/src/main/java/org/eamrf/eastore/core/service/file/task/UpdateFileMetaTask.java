package org.eamrf.eastore.core.service.file.task;

import org.eamrf.concurrent.task.AbstractQueuedTask;
import org.eamrf.concurrent.task.QueuedTaskManager;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.search.service.StoreIndexerService;
import org.eamrf.eastore.core.socket.messaging.ResourceChangeService;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.FileSystemRepository;
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
public class UpdateFileMetaTask extends AbstractQueuedTask<Void> {

	private Logger logger = LoggerFactory.getLogger(UpdateFileMetaTask.class);
	
	private FileMetaResource file;
	private String newName = null;
	private String newDesc = null;
	
	private StoreIndexerService indexerService = null;
	private FileSystemRepository fileSystemRepository;
	private ResourceChangeService resChangeService;
	private QueuedTaskManager indexWriterTaskManager;
	
	public static class Builder {
	
		private FileMetaResource file;
		private String newName = null;
		private String newDesc = null;
		
		private StoreIndexerService indexerService = null;
		private FileSystemRepository fileSystemRepository;
		private ResourceChangeService resChangeService;
		private QueuedTaskManager indexWriterTaskManager;
		
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
		
		public UpdateFileMetaTask build() {
			return new UpdateFileMetaTask(this);
		}
	}
	
	private UpdateFileMetaTask(Builder builder) {
		this.file = builder.file;
		this.newName = builder.newName;
		this.newDesc = builder.newDesc;
		this.indexerService = builder.indexerService;
		this.fileSystemRepository = builder.fileSystemRepository;
		this.resChangeService = builder.resChangeService;
		this.indexWriterTaskManager = builder.indexWriterTaskManager;
	}

	@Override
	public Void doWork() throws ServiceException {

		// update file
		try {
			fileSystemRepository.updateFile(file, newName, newDesc);
		} catch (Exception e) {
			throw new ServiceException("Error updating file with node id => " + file.getNodeId() + ". " + e.getMessage(), e);
		}			
		
		// broadcast resource change message
		resChangeService.directoryContentsChanged(file.getDirectory().getNodeId());
		
		// Child task for adding file to lucene index
		AddFileToSearchIndexTask indexTask = new AddFileToSearchIndexTask.Builder()
				.withResource(file)
				.withIndexer(indexerService)
				.withHaveExisting(true)
				.withTaskName("Index Writer Task [" + file.toString() + "]")
				.build();
		
		indexWriterTaskManager.addTask(indexTask);				
		
		return null;		
		
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

}
