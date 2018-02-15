package org.eamrf.eastore.core.search.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.IndexWriter;
import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.search.lucene.StoreSearcher;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

/**
 * Main service class for searching for documents within a store
 * 
 * @author slenzi
 *
 */
@Service
public class StoreSearchService {

    @InjectLogger
    private Logger logger;
    
    // maps all stores to their lucene searcher
    private Map<Store,StoreSearcher> storeSearcherMap = new HashMap<Store,StoreSearcher>();    
	
	public StoreSearchService() {
		
	}
	
	/**
	 * Initialize lucene searcher for the store
	 * 
	 * @param store
	 * @param indexWriter
	 * @throws IOException
	 */
	public void initializeSearcherForStore(Store store, IndexWriter indexWriter) throws IOException {
		
		StoreSearcher searcher = storeSearcherMap.get(store);
		
		if(searcher == null) {
			
			logger.info("Initializing store searcher for store [id=" + store.getId() + ", name=" + store.getName() + "]");
			
			searcher = new StoreSearcher(store, indexWriter);
			searcher.init();
			storeSearcherMap.put(store, searcher);			
		}
	}
	
	/**
	 * Get lucene searcher for the store
	 * 
	 * @param store
	 * @return
	 * @throws IOException
	 */
	public StoreSearcher getSearcherForStore(Store store) throws IOException {
		StoreSearcher searcher = storeSearcherMap.get(store);
		return searcher;
	}
	
	/**
	 * Shutdown searcher for all stores when application terminates.
	 */
	public void cleanup() {
		for(StoreSearcher searcher : storeSearcherMap.values()) {
			try {
				searcher.destroy();
			} catch (IOException e) {
				logger.error("Failed to shutdown lucene searcher for store " + searcher.getStore().getName());
				e.printStackTrace();
			}
		}
	}	

}
