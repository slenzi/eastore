/**
 * 
 */
package org.eamrf.eastore.web.jaxrs.core.rs;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.StringUtil;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.service.FileSystemService;
import org.eamrf.eastore.web.jaxrs.BaseResourceHandler;
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
    	
    	if(dirNodeId == null || StringUtil.isNullEmpty(name)){
    		handleError("Missing dirNodeId, and/or name params.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	Long newFileNodeId = -1L;
    	try {
			newFileNodeId = fileSystemService.addFile(dirNodeId, name);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
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
    public Response addDirectory(
    		@PathParam("dirNodeId") Long dirNodeId,
    		@PathParam("name") String name) throws WebServiceException {
    	
    	if(dirNodeId == null || StringUtil.isNullEmpty(name)){
    		handleError("Missing dirNodeId, and/or name params.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	Long newDirNodeId = -1L;
    	try {
			newDirNodeId = fileSystemService.addDirectory(dirNodeId, name);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	return Response.ok(buildJsonOK(), MediaType.APPLICATION_JSON).build(); 
    	
    }    

}
