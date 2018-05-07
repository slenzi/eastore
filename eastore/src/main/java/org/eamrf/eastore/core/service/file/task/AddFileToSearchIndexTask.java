/**
 * 
 */
package org.eamrf.eastore.core.service.file.task;

import java.io.IOException;

import org.eamrf.concurrent.task.AbstractQueuedTask;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.search.service.StoreIndexerService;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.FileMetaResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A task for adding a file to the lucene search index.
 * 
 * @author slenzi
 */
public class AddFileToSearchIndexTask extends AbstractQueuedTask<Void> {

	private Logger logger = LoggerFactory.getLogger(AddFileToSearchIndexTask.class);
	
	private FileMetaResource documentToIndex = null;
	private StoreIndexerService indexerService = null;
	private boolean haveExisting = false;
	
	public static class Builder {
	
		private FileMetaResource documentToIndex = null;
		private StoreIndexerService indexerService = null;
		private boolean haveExisting = false;
		private String taskName = null;
		
		public Builder() {
			
		}
		
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
		
		public AddFileToSearchIndexTask build() {
			return new AddFileToSearchIndexTask(this);
		}
		
	}
	
	private AddFileToSearchIndexTask(Builder builder) {
		
		this.documentToIndex = builder.documentToIndex;
		this.indexerService = builder.indexerService;
		this.haveExisting = builder.haveExisting;
		
		super.setName(builder.taskName);
	}

	/* (non-Javadoc)
	 * @see org.eamrf.concurrent.task.AbstractQueuedTask#doWork()
	 */
	@Override
	public Void doWork() throws ServiceException {
		try {
			if(haveExisting) {
				indexerService.getIndexerForStore(documentToIndex.getStore()).update(documentToIndex);
			}else {
				indexerService.getIndexerForStore(documentToIndex.getStore()).add(documentToIndex);
			}
			setChanged();
			notifyObservers();
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

}
