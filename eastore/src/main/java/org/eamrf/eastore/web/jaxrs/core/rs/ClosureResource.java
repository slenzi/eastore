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
import javax.ws.rs.core.Response;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.StringUtil;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.service.ClosureService;
import org.eamrf.eastore.web.jaxrs.BaseResourceHandler;
import org.eamrf.repository.oracle.ecoguser.eastore.model.Node;
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
public class ClosureResource extends BaseResourceHandler {

    @InjectLogger
    private Logger logger;
    
    @Autowired
    private ClosureService closureService;
    
	public ClosureResource() {

	}
    
    /**
     * Get parent-child top-down mappings (from root to leaf)
     * 
     * @param nodeId
     * @return
     * @throws WebServiceException
     */
    @GET
    @Path("/mappings/child/{nodeId}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Node> getChildMappings(@PathParam("nodeId") Long nodeId) throws WebServiceException {
    	
    	if(nodeId == null){
    		handleError("Missing nodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	List<Node> mappings = null;
    	try {
			mappings = closureService.getChildMappings(nodeId);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	return mappings;
    	
    }
    
    /**
     * Get parent-child top-down mappings (from root to leaf)
     * 
     * @param nodeId
     * @param depth
     * @return
     * @throws WebServiceException
     */
    @GET
    @Path("/mappings/child/{nodeId}/depth/{depth}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Node> getChildMappings(
    		@PathParam("nodeId") Long nodeId, @PathParam("depth") int depth) throws WebServiceException {
    	
    	if(nodeId == null){
    		handleError("Missing nodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	if(depth < 0){
    		handleError("Depth param must be positive.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	List<Node> mappings = null;
    	try {
			mappings = closureService.getChildMappings(nodeId, depth);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	return mappings;
    	
    }
    
    /**
     * Get parent-child, bottom-up mappings (from leaf to root)
     * 
     * @param nodeId
     * @return
     * @throws WebServiceException
     */
    @GET
    @Path("/mappings/parent/{nodeId}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Node> getParentMappings(@PathParam("nodeId") Long nodeId) throws WebServiceException {
    	
    	if(nodeId == null){
    		handleError("Missing nodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	List<Node> mappings = null;
    	try {
			mappings = closureService.getParentMappings(nodeId);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	return mappings;
    	
    }
    
    /**
     * Get parent-child, bottom-up mappings (from leaf to root)
     * 
     * @param nodeId
     * @param depth
     * @return
     * @throws WebServiceException
     */
    @GET
    @Path("/mappings/parent/{nodeId}/levels/{levels}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Node> getParentMappings(
    		@PathParam("nodeId") Long nodeId, @PathParam("levels") int levels) throws WebServiceException {
    	
    	if(nodeId == null){
    		handleError("Missing nodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	if(levels < 0){
    		handleError("Levels param must be positive.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	List<Node> mappings = null;
    	try {
			mappings = closureService.getParentMappings(nodeId, levels);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	return mappings;
    	
    }    
    
    /**
     * Add a new node.
     * 
     * @param parentNodeId
     * @param name
     * @return
     * @throws WebServiceException
     */
    @GET
    @POST
    @Path("/add/{parentNodeId}/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Node addNode(
    		@PathParam("parentNodeId") Long parentNodeId,
    		@PathParam("name") String name,
    		@PathParam("type") String type) throws WebServiceException {
    	
    	if(parentNodeId == null || StringUtil.isNullEmpty(name) || StringUtil.isNullEmpty(type)){
    		handleError("Missing parentNodeId, name, and/or type params.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	Node newNode = null;
    	try {
    		newNode = closureService.addNode(parentNodeId, name);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	return newNode;
    	
    }
    
    /**
     * Delete a node, and all children under it.
     * 
     * @param nodeId
     * @return
     * @throws WebServiceException
     */
    @GET
    @POST
    @Path("/deleteNode/{nodeId}")
    @Produces(MediaType.APPLICATION_JSON)    
    public Response deleteNode(@PathParam("nodeId") Long nodeId) throws WebServiceException {
    	
    	if(nodeId == null){
    		handleError("Missing nodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	try {
			closureService.deleteNode(nodeId);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	return Response.ok(buildJsonOK(), MediaType.APPLICATION_JSON).build();    	
    	
    }
    
    /**
     * Delete a node, and all children under it.
     * 
     * @param nodeId
     * @return
     * @throws WebServiceException
     */
    @GET
    @POST
    @Path("/deleteChildren/{nodeId}")
    @Produces(MediaType.APPLICATION_JSON)    
    public Response deleteChildren(@PathParam("nodeId") Long nodeId) throws WebServiceException {
    	
    	if(nodeId == null){
    		handleError("Missing nodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	try {
			closureService.deleteChildren(nodeId);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	return Response.ok(buildJsonOK(), MediaType.APPLICATION_JSON).build();    	
    	
    }
    
	/* (non-Javadoc)
	 * @see org.eamrf.eastore.web.jaxrs.BaseResourceHandler#getLogger()
	 */
	@Override
	public Logger getLogger() {
		return logger;
	}

}
