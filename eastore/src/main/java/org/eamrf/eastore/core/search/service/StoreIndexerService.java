package org.eamrf.eastore.core.search.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.search.extract.FileTextExtractor;
import org.eamrf.eastore.core.search.extract.MsExcelExtractor;
import org.eamrf.eastore.core.search.extract.MsPowerpointExtractor;
import org.eamrf.eastore.core.search.extract.MsWordExtractor;
import org.eamrf.eastore.core.search.extract.PdfExtractor;
import org.eamrf.eastore.core.search.extract.PlainTextExtractor;
import org.eamrf.eastore.core.search.lucene.StoreIndexer;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

/**
 * Manages initialization and updates to the Lucene search indexes for stores.
 * 
 * @author slenzi
 *
 */
@Service
public class StoreIndexerService {
	
    @InjectLogger
    private Logger logger;  

	// add more extractors for additional file/mime types
	private final List<FileTextExtractor> fileExtractors = Arrays.asList(
			new PlainTextExtractor(),
			new PdfExtractor(),
			new MsWordExtractor(),
			new MsPowerpointExtractor(),
			new MsExcelExtractor());
	
    // maps all stores to their lucene search indexer
    private Map<Store,StoreIndexer> storeIndexerMap = new HashMap<Store,StoreIndexer>();	
	
	public StoreIndexerService() {
		
	}
	
	/**
	 * Initialize the lucene indexer for the store if it already hasn't been initialized.
	 * 
	 * @param store
	 * @throws IOException
	 */
	public void initializeIndexerForStore(Store store) throws IOException {
		StoreIndexer indexer = storeIndexerMap.get(store);
		if(indexer == null) {
			indexer = new StoreIndexer(store, fileExtractors);
			indexer.init();
			storeIndexerMap.put(store, indexer);			
		}
	}
	
	/**
	 * Get a lucene indexer for the store. This will create the index if it doesn't already exist.
	 * 
	 * @param store
	 * @return
	 * @throws IOException
	 */
	public StoreIndexer getIndexerForStore(Store store) throws IOException {
		StoreIndexer indexer = storeIndexerMap.get(store);
		if(indexer == null) {
			initializeIndexerForStore(store);
		}
		return storeIndexerMap.get(store);
	}
	
	@PreDestroy
	public void cleanup() {
		for(StoreIndexer indexer : storeIndexerMap.values()) {
			try {
				indexer.destroy();
			} catch (IOException e) {
				logger.error("Failed to shutdown lucene indexer for store " + indexer.getStore().getName());
				e.printStackTrace();
			}
		}
	}

}
