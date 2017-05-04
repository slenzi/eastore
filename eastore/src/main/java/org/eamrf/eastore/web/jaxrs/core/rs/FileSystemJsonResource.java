package org.eamrf.eastore.web.jaxrs.core.rs;

import java.io.File;
import java.util.ArrayList;
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
import org.eamrf.eastore.core.service.PathResourceTreeService;
import org.eamrf.eastore.core.tree.Tree;
import org.eamrf.eastore.core.tree.TreeNode;
import org.eamrf.eastore.web.jaxrs.BaseResourceHandler;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.PathResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;
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
    
    @Autowired
    private PathResourceTreeService pathResourceTreeService;
	
	public FileSystemJsonResource() {
		
	}

	@Override
	public Logger getLogger() {
		return logger;
	}
	
	/**
	 * Fetch a path resource by ID
	 * 
	 * @param nodeId - node id of the resource
	 * @return The path resource
	 * @throws WebServiceException
	 */
	@GET
	@Path("/resource/nodeId/{nodeId}")
	@Produces(MediaType.APPLICATION_JSON)
	public PathResource getPathResourceById(
			@PathParam("nodeId") Long nodeId) throws WebServiceException {
		
		if( nodeId == null ){
			handleError("Missing nodeId parameter", WebExceptionType.CODE_IO_ERROR);
		}

		PathResource resource = null;
		try {
			resource = fileSystemService.getPathResource(nodeId);
		} catch (ServiceException e) {
			handleError("Error fetching path resource, " + 
					e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
		
		if(resource == null){
			handleError("Returned PathResource object was null. nodeId=" + nodeId, WebExceptionType.CODE_IO_ERROR);
		}
		
		return resource;
		
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
	@Path("/resource/path/{storeName}/{relPath:.+}")
	@Produces(MediaType.APPLICATION_JSON)
	public PathResource getPathResourceByPath(
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
			handleError("Error fetching path resource, " + 
					e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
		
		if(resource == null){
			handleError("Returned PathResource object was null. storeName=" + storeName + 
					", relPath=" + relPath, WebExceptionType.CODE_IO_ERROR);
		}
		
		return resource;
		
	}
	
	/**
	 * Fetch the store's root directory path resource
	 * 
	 * @param storeName
	 * @return
	 * @throws WebServiceException
	 */
	@GET
	@Path("/resource/storeName/{storeName}")
	@Produces(MediaType.APPLICATION_JSON)
	public PathResource getPathResourceForStore(@PathParam("storeName") String storeName) throws WebServiceException {
		
		if(StringUtil.isNullEmpty(storeName)){
			handleError("Missing storeName parameter", WebExceptionType.CODE_IO_ERROR);
		}
		storeName = storeName.trim();
		
		logger.info("Get Store: storeName=" + storeName);
		
		Store store = null;
		try {
			store = fileSystemService.getStoreByName(storeName);
		} catch (ServiceException e) {
			handleError("Error fetching store, storeName=" + storeName + ", " + 
					e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
		
		PathResource resource = null;
		try {
			resource = fileSystemService.getPathResource(store.getNodeId());
		} catch (ServiceException e) {
			handleError("Error fetching path resource, " + 
					e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}		
		
		return resource;
		
	}	
	
	/**
	 * Fetch the parent of a resource, or null if the resource is a root node and
	 * has no parent.
	 * 
	 * @param nodeId - id of the child node. the parent will be returned, or null
	 * if the node is a root node and has no parent.
	 * @return The parent resource, or null if the node is a root node and has no parent
	 * @throws WebServiceException
	 */
	@GET
	@Path("/parent/resource/nodeId/{nodeId}")
	@Produces(MediaType.APPLICATION_JSON)
	public PathResource getParentPathResourceById(
			@PathParam("nodeId") Long nodeId) throws WebServiceException {
		
		if( nodeId == null ){
			handleError("Missing nodeId parameter", WebExceptionType.CODE_IO_ERROR);
		}

		PathResource resource = null;
		try {
			resource = fileSystemService.getParentPathResource(nodeId);
		} catch (ServiceException e) {
			handleError("Error fetching parent path resource for node " + nodeId + ", " + 
					e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
		
		return resource;
		
	}
	
	/**
	 * Fetch the parent of a resource, or null if the resource is a root node and
	 * has no parent.
	 * 
	 * @param storeName - store that the resource is located in
	 * @param relPath - relative path of a resource in the store. this method will return it's parent resource,
	 * or null if the resource has no parent.
	 * @return
	 * @throws WebServiceException
	 */
	@GET
	@Path("/parent/resource/path/{storeName}/{relPath:.+}")
	@Produces(MediaType.APPLICATION_JSON)
	public PathResource getParentPathResourceByPath(
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
		
		// fetch the resource
		PathResource resource = null;
		try {
			resource = fileSystemService.getPathResource(storeName, relPath);
		} catch (ServiceException e) {
			handleError("Error fetching path resource, " + 
					e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
		
		if(resource == null){
			handleError("Returned PathResource object was null. storeName=" + storeName + 
					", relPath=" + relPath, WebExceptionType.CODE_IO_ERROR);
		}
		
		// fetch it's parent
		PathResource parentResource = null;
		try {
			parentResource = fileSystemService.getParentPathResource(resource.getNodeId());
		} catch (ServiceException e) {
			handleError("Error fetching parent path resource for node " + 
					resource.getNodeId() + ", storeName=" + storeName + ", rePath=" + relPath + ", " + 
					e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
		
		return parentResource;
		
	}
	
	/**
	 * Fetch all the first-level children for the resource
	 * 
	 * @param nodeId - id of the child node. the parent will be returned, or null
	 * if the node is a root node and has no parent.
	 * @return A list of all the first-level resources under the node
	 * @throws WebServiceException
	 */
	@GET
	@Path("/child/resource/nodeId/{nodeId}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<PathResource> getChildPathResourceById(
			@PathParam("nodeId") Long nodeId) throws WebServiceException {
		
		if( nodeId == null ){
			handleError("Missing nodeId parameter", WebExceptionType.CODE_IO_ERROR);
		}

		List<PathResource> children = null;
		try {
			children = fileSystemService.getChildPathResource(nodeId);
		} catch (ServiceException e) {
			handleError("Error fetching child path resources for node " + nodeId + ", " + 
					e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
		
		return children;
		
	}
	
	/**
	 * Fetch all the first-level children for the resource
	 * 
	 * @param storeName - name of the store that the resources reside under
	 * @param relPath - relative path of a resource within the store. this method will fetch all the first-level
	 * children under that resource.
	 * @return A list of all the first-level resources under the node
	 * @throws WebServiceException
	 */
	@GET
	@Path("/child/resource/path/{storeName}/{relPath:.+}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<PathResource> getChildPathResourceByPath(
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
		
		logger.info("Get Child PathResouces: storeName=" + storeName + ", relPath=" + relPath);
		
		// fetch the resource
		PathResource resource = null;
		try {
			resource = fileSystemService.getPathResource(storeName, relPath);
		} catch (ServiceException e) {
			handleError("Error fetching path resource, " + 
					e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
		
		if(resource == null){
			handleError("Returned PathResource object was null. storeName=" + storeName + 
					", relPath=" + relPath, WebExceptionType.CODE_IO_ERROR);
		}
		
		// fetch all the first-level resources under the resource
		List<PathResource> children = null;
		try {
			children = fileSystemService.getChildPathResource(resource.getNodeId());
		} catch (ServiceException e) {
			handleError("Error fetching child path resources for node " + 
					resource.getNodeId() + ", storeName=" + storeName + ", rePath=" + relPath + ", " + 
					e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
		
		return children;
		
	}
	
	/**
	 * Fetch all the parent tree (bottom-up) information for a specific node. This info can be use
	 * to build a link of bread crumbs, allowing a user to navigate back up the tree to
	 * the root node.
	 * 
	 * @param nodeId
	 * @return
	 * @throws WebServiceException
	 */
	@GET
	@Path("/breadcrumb/nodeId/{nodeId}")
	@Produces(MediaType.APPLICATION_JSON)	
    public List<PathResource> getBreadcrumbByNodeId(@PathParam("nodeId") Long nodeId) throws WebServiceException {
    	
    	if(nodeId == null){
    		handleError("Missing nodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	Tree<PathResource> tree = null;
    	try {
    		tree = pathResourceTreeService.buildParentPathResourceTree(nodeId);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for nodeId => " + nodeId, WebExceptionType.CODE_IO_ERROR);
    	}

    	List<PathResource> crumbs = new ArrayList<PathResource>();
    	
    	buildCrumbs(tree.getRootNode(), crumbs);
    	
    	return crumbs;
    	
    }
	
	/**
	 * Fetch all the parent tree (bottom-up) information for a specific node. This info can be use
	 * to build a link of bread crumbs, allowing a user to navigate back up the tree to
	 * the root node.
	 * 
	 * @param storeName - name of the store that the resources resides under
	 * @param relPath - relative path of a resource within the store. this method will fetch all parents (bottom-up)
	 * all the way to the root node.
	 * @return
	 * @throws WebServiceException
	 */
	@GET
	@Path("/breadcrumb/path/{storeName}/{relPath:.+}")
	@Produces(MediaType.APPLICATION_JSON)	
    public List<PathResource> getBreadcrumbByPath(
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
		
		logger.info("Getting breadcrumb: storeName=" + storeName + ", relPath=" + relPath);
		
		// fetch the resource
		PathResource resource = null;
		try {
			resource = fileSystemService.getPathResource(storeName, relPath);
		} catch (ServiceException e) {
			handleError("Error fetching path resource, " + 
					e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}		
    	
    	Tree<PathResource> tree = null;
    	try {
    		tree = pathResourceTreeService.buildParentPathResourceTree(resource.getNodeId());
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for nodeId => " + resource.getNodeId(), WebExceptionType.CODE_IO_ERROR);
    	}

    	List<PathResource> crumbs = new ArrayList<PathResource>();
    	
    	buildCrumbs(tree.getRootNode(), crumbs);
    	
    	return crumbs;
    	
    }	
	
	private void buildCrumbs(TreeNode<PathResource> node, List<PathResource> crumbs){
		crumbs.add(node.getData());
		if(node.hasChildren()){
			// building a parent tree, so there should only be one child
			buildCrumbs(node.getChildren().get(0), crumbs);
		}
	}
	
	/**
	 * Fetch store by name
	 * 
	 * @param storeName
	 * @return
	 * @throws WebServiceException
	 */
	@GET
	@Path("/store/name/{storeName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Store getStoreByName(@PathParam("storeName") String storeName) throws WebServiceException {
		
		if(StringUtil.isNullEmpty(storeName)){
			handleError("Missing storeName parameter", WebExceptionType.CODE_IO_ERROR);
		}
		storeName = storeName.trim();
		
		logger.info("Get Store: storeName=" + storeName);
		
		Store store = null;
		try {
			store = fileSystemService.getStoreByName(storeName);
		} catch (ServiceException e) {
			handleError("Error fetching store, storeName=" + storeName + ", " + 
					e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
		
		return store;
		
	}
	
	/**
	 * Fetch all stores
	 * 
	 * @param storeName
	 * @return
	 * @throws WebServiceException
	 */
	@GET
	@Path("/store")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Store> getStores() throws WebServiceException {
		
		List<Store> stores = null;
		try {
			stores = fileSystemService.getStores();
		} catch (ServiceException e) {
			handleError("Error fetching stores, " + 
					e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
		
		return stores;
		
	}	

}
