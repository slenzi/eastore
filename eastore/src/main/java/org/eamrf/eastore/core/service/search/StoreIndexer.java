package org.eamrf.eastore.core.service.search;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Future;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.FSDirectory;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;

public class StoreIndexer {

    private IndexWriter indexWriter = null;
    private Future commitFuture = null;
    private Store store = null;
	
	public StoreIndexer(Store store) {
		this.store = store;
	}
	
	public void init() throws IOException {
		
		Path storeFilePath = store.getPath();
		Path storePath = storeFilePath.getParent();
		Path storeLuceneIndexPath = Paths.get(storePath.toString(), Store.STORE_LUCENE_DIRECTORY);
		
		if(!Files.exists(storeLuceneIndexPath)) {
			Files.createDirectory(storeLuceneIndexPath);
		}
		
		FSDirectory dir = FSDirectory.open(storeLuceneIndexPath);
		IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
		config.setOpenMode(OpenMode.CREATE_OR_APPEND);
		indexWriter = new IndexWriter(dir, config);		
		
	}

}
