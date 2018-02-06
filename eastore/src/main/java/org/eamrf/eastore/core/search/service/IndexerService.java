package org.eamrf.eastore.core.search.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.eamrf.eastore.core.search.extract.FileTextExtractor;
import org.eamrf.eastore.core.search.extract.MsExcelExtractor;
import org.eamrf.eastore.core.search.extract.MsPowerpointExtractor;
import org.eamrf.eastore.core.search.extract.MsWordExtractor;
import org.eamrf.eastore.core.search.extract.PdfExtractor;
import org.eamrf.eastore.core.search.extract.PlainTextExtractor;
import org.eamrf.eastore.core.search.lucene.StoreIndexer;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;
import org.springframework.stereotype.Service;

/**
 * Manages initialization and updates to the Lucene search indexes for all stores.
 * 
 * @author slenzi
 *
 */
@Service
public class IndexerService {

	// add more extractors for additional file/mime types
	private final List<FileTextExtractor> fileExtractors = Arrays.asList(
			new PlainTextExtractor(),
			new PdfExtractor(),
			new MsWordExtractor(),
			new MsPowerpointExtractor(),
			new MsExcelExtractor());
	
	public IndexerService() {
		
	}
	
	/**
	 * Get a lucene indexer for the store. This will create the index if it doesn't already exist.
	 * 
	 * @param store
	 * @return
	 * @throws IOException
	 */
	public StoreIndexer getOrCreateStoreIndex(Store store) throws IOException {
		
		StoreIndexer index = new StoreIndexer(store, fileExtractors);
		
		index.init();
		
		return index;
		
	}

}
