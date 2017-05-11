package org.eamrf.eastore.core.concurrent;

import org.eamrf.concurrent.task.QueuedTaskManager;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;

/**
 * Maps a store to its QueuedTaskManager
 * 
 * @author slenzi
 */
public class StoreTaskManagerMap {

	private Store store = null;
	private QueuedTaskManager generalTaskManager = null;
	private QueuedTaskManager binaryTaskManager = null;
	
	public StoreTaskManagerMap() {
		
	}

	public StoreTaskManagerMap(Store store, QueuedTaskManager generalTaskManager, QueuedTaskManager binaryTaskManager) {
		super();
		this.store = store;
		this.generalTaskManager = generalTaskManager;
		this.binaryTaskManager = binaryTaskManager;
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

}
