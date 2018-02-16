package org.eamrf.eastore.web.jaxrs.core.rs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.search.lucene.StoreSearchResult;
import org.eamrf.eastore.core.search.service.StoreSearchService;
import org.eamrf.eastore.core.service.file.FileService;
import org.eamrf.eastore.web.jaxrs.BaseResourceHandler;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;
import org.eamrf.web.rs.exception.WebServiceException;
import org.eamrf.web.rs.exception.WebServiceException.WebExceptionType;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * jax-rs resource for searching our file system
 * 
 * @author slenzi
 */
@Path("/fsys/search")
@Service("fileSystemSearchService")
public class FileSystemSearchService extends BaseResourceHandler {

    @InjectLogger
    private Logger logger;
    
    @Autowired
    private FileService fileService;
    
    @Autowired
    private StoreSearchService searchService;
	
	public FileSystemSearchService() {
		
	}
	
	/**
	 * Perform a basic search on file content
	 * 
	 * @param storeId - the ID of the store to search
	 * @param searchTerm - the search term to search for
	 * @param userId - id of user performing the search
	 * @return An instance of StoreSearchResult which encapsulates the search results
	 * @throws WebServiceException
	 */
	@GET
	@Path("/basic/content")
	@Produces(MediaType.APPLICATION_JSON)
	public StoreSearchResult doSearch(
			@QueryParam("storeId") Long storeId,
			@QueryParam("searchTerm") String searchTerm,
			@QueryParam("userId") String userId) throws WebServiceException {
		
		Store store = null;
		try {
			store = fileService.getStoreById(storeId, userId);
		} catch (ServiceException e) {
			handleError("Error fetching store, id=" + storeId + ", " + e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
		
		StoreSearchResult result = null;
		try {
			result = searchService.searchByContent(store, searchTerm, userId);
		} catch (ServiceException e) {
			handleError("Error performing search for term '" + searchTerm + "' on store, id=" + storeId + ", " + e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
		
		return result;
		
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

}
