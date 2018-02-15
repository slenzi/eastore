package org.eamrf.eastore.core.search.service;

import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.CollectionUtil;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.service.file.FileService;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Bootstraps the lucene search setup
 * 
 * @author slenzi
 *
 */
@Service
public class SearchSetupInitializer {

    @InjectLogger
    private Logger logger;	
	
	@Autowired
	private FileService fileService;
	
    @Autowired
    private StoreIndexerService storeIndexerService;
    
    @Autowired
    private StoreSearchService storeSearchService;
	
	public SearchSetupInitializer() {
		
	}
	
	/**
	 * When application loads, setup the lucene indexer and search manager for each store
	 */
	@PostConstruct
	public void init(){
		
		List<Store> stores = null;
		try {
			stores = fileService.getStores(null);
		} catch (ServiceException e) {
			logger.error("Error fetching list of stores during the lucene search setup and initialization process.");
		}
		
		if(CollectionUtil.isEmpty(stores)) {
			return;
		}
		
		try {
			initLuceneSetup(stores);
		} catch (IOException e) {
			logger.error("Failed to initialize lucene indexes for all stores, " + e.getMessage(), e);
			return;
		}		
		
	}
	
	/**
	 * Initialize lucene indexer and search manager for each store
	 * 
	 * @param stores
	 */
	private void initLuceneSetup(List<Store> stores) throws IOException {
		for(Store store : stores){
			
			storeIndexerService.initializeIndexerForStore(store);
			
			storeSearchService.initializeSearcherForStore(store, storeIndexerService.getIndexerForStore(store).getIndexWriter());
			
			//logger.info("Initialized lucene index and search manager for store " + store.getName() + ", at " + store.getPath().toString());
		}
	}
	
	@PreDestroy
	public void cleanup() {
		
		storeSearchService.cleanup();
		
		storeIndexerService.cleanup();
		
	}

}
