package org.eamrf.eastore.core.search.lucene;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TopDocs;
import org.eamrf.eastore.core.search.service.SearchConstants;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StoreSearcher {

	private static final Logger logger = LoggerFactory.getLogger(StoreSearcher.class);

	private IndexWriter indexWriter = null;
    private SearcherManager searcherManager;
    private Future maybeRefreshFuture;
    private Store store = null;
    
    private ScheduledExecutorService scheduledExecutor = null;
   
	
    /**
     * Create a store searcher using an existing indexer for the store
     * 
     * @param indexWriter - the index writer for the store
     */
	public StoreSearcher(Store store, IndexWriter indexWriter) {
		this.store = store;
		this.indexWriter = indexWriter;
	}
	
	/**
	 * Initialize the lucene search manager for the store. Must be called
	 * before performing a search.
	 * 
	 * @throws IOException
	 */
    public void init() throws IOException {
        searcherManager = new SearcherManager(indexWriter, true, false, null);
        scheduledExecutor = Executors.newScheduledThreadPool(1);
        maybeRefreshFuture = scheduledExecutor.scheduleWithFixedDelay(() -> {
            try {
                searcherManager.maybeRefresh();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 0, 5, TimeUnit.SECONDS);
    }
    
    /**
	 * @return the store
	 */
	public Store getStore() {
		return store;
	}

	/**
     * Search by document content/body.
     * 
     * @param value - search value
     * @param topResults - number of top results to return
     * @return
     * @throws IOException 
     * @throws ParseException 
     */
    public TopDocs searchByContent(String value, int topResults) throws IOException, ParseException {
    	
    	IndexSearcher searcher = null;
        try {
            searcher = searcherManager.acquire();
            QueryParser qp = new QueryParser(SearchConstants.RESOURCE_CONTENT, new StandardAnalyzer());
            Query query = qp.parse(value); 
            TopDocs hits = searcher.search(query, topResults);
            return hits;
        } finally {
            if (searcher != null) {
                searcherManager.release(searcher);
            }
        }   	
    	
    }
    
    /**
     * Cancel refresh task and close the search manager
     * 
     * @throws IOException
     */
    public void destroy() throws IOException {
        maybeRefreshFuture.cancel(false);
        searcherManager.close();
    }    

}
