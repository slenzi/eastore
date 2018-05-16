/**
 * 
 */
package org.eamrf.eastore.core.service.file.task;

import java.io.IOException;

import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.search.service.StoreIndexerService;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.FileMetaResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A task for adding a file to the lucene search index.
 * 
 * @author slenzi
 */
public class AddFileToSearchIndexTask extends FileServiceTask<Void> {

	private Logger logger = LoggerFactory.getLogger(AddFileToSearchIndexTask.class);
	
	private FileMetaResource documentToIndex = null;
	private StoreIndexerService indexerService = null;
	private boolean haveExisting = false;
	private String userId = null;
	
	private int jobCount = 0;
	
	public static class Builder {
	
		private FileMetaResource documentToIndex = null;
		private StoreIndexerService indexerService = null;
		private boolean haveExisting = false;
		private String taskName = null;
		private String userId = null;
		
		public Builder() { }
		
		public Builder withResource(FileMetaResource documentToIndex) {
			this.documentToIndex = documentToIndex;
			return this;
		}
		
		public Builder withIndexer(StoreIndexerService indexerService) {
			this.indexerService = indexerService;
			return this;
		}
		
		public Builder withHaveExisting(boolean haveExisting) {
			this.haveExisting = haveExisting;
			return this;
		}
		
		public Builder withTaskName(String taskName) {
			this.taskName = taskName;
			return this;
		}
		
		public Builder withUserId(String userId) {
			this.userId = userId;
			return this;
		}
		
		public AddFileToSearchIndexTask build() {
			return new AddFileToSearchIndexTask(this);
		}
		
	}
	
	private AddFileToSearchIndexTask(Builder builder) {
		
		this.documentToIndex = builder.documentToIndex;
		this.indexerService = builder.indexerService;
		this.haveExisting = builder.haveExisting;
		this.userId = builder.userId;
		
		super.setName(builder.taskName);
		
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
		
		try {
			if(haveExisting) {
				indexerService.getIndexerForStore(documentToIndex.getStore()).update(documentToIndex);
			}else {
				indexerService.getIndexerForStore(documentToIndex.getStore()).add(documentToIndex);
			}
			setCompletedJobCount(getTaskId(), 1);
		} catch (IOException e) {
			logger.error("Error adding/updating document in search index, " + e.getMessage());
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
		
		if(getJobCount() <= 0) {
			return "Add file to lucene index task pending...";
		}else{
			return "Add file to lucene index task is " + Math.round(getProgress()) + "% complete (job " + this.getCompletedJobCount() + " of " + this.getJobCount() + " processed)";
		}		

	}
	
	@Override
	public String getUserId() {
		return userId;
	}	

}
