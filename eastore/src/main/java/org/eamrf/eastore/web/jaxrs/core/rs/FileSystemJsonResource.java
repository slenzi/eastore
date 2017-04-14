package org.eamrf.eastore.web.jaxrs.core.rs;

import java.io.File;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.StringUtil;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.service.FileSystemService;
import org.eamrf.eastore.web.jaxrs.BaseResourceHandler;
import org.eamrf.repository.oracle.ecoguser.eastore.model.FileMetaResource;
import org.eamrf.repository.oracle.ecoguser.eastore.model.PathResource;
import org.eamrf.web.rs.exception.WebServiceException;
import org.eamrf.web.rs.exception.WebServiceException.WebExceptionType;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * JAX-RS resource for viewing/fetching meta-data from our file resource,
 * e.g., get file and directory listings, perform searches, etc.
 * 
 * All data returned by this jax-rs resource is in JSON format.
 * 
 * @author slenzi
 */
@Path("/fsys/json")
@Service("fileSystemJsonResource")
public class FileSystemJsonResource extends BaseResourceHandler {

    @InjectLogger
    private Logger logger;
    
    @Autowired
    private FileSystemService fileSystemService;    
	
	public FileSystemJsonResource() {
		
	}

	@Override
	public Logger getLogger() {
		return logger;
	}
	
	/**
	 * Fetch a path resource by store name and resource relative path.
	 * 
	 * @param storeName - name of the store that the resource resides under
	 * @param relPath - relative path of resource within the store.
	 * @return The path resource
	 * @throws WebServiceException
	 */
	@GET
	@Path("/resource/{storeName}/{relPath:.+}")
	@Produces(MediaType.APPLICATION_JSON)
	public PathResource getPathResourceByName(
			@PathParam("storeName") String storeName,
			@PathParam("relPath") List<PathSegment> list) throws WebServiceException {
		
		if(StringUtil.isNullEmpty(storeName) || list == null || list.size() == 0){
			handleError("Missing storeName, and/or relPath segment parameters", WebExceptionType.CODE_IO_ERROR);
		}
		StringBuffer relativePath = new StringBuffer();
		for(PathSegment ps : list){
			relativePath.append(File.separator + ps.getPath().trim());
		}
		
		storeName = storeName.trim();
		String relPath = relativePath.toString().replace("\\", "/");
		
		logger.info("Get PathResouce: storeName=" + storeName + ", relPath=" + relPath);
		
		PathResource resource = null;
		try {
			resource = fileSystemService.getPathResource(storeName, relPath);
		} catch (ServiceException e) {
			handleError("Error downloading file, failed to get file resource with binary data, " + 
					e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
		
		if(resource == null){
			handleError("Returned PathResource object was null. storeName=" + storeName + 
					", relPath=" + relPath, WebExceptionType.CODE_IO_ERROR);
		}
		
		return resource;
		
	}

}
