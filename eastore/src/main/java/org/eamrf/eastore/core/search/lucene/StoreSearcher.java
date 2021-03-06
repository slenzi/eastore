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
    
    //private final 
   
	
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
                //logger.info("Executed 'maybe refresh' on search manager for store [id=" + store.getId() + ", name=" + store.getName() + "], refresh status = " + refreshStatus);
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
     * @oaram fragmentSize - length of search fragment
     * @return
     * @throws IOException 
     * @throws ParseException 
     */
    public StoreSearchResult searchByContent(String value, int topResults, int maxNumFragments, int fragmentSize) throws IOException, ServiceException {
    	
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
			// search results are highlighted with a yellow background and bold text
			Formatter formatter = new SimpleHTMLFormatter("<span class=\"luceneBasicHighlight\">", "</span>");
			Highlighter highlighter = new Highlighter(formatter, scorer);
			Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, 100);
			highlighter.setTextFragmenter(fragmenter);            
            
            TopDocs hits = searcher.search(query, topResults);
            
            searchResult.setSearchValue(value);
            searchResult.setNumResults(hits.scoreDocs.length);
            
            int docId = 0;
            Document doc = null;
            
            String resourceId = null, resourceContent = null, resourceName = null;
            String directoryId = null, directoryName = null, directoryRelativePath = null;
            String resourceDescription = null, resourceRelativePath = null;
            String resourcePath = null, storeId = null, storeName = null;
            
            String[] fragments = null;
            TokenStream tokeStream = null;
            
            for (int i = 0; i < hits.scoreDocs.length; i++) {
            	
            	StoreSearchHit hit = new StoreSearchHit();
            	
            	docId = hits.scoreDocs[i].doc;
            	doc = searcher.doc(docId);
            	
            	hit.setLuceneDocId(docId);
            	
            	resourceId = doc.get(SearchConstants.RESOURCE_ID);
            	if(!StringUtil.isNullEmpty(resourceId)) {
            		hit.setResourceId(Long.valueOf(resourceId));
            	}
            	
            	resourceName = doc.get(SearchConstants.RESOURCE_NAME);
            	hit.setResourceName(resourceName);
            	
            	resourceDescription = doc.get(SearchConstants.RESOURCE_DESC);
            	hit.setResourceDesc(resourceDescription);
            	
            	resourceRelativePath = doc.get(SearchConstants.RESOURCE_RELATIVE_PATH);
            	hit.setResourceRelativePath(resourceRelativePath); 
            	
            	resourcePath = doc.get(SearchConstants.RESOURCE_PATH);
            	if(!StringUtil.isNullEmpty(resourcePath)) {
            		hit.setResourcePath(Paths.get(resourcePath));
            	}
            	
            	directoryId = doc.get(SearchConstants.DIRECTORY_ID);
            	if(!StringUtil.isNullEmpty(directoryId)) {
            		hit.setDirectoryId(Long.valueOf(directoryId));
            	}
            	
            	directoryName = doc.get(SearchConstants.DIRECTORY_NAME);
            	hit.setDirectoryName(directoryName);
            	
            	directoryRelativePath = doc.get(SearchConstants.DIRECTORY_RELATIVE_PATH);
            	hit.setDirectoryRelativePath(directoryRelativePath);
            	
            	storeId = doc.get(SearchConstants.STORE_ID);
            	if(!StringUtil.isNullEmpty(storeId)) {
            		hit.setStoreId(Long.valueOf(storeId));
            	}
            	
            	storeName = doc.get(SearchConstants.STORE_NAME);
            	hit.setStoreName(storeName);
            	
            	resourceContent = doc.get(SearchConstants.RESOURCE_CONTENT);
            	if(!StringUtil.isNullEmpty(resourceContent)) {
                	tokeStream = analyzer.tokenStream(SearchConstants.RESOURCE_CONTENT, new StringReader(resourceContent));
                	fragments = highlighter.getBestFragments(tokeStream, resourceContent, maxNumFragments);
                	hit.setFragments(fragments);
            	}
            	
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
