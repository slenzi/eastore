/**
 * 
 */
package org.eamrf.eastore.web.jaxrs.core.rs;

import java.nio.file.Paths;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.StringUtil;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.service.FileSystemService;
import org.eamrf.eastore.web.jaxrs.BaseResourceHandler;
import org.eamrf.repository.oracle.ecoguser.eastore.model.DirectoryResource;
import org.eamrf.repository.oracle.ecoguser.eastore.model.Store;
import org.eamrf.web.rs.exception.WebServiceException;
import org.eamrf.web.rs.exception.WebServiceException.WebExceptionType;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author slenzi
 *
 */
@Path("/fsys")
@Service("eaFileSystemResource")
public class FileSystemResource extends BaseResourceHandler {

    @InjectLogger
    private Logger logger;	
	
    @Autowired
    private FileSystemService fileSystemService;
    
	public FileSystemResource() {

	}

	/* (non-Javadoc)
	 * @see org.eamrf.eastore.web.jaxrs.BaseResourceHandler#getLogger()
	 */
	@Override
	public Logger getLogger() {
		return logger;
	}
	
	/**
	 * Create a new store
	 * 
	 * @param dirNodeId
	 * @param name
	 * @return
	 * @throws WebServiceException
	 */
	@GET
	@POST
	@Path("/addStore")
	@Produces(MediaType.APPLICATION_JSON)
    public Store addStore(
    		@QueryParam("storeName") String storeName,
    		@QueryParam("storeDesc") String storeDesc,
    		@QueryParam("storePath") String storePath,
    		@QueryParam("rootDirName") String rootDirName,
    		@QueryParam("maxFileSizeDb") Long maxFileSizeDb) throws WebServiceException {
    	
		if(StringUtil.isNullEmpty(storeName) || StringUtil.isNullEmpty(storeDesc) || StringUtil.isNullEmpty(storePath) ||
				StringUtil.isNullEmpty(rootDirName) || maxFileSizeDb == null || maxFileSizeDb < 0){
			
			handleError("Missing required params. Plese check values for storeName, storeDesc, "
					+ "storePath, rootDirName, and maxFileSizeDb", WebExceptionType.CODE_IO_ERROR);
			
		}
		
		java.nio.file.Path newStorePath = null;
		try {
			newStorePath = Paths.get(storePath);
		} catch (Exception e) {
			handleError("Error converting String '" + storePath + "' to java.nio.file.Path: " + e.getMessage(), 
					WebExceptionType.CODE_IO_ERROR, e);
		}
		
		Store store = null;
    	try {
			store = fileSystemService.addStore(storeName, storeDesc, newStorePath, rootDirName, maxFileSizeDb);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	return store;
    	
    }
	
	/**
	 * Add a file...
	 * 
	 * @return
	 * @throws WebServiceException
	 */
    @GET
    @POST
    @Path("/addFile/{dirNodeId}/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addFile(
    		@PathParam("dirNodeId") Long dirNodeId,
    		@PathParam("name") String name) throws WebServiceException {
    	
    	// See -> http://stackoverflow.com/questions/25797650/fileupload-with-jax-rs
    	
    	/*
		@Consumes(MediaType.MULTIPART_FORM_DATA)
		public Response uploadFile(@Multipart(value = "vendor") String vendor,
        @Multipart(value = "uploadedFile") Attachment attr) {    	 
    	 */
    	
    	return Response.ok(buildJsonOK(), MediaType.APPLICATION_JSON).build(); 
    	
    }
    
	/**
	 * Add a file...
	 * 
	 * @return
	 * @throws WebServiceException
	 */
    @GET
    @POST
    @Path("/addDirectory/{dirNodeId}/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public DirectoryResource addDirectory(
    		@PathParam("dirNodeId") Long dirNodeId,
    		@PathParam("name") String name) throws WebServiceException {
    	
    	if(dirNodeId == null || StringUtil.isNullEmpty(name)){
    		handleError("Missing dirNodeId, and/or name params.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	DirectoryResource dirResource = null;
    	try {
    		dirResource = fileSystemService.addDirectory(dirNodeId, name);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	//return Response.ok(buildJsonOK(), MediaType.APPLICATION_JSON).build(); 
    	return dirResource;
    	
    }
    
    /**
     * Create the test store
     * 
     * @return
     * @throws WebServiceException
     */
    @GET
    @POST
    @Path("/createTestStore")
    @Produces(MediaType.APPLICATION_JSON)    
    public Store createTestStore() throws WebServiceException {
    	
    	Store testStore = null;
    	try {
			testStore = fileSystemService.createTestStore();
		} catch (ServiceException e) {
			handleError("Error creating test store, " + e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	return testStore;
    	
    }

}
