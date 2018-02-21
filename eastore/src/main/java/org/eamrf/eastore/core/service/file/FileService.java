package org.eamrf.eastore.core.service.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eamrf.concurrent.task.AbstractQueuedTask;
import org.eamrf.concurrent.task.QueuedTaskManager;
import org.eamrf.concurrent.task.TaskManagerProvider;
import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.CodeTimer;
import org.eamrf.core.util.CollectionUtil;
import org.eamrf.core.util.FileUtil;
import org.eamrf.core.util.StringUtil;
import org.eamrf.eastore.core.aop.profiler.MethodTimer;
import org.eamrf.eastore.core.concurrent.StoreTaskManagerMap;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.messaging.ResourceChangeService;
import org.eamrf.eastore.core.properties.ManagedProperties;
import org.eamrf.eastore.core.search.lucene.StoreIndexer;
import org.eamrf.eastore.core.search.service.StoreIndexerService;
import org.eamrf.eastore.core.service.security.GatekeeperService;
import org.eamrf.eastore.core.service.tree.file.PathResourceUtil;
import org.eamrf.eastore.core.service.tree.file.secure.SecurePathResourceTreeService;
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
 * Main service class for interacting with our file system
 * 
 * @author slenzi
 *
 */
@Service
public class FileService {
	
    @InjectLogger
    private Logger logger;	
	
	// Spring Boot takes care of initializing the JdbcTemplate and DataSource, so we can simply autowire it! Magic!
    @Autowired
    private JdbcTemplate jdbcTemplate; 
    
    @Autowired
    private ManagedProperties appProps;
	
	@Autowired
	private SecurePathResourceTreeService secureTreeService;
	
    @Autowired
    private FileSystemRepository fileSystemRepository;
    
    @Autowired
    private TaskManagerProvider taskManagerProvider;
    
    @Autowired
    private GatekeeperService gatekeeperService;
    
    @Autowired
    private StoreIndexerService indexerService;
    
    @Autowired
    private ResourceChangeService resChangeService;
    
    @Autowired
    private ErrorHandler errorHandler;
    
    // maps all stores to their task manager
    private Map<Store,StoreTaskManagerMap> storeTaskManagerMap = new HashMap<Store,StoreTaskManagerMap>();     
    
	public FileService() {
	
	}
	
	/**
	 * Initialize the service for use.
	 */
	@PostConstruct
	public void init(){
		
		List<Store> stores = null;
		
		try {
			stores = getStores(null);
		} catch (ServiceException e) {
			logger.error("Failed to fetch list of stores. Cannot initialize task managers. " + e.getMessage(), e);
			return;
		}
		
		if(stores == null){
			logger.warn("No stores found when initializing File System Service...");
			return;
		}
		
		initializeTaskManagers(stores);
		
	}
	
	/**
	 * Stop queued task managers and shutdown lucene search indexers
	 */
	@PreDestroy
	public void cleanup() {
		for(StoreTaskManagerMap map : storeTaskManagerMap.values()) {
			map.stopAllManagers();
		}
	}
	
	/**
	 * Initialize the task managers for each store.
	 * 
	 * Each store gets two task managers, one manager for tasks that involve adding binary data
	 * to the database, and one manager for everything else.
	 * 
	 * @param stores
	 */
	private void initializeTaskManagers(List<Store> stores) {
		for(Store store : stores){
			storeTaskManagerMap.put(store, createTaskManagersForStore(store));
		}
	}	
	
	/**
	 * Initializes the task managers for the store
	 * 
	 * @param store
	 * @return A new instance of StoreTaskManagerMap which contains references to all the task managers for the store.
	 */
	private StoreTaskManagerMap createTaskManagersForStore(Store store) {
		
		logger.info("Creating queued task managers for store [storeId=" + store.getId() + ", storeName=" + store.getName() + "]");
		
		// for adding/updating files & directories
		QueuedTaskManager generalManager = taskManagerProvider.createQueuedTaskManager();
		// for adding/updating binary (BLOB) data in the database
		QueuedTaskManager binaryManager = taskManagerProvider.createQueuedTaskManager();
		// for updating the lucene search index for the store
		QueuedTaskManager indexWriterManager = taskManagerProvider.createQueuedTaskManager();
		
		generalManager.setManagerName("General Task Manager [storeId=" + store.getId() + ", storeName=" + store.getName() + "]");
		binaryManager.setManagerName("Binary Task Manager [storeId=" + store.getId() + ", storeName=" + store.getName() + "]");
		indexWriterManager.setManagerName("Lucene Index Writer Task Manager [storeId=" + store.getId() + ", storeName=" + store.getName() + "]");
		
		ExecutorService generalExecutor = Executors.newSingleThreadExecutor();
		ExecutorService binaryExecutor = Executors.newSingleThreadExecutor();
		ExecutorService indexWriterExecutor = Executors.newSingleThreadExecutor();
		
		generalManager.startTaskManager(generalExecutor);
		binaryManager.startTaskManager(binaryExecutor);
		indexWriterManager.startTaskManager(indexWriterExecutor);
		
		StoreTaskManagerMap mapEntry = new StoreTaskManagerMap(store, generalManager, binaryManager, indexWriterManager);
		
		return mapEntry;
		
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
	 * Fetch the binary queued task manager for the store. This task manager is only for
	 * queuing tasks that perform work on file binary data
	 * 
	 * @param store
	 * @return
	 */
	private QueuedTaskManager getBinaryTaskManagerForStore(Store store){
		StoreTaskManagerMap map = storeTaskManagerMap.get(store);
		return map.getBinaryTaskManager();
	}
	
	/**
	 * fetch the search index writer task manager for the store.
	 * 
	 * @param store
	 * @return
	 */
	private QueuedTaskManager getIndexWriterTaskManagerForStore(Store store){
		StoreTaskManagerMap map = storeTaskManagerMap.get(store);
		return map.getSearchIndexWriterTaskManager();
	}	
	
	/**
	 * Fetch the store object from the PathResource object. If the store object is null then
	 * this method will attempt to fetch it from the database using the store id.
	 * 
	 * @param r
	 * @param userId - is of user completing the action
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public Store getStore(PathResource r, String userId) throws ServiceException {
		
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
		return getStoreById(r.getStoreId(), userId);
		
	}
	
	/**
	 * Fetch all stores
	 * 
	 * @param userId - id of user completing the action
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public List<Store> getStores(String userId) throws ServiceException {
		
		List<Store> stores = null;
		try {
			stores = fileSystemRepository.getStores();
		} catch (Exception e) {
			throw new ServiceException("Failed to fetch all stores, " + e.getMessage(), e);
		}
		
		// validate read, write, and execute bits
		if(!StringUtil.isNullEmpty(userId)) {
			Set<String> userGroupCodes = gatekeeperService.getUserGroupCodes(userId);
			DirectoryResource rootDir = null;
			for(Store nextStore : stores) {
				rootDir = nextStore.getRootDir();
				this.setAccessBits(userGroupCodes, rootDir);
			}
		}
		
		return stores;
	}
	
	/**
	 * fetch a store by id
	 * 
	 * @param storeId
	 * @param userId - is of user completing the action
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public Store getStoreById(Long storeId, String userId) throws ServiceException {
		
		Store store = null;
		try {
			store = fileSystemRepository.getStoreById(storeId);
		} catch (Exception e) {
			throw new ServiceException("Failed to get store for store id => " + storeId, e);
		}
		if(store == null) {
			return null;
		}		
		
		// validate read, write, and execute bits
		if(!StringUtil.isNullEmpty(userId)) {
			Set<String> userGroupCodes = gatekeeperService.getUserGroupCodes(userId);
			DirectoryResource rootDir = store.getRootDir();
			this.setAccessBits(userGroupCodes, rootDir);
		}
		
		return store;
		
	}
	
	/**
	 * fetch a store by name
	 * 
	 * @param storeName
	 * @param userId - is of user completing the action
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public Store getStoreByName(String storeName, String userId) throws ServiceException {
		
		String lowerStoreName = storeName.toLowerCase();
		
		Store store = null;
		try {
			store = fileSystemRepository.getStoreByName(lowerStoreName);
		} catch (Exception e) {
			throw new ServiceException("Failed to get store for store name => " + storeName, e);
		}
		if(store == null) {
			return null;
		}
		
		// validate read, write, and execute bits
		if(!StringUtil.isNullEmpty(userId)) {
			Set<String> userGroupCodes = gatekeeperService.getUserGroupCodes(userId);
			DirectoryResource rootDir = store.getRootDir();
			this.setAccessBits(userGroupCodes, rootDir);
		}		
		
		return store;
		
	}
	
	/**
	 * Set the access bits (read, write, execute) on the directory using the set of groups codes to determine access.
	 * 
	 * @param userGroupCodes
	 * @param dir
	 */
	private void setAccessBits(Set<String> userGroupCodes, DirectoryResource dir) {
		if(dir == null) {
			return;
		}
		if(CollectionUtil.isEmpty(userGroupCodes)) {
			dir.setCanRead(false);
			dir.setCanWrite(false);
			dir.setCanExecute(false);
			return;
		}
		if(dir.getReadGroups().stream().anyMatch(userGroupCodes::contains) /*|| store.getAccessRule() == AccessRule.ALLOW*/ ) {
			dir.setCanRead(true);
		}
		if(dir.getWriteGroups().stream().anyMatch(userGroupCodes::contains) /*|| store.getAccessRule() == AccessRule.ALLOW*/ ) {
			dir.setCanWrite(true);
		}
		if(dir.getExecuteGroups().stream().anyMatch(userGroupCodes::contains) /*|| store.getAccessRule() == AccessRule.ALLOW*/ ) {
			dir.setCanExecute(true);
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
		
		if(StringUtil.isAnyNullEmpty(storeName, storeDesc, rootDirName, rootDirDesc, readGroup, writeGroup, executeGroup) || 
				storePath == null || maxFileSizeDb == null){
			
			throw new ServiceException("Missing required parameter for creating a new store.");
		}
		
		// make the last directory in the store path all lowercase, and remove any spaces
		String storeDirName = storePath.getFileName().toString();
		// remove all non alphanumeric characters except spaces, and convert to lowercase
		String cleanStoreDirName = storeDirName.replaceAll("[^a-zA-Z0-9\\s]", "").toLowerCase();
		Path storeParentPath = storePath.getParent();
		Path cleanStorePath = Paths.get(storeParentPath.toString(), cleanStoreDirName);
		
		// make sure the store path does not already exist
		if(Files.exists(cleanStorePath)) {
			throw new ServiceException("Store path '" + cleanStorePath.toString() + "' already exists. Please provide a new directory that does not already exist.");
		}
		
		// make sure there doesn't exist already a store with the same name (case insensitive)
		// TOOO - pass in userId
		Store store = getStoreByName(storeName, null);
		if(store != null){
			throw new ServiceException("Store with name '" + storeName + "' already exists. Store names must be unique.");
		}
		
		// all files for the store are kept within a /files directory under the store directory
		cleanStorePath = Paths.get(cleanStorePath.toString(), Store.STORE_FILES_DIRECTORY);
		
		try {
			store = fileSystemRepository.addStore(
					storeName, storeDesc, cleanStorePath, rootDirName, rootDirDesc, maxFileSizeDb,
					readGroup, writeGroup, executeGroup, rule
					);
		} catch (Exception e) {
			throw new ServiceException("Error creating new store '" + storeName + "' at " + cleanStorePath.toString(), e);
		}
		
		storeTaskManagerMap.put(store, createTaskManagersForStore(store));
		
		try {
			indexerService.initializeIndexerForStore(store);
		} catch (IOException e) {
			logger.error("Error initializing lucene indexer for new store, [id=" + store.getId() + ", name=" + store.getName() + "]");
		}
		
		return store;
		
	}
	
	/**
	 * Update a store (add ability to change store path at a later date.)
	 * 
	 * @param storeId - if of store to update
	 * @param storeName - new name for store
	 * @param storeDesc - new description for store
	 * @param rootDirName - new name for root directory
	 * @param rootDirDesc - new description for root directory
	 * @param readGroup1 - new read group for root directory
	 * @param writeGroup1 - new write group for root directory
	 * @param executeGroup1 - new execute group for root directory
	 * @param userId - id of user completing the action
	 * @throws ServiceException
	 */
	public void updateStore(
			Long storeId,
			String storeName,
			String storeDesc,
			String rootDirName, 
			String rootDirDesc, 
			String readGroup1, 
			String writeGroup1, 
			String executeGroup1, 
			String userId) throws ServiceException {
		
		// fetch store
		Store storeToEdit = getStoreById(storeId, userId);
		Long rootDirId = storeToEdit.getRootDir().getNodeId();
		DirectoryResource rootDir = getDirectory(rootDirId, userId);
		
		// need execute permission on root directory of store in order to edit store
		if(!rootDir.getCanExecute()) {
			logger.info("No execute permission for store root directory. Permission to edit store denied.");
			errorHandler.handlePermissionDenied(PermissionError.EXECUTE, rootDir, userId);
		}
		
		// TODO - the following to function calls *should* be an atomic operation (currently not.)
		
		// make sure there doesn't exist already a store with the same name (excluding store we're editing in case we're 
		// simply changing the case (upper/lower) of the store)
		Store existingStore = getStoreByName(storeName, userId);
		if(existingStore != null && !storeId.equals(existingStore.getId())){
			throw new ServiceException("Store with name '" + storeName + "' already exists. Store names must be unique.");
		}		
		
		// update store name & description
		this.fileSystemRepository.updateStore(storeToEdit, storeName, storeDesc);
		
		// update root directory
		updateDirectory(rootDirId, rootDirName, rootDirDesc, readGroup1, writeGroup1, executeGroup1, userId);
		
		
		// TODO - rename task managers
	
	}
	
	/**
	 * Walk the tree and add any found FileMetaResource to the collection. Also, for every FileMetaResource
	 * set the directory that the file is in FileMetaResource.setDirectory
	 * 
	 * @param node - The node to start walking at
	 * @param files - The collection in which we collect all files
	 */
	private void collectFilesAndSetDirectory(TreeNode<PathResource> node, Collection<FileMetaResource> files) {
		
		collectFilesAndSetDirectory(node, null, files);
		
	}
	
	/**
	 * Walk the tree and add any found FileMetaResource to the collection. Also, for every FileMetaResource
	 * set the directory that the file is in FileMetaResource.setDirectory
	 * 
	 * @param node - The node to start walking at
	 * @param parent - The parent of 'node'
	 * @param files - The collection in which we collect all files
	 */
	private void collectFilesAndSetDirectory(TreeNode<PathResource> node, TreeNode<PathResource> parent, Collection<FileMetaResource> files) {
		
		PathResource resource = node.getData();
		if(resource.getResourceType() == ResourceType.DIRECTORY) {
			List<TreeNode<PathResource>> children = node.getChildren();
			if(!CollectionUtil.isEmpty(children)) {
				for(TreeNode<PathResource> child : children) {
					collectFilesAndSetDirectory(child, node, files);
				}
			}
		}else if(resource.getResourceType() == ResourceType.FILE) {
			FileMetaResource f = (FileMetaResource)resource;
			f.setDirectory( (DirectoryResource)parent.getData()  );
			files.add(f);
		}
		
	}
	
	/**
	 * Rebuilds the lucene search index by clearing all existing documents and re-adding all the ones from the store.
	 * 
	 * @param storeId
	 * @throws ServiceException 
	 */
	public void rebuildStoreSearchIndex(Long storeId, String userId) throws ServiceException {
		
		final Store store = getStoreById(storeId, userId);
		
    	Tree<PathResource> tree = secureTreeService.buildPathResourceTree(store.getRootDir().getNodeId(), userId);
    	
    	List<FileMetaResource> files = new ArrayList<FileMetaResource>();
    	
    	// this method will set the directory for each file resource. This allows us to store directory
    	// related meta-data for the file in the lucene index
    	collectFilesAndSetDirectory(tree.getRootNode(), files);
    	
    	/*
    	List<FileMetaResource> files = new ArrayList<FileMetaResource>();
    	try {
			Trees.walkTree(tree, (treeNode) -> {
				PathResource resource = treeNode.getData();
				if(resource.getResourceType() == ResourceType.FILE){
					files.add((FileMetaResource)resource);
				}
			}, WalkOption.PRE_ORDER_TRAVERSAL);
		} catch (TreeNodeVisitException e) {
			throw new ServiceException("Error walking file tree to get list of all files, " + e.getMessage());
		}
		*/
    	
    	logger.info("Store [id='" + store.getId() + "', name='" + store.getName() + "'] has " + 
    			files.size() + " files to be added to lucene search index.");
    	
    	StoreIndexer indexer = null;
    	try {
			indexer = indexerService.getIndexerForStore(store);
		} catch (IOException e) {
			throw new ServiceException("Error fetching store indexer for store [id='" + 
					store.getId() + "', name='" + store.getName() + "'], " + e.getMessage());
		}
    	
    	try {
			indexer.deleteAll();
		} catch (IOException e) {
			throw new ServiceException("Error clearing existing index for store [id='" + 
					store.getId() + "', name='" + store.getName() + "'], " + e.getMessage());
		}
    	
    	Future<Boolean> future = indexer.addAll(files);
    	
    	/*
    	Boolean ok;
		try {
			ok = future.get();
		} catch (InterruptedException e) {
			throw new ServiceException("InterruptedException thrown when reindexing index for store [id='" + 
					store.getId() + "', name='" + store.getName() + "'], " + e.getMessage());
		} catch (ExecutionException e) {
			throw new ServiceException("ExecutionException thrown when reindexing index for store [id='" + 
					store.getId() + "', name='" + store.getName() + "'], " + e.getMessage());
		}
  		*/
		
		/*
		class RebuildIndexTask extends AbstractQueuedTask<Boolean> {
			
			public Boolean doWork() throws ServiceException {
				

				
			}

			@Override
			public Logger getLogger() {
				return logger;
			}
		}
		
		RebuildIndexTask indexTask = new RebuildIndexTask();
		indexTask.setName("Rebuild search index for store [storeId=" + storeId + ", name=" + store.getName() + "]");
		generalTaskManager.addTask(indexTask);
		*/
		
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
		String testStorePath = PathResourceUtil.cleanFullPath(appProps.getProperty("store.test.path"));
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
	
	/**
	 * Fetch first-level file meta resource, by name (case insensitive), from the directory, provided one exists.
	 * 
	 * @param dirId
	 * @param name
	 * @param userId
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public FileMetaResource getChildFileMetaResource(Long dirId, String name, String userId) throws ServiceException {
		
		CodeTimer timer = new CodeTimer();
		timer.start();
		
		PathResource resource = secureTreeService.getChildResource(dirId, name, ResourceType.FILE, userId);
		
		timer.stop();
		
		//logger.info("getChildFileMetaResource completed in " + timer.getElapsedTime());
		
		if(resource != null) {
			return (FileMetaResource)resource;
		}
		return null;
		
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
		
		PathResource resource = secureTreeService.getPathResource(nodeId, userId);
		if(resource == null){
			throw new ServiceException("Failed to get file meta resource by node id, returned object was null. "
					+ "nodeId=" + nodeId + ", includeBinary=" + includeBinary);
		}
		if(resource.getResourceType() == ResourceType.FILE){
			FileMetaResource fileMeta = (FileMetaResource)resource;
			if(includeBinary){
				fileMeta = populateWithBinaryData(fileMeta, userId);
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
	 * @param userId - id of user completing the action
	 * @param includeBinary
	 * @return
	 * @throws Exception
	 */
	@MethodTimer
	public FileMetaResource getFileMetaResource(String storeName, String relativePath, String userId, boolean includeBinary) throws ServiceException {
		
		PathResource resource = secureTreeService.getPathResource(storeName, relativePath, userId);
		if(resource == null){
			throw new ServiceException("Failed to get file meta resource by store name and resource relative path, returned object was null. "
					+ "storeName=" + storeName + ", relativePath=" + relativePath + ", includeBinary=" + includeBinary);
		}
		if(resource.getResourceType() == ResourceType.FILE){
			FileMetaResource fileMeta = (FileMetaResource)resource;
			if(includeBinary){
				fileMeta = populateWithBinaryData(fileMeta, userId);
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
	 * @param userId - id of user completing the action
	 * @return
	 * @throws Exception
	 */
	private FileMetaResource populateWithBinaryData(FileMetaResource resource, String userId) throws ServiceException {
		
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
				store = getStoreById(storeId, userId);
				if(store == null){
					throw new ServiceException("Failed to fetch store from DB for FileMetaResource, returned store object "
							+ "was null, storeId => " + storeId + " Cannot populate FileMetaResource with binary data.");
				}
				resource.setStore(store);
			}
			Path pathToFile = PathResourceUtil.buildPath(store, resource);
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
	 * Adds new file to the database, waiting until complete. After file meta data is added it spawns a non-blocking child task for adding/refreshing the
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
	public FileMetaResource addFileBlock(Long dirNodeId, String userId, Path filePath, boolean replaceExisting) throws ServiceException {
		
		final DirectoryResource dirRes = getDirectory(dirNodeId, userId);
		
		return addFileBlock(dirRes, filePath, replaceExisting, userId);
		
	}
	
	/**
	 * Adds new file to the database, waiting until complete. After file meta data is added it spawns a non-blocking child task for adding/refreshing the
	 * binary data in the database.
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
	public FileMetaResource addFileBlock(String storeName, String dirRelPath, String userId, Path filePath, boolean replaceExisting) throws ServiceException {
		
		final DirectoryResource dirRes = getDirectory(storeName, dirRelPath, userId);
		
		return addFileBlock(dirRes, filePath, replaceExisting, userId);		
		
	}
	
	/**
	 * Adds new file to the database, waiting until complete. After file meta data is added it spawns a non-blocking child task for adding/refreshing the
	 * binary data in the database.
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
	public FileMetaResource addFileBlock(DirectoryResource toDir, Path filePath, boolean replaceExisting, String userId) throws ServiceException {
		
		// user must have write permission on destination directory
		if(!toDir.getCanWrite()){
			errorHandler.handlePermissionDenied(PermissionError.WRITE, toDir, userId);
		}		
		
		final Store store = getStore(toDir, userId);
		final QueuedTaskManager generalTaskManager = getGeneralTaskManagerForStore(store);
		final QueuedTaskManager binaryTaskManager = getBinaryTaskManagerForStore(store);
		final QueuedTaskManager indexWriterManager = getIndexWriterTaskManagerForStore(store);
		
		//
		// Parent task adds file meta data to database, spawns child task for refreshing binary data
		// in the database, then returns quickly. We must wait (block) for this parent task to complete.
		//
		class AddFileTask extends AbstractQueuedTask<FileMetaResource> {
			public FileMetaResource doWork() throws ServiceException {
				
				//logger.info("---- ADDING FILE WITHOUT BINARY DATA FOR FILE " + filePath.toString() + " (START)");
				
				String fileName = filePath.getFileName().toString();
				FileMetaResource existingResource = getChildFileMetaResource(toDir.getNodeId(), fileName, userId);
				final boolean haveExisting = existingResource != null ? true : false;
				
				FileMetaResource newOrUpdatedFileResource = null;
				if(haveExisting && !replaceExisting) {
					throw new ServiceException(" Directory [id=" + toDir.getNodeId() + ", relPath=" + toDir.getRelativePath() + "] "
							+ "already contains a file with the name '" + fileName + "', and 'replaceExisting' param is set to false.");					
				}else if(haveExisting) {
					// update existing file (remove current binary data, and update existing file meta data)
					try {
						newOrUpdatedFileResource = fileSystemRepository._updateFileDiscardOldBinary(toDir, filePath, existingResource);
					} catch (Exception e) {
						throw new ServiceException("Error updating existing file [id=" + existingResource.getNodeId() + ", relPath=" + existingResource.getRelativePath() + "] in "
								+ "directory [id=" + toDir.getNodeId() + ", relPath=" + toDir.getRelativePath() + "], " + e.getMessage());
					}					
				}else {
					try {
						newOrUpdatedFileResource = fileSystemRepository._addNewFileWithoutBinary(store, toDir, filePath);
					} catch (Exception e) {
						throw new ServiceException("Error adding new file " + filePath.toString() + " to "
								+ "directory [id=" + toDir.getNodeId() + ", relPath=" + toDir.getRelativePath() + "], " + e.getMessage());
					}					
				}
				
				// broadcast directory contents changed event
				resChangeService.directoryContentsChanged(toDir.getNodeId());
				
				//
				// Child task for adding document to search index
				//
				final FileMetaResource documentToIndex = newOrUpdatedFileResource;
				class AddFileSearchIndexTask extends AbstractQueuedTask<Void> {

					@Override
					public Void doWork() throws ServiceException {
						try {
							
							// set the directory so we can store that information in the lucene index
							documentToIndex.setDirectory(toDir);							
							
							if(haveExisting) {
								indexerService.getIndexerForStore(store).update(documentToIndex);
							}else {
								indexerService.getIndexerForStore(store).add(documentToIndex);
							}
						} catch (IOException e) {
							logger.error("Error adding/updating document in search index, " + e.getMessage());
						}
						return null;
					}

					@Override
					public Logger getLogger() {
						return logger;
					}
				}
				
				// add to index writer task manager
				AddFileSearchIndexTask indexTask = new AddFileSearchIndexTask();
				indexTask.setName("Index Writer Task [" + newOrUpdatedFileResource.toString() + "]");
				indexWriterManager.addTask(indexTask);
				
				//logger.info("---- ADDING FILE WITHOUT BINARY DATA FOR FILE " + filePath.toString() + " (END)");
				
				//
				// Child task refreshes the binary data in the database. We do not need to wait (block) for this to finish
				//
				//final FileMetaResource finalFileMetaResource = newOrUpdatedFileResource;
				final Long fileNodeId = newOrUpdatedFileResource.getNodeId();
				final String fileRelPath = newOrUpdatedFileResource.getRelativePath();
				class RefreshBinaryTask extends AbstractQueuedTask<Void> {
					public Void doWork() throws ServiceException {

						//logger.info("---- REFRESH BINARY DATA IN DB FOR FILE " + fileRelPath + "(START)");
						try {
							fileSystemRepository.refreshBinaryDataInDatabase(fileNodeId);
						} catch (Exception e) {
							throw new ServiceException("Error refreshing (or adding) binary data in database (eas_binary_resource) "
									+ "for file resource node => " + fileNodeId, e);
						}
						//logger.info("---- REFRESH BINARY DATA IN DB FOR FILE " + fileRelPath + "(END)");
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
		
		return fileMetaResource;
		
	}
	
	/**
	 * Adds new file to the database, then spawns a non-blocking child task for adding/refreshing the
	 * binary data in the database. This version does not wait for the file to be added to the database, instead
	 * it queues the process and returns instantly.
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
	public void addFile(Long dirNodeId, String userId, Path filePath, boolean replaceExisting) throws ServiceException {
		
		final DirectoryResource dirRes = getDirectory(dirNodeId, userId);
		
		this.addFile(dirRes, filePath, replaceExisting, userId);
		
	}
	
	/**
	 * Adds new file to the database, then spawns a non-blocking child task for adding/refreshing the
	 * binary data in the database. This version does not wait for the file to be added to the database, instead
	 * it queues the process and returns instantly.
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
	public void addFile(String storeName, String dirRelPath, String userId, Path filePath, boolean replaceExisting) throws ServiceException {
		
		final DirectoryResource dirRes = getDirectory(storeName, dirRelPath, userId);
		
		this.addFile(dirRes, filePath, replaceExisting, userId);		
		
	}
	
	/**
	 * Adds new file to the database, then spawns a non-blocking child task for adding/refreshing the
	 * binary data in the database. This version does not wait for the file to be added to the database, instead
	 * it queues the process and returns instantly. 
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
	public void addFile(DirectoryResource toDir, Path filePath, boolean replaceExisting, String userId) throws ServiceException {
		
		// user must have write permission on destination directory
		if(!toDir.getCanWrite()){
			errorHandler.handlePermissionDenied(PermissionError.WRITE, toDir, userId);
		}		
		
		final Store store = getStore(toDir, userId);
		final QueuedTaskManager generalTaskManager = getGeneralTaskManagerForStore(store);
		final QueuedTaskManager binaryTaskManager = getBinaryTaskManagerForStore(store);
		final QueuedTaskManager indexWriterManager = getIndexWriterTaskManagerForStore(store);
		
		//
		// Parent task adds file meta data to database, spawns child task for refreshing binary data
		// in the database, then returns quickly. We must wait (block) for this parent task to complete.
		//
		class AddFileTask extends AbstractQueuedTask<FileMetaResource> {
			public FileMetaResource doWork() throws ServiceException {
				
				CodeTimer timer = new CodeTimer();
				timer.start();
				//logger.info("---- (START) ---- ADDING FILE WITHOUT BINARY DATA FOR FILE " + filePath.toString());
				
				String fileName = filePath.getFileName().toString();
				FileMetaResource existingResource = getChildFileMetaResource(toDir.getNodeId(), fileName, userId);
				boolean haveExisting = existingResource != null ? true : false;
				
				FileMetaResource newOrUpdatedFileResource = null;
				if(haveExisting && !replaceExisting) {
					throw new ServiceException(" Directory [id=" + toDir.getNodeId() + ", relPath=" + toDir.getRelativePath() + "] "
							+ "already contains a file with the name '" + fileName + "', and 'replaceExisting' param is set to false.");					
				}else if(haveExisting) {
					// update existing file (remove current binary data, and update existing file meta data)
					try {
						newOrUpdatedFileResource = fileSystemRepository._updateFileDiscardOldBinary(toDir, filePath, existingResource);
					} catch (Exception e) {
						throw new ServiceException("Error updating existing file [id=" + existingResource.getNodeId() + ", relPath=" + existingResource.getRelativePath() + "] in "
								+ "directory [id=" + toDir.getNodeId() + ", relPath=" + toDir.getRelativePath() + "], " + e.getMessage());
					}					
				}else {
					try {
						newOrUpdatedFileResource = fileSystemRepository._addNewFileWithoutBinary(store, toDir, filePath);
					} catch (Exception e) {
						throw new ServiceException("Error adding new file " + filePath.toString() + " to "
								+ "directory [id=" + toDir.getNodeId() + ", relPath=" + toDir.getRelativePath() + "], " + e.getMessage());
					}					
				}
				
				// broadcast directory contents changed event
				resChangeService.directoryContentsChanged(toDir.getNodeId());				
				
				timer.stop();
				//logger.info("----- (END " + timer.getElapsedTime() + ") ----- ADDING FILE WITHOUT BINARY DATA FOR FILE " + filePath.toString());
				
				timer.reset();
				
				//
				// Child task for adding document to search index
				//
				final FileMetaResource documentToIndex = newOrUpdatedFileResource;
				class AddFileSearchIndexTask extends AbstractQueuedTask<Void> {

					@Override
					public Void doWork() throws ServiceException {
						try {
							
							// set the directory so we can store that information in the lucene index
							documentToIndex.setDirectory(toDir);
							
							if(haveExisting) {
								indexerService.getIndexerForStore(store).update(documentToIndex);
							}else {
								indexerService.getIndexerForStore(store).add(documentToIndex);
							}
						} catch (IOException e) {
							logger.error("Error adding/updating document in search index, " + e.getMessage());
						}
						return null;
					}

					@Override
					public Logger getLogger() {
						return logger;
					}
				}
				
				// add to index writer task manager
				AddFileSearchIndexTask indexTask = new AddFileSearchIndexTask();
				indexTask.setName("Index Writer Task [" + newOrUpdatedFileResource.toString() + "]");
				indexWriterManager.addTask(indexTask);
				
				timer.start();
				//logger.info("---- (START) ---- ADDING BINARY REFRESH TASK FOR FILE " + filePath.toString());				
				
				//
				// Child task refreshes the binary data in the database. We do not need to wait (block) for this to finish
				//
				//final FileMetaResource finalFileMetaResource = newOrUpdatedFileResource;
				final Long fileNodeId = newOrUpdatedFileResource.getNodeId();
				final String fileRelPath = newOrUpdatedFileResource.getRelativePath();
				class RefreshBinaryTask extends AbstractQueuedTask<Void> {
					public Void doWork() throws ServiceException {

						CodeTimer timer = new CodeTimer();
						timer.start();
						//logger.info("---- (START) ---- PERFORMING REFRESH BINARY DATA IN DB FOR FILE " + fileRelPath);
						
						try {
							fileSystemRepository.refreshBinaryDataInDatabase(fileNodeId);
						} catch (Exception e) {
							throw new ServiceException("Error refreshing (or adding) binary data in database (eas_binary_resource) "
									+ "for file resource node => " + fileNodeId, e);
						}
						timer.stop();
						//logger.info("----- (END " + timer.getElapsedTime() + ") ----- PERFORMING REFRESH BINARY DATA IN DB FOR FILE " + 
						//		fileRelPath);
						return null;				
						
					}
					public Logger getLogger() {
						return logger;
					}
				}
				
				// add to binary task manager (not general task manager)
				RefreshBinaryTask refreshTask = new RefreshBinaryTask();
				refreshTask.setName("Refresh file binary [" + newOrUpdatedFileResource.toString() + "]");
				binaryTaskManager.addTask(refreshTask);
				
				timer.stop();
				//logger.info("---- (END " + timer.getElapsedTime() + ") ---- ADDING BINARY REFRESH TASK FOR FILE " + filePath.toString());				
				
				return newOrUpdatedFileResource;				
			}
			public Logger getLogger() {
				return logger;
			}
		};
		
		AddFileTask addTask = new AddFileTask();
		addTask.setName("Add File [dirNodeId=" + toDir.getNodeId() + ", filePath=" + filePath + 
				", replaceExisting=" + replaceExisting + "]");
		generalTaskManager.addTask(addTask);
		
		//FileMetaResource fileMetaResource = addTask.get(); // block until finished
		
		//return fileMetaResource;
		
	}
	
	/**
	 * Updates the file
	 * 
	 * @param fileNodeId - id of file to update
	 * @param newName - new name
	 * @param newDesc - new description
	 * @param userId - id of user performing the action
	 * @throws ServiceException
	 */
	@MethodTimer
	public void updateFile(Long fileNodeId, String newName, String newDesc, String userId) throws ServiceException {
		
		logger.debug("Updating file [fileNodeId=" + fileNodeId + "]");
		
		// need execute & write permission on file
		FileMetaResource file = this.getFileMetaResource(fileNodeId, userId, false);
		if(!file.getCanExecute()) {
			logger.info("No execute permission");
			errorHandler.handlePermissionDenied(PermissionError.EXECUTE, file, userId);
		}
		if(!file.getCanWrite()) {
			logger.info("No write permission");
			errorHandler.handlePermissionDenied(PermissionError.WRITE, file, userId);
		}		
		
		// also need read & write permission on parent directory, if one exists
		DirectoryResource parentDir = getParentDirectory(fileNodeId, userId);
		if(parentDir != null) {
			if(!parentDir.getCanRead()) {
				logger.info("No read permission on parent");
				errorHandler.handlePermissionDenied(PermissionError.READ, parentDir, userId);
			}
			if(!parentDir.getCanWrite()) {
				logger.info("No write permission on parent");
				errorHandler.handlePermissionDenied(PermissionError.WRITE, parentDir, userId);
			}
		}else {
			throw new ServiceException("Error fetching parent directory file file resource with node id = " + fileNodeId);		
		}
		
		final Store store = getStore(file, userId);
		final QueuedTaskManager taskManager = getGeneralTaskManagerForStore(store);
		final QueuedTaskManager indexWriterManager = getIndexWriterTaskManagerForStore(store);
		
		class UpdateFileTask extends AbstractQueuedTask<Void> {

			@Override
			public Void doWork() throws ServiceException {
				
				// update file
				try {
					fileSystemRepository.updateFile(file, newName, newDesc);
				} catch (Exception e) {
					throw new ServiceException("Error updating file with node id => " + file.getNodeId() + ". " + e.getMessage(), e);
				}
				
				resChangeService.directoryContentsChanged(parentDir.getNodeId());
				
				//
				// Child task for adding document to search index
				//
				class AddFileSearchIndexTask extends AbstractQueuedTask<Void> {

					@Override
					public Void doWork() throws ServiceException {
						try {
							indexerService.getIndexerForStore(store).update(file);
						} catch (IOException e) {
							logger.error("Error updating document in search index, " + e.getMessage());
						}
						return null;
					}

					@Override
					public Logger getLogger() {
						return logger;
					}
				}
				
				// add to index writer task manager
				AddFileSearchIndexTask indexTask = new AddFileSearchIndexTask();
				indexTask.setName("Index Writer Task [" + file.toString() + "]");
				indexWriterManager.addTask(indexTask);				
				
				return null;
				
			}

			@Override
			public Logger getLogger() {
				return logger;
			}
			
			
		}
		
		UpdateFileTask task = new UpdateFileTask();
		task.setName("Update file [fileNodeId=" + file.getNodeId() + ", name=" + file.getNodeName() + "]");
		taskManager.addTask(task);
		
		task.waitComplete(); // block until complete		
		
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
		
		DirectoryResource parentDir = getParentDirectory(fileMetaResource.getNodeId(), userId);
		
		// user must have read & write access on parent directory
		// file resource inherits permission from parent directory, so this works.
		//if(!fileMetaResource.getCanRead()) {
		//	this.handlePermissionDenied(PermissionError.READ, fileMetaResource, userId);
		//}		
		//if(!fileMetaResource.getCanWrite()) {
		//	this.handlePermissionDenied(PermissionError.WRITE, fileMetaResource, userId);
		//}
		
		if(!parentDir.getCanRead()) {
			errorHandler.handlePermissionDenied(PermissionError.READ, parentDir, userId);
		}		
		if(!parentDir.getCanWrite()) {
			errorHandler.handlePermissionDenied(PermissionError.WRITE, parentDir, userId);
		}		
		
		final Store store = getStore(fileMetaResource, userId);
		final QueuedTaskManager taskManager = getGeneralTaskManagerForStore(store);		
		
		class Task extends AbstractQueuedTask<Void> {

			@Override
			public Void doWork() throws ServiceException {
	
				try {
					fileSystemRepository.removeFile(store, fileMetaResource);
				} catch (Exception e) {
					throw new ServiceException("Error removing file with node id => " + fileMetaResource.getNodeId() + ". " + e.getMessage(), e);
				}
				
				resChangeService.directoryContentsChanged(parentDir.getNodeId());
				
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
		
		FileMetaResource sourceFile = getFileMetaResource(fileNodeId, userId, false);
		DirectoryResource destitationDir = getDirectory(dirNodeId, userId);
		
		copyFile(sourceFile, destitationDir, replaceExisting, userId);
		
	}
	
	public void copyFile(FileMetaResource fileToCopy, DirectoryResource toDir, boolean replaceExisting, String userId) throws ServiceException {
		
		// user must have read on parent directory
		// file resource inherits permission from parent directory, so this works.
		if(!fileToCopy.getCanRead()) {
			errorHandler.handlePermissionDenied(PermissionError.READ, fileToCopy, userId);
		}
		
		Store soureStore = getStore(fileToCopy, userId);
		Path sourceFilePath = PathResourceUtil.buildPath(soureStore, fileToCopy);
		
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
		
		final FileMetaResource fileToMove = getFileMetaResource(fileNodeId, userId, false);
		final DirectoryResource destDir = getDirectory(dirNodeId, userId);
				
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
	public void moveFile(FileMetaResource fileToMove, DirectoryResource destDir, boolean replaceExisting, String userId) throws ServiceException {
		
		// user must have write access on destination directory
		if(!destDir.getCanWrite()){
			errorHandler.handlePermissionDenied(PermissionError.WRITE, destDir, userId);
		}
		
		// user must have read and write on parent directory of file being moved
		DirectoryResource sourceDir = getParentDirectory(fileToMove.getChildNodeId(), userId);	
		if(!sourceDir.getCanRead()) {
			errorHandler.handlePermissionDenied(PermissionError.READ, sourceDir, userId);
		}
		if(!sourceDir.getCanWrite()) {
			errorHandler.handlePermissionDenied(PermissionError.WRITE, sourceDir, userId);
		}
		
		final Store store = getStore(destDir, userId);
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
				
				resChangeService.directoryContentsChanged(sourceDir.getNodeId());
				resChangeService.directoryContentsChanged(destDir.getNodeId());
				
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
	 * Fetch first-level directory resource, by name (case insensitive), from the directory, provided one exists.
	 * 
	 * @param dirId
	 * @param name
	 * @param userId
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public DirectoryResource getChildDirectoryResource(Long dirId, String name, String userId) throws ServiceException {
		
		PathResource resource = secureTreeService.getChildResource(dirId, name, ResourceType.DIRECTORY, userId);
		if(resource != null) {
			return (DirectoryResource)resource;
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
		
		PathResource resource = secureTreeService.getPathResource(nodeId, userId);
		
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
		
		PathResource resource = secureTreeService.getPathResource(storeName, relativePath, userId);
		
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
		
		PathResource parent = secureTreeService.getParentPathResource(nodeId, userId);
		
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
			errorHandler.handlePermissionDenied(PermissionError.WRITE, parentDir, userId);
		}		
		
		final Store store = getStore(parentDir, userId);
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
				
				// after we create the directory we need to fetch it in order to have the permissions (read, write, & execute bits) properly evaluated.
				DirectoryResource evaluatedDir = getDirectory(dirResource.getNodeId(), userId);
				
				resChangeService.directoryContentsChanged(parentDir.getNodeId());
				
				return evaluatedDir;				
				
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
	 * Updates the directory
	 * 
	 * @param dirNodeId - id of directory to update
	 * @param name - new name
	 * @param desc - new description
	 * @param readGroup1 - new read group
	 * @param writeGroup1 - new write group
	 * @param executeGroup1 - new execute group
	 * @param userId - id of user completing the action
	 * @throws ServiceException
	 */
	@MethodTimer
	public void updateDirectory(Long dirNodeId, String name, String desc, String readGroup1, String writeGroup1, String executeGroup1, String userId) throws ServiceException {
		
		logger.debug("Updating directory [dirNodeId=" + dirNodeId + "]");
		
		// need execute & write permission on directory
		DirectoryResource dir = this.getDirectory(dirNodeId, userId);
		if(!dir.getCanExecute()) {
			logger.info("No execute permission on directory");
			errorHandler.handlePermissionDenied(PermissionError.EXECUTE, dir, userId);
		}
		if(!dir.getCanWrite()) {
			logger.info("No write permission on directory");
			errorHandler.handlePermissionDenied(PermissionError.WRITE, dir, userId);
		}		
		
		// also need read & write permission on parent directory, if one exists
		DirectoryResource parentDir = this.getParentDirectory(dirNodeId, userId);
		if(parentDir != null) {
			if(!parentDir.getCanRead()) {
				logger.info("No read permission on parent");
				errorHandler.handlePermissionDenied(PermissionError.READ, parentDir, userId);
			}
			if(!parentDir.getCanWrite()) {
				logger.info("No write permission on parent");
				errorHandler.handlePermissionDenied(PermissionError.WRITE, parentDir, userId);
			}
		}else {
			// no parent directory, so this must be a root directory for a store.
			// read, write, and execute groups are required for root directories.
			if(StringUtil.isAnyNullEmpty(readGroup1, writeGroup1, executeGroup1)) {
				throw new ServiceException("Read, write, and execute groups are required for root directories (directories that have no parent.) "
						+ "One or all of the values for read, write, and execute groups are null or blank.");
			}			
		}
		
		final Store store = getStore(dir, userId);
		final QueuedTaskManager taskManager = getGeneralTaskManagerForStore(store);
		
		class Task extends AbstractQueuedTask<Void> {

			@Override
			public Void doWork() throws ServiceException {
				
				// update directory
				try {
					fileSystemRepository.updateDirectory(dir, name, desc, readGroup1, writeGroup1, executeGroup1);
				} catch (Exception e) {
					throw new ServiceException("Error updating directory with node id => " + dir.getNodeId() + ". " + e.getMessage(), e);
				}
				
				// won't have a parent dir if this is a root directory for a store
				if(parentDir != null) {
					resChangeService.directoryContentsChanged(parentDir.getNodeId());
				}
				
				return null;
				
			}

			@Override
			public Logger getLogger() {
				return logger;
			}
			
			
		}
		
		Task task = new Task();
		task.setName("Update directory [dirNodeId=" + dir.getNodeId() + ", name=" + dir.getNodeName() + "]");
		taskManager.addTask(task);
		
		task.waitComplete(); // block until complete	
		
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
		
		final Store store = getStore(dirToDelete, userId);
		
		final QueuedTaskManager taskManager = getGeneralTaskManagerForStore(store);
		
		Long dirNodeId = dirToDelete.getNodeId();
		
		class Task extends AbstractQueuedTask<Void> {

			@Override
			public Void doWork() throws ServiceException {
				
				getLogger().info("Deleting Tree:");
				
				final Tree<PathResource> tree = secureTreeService.buildPathResourceTree(dirToDelete, userId);
				
				DirectoryResource rootDirToDelete = (DirectoryResource)tree.getRootNode().getData();
				if(rootDirToDelete.getParentNodeId().equals(0L)){
					throw new ServiceException("Node id => " + rootDirToDelete.getNodeId() + " points to a root directory for a store. "
							+ "You cannot use this method to remove a root directory.");
				}				
				
				//pathResTreeLogger.logTree(tree);
				
				try {
					
					// walk tree, bottom-up, from leafs to root node.
					Trees.walkTree(tree,
						(treeNode) -> {
							
							try {
								if(treeNode.getData().getResourceType() == ResourceType.FILE){
									
									FileMetaResource fileToDelete = (FileMetaResource)treeNode.getData();
									// this works because files inherit permissions from their directory
									if(!fileToDelete.getCanWrite()) {
										errorHandler.handlePermissionDenied(PermissionError.WRITE, fileToDelete, userId);
									}
									fileSystemRepository.removeFile(store, fileToDelete);
									
								}else if(treeNode.getData().getResourceType() == ResourceType.DIRECTORY){
									
									// we walk the tree bottom up, so by the time we remove a directory it will be empty
									DirectoryResource nextDirToDelete = (DirectoryResource)treeNode.getData();
									if(!nextDirToDelete.getCanWrite()) {
										errorHandler.handlePermissionDenied(PermissionError.WRITE, nextDirToDelete, userId);
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
				errorHandler.handlePermissionDenied(PermissionError.READ, fromDirParent, userId);
			}
			
		}
		
		final DirectoryResource fromDir = getDirectory(copyDirNodeId, userId);
		final DirectoryResource toDir = getDirectory(destDirNodeId, userId);
		final Store fromStore = getStore(fromDir, userId);
		final Store toStore = getStore(fromDir, userId);

		final Tree<PathResource> fromTree = secureTreeService.buildPathResourceTree(fromDir, userId);
		
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
				errorHandler.handlePermissionDenied(PermissionError.READ, dirToCopy, userId);
			}
			// user need write permission on destination directory
			if(!toDir.getCanWrite()) {
				errorHandler.handlePermissionDenied(PermissionError.WRITE, toDir, userId);
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
			
			copyFile( (FileMetaResource)resourceToCopy, toDir, replaceExisting, userId);
			
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
				errorHandler.handlePermissionDenied(PermissionError.READ, parentDir, userId);
			}
			if(!parentDir.getCanWrite()) {
				errorHandler.handlePermissionDenied(PermissionError.WRITE, parentDir, userId);
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

		final Tree<PathResource> fromTree = secureTreeService.buildPathResourceTree(dirToMove, userId);
		
		DirectoryResource toDir = this.getDirectory(destDirId, userId);
		final Store fromStore = getStore(dirToMove, userId);
		final Store toStore = getStore(toDir, userId);
		
		// walk the tree top-down and copy over directories one at a time, then use
		// existing moveFile method.
		moveDirectoryTraversal(fromStore, toStore, fromTree.getRootNode(), toDir, replaceExisting, userId);
		
		// remove from dir and all child directories
		removeDirectory(dirToMove.getNodeId(), userId);
		
	}
	
	/**
	 * Helper method for moving a directory. This method is called recursively to move all child
	 * directories in the directory tree.
	 * 
	 * @param fromStore
	 * @param toStore
	 * @param pathResourceNode
	 * @param toDir
	 * @param replaceExisting
	 * @param userId
	 * @throws ServiceException
	 */
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
				errorHandler.handlePermissionDenied(PermissionError.READ, dirToMove, userId);
			}
			if(!dirToMove.getCanWrite()) {
				errorHandler.handlePermissionDenied(PermissionError.WRITE, dirToMove, userId);
			}
			
			// user must have write access on destination directory
			if(!toDir.getCanWrite()) {
				errorHandler.handlePermissionDenied(PermissionError.WRITE, toDir, userId);
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
					addFile(dirNodeId, userId, pathToFile, replaceExisting);
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
					addFile(storeName, dirRelPath, userId, pathToFile, replaceExisting);
				} catch (ServiceException e) {
					throw new RuntimeException("Error adding file '" + pathToFile.toString() + "' to directory  with relPath'" + 
							dirRelPath + "', under store name '" + storeName + "'.", e);
				}
			});		
		
	}
	
	/**
	 * Get a map that specifies read access to all files in a store for a specific user.
	 * 
	 * @param store - the store
	 * @param userId - the ID of the user
	 * @return A map where keys are fileIds. Every file in the store will have its keys returned in the map. The values
	 * in the maps are booleans and specify whether or not the user has access to said file.  If map.get(fileId) == true then
	 * the user has read access to the file. If map.get(fileId) == false then the user does not have read access to the file.
	 * @throws ServiceException
	 */
	public Map<Long,Boolean> getFileReadAccessMap(final Store store, final String userId) throws ServiceException {
		
		//final Store store = getStoreById(storeId, userId);
		final Long rootNodeId = store.getRootDir().getNodeId();
		final Tree<PathResource> tree = secureTreeService.buildPathResourceTree(rootNodeId, userId);
		
		Map<Long,Boolean> accessMap = new HashMap<Long,Boolean>();
		
		try {
			// walk tree, top-down
			Trees.walkTree(tree,
				(treeNode) -> {
					if(treeNode.getData().getResourceType() == ResourceType.FILE){
						FileMetaResource file = (FileMetaResource)treeNode.getData();
						if(file.getCanRead()) {
							accessMap.put(file.getNodeId(), true);
						}
					}
				},
				WalkOption.PRE_ORDER_TRAVERSAL);
		}catch(TreeNodeVisitException e){
			throw new ServiceException("Encountered error when walking tree to build read access map. "
					+ "Tree root node Id => " + rootNodeId + ". " + e.getMessage(), e);
		}		
		
		return accessMap;
		
	}

}
