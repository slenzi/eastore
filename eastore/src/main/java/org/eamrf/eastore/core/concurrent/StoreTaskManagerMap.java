package org.eamrf.eastore.core.concurrent;

import org.eamrf.concurrent.task.QueuedTaskManager;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;

/**
 * Maps a store to its QueuedTaskManagers
 * 
 * @author slenzi
 */
public class StoreTaskManagerMap {

	private Store store = null;
	
	// task manager for adding/updating files/directories for a store
	private QueuedTaskManager generalTaskManager = null;
	
	// task manager for adding file binary data to database
	private QueuedTaskManager binaryTaskManager = null;
	
	// task manager for lucene search index operations
	private QueuedTaskManager indexWriterTaskManager = null;
	
	public StoreTaskManagerMap() {
		
	}

	public StoreTaskManagerMap(
			Store store, 
			QueuedTaskManager generalTaskManager, 
			QueuedTaskManager binaryTaskManager, 
			QueuedTaskManager indexWriterTaskManager) {
		
		super();
		this.store = store;
		this.generalTaskManager = generalTaskManager;
		this.binaryTaskManager = binaryTaskManager;
		this.indexWriterTaskManager = indexWriterTaskManager;
	}

	public Store getStore() {
		return store;
	}
	
	public void setStore(Store store) {
		this.store = store;
	}	

	public QueuedTaskManager getGeneralTaskManager() {
		return generalTaskManager;
	}

	public void setGeneralTaskManager(QueuedTaskManager generalTaskManager) {
		this.generalTaskManager = generalTaskManager;
	}

	public QueuedTaskManager getBinaryTaskManager() {
		return binaryTaskManager;
	}

	public void setBinaryTaskManager(QueuedTaskManager binaryTaskManager) {
		this.binaryTaskManager = binaryTaskManager;
	}

	public QueuedTaskManager getSearchIndexWriterTaskManager() {
		return indexWriterTaskManager;
	}

	public void setSearchIndexWriterTaskManager(QueuedTaskManager indexWriterTaskManager) {
		this.indexWriterTaskManager = indexWriterTaskManager;
	}
	
	public void stopAllManagers() {
		
		generalTaskManager.stopTaskManager();
		binaryTaskManager.stopTaskManager();
		indexWriterTaskManager.stopTaskManager();
		
	}

}
