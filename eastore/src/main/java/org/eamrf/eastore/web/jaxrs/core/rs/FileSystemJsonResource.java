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
import org.eamrf.eastore.core.service.file.FileService;
import org.eamrf.eastore.core.service.tree.file.secure.SecurePathResourceTreeService;
import org.eamrf.eastore.core.tree.Tree;
import org.eamrf.eastore.core.tree.TreeNode;
import org.eamrf.eastore.web.dto.map.PathResourceMapper;
import org.eamrf.eastore.web.dto.map.StoreMapper;
import org.eamrf.eastore.web.dto.model.PathResourceDto;
import org.eamrf.eastore.web.dto.model.StoreDto;
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
    private FileService fileService;
	
    @Autowired
    private SecurePathResourceTreeService securePathResourceService;
    
    private PathResourceMapper resourceMapper = new PathResourceMapper();
    private StoreMapper storeMapper = new StoreMapper();
    
	public FileSystemJsonResource() {
		
	}

	@Override
	public Logger getLogger() {
		return logger;
	}
	
	/**
	 * Map PathResource entity to a data transfer object (DTO)
	 * 
	 * @param resource
	 * @return
	 * @throws WebServiceException
	 */
	private PathResourceDto mapToDto(PathResource resource) throws WebServiceException {

		PathResourceDto dto = null;
		try {
			dto = resourceMapper.map(resource);
		} catch (ServiceException e) {
			handleError("Error mapping path resource entity [id=" + resource.getNodeId()
					+ "] to a data transfer object, " + e.getMessage(), WebExceptionType.CODE_IO_ERROR);
		}
		return dto;

	}
	
	/**
	 * Map list PathResource entities to a list of data transfer objects (DTOs)
	 * 
	 * @param resources
	 * @return
	 * @throws WebServiceException
	 */
	private List<PathResourceDto> mapToDto(List<PathResource> resources) throws WebServiceException {

		List<PathResourceDto> dtoList = null;
		try {
			dtoList = resourceMapper.map(resources);
		} catch (ServiceException e) {
			handleError("Error mapping path resource entity list to list of data transfer objects, " + e.getMessage(),
					WebExceptionType.CODE_IO_ERROR);
		}
		
		// when data is returned from our web service we want to make sure and empty
		// JSON array is returned when we return 0 path resources. 
		if(dtoList == null) {
			dtoList = new ArrayList<PathResourceDto>();
		}
		
		return dtoList;

	}	
	
	/**
	 * Fetch a path resource by ID
	 * 
	 * @param nodeId
	 *            - node id of the resource
	 * @param userId
	 *            - id of user performing the action
	 * @return The path resource
	 * @throws WebServiceException
	 */
	@GET
	@Path("/resource/userId/{userId}/nodeId/{nodeId}")
	@Produces(MediaType.APPLICATION_JSON)
	public PathResourceDto getPathResourceById(@PathParam("nodeId") Long nodeId, @PathParam("userId") String userId)
			throws WebServiceException {

		validateUserId(userId);

		if (nodeId == null) {
			handleError("Missing nodeId parameter", WebExceptionType.CODE_IO_ERROR);
		}

		PathResource resource = null;
		try {
			resource = securePathResourceService.getPathResource(nodeId, userId);
		} catch (ServiceException e) {
			handleError("Error fetching path resource, " + e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}

		if (resource == null) {
			handleError("Returned PathResource object was null. nodeId=" + nodeId, WebExceptionType.CODE_IO_ERROR);
		}

		return mapToDto(resource);

	}	
	
	/**
	 * Fetch a path resource by store name and resource relative path.
	 * 
	 * @param storeName
	 *            - name of the store that the resource resides under
	 * @param relPath
	 *            - relative path of resource within the store.
	 * @param userId
	 *            - id of user performing the action
	 * @return The path resource
	 * @throws WebServiceException
	 */
	@GET
	@Path("/resource/userId/{userId}/path/{storeName}/{relPath:.+}")
	@Produces(MediaType.APPLICATION_JSON)
	public PathResourceDto getPathResourceByPath(@PathParam("storeName") String storeName,
			@PathParam("relPath") List<PathSegment> list, @PathParam("userId") String userId)
			throws WebServiceException {

		validateUserId(userId);

		if (StringUtil.isNullEmpty(storeName) || list == null || list.size() == 0) {
			handleError("Missing storeName, and/or relPath segment parameters", WebExceptionType.CODE_IO_ERROR);
		}
		StringBuffer relativePath = new StringBuffer();
		for (PathSegment ps : list) {
			relativePath.append(File.separator + ps.getPath().trim());
		}

		storeName = storeName.trim();
		String relPath = relativePath.toString().replace("\\", "/");

		logger.info("Get PathResouce: storeName=" + storeName + ", relPath=" + relPath);

		PathResource resource = null;
		try {
			resource = securePathResourceService.getPathResource(storeName, relPath, userId);
		} catch (ServiceException e) {
			handleError("Error fetching path resource, " + e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}

		if (resource == null) {
			handleError("Returned PathResource object was null. storeName=" + storeName + ", relPath=" + relPath,
					WebExceptionType.CODE_IO_ERROR);
		}

		return mapToDto(resource);

	}
	
	/**
	 * Fetch the store's root directory path resource
	 * 
	 * @param storeName
	 * @param userId
	 *            - id of user performing the action
	 * @return
	 * @throws WebServiceException
	 */
	@GET
	@Path("/resource/userId/{userId}/storeName/{storeName}")
	@Produces(MediaType.APPLICATION_JSON)
	public PathResourceDto getPathResourceForStore(@PathParam("storeName") String storeName,
			@PathParam("userId") String userId) throws WebServiceException {

		validateUserId(userId);

		if (StringUtil.isNullEmpty(storeName)) {
			handleError("Missing storeName parameter", WebExceptionType.CODE_IO_ERROR);
		}
		storeName = storeName.trim();

		logger.info("Get Store: storeName=" + storeName);

		Store store = null;
		try {
			store = fileService.getStoreByName(storeName, userId);
		} catch (ServiceException e) {
			handleError("Error fetching store, storeName=" + storeName + ", " + e.getMessage(),
					WebExceptionType.CODE_IO_ERROR, e);
		}

		PathResource resource = null;
		try {
			resource = securePathResourceService.getPathResource(store.getNodeId(), userId);
		} catch (ServiceException e) {
			handleError("Error fetching path resource, " + e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}

		return mapToDto(resource);

	}	
	
	/**
	 * Fetch the parent of a resource, or null if the resource is a root node and
	 * has no parent.
	 * 
	 * @param nodeId
	 *            - id of the child node. the parent will be returned, or null if
	 *            the node is a root node and has no parent.
	 * @param userId
	 *            - id of user performing the action
	 * @return The parent resource, or null if the node is a root node and has no
	 *         parent
	 * @throws WebServiceException
	 */
	@GET
	@Path("/parent/resource/userId/{userId}/nodeId/{nodeId}")
	@Produces(MediaType.APPLICATION_JSON)
	public PathResourceDto getParentPathResourceById(@PathParam("nodeId") Long nodeId,
			@PathParam("userId") String userId) throws WebServiceException {

		validateUserId(userId);

		if (nodeId == null) {
			handleError("Missing nodeId parameter", WebExceptionType.CODE_IO_ERROR);
		}

		PathResource resource = null;
		try {
			resource = securePathResourceService.getParentPathResource(nodeId, userId);
		} catch (ServiceException e) {
			handleError("Error fetching parent path resource for node " + nodeId + ", " + e.getMessage(),
					WebExceptionType.CODE_IO_ERROR, e);
		}

		return mapToDto(resource);

	}
	
	/**
	 * Fetch the parent of a resource, or null if the resource is a root node and
	 * has no parent.
	 * 
	 * @param storeName
	 *            - store that the resource is located in
	 * @param relPath
	 *            - relative path of a resource in the store. this method will
	 *            return it's parent resource, or null if the resource has no
	 *            parent.
	 * @param userId
	 *            - id of user performing the action
	 * @return
	 * @throws WebServiceException
	 */
	@GET
	@Path("/parent/resource/userId/{userId}/path/{storeName}/{relPath:.+}")
	@Produces(MediaType.APPLICATION_JSON)
	public PathResourceDto getParentPathResourceByPath(@PathParam("storeName") String storeName,
			@PathParam("relPath") List<PathSegment> list, @PathParam("userId") String userId)
			throws WebServiceException {

		validateUserId(userId);

		if (StringUtil.isNullEmpty(storeName) || list == null || list.size() == 0) {
			handleError("Missing storeName, and/or relPath segment parameters", WebExceptionType.CODE_IO_ERROR);
		}
		StringBuffer relativePath = new StringBuffer();
		for (PathSegment ps : list) {
			relativePath.append(File.separator + ps.getPath().trim());
		}

		storeName = storeName.trim();
		String relPath = relativePath.toString().replace("\\", "/");

		logger.info("Get PathResouce: storeName=" + storeName + ", relPath=" + relPath);

		// fetch the resource
		PathResource resource = null;
		try {
			resource = securePathResourceService.getPathResource(storeName, relPath, userId);
		} catch (ServiceException e) {
			handleError("Error fetching path resource, " + e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}

		if (resource == null) {
			handleError("Returned PathResource object was null. storeName=" + storeName + ", relPath=" + relPath,
					WebExceptionType.CODE_IO_ERROR);
		}

		// fetch it's parent
		PathResource parentResource = null;
		try {
			parentResource = securePathResourceService.getParentPathResource(resource.getNodeId(), userId);
		} catch (ServiceException e) {
			handleError("Error fetching parent path resource for node " + resource.getNodeId() + ", storeName="
					+ storeName + ", rePath=" + relPath + ", " + e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}

		return mapToDto(parentResource);

	}

	/**
	 * Fetch all the first-level children for the resource
	 * 
	 * @param nodeId
	 *            - id of the child node. the parent will be returned, or null if
	 *            the node is a root node and has no parent.
	 * @param userId
	 *            - id of user performing the action
	 * @return A list of all the first-level resources under the node
	 * @throws WebServiceException
	 */
	@GET
	@Path("/child/resource/userId/{userId}/nodeId/{nodeId}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<PathResourceDto> getChildPathResourceById(@PathParam("nodeId") Long nodeId,
			@PathParam("userId") String userId) throws WebServiceException {

		validateUserId(userId);

		if (nodeId == null) {
			handleError("Missing nodeId parameter", WebExceptionType.CODE_IO_ERROR);
		}

		List<PathResource> children = null;
		try {
			children = securePathResourceService.getChildPathResources(nodeId, userId);
		} catch (ServiceException e) {
			handleError("Error fetching child path resources for node " + nodeId + ", " + e.getMessage(),
					WebExceptionType.CODE_IO_ERROR, e);
		}

		// return empty list if no children
		return mapToDto(children);

	}

	/**
	 * Fetch all the first-level children for the resource
	 * 
	 * @param storeName
	 *            - name of the store that the resources reside under
	 * @param relPath
	 *            - relative path of a resource within the store. this method will
	 *            fetch all the first-level children under that resource.
	 * @param userId
	 *            - id of user performing the action
	 * @return A list of all the first-level resources under the node
	 * @throws WebServiceException
	 */
	@GET
	@Path("/child/resource/userId/{userId}/path/{storeName}/{relPath:.+}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<PathResourceDto> getChildPathResourceByPath(@PathParam("storeName") String storeName,
			@PathParam("relPath") List<PathSegment> list, @PathParam("userId") String userId)
			throws WebServiceException {

		validateUserId(userId);

		if (StringUtil.isNullEmpty(storeName) || list == null || list.size() == 0) {
			handleError("Missing storeName, and/or relPath segment parameters", WebExceptionType.CODE_IO_ERROR);
		}
		StringBuffer relativePath = new StringBuffer();
		for (PathSegment ps : list) {
			relativePath.append(File.separator + ps.getPath().trim());
		}

		storeName = storeName.trim();
		String relPath = relativePath.toString().replace("\\", "/");

		logger.info("Get Child PathResouces: storeName=" + storeName + ", relPath=" + relPath);

		// fetch the resource
		PathResource resource = null;
		try {
			resource = securePathResourceService.getPathResource(storeName, relPath, userId);
		} catch (ServiceException e) {
			handleError("Error fetching path resource, " + e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}

		if (resource == null) {
			handleError("Returned PathResource object was null. storeName=" + storeName + ", relPath=" + relPath,
					WebExceptionType.CODE_IO_ERROR);
		}

		// fetch all the first-level resources under the resource
		List<PathResource> children = null;
		try {
			children = securePathResourceService.getChildPathResources(resource.getNodeId(), userId);
		} catch (ServiceException e) {
			handleError("Error fetching child path resources for node " + resource.getNodeId() + ", storeName="
					+ storeName + ", rePath=" + relPath + ", " + e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}

		// return empty list if no children
		return mapToDto(children);

	}

	/**
	 * Fetch all the parent tree (bottom-up) information for a specific node. This
	 * info can be use to build a link of bread crumbs, allowing a user to navigate
	 * back up the tree to the root node.
	 * 
	 * @param nodeId
	 * @param userId
	 *            - id of user performing the action
	 * @return
	 * @throws WebServiceException
	 */
	@GET
	@Path("/breadcrumb/userId/{userId}/nodeId/{nodeId}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<PathResourceDto> getBreadcrumbByNodeId(@PathParam("nodeId") Long nodeId,
			@PathParam("userId") String userId) throws WebServiceException {

		validateUserId(userId);

		if (nodeId == null) {
			handleError("Missing nodeId param.", WebExceptionType.CODE_IO_ERROR);
		}

		Tree<PathResource> tree = null;
		try {
			tree = securePathResourceService.buildParentPathResourceTree(nodeId, userId, false);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}

		if (tree == null) {
			handleError("Tree object was null for nodeId => " + nodeId, WebExceptionType.CODE_IO_ERROR);
		}

		List<PathResource> crumbs = new ArrayList<PathResource>();

		buildCrumbs(tree.getRootNode(), crumbs);

		// return empty list if no children
		return mapToDto(crumbs);

	}

	/**
	 * Fetch all the parent tree (bottom-up) information for a specific node. This
	 * info can be use to build a link of bread crumbs, allowing a user to navigate
	 * back up the tree to the root node.
	 * 
	 * @param storeName
	 *            - name of the store that the resources resides under
	 * @param relPath
	 *            - relative path of a resource within the store. this method will
	 *            fetch all parents (bottom-up) all the way to the root node.
	 * @param userId
	 *            - id of user performing the action
	 * @return
	 * @throws WebServiceException
	 */
	@GET
	@Path("/breadcrumb/path/userId/{userId}/{storeName}/{relPath:.+}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<PathResourceDto> getBreadcrumbByPath(@PathParam("storeName") String storeName,
			@PathParam("relPath") List<PathSegment> list, @PathParam("userId") String userId)
			throws WebServiceException {

		validateUserId(userId);

		if (StringUtil.isNullEmpty(storeName) || list == null || list.size() == 0) {
			handleError("Missing storeName, and/or relPath segment parameters", WebExceptionType.CODE_IO_ERROR);
		}
		StringBuffer relativePath = new StringBuffer();
		for (PathSegment ps : list) {
			relativePath.append(File.separator + ps.getPath().trim());
		}

		storeName = storeName.trim();
		String relPath = relativePath.toString().replace("\\", "/");

		logger.info("Getting breadcrumb: storeName=" + storeName + ", relPath=" + relPath);

		// fetch the resource
		PathResource resource = null;
		try {
			resource = securePathResourceService.getPathResource(storeName, relPath, userId);
		} catch (ServiceException e) {
			handleError("Error fetching path resource, " + e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
		if (resource == null) {
			handleError("Error fetching path resource, returned object was null. " + "storeName=" + storeName
					+ ", relPath=" + relPath, WebExceptionType.CODE_IO_ERROR);
		}

		Tree<PathResource> tree = null;
		try {
			tree = securePathResourceService.buildParentPathResourceTree(resource.getNodeId(), userId, false);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}

		if (tree == null) {
			handleError("Tree object was null for nodeId => " + resource.getNodeId(), WebExceptionType.CODE_IO_ERROR);
		}

		List<PathResource> crumbs = new ArrayList<PathResource>();

		buildCrumbs(tree.getRootNode(), crumbs);

		// return empty list if no children
		return mapToDto(crumbs);

	}

	private void buildCrumbs(TreeNode<PathResource> node, List<PathResource> crumbs) {
		crumbs.add(node.getData());
		if (node.hasChildren()) {
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
	@Path("/store/userId/{userId}/name/{storeName}")
	@Produces(MediaType.APPLICATION_JSON)
	public StoreDto getStoreByName(@PathParam("storeName") String storeName, @PathParam("userId") String userId)
			throws WebServiceException {

		validateUserId(userId);

		if (StringUtil.isNullEmpty(storeName)) {
			handleError("Missing storeName parameter", WebExceptionType.CODE_IO_ERROR);
		}
		storeName = storeName.trim();

		logger.info("Get Store: storeName=" + storeName);

		Store store = null;
		try {
			store = fileService.getStoreByName(storeName, userId);
		} catch (ServiceException e) {
			handleError("Error fetching store, storeName=" + storeName + ", " + e.getMessage(),
					WebExceptionType.CODE_IO_ERROR, e);
		}

		return storeMapper.map(store);

	}

	/**
	 * Fetch all stores
	 * 
	 * @param storeName
	 * @return
	 * @throws WebServiceException
	 */
	@GET
	@Path("/store/userId/{userId}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<StoreDto> getStores(@PathParam("userId") String userId) throws WebServiceException {

		validateUserId(userId);

		List<Store> stores = null;
		try {
			stores = fileService.getStores(userId);
		} catch (ServiceException e) {
			handleError("Error fetching stores, " + e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}

		return storeMapper.map(stores);

	}

}
