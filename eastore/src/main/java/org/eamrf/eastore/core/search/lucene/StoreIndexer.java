package org.eamrf.eastore.core.search.lucene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.eamrf.core.util.CollectionUtil;
import org.eamrf.core.util.FileUtil;
import org.eamrf.core.util.StringUtil;
import org.eamrf.eastore.core.search.extract.FileTextExtractor;
import org.eamrf.eastore.core.search.service.SearchConstants;
import org.eamrf.eastore.core.service.tree.file.PathResourceUtil;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.FileMetaResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the lucene index for a store, including writing and updating documents
 * 
 * @author slenzi
 *
 */
public class StoreIndexer {

	private static final Logger logger = LoggerFactory.getLogger(StoreIndexer.class);

	private IndexWriter indexWriter = null;
    private Future commitFuture = null;
    private Store store = null;
    private ScheduledExecutorService scheduledExecutor = null;
    
    private Map<String,FileTextExtractor> extractorMap = new HashMap<String,FileTextExtractor>();
    private Collection<FileTextExtractor> allExtractors = null;
    
	ExecutorService executorService = Executors.newSingleThreadExecutor();
	
	//private boolean rebuildingIndex = false;
	
	public StoreIndexer(Store store, Collection<FileTextExtractor> extractors) {
		this.store = store;
		this.allExtractors = extractors;
	}
	
	/**
	 * Initialize the lucene index for the store. Must be called before adding/updating documents.
	 * 
	 * @throws IOException
	 */
	public void init() throws IOException {
		
		Path storeLuceneIndexPath = getIndexPath();
		
		if(!Files.exists(storeLuceneIndexPath)) {
			Files.createDirectory(storeLuceneIndexPath);
		}
		
		FSDirectory dir = FSDirectory.open(storeLuceneIndexPath);
		IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
		config.setOpenMode(OpenMode.CREATE_OR_APPEND);
		indexWriter = new IndexWriter(dir, config);
		scheduledExecutor = Executors.newScheduledThreadPool(1);
        commitFuture = scheduledExecutor.scheduleWithFixedDelay(() -> {
            try {
                indexWriter.commit();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 5, 5, TimeUnit.MINUTES);		
		
	}
	
	/**
	 * Get the store for this indexer
	 * 
	 * @return
	 */
	public Store getStore() {
		return this.store;
	}	
	
	/**
	 * Get the full path to the lucene index directory for the store
	 * 
	 * @return
	 */
	private Path getIndexPath() {
		Path storeFilePath = store.getPath();
		Path storePath = storeFilePath.getParent();
		Path storeLuceneIndexPath = Paths.get(storePath.toString(), Store.STORE_LUCENE_DIRECTORY);
		return storeLuceneIndexPath;
	}
	
	/**
	 * Check if the store has an index
	 * 
	 * @return
	 * @throws IOException 
	 */
	public boolean indexExists() throws IOException {
        Directory dir = FSDirectory.open(getIndexPath());
        return DirectoryReader.indexExists(dir);	
	}
	
	/**
	 * Get the number of document in the index
	 * 
	 * @return
	 * @throws IOException
	 */
	public int numDocs() throws IOException {
		
        Directory dir = FSDirectory.open(getIndexPath());
        IndexReader reader = DirectoryReader.open(dir);
        int numDocs = reader.numDocs();
        reader.close();
        return numDocs;
		
	}
	
	/**
	 * Fetch the extractor that can extract text from files with the specified mime type.
	 * 
	 * @param mimeType
	 * @return
	 */
	private FileTextExtractor getExtractorForMime(String mimeType) {
		FileTextExtractor extractor = extractorMap.get(mimeType);
		if(extractor == null && allExtractors != null && allExtractors.size() > 0) {
			for(FileTextExtractor ext : allExtractors) {
				if(ext.canExtract(mimeType)) {
					extractor = ext;
					extractorMap.put(mimeType, extractor);
					return extractor;
				}
			}
		}
		return extractor;
	}
	
	/**
	 * Add a document to the index
	 * 
	 * @param fileResource
	 * @throws IOException
	 */
	public void add(FileMetaResource fileResource) throws IOException {
		
		if(!isInitialized()) {
			return;
		}		
		
		Document doc = new Document();
		
		doc.add(new StringField(SearchConstants.RESOURCE_ID, fileResource.getNodeId().toString(), Field.Store.YES));
		doc.add(new StringField(SearchConstants.RESOURCE_NAME, fileResource.getPathName(), Field.Store.YES));
		doc.add(new StringField(SearchConstants.RESOURCE_DESC, StringUtil.changeNull(fileResource.getDesc()), Field.Store.YES));
		
		doc.add(new TextField(SearchConstants.RESOURCE_RELATIVE_PATH, fileResource.getRelativePath(), Field.Store.YES));
		
		Store fileStore = fileResource.getStore();
		if(fileStore != null) {
			
			Path filePath = PathResourceUtil.buildPath(fileStore, fileResource.getRelativePath());
			doc.add(new StringField(SearchConstants.RESOURCE_PATH, filePath.toString() , Field.Store.YES));
			
			String mimeType = FileUtil.detectMimeType(filePath);
			
			FileTextExtractor extractor = getExtractorForMime(mimeType);
			if(extractor != null) {
				doc.add(new TextField(SearchConstants.RESOURCE_CONTENT, StringUtil.changeNull(extractor.extract(filePath)) , Field.Store.YES));
			}
			
		}
		
		indexWriter.addDocument(doc);
		
	}
	
	/**
	 * Create a task that adds all files to the lucene index. The task is submitted to an executor for execution.
	 * 
	 * @param fileResources
	 * @return A future for the task.
	 * @throws IOException
	 */
	public Future<Boolean> addAll(final Collection<FileMetaResource> fileResources) {
		
		if(CollectionUtil.isEmpty(fileResources)) {
			return null;
		}
		
		AtomicBoolean wasError = new AtomicBoolean(false);
		StringBuffer errors = new StringBuffer();
		
		Callable<Boolean> callableTask = () -> {
			for(FileMetaResource resource : fileResources) {
				try {
					if(!resource.getStore().getId().equals(getStore().getId())) {
						wasError.set(true);
						errors.append("Failed to add file " + resource.getRelativePath() + " to index for store " + getStore().getId() + 
								". Resource belongs to different store with id " + resource.getStore().getId() + ".\n");						
					}else {
						
						logger.info("Adding file " + resource.getRelativePath() + " to store index [id=" + getStore().getId() + 
								", name=" + getStore().getName() + "]");						
						
						add(resource);
					}
				} catch (IOException e) {
					wasError.set(true);
					errors.append("Failed to add file " + resource.getRelativePath() + " to index for store " + getStore().getId() + 
							", " + e.getMessage() + ".\n");
				}
			}
			if(wasError.get()) {
				logger.error("Error adding all " + fileResources.size() + " resources to lucene index for store [id=" + getStore().getId() + 
						", name=" + getStore().getName() + "]\n");
				logger.error(errors.toString());
				return false;
			} else {
				return true;
			}
		};		
		
		Future<Boolean> future = executorService.submit(callableTask);
		
		return future;
	
	}
	
	/**
	 * Update a document in the index
	 * 
	 * @param fileResource
	 * @throws IOException
	 */
	public void update(FileMetaResource fileResource) throws IOException {
		
		if(!isInitialized()) {
			return;
		}
		
		delete(fileResource);
		add(fileResource);
		
	}
	
	/**
	 * Remove a document from the index
	 * 
	 * @param fileResource
	 * @throws IOException
	 */
	public void delete(FileMetaResource fileResource) throws IOException {
		
		if(!isInitialized()) {
			return;
		}
		
		indexWriter.deleteDocuments(new Term(SearchConstants.RESOURCE_ID, fileResource.getNodeId().toString()));
		
	}
	
	/**
	 * Delete all documents from the index
	 * 
	 * @throws IOException
	 */
	public void deleteAll() throws IOException {
		
		logger.info("Deleting all documents in search index for store [id=" + getStore().getId() + 
						", name=" + getStore().getName() + "]");
		
		indexWriter.deleteAll();
		indexWriter.commit();
		
	}
	
	/**
	 * Close the index. Call this when shutting down your application
	 * 
	 * @throws IOException
	 */
	public void destroy() throws IOException {
		commitFuture.cancel(false);
        indexWriter.close();
        scheduledExecutor.shutdown();
	}
	
	/**
	 * Return true/false whether or not the index has been initialized
	 * 
	 * @see init() method
	 * 
	 * @return
	 */
	private boolean isInitialized() {
		
		if(indexWriter != null && indexWriter.isOpen()) {
			return true;
		}
		return false;
		
	}

}
