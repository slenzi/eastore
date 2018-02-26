package org.eamrf.eastore.core.search.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.IndexWriter;
import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.search.lucene.StoreSearchHit;
import org.eamrf.eastore.core.search.lucene.StoreSearchResult;
import org.eamrf.eastore.core.search.lucene.StoreSearcher;
import org.eamrf.eastore.core.service.file.FileService;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    @Autowired
    private FileService fileService;
    
    // maps all stores to their lucene searcher
    private Map<Store,StoreSearcher> storeSearcherMap = new HashMap<Store,StoreSearcher>();
    
    private final int MAX_NUM_SEARCH_RESULTS 	= 50;
    private final int MAX_NUM_SEARCH_FRAGMENTS	= 3;
    private final int MAX_FRAGMENT_LENGTH		= 300;
	
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
	 * Perform search by file content.
	 * 
	 * @param store - the store to search
	 * @param value - the search term value
	 * @param userId - ID of user performing the search. Read access will be taken into consideration to filter search results.
	 * @return
	 * @throws ServiceException
	 */
	public StoreSearchResult searchByContent(Store store, String value, String userId) throws ServiceException {
		
		StoreSearcher searcher = null;
		try {
			searcher = this.getSearcherForStore(store);
		} catch (IOException e) {
			throw new ServiceException("IOException thrown when attempting to retrieve searcher for store [id=" + store.getId() + ", name=" + store.getName() + "]", e);
		}
		
		StoreSearchResult result = null;
		try {
			result = searcher.searchByContent(value, MAX_NUM_SEARCH_RESULTS, MAX_NUM_SEARCH_FRAGMENTS, MAX_FRAGMENT_LENGTH);
		} catch (IOException e) {
			throw new ServiceException("IOException thrown when searching store [id=" + store.getId() + ", name=" + store.getName() + "] for term '" + value + "'", e);
		}
		
		return filterByReadAccess(result, store, userId);
		
	}
	
	/**
	 * Filter out search results that the user does not have read permission on.
	 * 
	 * @param result
	 * @param store
	 * @param userId
	 * @return
	 * @throws ServiceException
	 */
	private StoreSearchResult filterByReadAccess(StoreSearchResult result, Store store, String userId) throws ServiceException {
		
		if(result == null || result.haveHits() == false) {
			return result;
		}
		
		// filter hits based on read access for user
		List<StoreSearchHit> allHits = result.getHits();
		List<StoreSearchHit> filteredHits = new ArrayList<StoreSearchHit>();
		Map<Long,Boolean> readAccessMap = fileService.getFileReadAccessMap(store, userId);
		Boolean canRead = false;
		for(StoreSearchHit hit : allHits) {
			canRead = readAccessMap.get(hit.getResourceId());
			if(canRead != null && canRead) {
				filteredHits.add(hit);
			}
		}
		result.setHits(filteredHits);
		
		return result;		
		
	}
	
	/**
	 * Get lucene searcher for the store
	 * 
	 * @param store
	 * @return
	 * @throws IOException
	 */
	private StoreSearcher getSearcherForStore(Store store) throws IOException {
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
