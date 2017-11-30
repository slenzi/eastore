/**
 * 
 */
package org.eamrf.eastore.core.service.tree.file.secure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.eamrf.concurrent.task.AbstractQueuedTask;
import org.eamrf.concurrent.task.QueuedTaskManager;
import org.eamrf.concurrent.task.TaskManagerProvider;
import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.FileUtil;
import org.eamrf.core.util.StringUtil;
import org.eamrf.eastore.core.aop.profiler.MethodTimer;
import org.eamrf.eastore.core.concurrent.StoreTaskManagerMap;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.messaging.ResourceChangeService;
import org.eamrf.eastore.core.properties.ManagedProperties;
import org.eamrf.eastore.core.service.tree.file.FileSystemUtil;
import org.eamrf.eastore.core.service.tree.file.PathResourceTreeLogger;
import org.eamrf.eastore.core.tree.Tree;
import org.eamrf.eastore.core.tree.TreeNode;
import org.eamrf.eastore.core.tree.TreeNodeVisitException;
import org.eamrf.eastore.core.tree.Trees;
import org.eamrf.eastore.core.tree.Trees.WalkOption;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.FileSystemRepository;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.BinaryResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.DirectoryResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.FileMetaResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.PathResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.ResourceType;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store.AccessRule;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * A version of org.eamrf.eastore.core.service.tree.file.PathResourceTreeService, but in this version we 
 * evaluate user access permissions using Gatekeeper when building the trees.
 * 
 * @author slenzi
 */
@Service
public class SecurePathResourceTreeService {

    @InjectLogger
    private Logger logger;
    
	// Spring Boot takes care of initializing the JdbcTemplate and DataSource, so we can simply autowire it! Magic!
    @Autowired
    private JdbcTemplate jdbcTemplate;    
    
    @Autowired
    private ManagedProperties appProps;     
    
    @Autowired
    private FileSystemRepository fileSystemRepository;
    
    @Autowired
    private FileSystemUtil fileSystemUtil;    
    
    @Autowired
    private TaskManagerProvider taskManagerProvider;
    
    @Autowired
    private PathResourceTreeLogger pathResTreeLogger;    
    
    @Autowired
    private SecurePathResourceTreeBuilder securePathResourceUtil;
    
    @Autowired
    private ResourceChangeService resChangeService;    
    
    // maps all stores to their task manager
    private Map<Store,StoreTaskManagerMap> storeTaskManagerMap = new HashMap<Store,StoreTaskManagerMap>();
	    
	
	/**
	 * 
	 */
	public SecurePathResourceTreeService() {
		
	}
	
	/**
	 * Create queued task managers for all stores. Any SQL update operations are queued per store.
	 * 
	 * Each store gets two task managers, one manager for tasks that involve adding binary data
	 * to the database, and one manager for everything else.
	 */
	@MethodTimer
	@PostConstruct
	public void init(){
		
		List<Store> stores = null;
		
		try {
			stores = getStores();
		} catch (ServiceException e) {
			logger.error("Failed to fetch list of stores. Cannot initialize task managers. " + e.getMessage(), e);
			return;
		}
		
		if(stores == null){
			logger.warn("No stores found when initializing File System Service...");
			return;
		}
		
		for(Store store : stores){
			
			logger.info("Creating queued task managers for store [storeId=" + store.getId() + ", storeName=" + store.getName() + "]");
			
			QueuedTaskManager generalManager = taskManagerProvider.createQueuedTaskManager();
			QueuedTaskManager binaryManager = taskManagerProvider.createQueuedTaskManager();
			
			generalManager.setManagerName("General Task Manager [storeId=" + store.getId() + ", storeName=" + store.getName() + "]");
			binaryManager.setManagerName("Binary Task Manager [storeId=" + store.getId() + ", storeName=" + store.getName() + "]");
			
			ExecutorService generalExecutor = Executors.newSingleThreadExecutor();
			ExecutorService binaryExecutor = Executors.newSingleThreadExecutor();
			
			generalManager.startTaskManager(generalExecutor);
			binaryManager.startTaskManager(binaryExecutor);
			
			StoreTaskManagerMap mapEntry = new StoreTaskManagerMap(store, generalManager, binaryManager);
			storeTaskManagerMap.put(store, mapEntry);
			
		}
		
	}	
	
	/**
	 * Get a list of PathResource starting at the specified dirNodeId, but only up to a specified depth.
	 * With this information you can build a tree. This will not contain the binary data for files.
	 * 
	 * This is functionally equivalent to ClosureService.getChildMappings(Long nodeId, int depth)
	 * 
	 * @param nodeId
	 * @param depth
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public List<PathResource> getPathResourceTree(Long nodeId, int depth) throws ServiceException {
		
		List<PathResource> resources = null;
		try {
			resources = fileSystemRepository.getPathResourceTree(nodeId, depth);
		} catch (Exception e) {
			throw new ServiceException("Error getting PathResource tree for node " + 
					nodeId + ". " + e.getMessage(), e);
		}
		return resources;		
		
	}
	
	/**
	 * Fetch bottom-up (leaf node to root node) PathResource list, up to a specified levels up. This can
	 * be used to build a tree (or more of a single path) from root to leaf.
	 * 
	 * This is functionally equivalent to ClosureService.getParentMappings(Long nodeId, int depth)
	 * 
	 * @param nodeId
	 * @param levels
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public List<PathResource> getParentPathResourceTree(Long nodeId, int levels) throws ServiceException {
		
		List<PathResource> resources = null;
		try {
			resources = fileSystemRepository.getParentPathResourceTree(nodeId, levels);
		} catch (Exception e) {
			throw new ServiceException("Error getting PathResource tree for node " + 
					nodeId + ". " + e.getMessage(), e);
		}
		return resources;		
		
	}	
	
	/**
	 * Build a top-down (from root node to leaf nodes) tree of PathResource objects.
	 * 
	 * Access permissions will be evaluated for the user using the Gatekeeper groups assigned to
	 * the resources.
	 * 
	 * @param dirNodeId - Id of node which will become the root of the tree.
	 * @param userId - User ID used to evaluate access permissions (e.g. CTEP ID).
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public Tree<PathResource> buildPathResourceTree(Long dirNodeId, String userId) throws ServiceException {
		
		DirectoryResource dirResource = this.getDirectory(dirNodeId, userId);
		
		return this.buildPathResourceTree(dirResource, userId, Integer.MAX_VALUE);
		
	}
	
	/**
	 * Build a top-down (from root node to leaf nodes) tree of PathResource objects.
	 * 
	 * Access permissions will be evaluated for the user using the Gatekeeper groups assigned to
	 * the resources.
	 * 
	 * @param dirResource - directory which will become the root of the tree
	 * @param userId - User ID used to evaluate access permissions (e.g. CTEP ID).
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public Tree<PathResource> buildPathResourceTree(DirectoryResource dirResource, String userId) throws ServiceException {
		
		return this.buildPathResourceTree(dirResource, userId, Integer.MAX_VALUE);
		
	}
	
	/**
	 * Build a top-down (from root node to leaf nodes) tree of PathResource objects.
	 * 
	 * Access permissions will be evaluated for the user using the Gatekeeper groups assigned to
	 * the resources.
	 * 
	 * @param dirNodeId - Id of node which will become the root of the tree.
	 * @param userId - User ID used to evaluate access permissions (e.g. CTEP ID).
	 * @param depth - depth of child nodes to include.
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public Tree<PathResource> buildPathResourceTree(Long dirNodeId, String userId, int depth) throws ServiceException {
		
		DirectoryResource dirResource = this.getDirectory(dirNodeId, userId);
		
		return this.buildPathResourceTree(dirResource, userId, Integer.MAX_VALUE);		
		
	}	
	
	/**
	 * Build a top-down (from root node to leaf nodes) tree of PathResource objects,
	 * but only include nodes up to a specified depth.
	 * 
	 * @param dirResource - directory which will become the root of the tree
	 * @param userId - User ID used to evaluate access permissions (e.g. CTEP ID).
	 * @param depth - depth of child nodes to include.
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public Tree<PathResource> buildPathResourceTree(DirectoryResource dirResource, String userId, int depth) throws ServiceException {
		
		List<PathResource> resources = this.getPathResourceTree(dirResource.getNodeId(), depth);
		
		if(resources == null || resources.size() == 0){
			throw new ServiceException("No top-down PathResource tree for directory node " + dirResource.getNodeId() + 
					". Returned list was null or empty.");
		}
		
		return securePathResourceUtil.buildPathResourceTree(resources, userId, dirResource);		
		
	}
	
	/**
	 * Build a bottom-up (leaf node to root node) tree of PathResource objects.
	 * 
	 * @param nodeId - Id of the child node which will become the root of the tree.
	 * @param userId - User ID used to evaluate access permissions (e.g. CTEP ID).
	 * @param reverse - get the tree in reverse order (leaf node becomes root node)
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public Tree<PathResource> buildParentPathResourceTree(Long nodeId, String userId, boolean reverse) throws ServiceException {
		
		return buildParentPathResourceTree(nodeId, userId, Integer.MAX_VALUE, reverse);
		
	}
	
	/**
	 * Build a bottom-up (leaf node to root node) tree of PathResource objects, but only up
	 * to a specified number of levels.
	 * 
	 * In order to properly evaluate the permissions we need ALL parent nodes, all the way
	 * to the root node for the store. This is because permission on resources are inherited
	 * from their parent directory resources.
	 * 
	 * @param nodeId - Id of the child node which will become the root of the tree
	 * @param userId - User ID used to evaluate access permissions (e.g. CTEP ID).
	 * @param levels - number of parent levels to include.
	 * @param reverse - get the tree in reverse order (leaf node becomes root node)
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	private Tree<PathResource> buildParentPathResourceTree(Long nodeId, String userId, int levels, boolean reverse) throws ServiceException {
		
		List<PathResource> resources = getParentPathResourceTree(nodeId, levels);
		
		if(resources == null || resources.size() == 0){
			throw new ServiceException("No bottom-up PathResource tree for nodeId " + nodeId + 
					". Returned list was null or empty.");
		}
		
		return securePathResourceUtil.buildParentPathResourceTree(resources, userId, reverse);		
		
	}

	/**
	 * Build a bottom-up (leaf node to root node) tree of PathResource objects.
	 * 
	 * @param storeName - name of the store
	 * @param relativePath - path of resource relative to it's tore
	 * @param userId - User ID used to evaluate access permissions (e.g. CTEP ID).
	 * @param reverse - get the tree in reverse order (leaf node becomes root node)
	 * @return
	 */
	@MethodTimer
	public Tree<PathResource> buildParentPathResourceTree(String storeName, String relativePath, String userId, boolean reverse) throws ServiceException {
		
		PathResource resource = null;
		try {
			resource = fileSystemRepository.getPathResource(storeName, relativePath);
		} catch (Exception e) {
			throw new ServiceException("Error fetching PathResource for storeName " + storeName + 
					" and relativePath " + relativePath + ", " + e.getMessage(), e);
		}
		
		return buildParentPathResourceTree(resource.getNodeId(), userId, reverse);
		
	}
	
	/**
	 * Fetch a PathResource by Id, and evaluate the permissions.
	 * 
	 * In order to properly evaluate the permissions we have to fetch the entire parent tree,
	 * since resource inherit permissions from their parent (directory) resources.
	 * 
	 * @param nodeId
	 * @param userId
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public PathResource getPathResource(Long nodeId, String userId) throws ServiceException {
		
		Tree<PathResource> tree = this.buildParentPathResourceTree(nodeId, userId, true);
		
		return tree.getRootNode().getData();
		
	}
	
	/**
	 * Fetch a PathResource by store name and relative path of resource, and evaluate the permissions.
	 * 
	 * In order to properly evaluate the permissions we have to fetch the entire parent tree,
	 * since resource inherit permissions from their parent (directory) resources.
	 * 
	 * @param storeName
	 * @param relativePath
	 * @param userId
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public PathResource getPathResource(String storeName, String relativePath, String userId) throws ServiceException {
		
		Tree<PathResource> tree = this.buildParentPathResourceTree(storeName, relativePath, userId, true);
		
		return tree.getRootNode().getData();
		
	}
	
	/**
	 * TODO - Have to test to make sure this works.
	 * 
	 * Fetch the parent path resource for the specified node. If the node is a root node, and
	 * has no parent, then null will be returned.
	 * 
	 * @param nodeId
	 * @param userId
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public PathResource getParentPathResource(Long nodeId, String userId) throws ServiceException {
		
		Tree<PathResource> tree = this.buildParentPathResourceTree(nodeId, userId, true);
		
		PathResource resource = tree.getRootNode().getData();
		if(resource.getNodeId().equals(nodeId) && resource.getParentNodeId().equals(0L)){
			// this is a root node with no parent
			return null;
		}else{
			// tree is in reverse order, so root node is the child and child is the parent :-)
			return tree.getRootNode().getFirstChild().getData();
		}
		
	}
	
	/**
	 * TODO - Have to test to make sure this works.
	 * 
	 * Fetch the parent path resource for the specified node. If the node is a root node, and
	 * has no parent, then null will be returned.
	 * 
	 * @param storeName
	 * @param relativePath
	 * @param userId
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public PathResource getParentPathResource(String storeName, String relativePath, String userId) throws ServiceException {
		
		Tree<PathResource> tree = this.buildParentPathResourceTree(storeName, relativePath, userId, true);
		
		PathResource resource = tree.getRootNode().getData();
		if(resource.getRelativePath().equals(relativePath) && resource.getParentNodeId().equals(0L)){
			// this is a root node with no parent
			return null;
		}else{
			// tree is in reverse order, so root node is the child and child is the parent :-)
			return tree.getRootNode().getFirstChild().getData();
		}
		
	}
	
	/**
	 * TODO - have to test to make sure this works
	 * 
	 * Fetch the first level children for the path resource. This is only applicable for
	 * directory resources
	 * 
	 * @param dirId - id of directory node
	 * @param userId - id of user completing the action
	 * @return
	 * @throws ServiceException
	 */
	public List<PathResource> getChildPathResources(Long dirId, String userId) throws ServiceException {
		
		// this function will properly evaluate the permissions for the parent directory
		// by fetching the entire parent tree and evaluating all parent permissions.
		Tree<PathResource> tree = this.buildPathResourceTree(dirId, userId, 1);
		
		if(tree.getRootNode().hasChildren()){
			return tree.getRootNode().getChildren()
				.stream()
				.map((treeNode) -> { return treeNode.getData(); } )
				.collect(Collectors.toList());
		}
		
		return null;
		
	}
	
	/**
	 * Fetch first-level resource, by name (case insensitive), from the directory, provided one exists.
	 * 
	 * @param dirId
	 * @param dirName
	 * @param userId
	 * @return
	 * @throws ServiceException
	 */
	public PathResource getChildResource(Long dirId, String name, ResourceType type, String userId) throws ServiceException {
		
		List<PathResource> childResources = this.getChildPathResources(dirId, userId);
		if(childResources == null || childResources.size() == 0) {
			return null;
		}
		for(PathResource pr : childResources){
			if(pr.getParentNodeId().equals(dirId)
					&& pr.getResourceType() == type
					&& pr.getPathName().toLowerCase().equals(name.toLowerCase())){
				
				return pr;
				
			}
		}
		return null;
		
	}
	
	/**
	 * Fetch first-level directory resource, by name (case insensitive), from the directory, provided one exists.
	 * 
	 * @param dirId
	 * @param name
	 * @param userId
	 * @return
	 * @throws ServiceException
	 */
	public DirectoryResource getChildDirectoryResource(Long dirId, String name, String userId) throws ServiceException {
		
		PathResource resource = this.getChildResource(dirId, name, ResourceType.DIRECTORY, userId);
		if(resource != null) {
			return (DirectoryResource)resource;
		}
		return null;
		
	}
	
	/**
	 * Fetch first-level file meta resource, by name (case insensitive), from the directory, provided one exists.
	 * 
	 * @param dirId
	 * @param name
	 * @param userId
	 * @return
	 * @throws ServiceException
	 */
	public FileMetaResource getChildFileMetaResource(Long dirId, String name, String userId) throws ServiceException {
		
		PathResource resource = this.getChildResource(dirId, name, ResourceType.FILE, userId);
		if(resource != null) {
			return (FileMetaResource)resource;
		}
		return null;
		
	}	
	
	/**
	 * Fetch a DirectoryResource by id
	 * 
	 * In order to properly evaluate the permissions we have to fetch the entire parent tree,
	 * since resource inherit permissions from their parent (directory) resources.
	 * 
	 * @param nodeId
	 * @param userId
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public DirectoryResource getDirectory(Long nodeId, String userId) throws ServiceException {
		
		PathResource resource = this.getPathResource(nodeId, userId);
		
		if(resource == null){
			throw new ServiceException("Failed to get directory by id, no path resource for nodeId=" + nodeId + ". Returned object was null.");
		}
		if(resource.getResourceType() == ResourceType.DIRECTORY){
			return (DirectoryResource)resource;
		}else{
			throw new ServiceException("Error fetching directory resource, nodeId => " + nodeId + " is not a directory resource.");
		}		
		
	}
	
	/**
	 * Fetch a DirectoryResource by store name and resource relative path.
	 * 
	 * In order to properly evaluate the permissions we have to fetch the entire parent tree,
	 * since resource inherit permissions from their parent (directory) resources.
	 * 
	 * @param storeName
	 * @param relativePath
	 * @param userId
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public DirectoryResource getDirectory(String storeName, String relativePath, String userId) throws ServiceException {
		
		PathResource resource = this.getPathResource(storeName, relativePath, userId);
		
		if(resource == null){
			throw new ServiceException("Failed to get directory by store name and resource relative path, "
					+ "returned object was null. storeName=" + storeName + ", relativePath=" + relativePath);
		}
		if(resource.getResourceType() == ResourceType.DIRECTORY){
			return (DirectoryResource)resource;
		}else{
			throw new ServiceException("Error fetching directory resource, storeName=" + storeName + 
					", relativePath=" + relativePath + ", is not a directory resource.");
		}		
		
	}
	
	/**
	 * Fetch the parent directory of a resource
	 * 
	 * @param nodeId - id of resource. If this is an ID of a root directory of a store, then there is no parent. Null will be returned.
	 * @param userId - id of user completing the action
	 * @return The parent directory of the resource, or null if the resource has not parent (root directories have not parent.)
	 * @throws ServiceException
	 */
	@MethodTimer
	public DirectoryResource getParentDirectory(Long nodeId, String userId) throws ServiceException {
		
		PathResource parent = this.getParentPathResource(nodeId, userId);
		
		if(parent == null) {
			return null;
		}
		if(parent.getResourceType() == ResourceType.DIRECTORY) {
			return (DirectoryResource)parent;
		}else {
			throw new ServiceException("The parent resource for resource with id " + nodeId + " is not a directory resource, yet it should be.");
		}
		
	}	
	
	/**
	 * Fetch FileMetaResource by ID.
	 * 
	 * In order to properly evaluate the permissions we have to fetch the entire parent tree,
	 * since resource inherit permissions from their parent (directory) resources.
	 * 
	 * @param nodeId
	 * @param userId
	 * @param includeBinary
	 * @return
	 * @throws Exception
	 */
	@MethodTimer
	public FileMetaResource getFileMetaResource(Long nodeId, String userId, boolean includeBinary) throws ServiceException {
		
		PathResource resource = this.getPathResource(nodeId, userId);
		if(resource == null){
			throw new ServiceException("Failed to get file meta resource by node id, returned object was null. "
					+ "nodeId=" + nodeId + ", includeBinary=" + includeBinary);
		}
		if(resource.getResourceType() == ResourceType.FILE){
			FileMetaResource fileMeta = (FileMetaResource)resource;
			if(includeBinary){
				fileMeta = populateWithBinaryData(fileMeta);
			}
			return fileMeta;
		}else{
			throw new ServiceException("Error fetching file meta resource for nodeId=" + nodeId + 
					". Path resource is not a file meta resource.");
		}
		
	}
	
	/**
	 * Fetch FileMetaResource by store name, and resource relative path.
	 * 
	 * In order to properly evaluate the permissions we have to fetch the entire parent tree,
	 * since resource inherit permissions from their parent (directory) resources.
	 * 
	 * @param storeName
	 * @param relativePath
	 * @param includeBinary
	 * @return
	 * @throws Exception
	 */
	@MethodTimer
	public FileMetaResource getFileMetaResource(String storeName, String relativePath, String userId, boolean includeBinary) throws ServiceException {
		
		PathResource resource = this.getPathResource(storeName, relativePath, userId);
		if(resource == null){
			throw new ServiceException("Failed to get file meta resource by store name and resource relative path, returned object was null. "
					+ "storeName=" + storeName + ", relativePath=" + relativePath + ", includeBinary=" + includeBinary);
		}
		if(resource.getResourceType() == ResourceType.FILE){
			FileMetaResource fileMeta = (FileMetaResource)resource;
			if(includeBinary){
				fileMeta = populateWithBinaryData(fileMeta);
			}
			return fileMeta;
		}else{
			throw new ServiceException("Error fetching file meta resource, storeName=" + storeName + 
					", relativePath=" + relativePath + ", includeBinary=" + includeBinary + 
					". Path resource is not a file meta resource.");
		}
		
	}
	
	/**
	 * Adds a BinaryResource object to the FileMetaResource with either the byte[] data from
	 * the database (if it exists) or from the local file system.
	 * 
	 * @param resource
	 * @return
	 * @throws Exception
	 */
	private FileMetaResource populateWithBinaryData(FileMetaResource resource) throws ServiceException {
		
		// error checking
		if(resource == null){
			throw new ServiceException("FileMetaResource object is null. Cannot populate FileMetaResource with binary data.");
		}else if(resource.getNodeId() == null){
			throw new ServiceException("FileMetaResource nodeId value is null. Cannot populate FileMetaResource with binary data.");
		}
		
		// get binary data from database
		if(resource.getIsBinaryInDatabase()){
			
			BinaryResource binRes = jdbcTemplate.queryForObject(
					"select node_id, file_data from eas_binary_resource where node_id = ?",
					new Object[]{ resource.getNodeId() }, (rs, rowNum) -> {
						BinaryResource br = new BinaryResource();
						br.setNodeId(rs.getLong("node_id"));
						br.setFileData(rs.getBytes(2));
						return br;
					});
			resource.setBinaryResource(binRes);
			return resource;
		
		// else get data from local file system
		}else{
		
			Store store = resource.getStore();
			if(store == null){
				Long storeId = resource.getStoreId();
				if(storeId == null){
					throw new ServiceException("FileMetaResource storeId value is null. Need store path information to read file "
							+ "data from local file system. Cannot populate FileMetaResource with binary data.");
				}
				store = getStoreById(storeId);
				if(store == null){
					throw new ServiceException("Failed to fetch store from DB for FileMetaResource, returned store object "
							+ "was null, storeId => " + storeId + " Cannot populate FileMetaResource with binary data.");
				}
				resource.setStore(store);
			}
			Path pathToFile = fileSystemUtil.buildPath(store, resource);
			if(!Files.exists(pathToFile)){
				throw new ServiceException("Error, file on local file system does not exist for FileMetaResource "
						+ "with file node id => " + resource.getNodeId() + ", path => " + pathToFile.toString() +
						". Cannot populate FileMetaResource with binary data.");				
			}
			byte[] fileBytes = null;
			try {
				fileBytes = Files.readAllBytes(pathToFile);
			} catch (IOException e) {
				throw new ServiceException("Error reading byte data from file on local file system, file resource id = " + resource.getNodeId() + 
						", resource relative path = " + resource.getRelativePath() + ", file path on disk = " + pathToFile.toString());
			}
			BinaryResource br = new BinaryResource();
			br.setNodeId(resource.getNodeId());
			br.setFileData(fileBytes);
			resource.setBinaryResource(br);
			
			return resource;
			
		}
		
	}	

	/**
	 * Create a new store, and create it's queued task manager
	 * 
	 * @param storeName
	 * @param storeDesc
	 * @param storePath
	 * @param rootDirName
	 * @param rootDirDesc
	 * @param maxFileSizeDb
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public Store addStore(
			String storeName, 
			String storeDesc, 
			Path storePath, 
			String rootDirName, 
			String rootDirDesc, 
			Long maxFileSizeDb,
			String readGroup,
			String writeGroup,
			String executeGroup,
			AccessRule rule) throws ServiceException {
		
		if(StringUtil.isAnyNullEmpty(storeName, storeDesc, rootDirName, rootDirDesc) || storePath == null || maxFileSizeDb == null){
			throw new ServiceException("Missing required parameter for creating a new store.");
		}
		
		// store name and root dir name are used for the directory names. some environments are case insensitive
		// so we make everything lowercase
		// storePath should all be lowercase as well...
		storeName = storeName.toLowerCase();
		rootDirName = rootDirName.toLowerCase();
		
		Store store = getStoreByName(storeName);
		if(store != null){
			throw new ServiceException("Store with name '" + storeName + "' already exists. Store names must be unique.");
		}
		
		try {
			store = fileSystemRepository.addStore(
					storeName, 
					storeDesc, 
					storePath, 
					rootDirName, 
					rootDirDesc, 
					maxFileSizeDb,
					readGroup,
					writeGroup,
					executeGroup,
					rule);
		} catch (Exception e) {
			throw new ServiceException("Error creating new store '" + storeName + "' at " + storePath.toString(), e);
		}
		
		QueuedTaskManager generalManager = taskManagerProvider.createQueuedTaskManager();
		QueuedTaskManager binaryManager = taskManagerProvider.createQueuedTaskManager();
		
		generalManager.setManagerName("General Task Manager [storeId=" + store.getId() + ", storeName=" + store.getName() + "]");
		binaryManager.setManagerName("Binary Task Manager [storeId=" + store.getId() + ", storeName=" + store.getName() + "]");
		
		ExecutorService generalExecutor = Executors.newSingleThreadExecutor();
		ExecutorService binaryExecutor = Executors.newSingleThreadExecutor();
		
		generalManager.startTaskManager(generalExecutor);
		binaryManager.startTaskManager(binaryExecutor);
		
		StoreTaskManagerMap mapEntry = new StoreTaskManagerMap(store, generalManager, binaryManager);
		storeTaskManagerMap.put(store, mapEntry);		
		
		return store;
		
	}
	
	/**
	 * Fetch the general queued task manager for the store;
	 * 
	 * @param store
	 * @return
	 */
	private QueuedTaskManager getGeneralTaskManagerForStore(Store store){
		
		StoreTaskManagerMap map = storeTaskManagerMap.get(store);
		return map.getGeneralTaskManager();
		
	}
	
	/**
	 * Fetch the binary queued task manager for the store;
	 * 
	 * @param store
	 * @return
	 */
	private QueuedTaskManager getBinaryTaskManagerForStore(Store store){
		
		StoreTaskManagerMap map = storeTaskManagerMap.get(store);
		return map.getBinaryTaskManager();
		
	}	
	
	/**
	 * fetch a store by id
	 * 
	 * @param storeId
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public Store getStoreById(Long storeId) throws ServiceException {
		
		Store store = null;
		try {
			store = fileSystemRepository.getStoreById(storeId);
		} catch (Exception e) {
			throw new ServiceException("Failed to get store for store id => " + storeId, e);
		}
		return store;
		
	}
	
	/**
	 * fetch a store by name
	 * 
	 * @param storeName
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public Store getStoreByName(String storeName) throws ServiceException {
		
		Store store = null;
		try {
			store = fileSystemRepository.getStoreByName(storeName);
		} catch (Exception e) {
			throw new ServiceException("Failed to get store for store name => " + storeName, e);
		}
		return store;
		
	}	
	
	/**
	 * Fetch the store object from the PathResource object. If the store object is null then
	 * this method will attempt to fetch it from the database using the store id.
	 * 
	 * @param r
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public Store getStore(PathResource r) throws ServiceException {
		
		if(r == null){
			throw new ServiceException("Cannot get store from PathResource, the PathResource object is null");
		}
		Store s = r.getStore();
		if(s != null){
			return s;
		}
		if(r.getStoreId() == null){
			throw new ServiceException("Cannot get store from PathResource, the PathResource storeId value is null");
		}
		return getStoreById(r.getStoreId());
		
	}	
	
	/**
	 * Fetch all stores
	 * 
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public List<Store> getStores() throws ServiceException {
		
		List<Store> stores = null;
		try {
			stores = fileSystemRepository.getStores();
		} catch (Exception e) {
			throw new ServiceException("Failed to fetch all stores, " + e.getMessage(), e);
		}
		return stores;
	}
	
	/**
	 * Adds new file to the database, then spawns a non-blocking child task for adding/refreshing the
	 * binary data in the database.
	 * 
	 * @param dirNodeId - id of directory where file will be added
	 * @param userId - Id of the user adding the file
	 * @param filePath - path to file on local file system
	 * @param replaceExisting - pass true to overwrite any file with the same name that's already in the directory tree. If you
	 * pass false, and there exists a file with the same name (case insensitive) then an ServiceException will be thrown.
	 * @return A new FileMetaResource object (without the binary data)
	 * @throws ServiceException
	 */
	@MethodTimer
	public FileMetaResource addFile(Long dirNodeId, String userId, Path filePath, boolean replaceExisting) throws ServiceException {
		
		final DirectoryResource dirRes = this.getDirectory(dirNodeId, userId);
		
		return addFile(dirRes, filePath, replaceExisting, userId);
		
	}
	
	/**
	 * Adds new file to the database, then spawns a non-blocking child task for adding/refreshing the
	 * binary data in the database
	 * 
	 * @param storeName - name of the store
	 * @param dirRelPath - relative path of directory resource within the store
	 * @param userId - Id of the user adding the file
	 * @param filePath - path to file on local file system
	 * @param replaceExisting - pass true to overwrite any file with the same name that's already in the directory tree. If you
	 * pass false, and there exists a file with the same name (case insensitive) then an ServiceException will be thrown.
	 * @return A new FileMetaResource object (without the binary data)
	 * @throws ServiceException
	 */
	@MethodTimer
	public FileMetaResource addFile(String storeName, String dirRelPath, String userId, Path filePath, boolean replaceExisting) throws ServiceException {
		
		final DirectoryResource dirRes = this.getDirectory(storeName, dirRelPath, userId);
		
		return addFile(dirRes, filePath, replaceExisting, userId);		
		
	}
	
	/**
	 * Adds new file to the database, then spawns a non-blocking child task for adding/refreshing the
	 * binary data in the database
	 * 
	 * @param toDir - directory where file will be added
	 * @param filePath - path to file on local file system
	 * @param replaceExisting - pass true to overwrite any file with the same name that's already in the directory tree. If you
	 * pass false, and there exists a file with the same name (case insensitive) then an ServiceException will be thrown.
	 * @param userId - Id of the user adding the file
	 * @return A new FileMetaResource object (without the binary data)
	 * @throws ServiceException
	 */
	@MethodTimer
	public FileMetaResource addFile(DirectoryResource toDir, Path filePath, boolean replaceExisting, String userId) throws ServiceException {
		
		// user must have write permission on destination directory
		if(!toDir.getCanWrite()){
			this.handlePermissionDenied(PermissionError.WRITE, toDir, userId);
		}		
		
		final Store store = getStore(toDir);
		final QueuedTaskManager generalTaskManager = getGeneralTaskManagerForStore(store);
		final QueuedTaskManager binaryTaskManager = getBinaryTaskManagerForStore(store);		
		
		//
		// parent task adds file meta data to database, spawns child task for refreshing binary data
		// in the database, then returns quickly. We must wait (block) for this parent task to complete.
		//
		class AddFileTask extends AbstractQueuedTask<FileMetaResource> {
			public FileMetaResource doWork() throws ServiceException {
				
				logger.info("---- ADDING FILE WITHOUT BINARY DATA FOR FILE " + filePath.toString() + " (START)");
				
				String fileName = filePath.getFileName().toString();
				FileMetaResource existingResource = getChildFileMetaResource(toDir.getNodeId(), fileName, userId);
				boolean haveExisting = existingResource != null ? true : false;
				
				FileMetaResource newOrUpdatedFileResource = null;
				if(haveExisting){
					if(!replaceExisting){
						throw new ServiceException(" Directory [id=" + toDir.getNodeId() + ", relPath=" + toDir.getRelativePath() + "] "
								+ "already contains a file with the name '" + fileName + "', and 'replaceExisting' param is set to false.");
					
					// files no longer have their own permissions. It's now controlled by permissions on the directory they are in
					//}else if(!existingResource.getCanWrite()){
					//	(PermissionError.WRITE, existingResource, toDir, userId);						
					
					}else{
						// update existing file (remove current binary data, and update existing file meta data)
						try {
							newOrUpdatedFileResource = fileSystemRepository._updateFileDiscardOldBinary(toDir, filePath, existingResource);
						} catch (Exception e) {
							throw new ServiceException("Error updating existing file [id=" + existingResource.getNodeId() + ", relPath=" + existingResource.getRelativePath() + "] in "
									+ "directory [id=" + toDir.getNodeId() + ", relPath=" + toDir.getRelativePath() + "], " + e.getMessage());
						}
					}
						
				}else{
					try {
						newOrUpdatedFileResource = fileSystemRepository._addNewFileWithoutBinary(toDir, filePath);
					} catch (Exception e) {
						throw new ServiceException("Error adding new file " + filePath.toString() + " to "
								+ "directory [id=" + toDir.getNodeId() + ", relPath=" + toDir.getRelativePath() + "], " + e.getMessage());
					}
				}
				
				logger.info("---- ADDING FILE WITHOUT BINARY DATA FOR FILE " + filePath.toString() + " (END)");
				
				//
				// child task refreshes the binary data in the database. We do not need to wait (block) for this to finish
				//
				final FileMetaResource finalFileMetaResource = newOrUpdatedFileResource;
				class RefreshBinaryTask extends AbstractQueuedTask<Void> {
					public Void doWork() throws ServiceException {

						logger.info("---- REFRESH BINARY DATA IN DB FOR FILE " + finalFileMetaResource.getRelativePath() + "(START)");
						try {
							fileSystemRepository.refreshBinaryDataInDatabase(finalFileMetaResource);
						} catch (Exception e) {
							throw new ServiceException("Error refreshing (or adding) binary data in database (eas_binary_resource) "
									+ "for file resource node => " + finalFileMetaResource.getNodeId(), e);
						}
						logger.info("---- REFRESH BINARY DATA IN DB FOR FILE " + finalFileMetaResource.getRelativePath() + "(END)");
						return null;				
						
					}
					public Logger getLogger() {
						return logger;
					}
				}
				
				// add to binary task manager (not general task manager)
				RefreshBinaryTask refreshTask = new RefreshBinaryTask();
				refreshTask.setName("Refresh binary data in DB [" + newOrUpdatedFileResource.toString() + "]");
				binaryTaskManager.addTask(refreshTask);				
				
				return newOrUpdatedFileResource;				
			}
			public Logger getLogger() {
				return logger;
			}
		};
		
		AddFileTask addTask = new AddFileTask();
		addTask.setName("Add File Without Binary [dirNodeId=" + toDir.getNodeId() + ", filePath=" + filePath + 
				", replaceExisting=" + replaceExisting + "]");
		generalTaskManager.addTask(addTask);
		
		FileMetaResource fileMetaResource = addTask.get(); // block until finished
		
		// broadcast directory contents changed event
		resChangeService.directoryContentsChanged(toDir.getNodeId());
		
		return fileMetaResource;
		
	}
	
	/*
	private FileMetaResource getFileInDirectory(Long dirNodeId, String userId, String fileName) throws ServiceException {
		
		List<PathResource> childResources = getChildPathResources(dirNodeId, userId);
		
		if(childResources == null || childResources.size() == 0){
			return null;
		}
		
		for(PathResource res : childResources){
			if(res.getParentNodeId().equals(dirNodeId) && res.getResourceType() == ResourceType.FILE 
					&& res.getPathName().toLowerCase().equals(fileName.toLowerCase())){
				return (FileMetaResource)res;
			}
		}
		return null;
		
	}
	*/	
	
	/**
	 * Renames the path resource. If the path resource is a FileMetaResource then we simply
	 * rename the file. If the path resource is a DirectoryResource then we recursively walk
	 * the tree to rename the directory, and update the relative path data for all resources
	 * under the directory.
	 * 
	 * @param nodeId - id of resource to rename
	 * @param newName - new name for resource
	 * @param userId - id of user performing the rename action
	 * @throws ServiceException
	 */
	@MethodTimer
	public void renamePathResource(Long nodeId, String newName, String userId) throws ServiceException {

		// works for both files and directories. Files will inherit their permissions from the directories they are in.
		
		PathResource resource = this.getPathResource(nodeId, userId);
		if(!resource.getCanWrite()) {
			this.handlePermissionDenied(PermissionError.WRITE, resource, userId);
		}
		
		try {
			fileSystemRepository.renamePathResource(resource, newName);
		} catch (Exception e) {
			throw new ServiceException("Error renaming path resource, nodeId=" + nodeId + 
					", newName=" + newName + ", " + e.getMessage(), e);
		}
		
	}
	
	/**
	 * Add new directory
	 * 
	 * @param dirNodeId - id of parent directory
	 * @param name - name of new directory
	 * @param desc - description for new directory
	 * @param userId - id of user performing the action
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public DirectoryResource addDirectory(
			Long dirNodeId, String name, String desc, String userId) throws ServiceException {
		
		final DirectoryResource resource = this.getDirectory(dirNodeId, userId);		
		
		return addDirectory(resource, name, desc, null, null, null, userId);
		
	}	
	
	/**
	 * Add new directory
	 * 
	 * @param dirNodeId - id of parent directory
	 * @param name - name of new directory
	 * @param desc - description for new directory
	 * @param readGroup1 - optional read group
	 * @param writeGroup1 - optional write group
	 * @param executeGroup1 - optional execute group
	 * @param userId - id of user performing the action
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public DirectoryResource addDirectory(
			Long dirNodeId, String name, String desc, String readGroup1, String writeGroup1, 
			String executeGroup1, String userId) throws ServiceException {
		
		final DirectoryResource resource = this.getDirectory(dirNodeId, userId);		
		
		return addDirectory(resource, name, desc, readGroup1, writeGroup1, executeGroup1, userId);
		
	}
	
	/**
	 * Add new directory
	 * 
	 * @param parentDir - the parent directory under which the new directory will be created
	 * @param name - name of new directory
	 * @param desc - description for new directory
	 * @param userId - id of user performing the action
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public DirectoryResource addDirectory(DirectoryResource parentDir, String name, String desc, String userId) throws ServiceException {
		
		return this.addDirectory(parentDir, name, desc, null, null, null, userId);
		
	}
		
	
	/**
	 * Add new directory
	 * 
	 * @param parentDir - the parent directory under which the new directory will be created
	 * @param name - name of new directory
	 * @param desc - description for new directory
	 * @param readGroup1 - optional read group
	 * @param writeGroup1 - optional write group
	 * @param executeGroup1 - optional execute group
	 * @param userId - id of user performing the action
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public DirectoryResource addDirectory(DirectoryResource parentDir, String name, String desc, String readGroup1, String writeGroup1, String executeGroup1, String userId) throws ServiceException {
		
		// user must have write permission on parent directory
		if(!parentDir.getCanWrite()) {
			this.handlePermissionDenied(PermissionError.WRITE, parentDir, userId);
		}		
		
		final Store store = getStore(parentDir);
		final QueuedTaskManager taskManager = getGeneralTaskManagerForStore(store);		
		
		class Task extends AbstractQueuedTask<DirectoryResource> {

			@Override
			public DirectoryResource doWork() throws ServiceException {

				DirectoryResource dirResource = null;
				try {
					dirResource = fileSystemRepository.addDirectory(parentDir, name, desc, readGroup1, writeGroup1, executeGroup1);
				} catch (Exception e) {
					throw new ServiceException("Error adding new subdirectory to directory " + parentDir.getNodeId(), e);
				}
				return dirResource;				
				
			}

			@Override
			public Logger getLogger() {
				return logger;
			}
			
			
		}
		
		Task task = new Task();
		task.setName("Add directory [dirNodeId=" + parentDir.getNodeId() + ", name=" + name + "]");
		taskManager.addTask(task);
		
		DirectoryResource newDir = task.get(); // block until complete
		
		return newDir;
		
	}
	
	/**
	 * Remove the file, from database and disk. No undo.
	 * 
	 * @param fileNodeId - id of file resource to be removed
	 * @param userId - id of user performing the action
	 * @throws ServiceException
	 */
	@MethodTimer
	public void removeFile(Long fileNodeId, String userId) throws ServiceException {
		
		final FileMetaResource fileMetaResource = this.getFileMetaResource(fileNodeId, userId, false);
		
		this.removeFile(fileMetaResource, userId);
		
	}
	
	/**
	 * Remove the file, from database and disk. No undo.
	 * 
	 * @param fileMetaResource - the file resource to remove
	 * @param userId - id of user performing the action
	 * @throws ServiceException
	 */
	@MethodTimer
	public void removeFile(FileMetaResource fileMetaResource, String userId) throws ServiceException {
		
		// user must have read & write access on parent directory
		// file resource inherits permission from parent directory, so this works.
		if(!fileMetaResource.getCanRead()) {
			this.handlePermissionDenied(PermissionError.READ, fileMetaResource, userId);
		}		
		if(!fileMetaResource.getCanWrite()) {
			this.handlePermissionDenied(PermissionError.WRITE, fileMetaResource, userId);
		}		
		
		final Store store = getStore(fileMetaResource);
		final QueuedTaskManager taskManager = getGeneralTaskManagerForStore(store);		
		
		class Task extends AbstractQueuedTask<Void> {

			@Override
			public Void doWork() throws ServiceException {
	
				try {
					fileSystemRepository.removeFile(store, fileMetaResource);
				} catch (Exception e) {
					throw new ServiceException("Error removing file with node id => " + fileMetaResource.getNodeId() + ". " + e.getMessage(), e);
				}
				
				return null;
				
			}

			@Override
			public Logger getLogger() {
				return logger;
			}
			
		}
		
		Task task = new Task();
		task.setName("Remove file [fileNodeId=" + fileMetaResource.getNodeId() + "]");
		taskManager.addTask(task);
		
		task.waitComplete(); // block until finished		
		
	}
	
	/**
	 * Remove a directory. Walks the tree in POST_ORDER_TRAVERSAL, from leafs to root node.
	 * 
	 * @param dirNodeId - id of directory to remove
	 * @throws ServiceException
	 */
	@MethodTimer
	public void removeDirectory(Long dirNodeId, String userId) throws ServiceException {
		
		final DirectoryResource dirToDelete = this.getDirectory(dirNodeId, userId);
		
		this.removeDirectory(dirToDelete, userId);		
		
	}	
	
	/**
	 * Remove a directory. Walks the tree in POST_ORDER_TRAVERSAL, from leafs to root node.
	 * 
	 * @param dirNodeId - id of directory to remove
	 * @throws ServiceException
	 */
	@MethodTimer
	public void removeDirectory(DirectoryResource dirToDelete, String userId) throws ServiceException {
		
		final Store store = getStore(dirToDelete);
		
		final QueuedTaskManager taskManager = getGeneralTaskManagerForStore(store);
		
		Long dirNodeId = dirToDelete.getNodeId();
		
		class Task extends AbstractQueuedTask<Void> {

			@Override
			public Void doWork() throws ServiceException {
				
				getLogger().info("Deleting Tree:");
				
				final Tree<PathResource> tree = buildPathResourceTree(dirToDelete, userId);
				
				DirectoryResource rootDirToDelete = (DirectoryResource)tree.getRootNode().getData();
				if(rootDirToDelete.getParentNodeId().equals(0L)){
					throw new ServiceException("Node id => " + rootDirToDelete.getNodeId() + " points to a root directory for a store. "
							+ "You cannot use this method to remove a root directory.");
				}				
				
				pathResTreeLogger.logTree(tree);
				
				try {
					
					// walk tree, bottom-up, from leafs to root node.
					Trees.walkTree(tree,
						(treeNode) -> {
							
							try {
								if(treeNode.getData().getResourceType() == ResourceType.FILE){
									
									FileMetaResource fileToDelete = (FileMetaResource)treeNode.getData();
									// this works because files inherit permissions from their directory
									if(!fileToDelete.getCanWrite()) {
										handlePermissionDenied(PermissionError.WRITE, fileToDelete, userId);
									}
									fileSystemRepository.removeFile(store, fileToDelete);
									
								}else if(treeNode.getData().getResourceType() == ResourceType.DIRECTORY){
									
									// we walk the tree bottom up, so by the time we remove a directory it will be empty
									DirectoryResource nextDirToDelete = (DirectoryResource)treeNode.getData();
									if(!nextDirToDelete.getCanWrite()) {
										handlePermissionDenied(PermissionError.WRITE, nextDirToDelete, userId);
									}									
									fileSystemRepository.removeDirectory(store, nextDirToDelete);
									
								}
							}catch(Exception e){
								
								PathResource presource = treeNode.getData();
								
								throw new TreeNodeVisitException("Error removing path resource with node id => " + 
										presource.getNodeId() + ", of resource type => " + 
										presource.getResourceType().getTypeString() +", " + e.getMessage(), e);
								
							}
							
						},
						WalkOption.POST_ORDER_TRAVERSAL);
				
				}catch(TreeNodeVisitException e){
					throw new ServiceException("Encountered error when deleting directory with node id => " + 
							dirNodeId + ". " + e.getMessage(), e);
				}				
				
				return null;
			}

			@Override
			public Logger getLogger() {
				return logger;
			}
			
		}
		
		Task task = new Task();
		task.setName("Remove directory [dirNodeId=" + dirNodeId + "]");
		taskManager.addTask(task);
		
		task.waitComplete(); // block until finished		
		
	}
	
	/**
	 * Copy file to another directory (could be in another store)
	 * 
	 * @param fileNodeId - the file to copy
	 * @param dirNodeId - the destination directory
	 * @param replaceExisting - pass true to replace any existing file in the destination directory with
	 * same name. If you pass false, and a file already exists, then an exception will be thrown.
	 * @param userId - Id of user performing the action
	 * @throws ServiceException
	 */
	@MethodTimer
	public void copyFile(Long fileNodeId, Long dirNodeId, boolean replaceExisting, String userId) throws ServiceException {
		
		FileMetaResource sourceFile = this.getFileMetaResource(fileNodeId, userId, false);
		DirectoryResource destitationDir = this.getDirectory(dirNodeId, userId);
		
		copyFile(sourceFile, destitationDir, replaceExisting, userId);
		
	}
	
	private void copyFile(FileMetaResource fileToCopy, DirectoryResource toDir, boolean replaceExisting, String userId) throws ServiceException {
		
		// user must have read on parent directory
		// file resource inherits permission from parent directory, so this works.
		if(!fileToCopy.getCanRead()) {
			handlePermissionDenied(PermissionError.READ, fileToCopy, userId);
		}
		
		Store soureStore = getStore(fileToCopy);
		Path sourceFilePath = fileSystemUtil.buildPath(soureStore, fileToCopy);
		
		// can't copy a file to the directory it's already in
		if(fileToCopy.getParentNodeId().equals(toDir.getNodeId())){
			throw new ServiceException("You cannot copy a file to the directory that it's already in. "
					+ "fileNodeId => " + fileToCopy.getNodeId() + ", dirNodeId => " + toDir.getNodeId());
		}
		
		this.addFile(toDir, sourceFilePath, replaceExisting, userId);
		
		// TODO - consider the idea of adding a new field to eas_path_resource called "is_locked" which can be set to Y/N.
		// If the path resource is locked then no update operations (delete, move, update, copy, etc) can be performed.
		// we can lock a file meta resource right when we add it, then unlock it after we refresh the binary data.
		
		// TODO - do we want to block for updating binary data in the database?  Uhg!
		// If we don't block then it's possible for one of those update tasks to fail (someone else might
		// delete a file before the update process runs.)  We should make those tasks fail gracefully
		
	}
	
	/**
	 * Copies directory 'copyDirNodeId' to destination directory 'destDirNodeId'. The destination directory
	 * may already contain files and sub-directories with the same name. Directories will be merged. Files
	 * will be overwritten if 'replaceExisting' is set to true. If 'replaceExisting' is set to false and there
	 * exists a file with the same name then a ServiceException will be thrown.
	 * 
	 * @param copyDirNodeId
	 * @param destDirNodeId
	 * @param replaceExisting
	 * @param userId
	 * @throws ServiceException
	 */
	@MethodTimer
	public void copyDirectory(
			Long copyDirNodeId, 
			Long destDirNodeId, 
			boolean replaceExisting, 
			String userId) throws ServiceException {
		
		if(copyDirNodeId.equals(destDirNodeId)){
			throw new ServiceException("Source directory and destination directory are the same. "
					+ "You cannot copy a directory to itself. copyDirNodeId=" + copyDirNodeId + 
					", destDirNodeId=" + destDirNodeId + ", replaceExisting=" + replaceExisting);
		}
		
		final DirectoryResource fromDirParent = this.getParentDirectory(copyDirNodeId, userId);
		if(fromDirParent != null) {
			
			// if the directory being copied has a parent directory, then the user must have read access
			// on that directory in order to perform copy.
			if(!fromDirParent.getCanRead()) {
				handlePermissionDenied(PermissionError.READ, fromDirParent, userId);
			}
			
		}
		
		final DirectoryResource fromDir = this.getDirectory(copyDirNodeId, userId);
		final DirectoryResource toDir = this.getDirectory(destDirNodeId, userId);
		final Store fromStore = getStore(fromDir);
		final Store toStore = getStore(fromDir);

		final Tree<PathResource> fromTree = this.buildPathResourceTree(fromDir, userId);
		
		copyDirectoryTraversal(fromStore, toStore, fromTree.getRootNode(), toDir, replaceExisting, userId);
		
	}
	
	/**
	 * Recursively walk the tree to copy all child path resources
	 * 
	 * @param fromStore
	 * @param toStore
	 * @param pathResourceNode
	 * @param toDir
	 * @param replaceExisting
	 * @param userId
	 * @throws ServiceException
	 */
	private void copyDirectoryTraversal(
			Store fromStore, 
			Store toStore, 
			TreeNode<PathResource> pathResourceNode, 
			DirectoryResource toDir, 
			boolean replaceExisting,
			String userId) throws ServiceException {
		
		PathResource resourceToCopy = pathResourceNode.getData();
		
		if(resourceToCopy.getResourceType() == ResourceType.DIRECTORY){
			
			DirectoryResource dirToCopy = (DirectoryResource) resourceToCopy;
			
			// user needs read permission on directory to copy
			if(!dirToCopy.getCanRead()) {
				handlePermissionDenied(PermissionError.READ, dirToCopy, userId);
			}
			// user need write permission on destination directory
			if(!toDir.getCanWrite()) {
				handlePermissionDenied(PermissionError.WRITE, toDir, userId);
			}			
			
			// TODO - we perform a case insensitive match. If the directory names differ in case, do we want
			// to keep the directory that already exists (which we do now) or rename it to match exactly of
			// the one we are copying?
			DirectoryResource newToDir = _createCopyOfDirectory(dirToCopy, toDir, userId);
			
			// copy over children of the directory (files and sub-directories)
			if(pathResourceNode.hasChildren()){
				for(TreeNode<PathResource> child : pathResourceNode.getChildren()){
					copyDirectoryTraversal(fromStore, toStore, child, newToDir, replaceExisting, userId);
				}
			}
			
		}else if(resourceToCopy.getResourceType() == ResourceType.FILE){
			
			this.copyFile( (FileMetaResource)resourceToCopy, toDir, replaceExisting, userId);
			
		}
		
	}
	
	/**
	 * Makes a copy of 'dirToCopy' under directory 'toDir'. If there already exists a directory under 'toDir' with the
	 * same name as directory 'dirToCopy' then the existing directory is returned. If not then a new directory is created.
	 * 
	 * @param dirToCopy
	 * @param toDir
	 * @param userId
	 * @return
	 * @throws Exception
	 */
	private DirectoryResource _createCopyOfDirectory(
			DirectoryResource dirToCopy, 
			DirectoryResource toDir, 
			String userId) throws ServiceException {
		
		// see if there already exists a child directory with the same name
		DirectoryResource existingChildDir = null;
		try {
			existingChildDir = this.getChildDirectoryResource(toDir.getNodeId(), dirToCopy.getPathName(), userId);
		} catch (Exception e) {
			throw new ServiceException("Failed to check for existing child directory with name '" + 
					dirToCopy.getPathName() + "' under directory node " + toDir.getNodeId(), e);
		}
		
		if(existingChildDir != null){
			
			// directory with the same name already exists in the 'toDir'
			// TODO - directory names might differ by case (uppercase/lowercase etc). Do we want to change
			// the name of the existing directory to exactly match the one being copied?
			return existingChildDir;
			
		}else{
			
			// create new directory
			DirectoryResource newCopy = this.addDirectory(toDir, dirToCopy.getPathName(), dirToCopy.getDesc(), 
					dirToCopy.getReadGroup1(), dirToCopy.getWriteGroup1(), dirToCopy.getExecuteGroup1(), userId);
			
			// read/write/execute bits should be the same
			newCopy.setCanRead(dirToCopy.getCanRead());
			newCopy.setCanWrite(dirToCopy.getCanWrite());
			newCopy.setCanExecute(dirToCopy.getCanExecute());
			
			return newCopy;
			
		}
		
	}
	
	/**
	 * Move a file, preserving same node id.
	 * 
	 * @param fileNodeId - id of the file to move
	 * @param dirNodeId - id of the directory where the file will be moved to
	 * @param replaceExisting
	 * @param userId - id of user performing action
	 * @throws ServiceException
	 */
	@MethodTimer
	public void moveFile(Long fileNodeId, Long dirNodeId, boolean replaceExisting, String userId) throws ServiceException {
		
		final FileMetaResource fileToMove = this.getFileMetaResource(fileNodeId, userId, false);
		final DirectoryResource destDir = this.getDirectory(dirNodeId, userId);
				
		moveFile(fileToMove, destDir, replaceExisting, userId);
		
	}
	
	/**
	 * Move a file, preserving same node id.
	 * 
	 * @param fileNodeId - the file to move
	 * @param dirNodeId - the directory where the file will be moved to
	 * @param replaceExisting
	 * @param userId - id of user performing action
	 * @throws ServiceException
	 */
	private void moveFile(FileMetaResource fileToMove, DirectoryResource destDir, boolean replaceExisting, String userId) throws ServiceException {
		
		// user must have write access on destination directory
		if(!destDir.getCanWrite()){
			handlePermissionDenied(PermissionError.WRITE, destDir, userId);
		}
		// user must have read and write on parent directory of file being moved
		// file resource inherits permission from parent directory, so this works.		
		if(!fileToMove.getCanRead()) {
			handlePermissionDenied(PermissionError.READ, fileToMove, userId);
		}
		if(!fileToMove.getCanWrite()) {
			handlePermissionDenied(PermissionError.WRITE, fileToMove, userId);
		}
		
		final Store store = getStore(destDir);
		final QueuedTaskManager taskManager = getGeneralTaskManagerForStore(store);
		
		class Task extends AbstractQueuedTask<Void> {

			@Override
			public Void doWork() throws ServiceException {

				try {
					fileSystemRepository.moveFile(fileToMove, destDir, replaceExisting);
				} catch (Exception e) {
					throw new ServiceException("Error moving file " + fileToMove.getNodeId() + " to directory " + 
							destDir.getNodeId() + ", replaceExisting = " + replaceExisting + ". " + e.getMessage(), e);
				}
				
				return null;
				
			}

			@Override
			public Logger getLogger() {
				return logger;
			}
				
		}		
		
		Task task = new Task();
		task.setName("Move file [fileNodeId=" + fileToMove.getNodeId() + ", dirNodeId=" + destDir.getNodeId() + ", replaceExisting=" + replaceExisting + "]");
		taskManager.addTask(task);
		
		task.waitComplete(); // block until finished	
		
	}
	
	/**
	 * Move a directory (does not preserve node IDs for directories, but does for files.)
	 * 
	 * @param moveDirId - the directory to move
	 * @param destDirId - the directory where 'moveDirId' will be moved to (under). 
	 * @param replaceExisting
	 * @param userId - id of user completing the action
	 * @throws ServiceException
	 */
	@MethodTimer
	public void moveDirectory(Long moveDirId, Long destDirId, boolean replaceExisting, String userId) throws ServiceException {
		
		// TODO - wrap in queued task
		
		// we can preserve file IDs but hard to preserve directory IDs...
		// if you eventually manage to preserve directory IDs, then you might have to worry about
		// updating the eas_store.node_id (root node) value.
		
		DirectoryResource dirToMove = this.getDirectory(moveDirId, userId);
		
		// make sure the user is not trying to move a root directory for a store
		if(dirToMove.getParentNodeId().equals(0L)){
			throw new ServiceException("You cannot move a root directory of a store. All stores require a root directory. "
					+ "moveDirId = " + moveDirId + ", destDirId = " + destDirId);
		}
		
		// user must have read and write on parent directory
		DirectoryResource parentDir = this.getParentDirectory(moveDirId, userId);
		if(parentDir != null) {
			if(!parentDir.getCanRead()) {
				handlePermissionDenied(PermissionError.READ, parentDir, userId);
			}
			if(!parentDir.getCanWrite()) {
				handlePermissionDenied(PermissionError.WRITE, parentDir, userId);
			}			
		}
		
		// make sure destDirId is not a child node under moveDirId. Cannot move a directory to under itself.
		boolean isChild = false;
		try {
			isChild = fileSystemRepository.isChild(moveDirId, destDirId);
		} catch (Exception e) {
			throw new ServiceException("Error checking if directory " + destDirId + 
					" is a child directory (at any depth) of directory " + moveDirId, e);
		}
		if(isChild){
			throw new ServiceException("Cannot move directory " + moveDirId + " to under directory " + 
					destDirId + " because directory " + destDirId + " is a child of directory " + moveDirId + ".");
		}

		final Tree<PathResource> fromTree = this.buildPathResourceTree(dirToMove, userId);
		
		DirectoryResource toDir = this.getDirectory(destDirId, userId);
		final Store fromStore = getStore(dirToMove);
		final Store toStore = getStore(toDir);
		
		// walk the tree top-down and copy over directories one at a time, then use
		// existing moveFile method.
		moveDirectoryTraversal(fromStore, toStore, fromTree.getRootNode(), toDir, replaceExisting, userId);
		
		// remove from dir and all child directories
		removeDirectory(dirToMove.getNodeId(), userId);
		
	}
	
	private void moveDirectoryTraversal(
			Store fromStore,
			Store toStore, 
			TreeNode<PathResource> pathResourceNode, 
			DirectoryResource toDir, 
			boolean replaceExisting,
			String userId) throws ServiceException {
		
		PathResource resourceToMove = pathResourceNode.getData();
		
		if(resourceToMove.getResourceType() == ResourceType.DIRECTORY){
			
			DirectoryResource dirToMove = (DirectoryResource)resourceToMove;
			
			// user must have read & write access on directory to move
			if(!dirToMove.getCanRead()) {
				handlePermissionDenied(PermissionError.READ, dirToMove, userId);
			}
			if(!dirToMove.getCanWrite()) {
				handlePermissionDenied(PermissionError.WRITE, dirToMove, userId);
			}
			
			// user must have write access on destination directory
			if(!toDir.getCanWrite()) {
				handlePermissionDenied(PermissionError.WRITE, toDir, userId);
			}
			
			// TODO - we perform a case insensitive match. If the directory names differ in case, do we want
			// to keep the directory that already exists (which we do now) or rename it to match exactly of
			// the one we are copying?
			DirectoryResource newToDir = _createCopyOfDirectory(dirToMove, toDir, userId);
			
			// move children of the directory (files and sub-directories)
			if(pathResourceNode.hasChildren()){
				for(TreeNode<PathResource> child : pathResourceNode.getChildren()){
					
					moveDirectoryTraversal(fromStore, toStore, child, newToDir, replaceExisting, userId);
					
				}
			}
			
		}else if(resourceToMove.getResourceType() == ResourceType.FILE){
			
			moveFile( (FileMetaResource)resourceToMove, toDir, replaceExisting, userId);
			
		}
		
	}	
	
	/**
	 * All files in 'tempDir' will be added to directory 'dirNodeId'
	 * 
	 * @param dirNodeId - id of the directory node
	 * @param tempDir - directory where files are located.
	 * @param replaceExisting
	 * @param userId - id of user performing the action
	 * @throws ServiceException
	 */
	@MethodTimer
	public void processToDirectory(
			Long dirNodeId, 
			Path tempDir, 
			boolean replaceExisting,
			String userId) throws ServiceException {
		
		List<Path> filePaths = null;
		try {
			filePaths = FileUtil.listFilesToDepth(tempDir, 1);
		} catch (IOException e) {
			throw new ServiceException("Error listing files in temporary directory " + tempDir.toString());
		}
		
		filePaths.stream().forEach(
			(pathToFile) ->{
				try {
					this.addFile(dirNodeId, userId, pathToFile, replaceExisting);
				} catch (ServiceException e) {
					throw new RuntimeException("Error adding file '" + pathToFile.toString() + "' to directory with id '" + dirNodeId + "'.", e);
				}
			});
		
	}
	
	/**
	 * All files in 'tempDir' will be added to the directory (the correct directory will be determined using the
	 * store name and dirRelPath values.)
	 * 
	 * @param storeName - the name of the store
	 * @param dirRelPath - the relative path of the directory resource within the store
	 * @param tempDir - directory where files are located.
	 * @param replaceExisting
	 * @param userId - id of user performing the action
	 * @throws ServiceException
	 */
	@MethodTimer
	public void processToDirectory(
			String storeName,
			String dirRelPath, 
			Path tempDir, 
			boolean replaceExisting,
			String userId) throws ServiceException{
	
		List<Path> filePaths = null;
		try {
			filePaths = FileUtil.listFilesToDepth(tempDir, 1);
		} catch (IOException e) {
			throw new ServiceException("Error listing files in temporary directory " + tempDir.toString());
		}
		
		filePaths.stream().forEach(
			(pathToFile) ->{
				try {
					this.addFile(storeName, dirRelPath, userId, pathToFile, replaceExisting);
				} catch (ServiceException e) {
					throw new RuntimeException("Error adding file '" + pathToFile.toString() + "' to directory  with relPath'" + 
							dirRelPath + "', under store name '" + storeName + "'.", e);
				}
			});		
		
	}
	
	/**
	 * Create sample store for testing, with some sub-directories.
	 * 
	 * @throws ServiceException
	 */
	@SuppressWarnings("unused")
	@MethodTimer
	public Store createTestStore() throws ServiceException {
		
		String userId = appProps.getProperty("store.test.user.id");
		String testStoreName = appProps.getProperty("store.test.name");
		String testStoreDesc = appProps.getProperty("store.test.desc");
		String testStorePath = fileSystemUtil.cleanFullPath(appProps.getProperty("store.test.path"));
		String testStoreMaxFileSizeBytes = appProps.getProperty("store.test.max.file.size.bytes");
		String testStoreRootDirName = appProps.getProperty("store.test.root.dir.name");
		String testStoreRootDirDesc = appProps.getProperty("store.test.root.dir.desc");
		String readGroup = appProps.getProperty("store.test.root.dir.read");
		String writeGroup = appProps.getProperty("store.test.root.dir.write");
		String executeGroup = appProps.getProperty("store.test.root.dir.execute");
		
		Long maxBytes = 0L;
		try {
			maxBytes = Long.valueOf(testStoreMaxFileSizeBytes);
		} catch (NumberFormatException e) {
			throw new ServiceException("Error parsing store.test.max.file.size.bytes to long. " + e.getMessage(), e);
		}
		
		Store store = addStore(testStoreName, testStoreDesc, Paths.get(testStorePath), 
				testStoreRootDirName, testStoreRootDirDesc, maxBytes, 
				readGroup, writeGroup, executeGroup, AccessRule.DENY);
		
		DirectoryResource dirMore  = addDirectory(store.getNodeId(), "more", "more desc", userId);
		DirectoryResource dirOther = addDirectory(store.getNodeId(), "other", "other desc", userId);
			DirectoryResource dirThings = addDirectory(dirOther, "things", "things desc", userId);
			DirectoryResource dirFoo = addDirectory(dirOther, "foo", "foo desc", userId);
				DirectoryResource dirCats = addDirectory(dirFoo, "cats", "cats desc", userId);
				DirectoryResource dirDogs = addDirectory(dirFoo, "dogs", "dogs desc", userId);
					DirectoryResource dirBig = addDirectory(dirDogs, "big", "big desc", userId);
					DirectoryResource dirSmall = addDirectory(dirDogs, "small", "small desc", userId);
						DirectoryResource dirPics = addDirectory(dirSmall, "pics", "pics desc", userId);		
		
		return store;
	
	}	
	
	private enum PermissionError {
		
		// write, rename, delete
		WRITE("Denied Write Permission"),
		
		// reading
		READ("Denied Read Permission"),
		
		// administering group permissions
		EXECUTE("Denied Execute Permission");
		
		private final String error;
		
		private PermissionError(final String error) {
			this.error = error;
		}

		@Override
		public String toString() {
			return this.error;
		}
		
	}
	
	/**
	 * Helper method for processing permission related errors
	 * 
	 * @param error
	 * @param fileResource
	 * @param userId
	 * @throws ServiceException
	 */
	private void handlePermissionDenied(PermissionError error, PathResource resource, String userId) throws ServiceException {
		// TODO create a new exception type for permission not allowed
		throw new ServiceException(error.toString() + ". User " + userId + " does not have permission to perform action on " + 
				" path resource [id=" + resource.getNodeId() + ", type=" + resource.getResourceType().getTypeString() + 
				", relPath=" + resource.getRelativePath() + ", store=" + resource.getStore().getName() + "]");
	}	
	
	/**
	 * Helper method for processing permission related errors
	 * 
	 * @param error
	 * @param fileResource
	 * @param userId
	 * @throws ServiceException
	 */
	private void handlePermissionDenied(PermissionError error, FileMetaResource fileResource, String userId) throws ServiceException {
		// TODO create a new exception type for permission not allowed
		throw new ServiceException(error.toString() + ". User " + userId + " does not have permission to perform action on " + 
				" file resource [id=" + fileResource.getNodeId() + ", type=" + fileResource.getResourceType().getTypeString() + 
				", relPath=" + fileResource.getRelativePath() + ", store=" + fileResource.getStore().getName() + "]");
	}
	
	/**
	 * Helper method for processing permission related errors
	 * 
	 * @param error
	 * @param fileResource
	 * @param userId
	 * @throws ServiceException
	 */
	private void handlePermissionDenied(PermissionError error, DirectoryResource directoryResource, String userId) throws ServiceException {
		// TODO create a new exception type for permission not allowed
		throw new ServiceException(error.toString() + ". User " + userId + " does not have permission to perform action on " + 
				" directory resource [id=" + directoryResource.getNodeId() + ", type=" + directoryResource.getResourceType().getTypeString() + 
				", relPath=" + directoryResource.getRelativePath() + ", store=" + directoryResource.getStore().getName() + "]");
	}	
	
	/**
	 * Helper method for processing permission related errors
	 * 
	 * @param error
	 * @param fileResource
	 * @param directoryResource
	 * @param userId
	 * @throws ServiceException
	 */
	private void handlePermissionDenied(PermissionError error, FileMetaResource fileResource, DirectoryResource directoryResource, String userId) throws ServiceException {
		// TODO create a new exception type for permission not allowed
		throw new ServiceException(error.toString() + ". User " + userId + " does not have permission to perform action on " + 
				" file resource [id=" + fileResource.getNodeId() + ", type=" + fileResource.getResourceType().getTypeString() + 
				", relPath=" + fileResource.getRelativePath() + ", store=" + fileResource.getStore().getName() + "] " +
				" under directory resource [id=" + directoryResource.getNodeId() + ", type=" + directoryResource.getResourceType().getTypeString() + 
				", relPath=" + directoryResource.getRelativePath() + ", store=" + directoryResource.getStore().getName() + "] "				
				);
	}	

}
