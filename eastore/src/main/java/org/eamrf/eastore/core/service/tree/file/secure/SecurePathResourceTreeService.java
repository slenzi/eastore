/**
 * 
 */
package org.eamrf.eastore.core.service.tree.file.secure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.eamrf.concurrent.task.QueuedTaskManager;
import org.eamrf.concurrent.task.TaskManagerProvider;
import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.StringUtil;
import org.eamrf.eastore.core.aop.profiler.MethodTimer;
import org.eamrf.eastore.core.concurrent.StoreTaskManagerMap;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.messaging.ResourceChangeService;
import org.eamrf.eastore.core.properties.ManagedProperties;
import org.eamrf.eastore.core.service.tree.file.FileSystemUtil;
import org.eamrf.eastore.core.service.tree.file.PathResourceTreeLogger;
import org.eamrf.eastore.core.tree.Tree;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.FileSystemRepository;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.BinaryResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.DirectoryResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.FileMetaResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.PathResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.ResourceType;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;
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
		
		return buildPathResourceTree(dirNodeId, userId, Integer.MAX_VALUE);
		
	}
	
	/**
	 * Build a top-down (from root node to leaf nodes) tree of PathResource objects,
	 * but only include nodes up to a specified depth.
	 * 
	 * @param dirNodeId - Id of node which will become the root of the tree.
	 * @param userId - User ID used to evaluate access permissions (e.g. CTEP ID).
	 * @param depth - depth of child nodes to include.
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public Tree<PathResource> buildPathResourceTree(Long dirNodeId, String userId, int depth) throws ServiceException {
		
		// We need to evaluate the permissions for directory with dirNodeId in order
		// to properly evaluate permissions for all the children. The getDirectory method
		// will fetch the parent tree and evaluate all the group permissions, and set
		// the read, write, and execute bits.
		DirectoryResource dirResource = this.getDirectory(dirNodeId, userId);
		
		List<PathResource> resources = this.getPathResourceTree(dirResource.getNodeId(), depth);
		
		if(resources == null || resources.size() == 0){
			throw new ServiceException("No top-down PathResource tree for directory node " + dirNodeId + 
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
	 * Fetch the first level children for the path resource. This is only applicable for
	 * directory resources
	 * 
	 * @param dirId
	 * @param userId
	 * @return
	 * @throws ServiceException
	 */
	public List<PathResource> getChildPathResource(Long dirId, String userId) throws ServiceException {
		
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
	public Store addStore(String storeName, String storeDesc, Path storePath, 
			String rootDirName, String rootDirDesc, Long maxFileSizeDb) throws ServiceException {
		
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
			store = fileSystemRepository.addStore(storeName, storeDesc, storePath, rootDirName, rootDirDesc, maxFileSizeDb);
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
	
	

}
