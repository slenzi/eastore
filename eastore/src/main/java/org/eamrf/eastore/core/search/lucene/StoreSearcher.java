package org.eamrf.eastore.core.search.lucene;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.eamrf.core.util.StringUtil;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.search.service.SearchConstants;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StoreSearcher {

	private static final Logger logger = LoggerFactory.getLogger(StoreSearcher.class);

	private IndexWriter indexWriter = null;
    private SearcherManager searcherManager = null;
    private Future<?> maybeRefreshFuture = null;
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
        //searcherManager = new SearcherManager(indexWriter, true, false, null);
    	searcherManager = new SearcherManager(indexWriter, null);
        scheduledExecutor = Executors.newScheduledThreadPool(1);
        maybeRefreshFuture = scheduledExecutor.scheduleWithFixedDelay(() -> {
            try {
                boolean refreshStatus = searcherManager.maybeRefresh();
                logger.info("Executed 'maybe refresh' on search manager for store [id=" + store.getId() + ", name=" + store.getName() + "], refresh status = " + refreshStatus);
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
     * @param topResults - number of top hit results to return
     * @param maxNumFragments - max number of fragments to return for each hit.
     * @return
     * @throws IOException 
     * @throws ParseException 
     */
    public StoreSearchResult searchByContent(String value, int topResults, int maxNumFragments) throws IOException, ServiceException {
    	
    	if(StringUtil.isNullEmpty(value)) {
    		return null;
    	}
    	
    	logger.info("Searching for term '" + value + "' in store [id=" + store.getId() + ", name=" + store.getName() + "]");
    	
    	StoreSearchResult searchResult = new StoreSearchResult();
    	IndexSearcher searcher = null;
        
    	try {
        	
            searcher = searcherManager.acquire();
            
            Analyzer analyzer = new StandardAnalyzer();
            
            QueryParser qp = new QueryParser(SearchConstants.RESOURCE_CONTENT, analyzer);
            Query query = qp.parse(value); 
            
			QueryScorer scorer = new QueryScorer(query);
			Formatter formatter = new SimpleHTMLFormatter();
			Highlighter highlighter = new Highlighter(formatter, scorer);
			Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, 100);
			highlighter.setTextFragmenter(fragmenter);            
            
            TopDocs hits = searcher.search(query, topResults);
            
            searchResult.setSearchValue(value);
            searchResult.setNumResults(hits.scoreDocs.length);
            
            int docId = 0;
            Document doc = null;
            
            String resourceId = null, resourceContent = null, resourceName = null;
            String resourceDescription = null, resourceRelativePath = null;
            String resourcePath = null, storeId = null, storeName = null;
            
            String[] fragments = null;
            TokenStream tokeStream = null;
            
            for (int i = 0; i < hits.scoreDocs.length; i++) {
            	
            	docId = hits.scoreDocs[i].doc;
            	doc = searcher.doc(docId);
            	
            	resourceId = doc.get(SearchConstants.RESOURCE_ID);
            	resourceName = doc.get(SearchConstants.RESOURCE_NAME);
            	resourceDescription = doc.get(SearchConstants.RESOURCE_DESC);
            	resourceRelativePath = doc.get(SearchConstants.RESOURCE_RELATIVE_PATH);
            	resourcePath = doc.get(SearchConstants.RESOURCE_PATH);
            	resourceContent = doc.get(SearchConstants.RESOURCE_CONTENT);
            	
            	storeId = doc.get(SearchConstants.STORE_ID);
            	storeName = doc.get(SearchConstants.STORE_NAME);
            	
            	tokeStream = analyzer.tokenStream(SearchConstants.RESOURCE_CONTENT, new StringReader(resourceContent));
            	
            	fragments = highlighter.getBestFragments(tokeStream, resourceContent, maxNumFragments);
            	
            	StoreSearchHit hit = new StoreSearchHit();
            	hit.setFragments(fragments);
            	hit.setLuceneDocId(docId);
            	hit.setResourceDesc(resourceDescription);
            	hit.setResourceId(Long.valueOf(resourceId));
            	hit.setResourceName(resourceName);
            	hit.setResourcePath(Paths.get(resourcePath));
            	hit.setResourceRelativePath(resourceRelativePath);
            	hit.setStoreId(Long.valueOf(storeId));
            	hit.setStoreName(storeName);
            	
            	searchResult.addHit(hit);
            	
            }
            
        } catch (InvalidTokenOffsetsException e) {
			throw new ServiceException("InvalidTokenOffsetsException thrown when performing search", e);
		} catch (ParseException e) {
			throw new ServiceException("ParseException thrown when performing search", e);
		} finally {
            if (searcher != null) {
                searcherManager.release(searcher);
            }
        }
        
        return searchResult;
    	
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
