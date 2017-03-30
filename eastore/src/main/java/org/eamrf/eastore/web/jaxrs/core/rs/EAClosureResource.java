/**
 * 
 */
package org.eamrf.eastore.web.jaxrs.core.rs;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.StringUtil;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.properties.ManagedProperties;
import org.eamrf.eastore.core.service.ClosureService;
import org.eamrf.eastore.web.jaxrs.BaseResourceHandler;
import org.eamrf.repository.oracle.ecoguser.eastore.model.ParentChildMap;
import org.eamrf.web.rs.exception.WebServiceException;
import org.eamrf.web.rs.exception.WebServiceException.WebExceptionType;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author slenzi
 */
@Path("/closure")
@Service("eaClosureResource")
public class EAClosureResource extends BaseResourceHandler {

    @InjectLogger
    private Logger logger;
    
    @Autowired
    private ManagedProperties appProps;
    
    @Autowired
    private ClosureService closureService;
    
	public EAClosureResource() {

	}
    
    /**
     * Get parent-child mappings
     * 
     * @param nodeId
     * @return
     * @throws WebServiceException
     */
    @GET
    @Path("/mappings/{nodeId}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ParentChildMap> getParentChildMappings(@PathParam("nodeId") Long nodeId) throws WebServiceException {
    	
    	if(nodeId == null){
    		handleError("Missing nodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	List<ParentChildMap> mappings = null;
    	try {
			mappings = closureService.getMappings(nodeId);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	return mappings;
    	
    }
    
    /**
     * Get parent-child mappings
     * 
     * @param nodeId
     * @return
     * @throws WebServiceException
     */
    @GET
    @Path("/mappings/{nodeId}/depth/{depth}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ParentChildMap> getParentChildMappings(
    		@PathParam("nodeId") Long nodeId, @PathParam("depth") int depth) throws WebServiceException {
    	
    	if(nodeId == null){
    		handleError("Missing nodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	if(depth < 0){
    		handleError("Depth param must be positive.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	List<ParentChildMap> mappings = null;
    	try {
			mappings = closureService.getMappings(nodeId, depth);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	return mappings;
    	
    }
    
    /**
     * Add a new node. Currently does not check if the parent node already has a child node with
     * the same names...
     * 
     * @param parentNodeId
     * @param name
     * @return
     * @throws WebServiceException
     */
    @GET
    @POST
    @Path("/add/{parentNodeId}/name/{name}/type/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    public Long addNode(
    		@PathParam("parentNodeId") Long parentNodeId,
    		@PathParam("name") String name,
    		@PathParam("type") String type) throws WebServiceException {
    	
    	if(parentNodeId == null || StringUtil.isNullEmpty(name) || StringUtil.isNullEmpty(type)){
    		handleError("Missing parentNodeId, name, and/or type params.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	// TODO - make sure parent node doesn't already have a child with the same name
    	
    	Long newNodeId = null;
    	try {
			newNodeId = closureService.addNode(parentNodeId, name, type);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	return newNodeId;
    	
    }
    
	/* (non-Javadoc)
	 * @see org.eamrf.eastore.web.jaxrs.BaseResourceHandler#getLogger()
	 */
	@Override
	public Logger getLogger() {
		return logger;
	}

}
