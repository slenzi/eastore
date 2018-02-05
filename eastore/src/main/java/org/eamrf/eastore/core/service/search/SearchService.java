package org.eamrf.eastore.core.service.search;

import java.io.IOException;

import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;
import org.springframework.stereotype.Service;

/**
 * Manages initialization and updates to the Lucene search indexes for all stores.
 * 
 * @author slenzi
 *
 */
@Service
public class SearchService {

	public SearchService() {
		
	}
	
	public StoreIndexer getOrCreateIndex(Store store) throws IOException {
		
		StoreIndexer index = new StoreIndexer(store);
		
		index.init();
		
		return index;
		
	}

}
