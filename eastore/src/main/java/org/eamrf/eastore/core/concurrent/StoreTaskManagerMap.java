package org.eamrf.eastore.core.concurrent;

import java.util.concurrent.ExecutorService;

import org.eamrf.concurrent.task.QueuedTaskManager;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;

/**
 * Maps a store to its QueuedTaskManager
 * 
 * @author slenzi
 */
public class StoreTaskManagerMap {

	private Store store = null;
	private QueuedTaskManager taskManager = null;
	
	public StoreTaskManagerMap() {
		
	}

	public StoreTaskManagerMap(Store store, QueuedTaskManager taskManager) {
		super();
		this.store = store;
		this.taskManager = taskManager;
	}

	public Store getStore() {
		return store;
	}

	public QueuedTaskManager getTaskManager() {
		return taskManager;
	}

	public void setStore(Store store) {
		this.store = store;
	}

	public void setTaskManager(QueuedTaskManager taskManager) {
		this.taskManager = taskManager;
	}

}
