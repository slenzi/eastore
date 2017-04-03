/**
 * 
 */
package org.eamrf.eastore.web.jaxrs.core.rs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.service.TreeService;
import org.eamrf.eastore.core.tree.Tree;
import org.eamrf.eastore.web.jaxrs.BaseResourceHandler;
import org.eamrf.repository.oracle.ecoguser.eastore.model.ParentChildMap;
import org.eamrf.repository.oracle.ecoguser.eastore.model.PathResource;
import org.eamrf.web.rs.exception.WebServiceException;
import org.eamrf.web.rs.exception.WebServiceException.WebExceptionType;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * jax-rs resource for fetching tree data. Useful for debugging purposes.
 * 
 * @author slenzi
 */
@Path("/tree")
@Service("eaTreeResource")
public class TreeResource extends BaseResourceHandler {

    @InjectLogger
    private Logger logger;
    
    @Autowired
    private TreeService treeService;  
    
	public TreeResource() {

	}
	
	/**
	 * Fetch ParentChildMap (PCM) top-down (root to all leafs) tree in HTML representation
	 * 
	 * @param nodeId
	 * @return
	 * @throws WebServiceException
	 */
    @GET
    @Path("/pcm/html/{nodeId}")
    @Produces(MediaType.TEXT_HTML)
    public Response getPCMTree(@PathParam("nodeId") Long nodeId) throws WebServiceException {
    	
    	if(nodeId == null){
    		handleError("Missing nodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	Tree<ParentChildMap> tree = null;
    	try {
    		tree = treeService.buildPCMTree(nodeId);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for node => " + nodeId, WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	StringBuffer buf = new StringBuffer();
    	buf.append( tree.printHtmlTree() );
    	
    	treeService.logPCMTree(tree);

    	return Response.ok(buf.toString(), MediaType.TEXT_HTML).build();
    	
    }
    
    /**
     * Fetch ParentChildMap (PCM) top-down (root to all leafs) tree in HTML representation, but only include nodes up to a specified depth.
     * 
     * @param nodeId
     * @param depth
     * @return
     * @throws WebServiceException
     */
    @GET
    @Path("/pcm/html/{nodeId}/depth/{depth}")
    @Produces(MediaType.TEXT_HTML)
    public Response getPCMTree(@PathParam("nodeId") Long nodeId, @PathParam("depth") int depth) throws WebServiceException {
    	
    	if(nodeId == null){
    		handleError("Missing nodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	if(depth < 0){
    		handleError("Depth param must be positive.", WebExceptionType.CODE_IO_ERROR);
    	}    	
    	
    	Tree<ParentChildMap> tree = null;
    	try {
    		tree = treeService.buildPCMTree(nodeId, depth);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for node => " + nodeId, WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	StringBuffer buf = new StringBuffer();
    	buf.append( tree.printHtmlTree() );

    	treeService.logPCMTree(tree);
    	
    	return Response.ok(buf.toString(), MediaType.TEXT_HTML).build();
    	
    }
    
	/**
	 * Fetch ParentChildMap (PCM) bottom-up tree (leaf to root) in HTML representation
	 * 
	 * @param nodeId - some leaf node
	 * @return
	 * @throws WebServiceException
	 */
    @GET
    @Path("/pcm/parent/html/{nodeId}")
    @Produces(MediaType.TEXT_HTML)
    public Response getPCMParentTree(@PathParam("nodeId") Long nodeId) throws WebServiceException {
    	
    	if(nodeId == null){
    		handleError("Missing nodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	Tree<ParentChildMap> tree = null;
    	try {
    		tree = treeService.buildParentPCMTree(nodeId);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for node => " + nodeId, WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	StringBuffer buf = new StringBuffer();
    	buf.append( tree.printHtmlTree() );
    	
    	treeService.logPCMTree(tree);

    	return Response.ok(buf.toString(), MediaType.TEXT_HTML).build();
    	
    }
    
	/**
	 * Fetch bottom-up tree (leaf to root) in HTML representation, but only include so many levels up (towards the root node)
	 * 
	 * @param nodeId - some leaf node
	 * @return
	 * @throws WebServiceException
	 */
    @GET
    @Path("/pcm/parent/html/{nodeId}/levels/{levels}")
    @Produces(MediaType.TEXT_HTML)
    public Response getPCMParentTree(@PathParam("nodeId") Long nodeId, @PathParam("levels") int levels) throws WebServiceException {
    	
    	if(nodeId == null){
    		handleError("Missing nodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	if(levels < 0){
    		handleError("Levels param must be positive.", WebExceptionType.CODE_IO_ERROR);
    	}    	
    	
    	Tree<ParentChildMap> tree = null;
    	try {
    		tree = treeService.buildParentPCMTree(nodeId, levels);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for node => " + nodeId, WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	StringBuffer buf = new StringBuffer();
    	buf.append( tree.printHtmlTree() );
    	
    	treeService.logPCMTree(tree);

    	return Response.ok(buf.toString(), MediaType.TEXT_HTML).build();
    	
    }
    
	/**
	 * Fetch PathResource top-down (root to all leafs) tree in HTML representation
	 * 
	 * @param dirNodeId
	 * @return
	 * @throws WebServiceException
	 */
    @GET
    @Path("/pathresource/html/{dirNodeId}")
    @Produces(MediaType.TEXT_HTML)
    public Response getPathResourceTree(@PathParam("dirNodeId") Long dirNodeId) throws WebServiceException {
    	
    	if(dirNodeId == null){
    		handleError("Missing dirNodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	Tree<PathResource> tree = null;
    	try {
    		tree = treeService.buildPathResourceTree(dirNodeId);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for dirNodeId => " + dirNodeId, WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	StringBuffer buf = new StringBuffer();
    	buf.append( tree.printHtmlTree() );
    	
    	treeService.logPathResourceTree(tree);

    	return Response.ok(buf.toString(), MediaType.TEXT_HTML).build();
    	
    }
    
    /**
     * Fetch PathResource top-down (root to all leafs) tree in HTML representation, but only include nodes up to a specified depth.
     * 
     * @param dirNodeId
     * @param depth
     * @return
     * @throws WebServiceException
     */
    @GET
    @Path("/pathresource/html/{dirNodeId}/depth/{depth}")
    @Produces(MediaType.TEXT_HTML)
    public Response getPathResourceTree(@PathParam("dirNodeId") Long dirNodeId, @PathParam("depth") int depth) throws WebServiceException {
    	
    	if(dirNodeId == null){
    		handleError("Missing dirNodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	if(depth < 0){
    		handleError("Depth param must be positive.", WebExceptionType.CODE_IO_ERROR);
    	}    	
    	
    	Tree<PathResource> tree = null;
    	try {
    		tree = treeService.buildPathResourceTree(dirNodeId, depth);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for dirNodeId => " + dirNodeId, WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	StringBuffer buf = new StringBuffer();
    	buf.append( tree.printHtmlTree() );

    	treeService.logPathResourceTree(tree);
    	
    	return Response.ok(buf.toString(), MediaType.TEXT_HTML).build();
    	
    }
    
	/**
	 * Fetch PathResource bottom-up tree (leaf to root) in HTML representation
	 * 
	 * @param dirNodeId
	 * @return
	 * @throws WebServiceException
	 */
    @GET
    @Path("/pathresource/parent/html/{dirNodeId}")
    @Produces(MediaType.TEXT_HTML)
    public Response getPathResourceParentTree(@PathParam("dirNodeId") Long dirNodeId) throws WebServiceException {
    	
    	if(dirNodeId == null){
    		handleError("Missing dirNodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	Tree<PathResource> tree = null;
    	try {
    		tree = treeService.buildParentPathResourceTree(dirNodeId);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for dirNodeId => " + dirNodeId, WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	StringBuffer buf = new StringBuffer();
    	buf.append( tree.printHtmlTree() );
    	
    	treeService.logPathResourceTree(tree);

    	return Response.ok(buf.toString(), MediaType.TEXT_HTML).build();
    	
    }
    
	/**
	 * Fetch bottom-up tree (leaf to root) in HTML representation, but only include so many levels up (towards the root node)
	 * 
	 * @param nodeId - some leaf node
	 * @return
	 * @throws WebServiceException
	 */
    @GET
    @Path("/pathresource/parent/html/{dirNodeId}/levels/{levels}")
    @Produces(MediaType.TEXT_HTML)
    public Response getPathResourceParentTree(@PathParam("dirNodeId") Long dirNodeId, @PathParam("levels") int levels) throws WebServiceException {
    	
    	if(dirNodeId == null){
    		handleError("Missing dirNodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	if(levels < 0){
    		handleError("Levels param must be positive.", WebExceptionType.CODE_IO_ERROR);
    	}    	
    	
    	Tree<PathResource> tree = null;
    	try {
    		tree = treeService.buildParentPathResourceTree(dirNodeId, levels);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for node => " + dirNodeId, WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	StringBuffer buf = new StringBuffer();
    	buf.append( tree.printHtmlTree() );
    	
    	treeService.logPathResourceTree(tree);

    	return Response.ok(buf.toString(), MediaType.TEXT_HTML).build();
    	
    }    

	/* (non-Javadoc)
	 * @see org.eamrf.eastore.web.jaxrs.BaseResourceHandler#getLogger()
	 */
	@Override
	public Logger getLogger() {
		return logger;
	}

}
