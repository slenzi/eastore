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
import org.eamrf.eastore.core.service.NodeTreeService;
import org.eamrf.eastore.core.service.PathResourceTreeService;
import org.eamrf.eastore.core.tree.Tree;
import org.eamrf.eastore.web.jaxrs.BaseResourceHandler;
import org.eamrf.repository.oracle.ecoguser.eastore.model.Node;
import org.eamrf.repository.oracle.ecoguser.eastore.model.PathResource;
import org.eamrf.web.rs.exception.WebServiceException;
import org.eamrf.web.rs.exception.WebServiceException.WebExceptionType;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * JAX-RS resource for fetching tree data. Useful for debugging purposes.
 * 
 * @author slenzi
 */
@Path("/tree")
@Service("eaTreeResource")
public class TreeResource extends BaseResourceHandler {

    @InjectLogger
    private Logger logger;
    
    @Autowired
    private NodeTreeService nodeTreeService;
    
    @Autowired
    private PathResourceTreeService pathResourceTreeService;  
    
	public TreeResource() {

	}
	
	/**
	 * Fetch Node top-down (root to all leafs) tree in HTML representation
	 * 
	 * @param nodeId
	 * @return
	 * @throws WebServiceException
	 */
    @GET
    @Path("/node/html/{nodeId}")
    @Produces(MediaType.TEXT_HTML)
    public Response getNodeTree(@PathParam("nodeId") Long nodeId) throws WebServiceException {
    	
    	if(nodeId == null){
    		handleError("Missing nodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	Tree<Node> tree = null;
    	try {
    		tree = nodeTreeService.buildNodeTree(nodeId);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for node => " + nodeId, WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	StringBuffer buf = new StringBuffer();
    	buf.append( tree.printHtmlTree() );
    	
    	nodeTreeService.logNodeTree(tree);

    	return Response.ok(buf.toString(), MediaType.TEXT_HTML).build();
    	
    }
    
    /**
     * Fetch Node top-down (root to all leafs) tree in HTML representation, but only include nodes up to a specified depth.
     * 
     * @param nodeId
     * @param depth
     * @return
     * @throws WebServiceException
     */
    @GET
    @Path("/node/html/{nodeId}/depth/{depth}")
    @Produces(MediaType.TEXT_HTML)
    public Response getNodeTree(@PathParam("nodeId") Long nodeId, @PathParam("depth") int depth) throws WebServiceException {
    	
    	if(nodeId == null){
    		handleError("Missing nodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	if(depth < 0){
    		handleError("Depth param must be positive.", WebExceptionType.CODE_IO_ERROR);
    	}    	
    	
    	Tree<Node> tree = null;
    	try {
    		tree = nodeTreeService.buildNodeTree(nodeId, depth);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for node => " + nodeId, WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	StringBuffer buf = new StringBuffer();
    	buf.append( tree.printHtmlTree() );

    	nodeTreeService.logNodeTree(tree);
    	
    	return Response.ok(buf.toString(), MediaType.TEXT_HTML).build();
    	
    }
    
	/**
	 * Fetch Node bottom-up tree (leaf to root) in HTML representation
	 * 
	 * @param nodeId - some leaf node
	 * @return
	 * @throws WebServiceException
	 */
    @GET
    @Path("/node/parent/html/{nodeId}")
    @Produces(MediaType.TEXT_HTML)
    public Response getNodeParentTree(@PathParam("nodeId") Long nodeId) throws WebServiceException {
    	
    	if(nodeId == null){
    		handleError("Missing nodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	Tree<Node> tree = null;
    	try {
    		tree = nodeTreeService.buildParentNodeTree(nodeId);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for node => " + nodeId, WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	StringBuffer buf = new StringBuffer();
    	buf.append( tree.printHtmlTree() );
    	
    	nodeTreeService.logNodeTree(tree);

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
    @Path("/node/parent/html/{nodeId}/levels/{levels}")
    @Produces(MediaType.TEXT_HTML)
    public Response getNodeParentTree(@PathParam("nodeId") Long nodeId, @PathParam("levels") int levels) throws WebServiceException {
    	
    	if(nodeId == null){
    		handleError("Missing nodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	if(levels < 0){
    		handleError("Levels param must be positive.", WebExceptionType.CODE_IO_ERROR);
    	}    	
    	
    	Tree<Node> tree = null;
    	try {
    		tree = nodeTreeService.buildParentNodeTree(nodeId, levels);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for node => " + nodeId, WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	StringBuffer buf = new StringBuffer();
    	buf.append( tree.printHtmlTree() );
    	
    	nodeTreeService.logNodeTree(tree);

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
    		tree = pathResourceTreeService.buildPathResourceTree(dirNodeId);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for dirNodeId => " + dirNodeId, WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	StringBuffer buf = new StringBuffer();
    	buf.append( tree.printHtmlTree() );
    	
    	pathResourceTreeService.logPathResourceTree(tree);

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
    		tree = pathResourceTreeService.buildPathResourceTree(dirNodeId, depth);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for dirNodeId => " + dirNodeId, WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	StringBuffer buf = new StringBuffer();
    	buf.append( tree.printHtmlTree() );

    	pathResourceTreeService.logPathResourceTree(tree);
    	
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
    		tree = pathResourceTreeService.buildParentPathResourceTree(dirNodeId);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for dirNodeId => " + dirNodeId, WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	StringBuffer buf = new StringBuffer();
    	buf.append( tree.printHtmlTree() );
    	
    	pathResourceTreeService.logPathResourceTree(tree);

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
    		tree = pathResourceTreeService.buildParentPathResourceTree(dirNodeId, levels);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for node => " + dirNodeId, WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	StringBuffer buf = new StringBuffer();
    	buf.append( tree.printHtmlTree() );
    	
    	pathResourceTreeService.logPathResourceTree(tree);

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
