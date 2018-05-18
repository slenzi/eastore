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

import org.eamrf.concurrent.task.QueuedTaskManager;
import org.eamrf.concurrent.task.TaskIdGenerator;
import org.eamrf.concurrent.task.TaskManagerProvider;
import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.CollectionUtil;
import org.eamrf.core.util.FileUtil;
import org.eamrf.core.util.StringUtil;
import org.eamrf.eastore.core.aop.profiler.MethodTimer;
import org.eamrf.eastore.core.concurrent.StoreTaskManagerMap;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.properties.ManagedProperties;
import org.eamrf.eastore.core.search.lucene.StoreIndexer;
import org.eamrf.eastore.core.search.service.StoreIndexerService;
import org.eamrf.eastore.core.service.file.task.AddDirectoryTask;
import org.eamrf.eastore.core.service.file.task.AddFileTask;
import org.eamrf.eastore.core.service.file.task.CopyDirectoryTask;
import org.eamrf.eastore.core.service.file.task.CopyFileTask;
import org.eamrf.eastore.core.service.file.task.FileServiceTaskListener;
import org.eamrf.eastore.core.service.file.task.MoveDirectoryTask;
import org.eamrf.eastore.core.service.file.task.MoveFileTask;
import org.eamrf.eastore.core.service.file.task.RemoveDirectoryTask;
import org.eamrf.eastore.core.service.file.task.RemoveFileTask;
import org.eamrf.eastore.core.service.file.task.UpdateDirectoryTask;
import org.eamrf.eastore.core.service.file.task.UpdateFileMetaTask;
import org.eamrf.eastore.core.service.security.GatekeeperService;
import org.eamrf.eastore.core.service.tree.file.PathResourceUtil;
import org.eamrf.eastore.core.service.tree.file.secure.SecurePathResourceTreeService;
import org.eamrf.eastore.core.socket.messaging.ResourceChangeService;
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
				setAccessBits(userGroupCodes, rootDir);
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
			setAccessBits(userGroupCodes, rootDir);
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
			setAccessBits(userGroupCodes, rootDir);
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
	 * @param listener - a listener to track progress of the operation
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
			String userId,
			FileServiceTaskListener listener) throws ServiceException {
		
		// fetch store
		Store storeToEdit = getStoreById(storeId, userId);
		Long rootDirId = storeToEdit.getRootDir().getNodeId();
		DirectoryResource rootDir = getDirectory(rootDirId, userId);
		
		// need execute permission on root directory of store in order to edit store
		if(!rootDir.getCanExecute()) {
			logger.info("No execute permission for store root directory. Permission to edit store denied.");
			errorHandler.handlePermissionDenied(PermissionError.EXECUTE, rootDir, userId);
		}
		
		// make sure there doesn't exist already a store with the same name (excluding store we're editing in case we're 
		// simply changing the case (upper/lower) of the store)
		Store existingStore = getStoreByName(storeName, userId);
		if(existingStore != null && !storeId.equals(existingStore.getId())){
			throw new ServiceException("Store with name '" + storeName + "' already exists. Store names must be unique.");
		}		
		
		// TODO - the following two function calls *should* be an atomic operation (currently not.)
		
		// update store name & description
		fileSystemRepository.updateStore(storeToEdit, storeName, storeDesc);
		
		// update root directory
		updateDirectory(rootDirId, rootDirName, rootDirDesc, readGroup1, writeGroup1, executeGroup1, userId, listener);
		
		
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
		
		FileServiceTaskListener listener = task -> {
			logger.info("Create directory for test store at " + Math.round(task.getProgress()) + "%");
		};
		
		DirectoryResource dirMore  = addDirectory(store.getNodeId(), "more", "more desc", userId, listener);
		DirectoryResource dirOther = addDirectory(store.getNodeId(), "other", "other desc", userId, listener);
			DirectoryResource dirThings = addDirectory(dirOther, "things", "things desc", userId, listener);
			DirectoryResource dirFoo = addDirectory(dirOther, "foo", "foo desc", userId, listener);
				DirectoryResource dirCats = addDirectory(dirFoo, "cats", "cats desc", userId, listener);
				DirectoryResource dirDogs = addDirectory(dirFoo, "dogs", "dogs desc", userId, listener);
					DirectoryResource dirBig = addDirectory(dirDogs, "big", "big desc", userId, listener);
					DirectoryResource dirSmall = addDirectory(dirDogs, "small", "small desc", userId, listener);
						DirectoryResource dirPics = addDirectory(dirSmall, "pics", "pics desc", userId, listener);		
		
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
		
		//CodeTimer timer = new CodeTimer();
		//timer.start();
		
		PathResource resource = secureTreeService.getChildResource(dirId, name, ResourceType.FILE, userId);
		
		//timer.stop();
		
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
		
			Store store = getStore(resource, userId);
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
	public void addFile(Long dirNodeId, String userId, Path filePath, boolean replaceExisting, FileServiceTaskListener listener) throws ServiceException {
		
		final DirectoryResource dirRes = getDirectory(dirNodeId, userId);
		
		addFile(dirRes, filePath, replaceExisting, userId, listener);
		
	}
	
	/**
	 * Adds new file to the database, then spawns two non-blocking child tasks for adding/refreshing the
	 * binary data in the database, and adding the file to the lucene search index.
	 * 
	 * @param storeName - name of the store
	 * @param dirRelPath - relative path of directory resource within the store
	 * @param userId - Id of the user adding the file
	 * @param filePath - path to file on local file system
	 * @param replaceExisting - pass true to overwrite any file with the same name that's already in the directory tree. If you
	 * pass false, and there exists a file with the same name (case insensitive) then an ServiceException will be thrown.
	 * @param listener - a listener to track progress of the operation
	 * @throws ServiceException
	 */
	@MethodTimer
	public void addFile(String storeName, String dirRelPath, String userId, Path filePath, boolean replaceExisting, FileServiceTaskListener listener) throws ServiceException {
		
		final DirectoryResource dirRes = getDirectory(storeName, dirRelPath, userId);
		
		addFile(dirRes, filePath, replaceExisting, userId, listener);		
		
	}
	
	/**
	 * Adds new file to the database, then spawns two non-blocking child tasks for adding/refreshing the
	 * binary data in the database, and adding the file to the lucene search index.
	 * 
	 * @param toDir - directory where file will be added
	 * @param filePath - path to file on local file system
	 * @param replaceExisting - pass true to overwrite any file with the same name that's already in the directory tree. If you
	 * pass false, and there exists a file with the same name (case insensitive) then an ServiceException will be thrown.
	 * @param userId - Id of the user adding the file
	 * @param listener - a listener to track progress of the operation
	 * @throws ServiceException
	 */
	@MethodTimer
	public void addFile(DirectoryResource toDir, Path filePath, boolean replaceExisting, String userId, FileServiceTaskListener listener) throws ServiceException {		
		
		final Store store = getStore(toDir, userId);
		final QueuedTaskManager generalTaskManager = getGeneralTaskManagerForStore(store);
		final QueuedTaskManager binaryTaskManager = getBinaryTaskManagerForStore(store);
		final QueuedTaskManager indexWriterTaskManager = getIndexWriterTaskManagerForStore(store);
		
		AddFileTask addTask = new AddFileTask(filePath, replaceExisting, toDir, userId, 
				fileSystemRepository, resChangeService, indexerService, 
				binaryTaskManager, indexWriterTaskManager, this, errorHandler);		
		
		addTask.setName("Add File [dirNodeId=" + toDir.getNodeId() + ", filePath=" + filePath + 
				", replaceExisting=" + replaceExisting + "]");
		
		if(listener != null) {
			addTask.registerProgressListener(listener);
		}
		
		generalTaskManager.addTask(addTask);
		
	}	
	
	/**
	 * Updates the file
	 * 
	 * @param fileNodeId - id of file to update
	 * @param newName - new name
	 * @param newDesc - new description
	 * @param userId - id of user performing the action
	 * @param listener - a listener to track progress of the operation
	 * @throws ServiceException
	 */
	@MethodTimer
	public void updateFile(Long fileNodeId, String newName, String newDesc, String userId, FileServiceTaskListener listener) throws ServiceException {
		
		logger.debug("Updating file [fileNodeId=" + fileNodeId + "]");
				
		FileMetaResource file = getFileMetaResource(fileNodeId, userId, false);

		final QueuedTaskManager generalTaskManager = getGeneralTaskManagerForStore(getStore(file, userId));
		final QueuedTaskManager indexWriterTaskManager = getIndexWriterTaskManagerForStore(getStore(file, userId));
		
		/*
		UpdateFileMetaTask updateTask = new UpdateFileMetaTask.Builder(file)
				.withNewName(newName)
				.withNewDesc(newDesc)
				.withFileRepository(fileSystemRepository)
				.withIndexer(indexerService)
				.withIndexWriterTaskManager(indexWriterTaskManager)
				.withResourceChangeService(resChangeService)
				.withFileService(this)
				.withErrorHandler(errorHandler)
				.build();
				*/
		
		UpdateFileMetaTask updateTask = new UpdateFileMetaTask(
				file, newName, newDesc, userId, fileSystemRepository, indexerService,
				resChangeService, indexWriterTaskManager, this, errorHandler);
		
		if(listener != null) {
			updateTask.registerProgressListener(listener);
		}		
		
		updateTask.setName("Update File [fileId = " + file.getNodeId() + ", path = " + file.getRelativePath() + "]");
		generalTaskManager.addTask(updateTask);
		
		updateTask.waitComplete(); // block until complete		
		
	}
	
	/**
	 * Remove the file, from database and disk. No undo.
	 * 
	 * @param fileNodeId - id of file resource to be removed
	 * @param userId - id of user performing the action
	 * @param listener - a listener to track progress of the operation
	 * @throws ServiceException
	 */
	@MethodTimer
	public void removeFile(Long fileNodeId, String userId, FileServiceTaskListener listener) throws ServiceException {
		
		final FileMetaResource fileMetaResource = getFileMetaResource(fileNodeId, userId, false);
		
		removeFile(fileMetaResource, userId, listener);
		
	}	
	
	/**
	 * Remove the file, from database and disk. No undo.
	 * 
	 * @param file - the file resource to remove
	 * @param userId - id of user performing the action
	 * @param listener - a listener to track progress of the operation
	 * @throws ServiceException
	 */
	@MethodTimer
	public void removeFile(FileMetaResource file, String userId, FileServiceTaskListener listener) throws ServiceException {
		
		final QueuedTaskManager taskManager = getGeneralTaskManagerForStore(getStore(file, userId));
		
		RemoveFileTask removeFileTask = new RemoveFileTask(
				file, userId, fileSystemRepository, resChangeService, this, errorHandler);
		removeFileTask.setName("Remove File [fileId = " + file.getNodeId() + ", path = " + file.getRelativePath() + "]");
		
		if(listener != null) {
			removeFileTask.registerProgressListener(listener);
		}			
		
		taskManager.addTask(removeFileTask);
		
		removeFileTask.waitComplete(); // block until finished		
		
	}	

	/**
	 * Copy file to another directory (could be in another store)
	 * 
	 * @param fileNodeId - id of the file to copy
	 * @param dirNodeId - id of the destination directory
	 * @param replaceExisting - pass true to replace any existing file in the destination directory with
	 * same name. If you pass false, and a file already exists, then an exception will be thrown.
	 * @param userId - Id of user performing the action
	 * @param listener - a listener to track progress of the operation
	 * @throws ServiceException
	 */
	@MethodTimer
	public void copyFile(Long fileNodeId, Long dirNodeId, boolean replaceExisting, String userId, FileServiceTaskListener listener) throws ServiceException {
		
		FileMetaResource sourceFile = getFileMetaResource(fileNodeId, userId, false);
		DirectoryResource destitationDir = getDirectory(dirNodeId, userId);
		
		copyFile(sourceFile, destitationDir, replaceExisting, userId, listener);
		
	}
	
	/**
	 * Copy file to another directory (could be in another store)
	 * 
	 * @param fileToCopy - the file to copy
	 * @param toDir - the destination directory
	 * @param replaceExisting - pass true to replace any existing file in the destination directory with
	 * same name. If you pass false, and a file already exists, then an exception will be thrown.
	 * @param userId - Id of user performing the action
	 * @param listener - a listener to track progress of the operation
	 * @throws ServiceException
	 */
	@MethodTimer
	public void copyFile(FileMetaResource fileToCopy, DirectoryResource toDir, boolean replaceExisting, String userId, FileServiceTaskListener listener) throws ServiceException {
		
		final QueuedTaskManager taskManager = getGeneralTaskManagerForStore(getStore(toDir, userId));
		
		CopyFileTask task = new CopyFileTask(fileToCopy, toDir, replaceExisting, userId, this, errorHandler);
		
		task.setName("Copy File [fileId = " + fileToCopy.getNodeId() + 
				", toDir = " + toDir.getNodeId() + ", replaceExisting=" + replaceExisting + "]");
		
		if(listener != null) {
			task.registerProgressListener(listener);
		}			
		
		taskManager.addTask(task);		
		
	}
		
	/**
	 * Move a file, preserving same node id.
	 * 
	 * @param fileNodeId - id of the file to move
	 * @param dirNodeId - id of the directory where the file will be moved to
	 * @param replaceExisting - pass true to replace any existing file in the destination directory with
	 * same name. If you pass false, and a file already exists, then an exception will be thrown.
	 * @param userId - id of user performing action
	 * @param listener - a listener to track progress of the operation
	 * @throws ServiceException
	 */
	@MethodTimer
	public void moveFile(Long fileNodeId, Long dirNodeId, boolean replaceExisting, String userId, FileServiceTaskListener listener) throws ServiceException {
		
		final FileMetaResource fileToMove = getFileMetaResource(fileNodeId, userId, false);
		final DirectoryResource destDir = getDirectory(dirNodeId, userId);
				
		moveFile(fileToMove, destDir, replaceExisting, userId, listener);
		
	}
	
	/**
	 * Move a file, preserving same node id.
	 * 
	 * @param fileToMove - file to move
	 * @param destDir - the directory where the file will be moved to
	 * @param replaceExisting - pass true to replace any existing file in the destination directory with
	 * same name. If you pass false, and a file already exists, then an exception will be thrown.
	 * @param userId - id of user performing action
	 * @param listener - a listener to track progress of the operation
	 * @throws ServiceException
	 */
	@MethodTimer
	public void moveFile(FileMetaResource fileToMove, DirectoryResource destDir, boolean replaceExisting, String userId, FileServiceTaskListener listener) throws ServiceException {
	
		final QueuedTaskManager taskManager = getGeneralTaskManagerForStore(getStore(destDir, userId));
		
		MoveFileTask moveTask = new MoveFileTask(
				userId, fileToMove, destDir, replaceExisting, 
				fileSystemRepository, resChangeService, errorHandler, this);
		
		moveTask.setName("Move file [fileNodeId=" + fileToMove.getNodeId() + ", dirNodeId=" + destDir.getNodeId() + ", replaceExisting=" + replaceExisting + "]");
		
		if(listener != null) {
			moveTask.registerProgressListener(listener);
		}		
		
		taskManager.addTask(moveTask);
		
		moveTask.waitComplete(); // MUST block until finished!	
		
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
	 * @param listener - a listener to track progress of the operation
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public DirectoryResource addDirectory(
			Long dirNodeId, String name, String desc, String userId, FileServiceTaskListener listener) throws ServiceException {
		
		final DirectoryResource resource = getDirectory(dirNodeId, userId);		
		
		return addDirectory(resource, name, desc, null, null, null, userId, listener);
		
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
	 * @param listener - a listener to track progress of the operation
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public DirectoryResource addDirectory(
			Long dirNodeId, String name, String desc, String readGroup1, String writeGroup1, 
			String executeGroup1, String userId, FileServiceTaskListener listener) throws ServiceException {
		
		final DirectoryResource resource = getDirectory(dirNodeId, userId);		
		
		return addDirectory(resource, name, desc, readGroup1, writeGroup1, executeGroup1, userId, listener);
		
	}
	
	/**
	 * Add new directory
	 * 
	 * @param parentDir - the parent directory under which the new directory will be created
	 * @param name - name of new directory
	 * @param desc - description for new directory
	 * @param userId - id of user performing the action
	 * @param listener - a listener to track progress of the operation
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public DirectoryResource addDirectory(DirectoryResource parentDir, String name, String desc, String userId, FileServiceTaskListener listener) throws ServiceException {
		
		return addDirectory(parentDir, name, desc, null, null, null, userId, listener);
		
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
	/*
	@MethodTimer
	public DirectoryResource addDirectory(
			DirectoryResource parentDir, String name, String desc, 
			String readGroup1, String writeGroup1, String executeGroup1, String userId) throws ServiceException {
		
		return addDirectory(parentDir, name, desc, readGroup1, writeGroup1, executeGroup1, userId, null);
		
	}
	*/
	
	/**
	 * Add new directory, with an observer that monitors the process.
	 * 
	 * @param parentDir
	 * @param name
	 * @param desc
	 * @param readGroup1
	 * @param writeGroup1
	 * @param executeGroup1
	 * @param userId
	 * @param listener - a listener to track progress of the operation
	 * @param taskObserver
	 * @return
	 * @throws ServiceException
	 */
	private DirectoryResource addDirectory(
			DirectoryResource parentDir, String name, String desc, 
			String readGroup1, String writeGroup1, String executeGroup1, 
			String userId, FileServiceTaskListener listener) throws ServiceException {
		
		final QueuedTaskManager taskManager = getGeneralTaskManagerForStore(getStore(parentDir, userId));		
		
		AddDirectoryTask task = new AddDirectoryTask(
				parentDir, name, desc, readGroup1, writeGroup1, executeGroup1, userId,
				fileSystemRepository, resChangeService, this, errorHandler);
		
		task.setName("Add directory [dirNodeId=" + parentDir.getNodeId() + ", name=" + name + "]");
		taskManager.addTask(task);
		
		if(listener != null) {
			task.registerProgressListener(listener);
		}
		
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
	 * @param listener - a listener to track progress of the operation
	 * @throws ServiceException
	 */
	@MethodTimer
	public void updateDirectory(
			Long dirNodeId, String name, String desc, 
			String readGroup1, String writeGroup1, String executeGroup1, String userId, FileServiceTaskListener listener) throws ServiceException {
		
		logger.debug("Updating directory [dirNodeId=" + dirNodeId + "]");
		
		DirectoryResource dir = getDirectory(dirNodeId, userId);
		
		final QueuedTaskManager taskManager = getGeneralTaskManagerForStore(getStore(dir, userId));
		
		UpdateDirectoryTask task = new UpdateDirectoryTask(
				dir, name, desc, readGroup1, writeGroup1, executeGroup1, userId,
				fileSystemRepository, resChangeService, this, errorHandler);

		task.setName("Update directory [dirNodeId=" + dir.getNodeId() + ", name=" + dir.getNodeName() + "]");
		
		if(listener != null) {
			task.registerProgressListener(listener);
		}		
		
		taskManager.addTask(task);
		
		task.waitComplete(); // block until complete	
		
	}
	
	/**
	 * Remove a directory. Walks the tree in POST_ORDER_TRAVERSAL, from leafs to root node.
	 * 
	 * @param dirNodeId - id of directory to remove
	 * @param userId - id of user completing the action
	 * @param listener - a listener to track progress of the operation
	 * @throws ServiceException
	 */
	@MethodTimer
	public void removeDirectory(Long dirNodeId, String userId, FileServiceTaskListener listener) throws ServiceException {
		
		final DirectoryResource dirToDelete = getDirectory(dirNodeId, userId);
		
		removeDirectory(dirToDelete, userId, listener);		
		
	}	
	
	/**
	 * Remove a directory. Walks the tree in POST_ORDER_TRAVERSAL, from leafs to root node.
	 * 
	 * @param dirNodeId - id of directory to remove
	 * @param userId - id of user completing the action
	 * @param listener - a listener to track progress of the operation
	 * @throws ServiceException
	 */
	@MethodTimer
	public void removeDirectory(DirectoryResource dirToDelete, String userId, FileServiceTaskListener listener) throws ServiceException {
		
		//final Store store = getStore(dirToDelete, userId);
		
		final QueuedTaskManager taskManager = getGeneralTaskManagerForStore(getStore(dirToDelete, userId));
		
		Long dirNodeId = dirToDelete.getNodeId();
		
		// TODO - check permissions to make sure user has permission to delete. Need to check permissions on each child directory...
		
		DirectoryResource parentDir = getParentDirectory(dirNodeId, userId);
		
		RemoveDirectoryTask task = new RemoveDirectoryTask(
				dirToDelete, parentDir, userId, secureTreeService, fileSystemRepository, resChangeService, errorHandler);
		task.setName("Remove directory [dirNodeId=" + dirNodeId + "]");
		
		if(listener != null) {
			task.registerProgressListener(listener);
		}		
		
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
	 * @param userId - id of user completing the action
	 * @param listener - a listener to track progress of the operation
	 * @throws ServiceException
	 */
	@MethodTimer
	public void copyDirectory(
			Long copyDirNodeId, 
			Long destDirNodeId, 
			boolean replaceExisting, 
			String userId,
			FileServiceTaskListener listener) throws ServiceException {
		
		final DirectoryResource fromDir = getDirectory(copyDirNodeId, userId);
		final DirectoryResource toDir = getDirectory(destDirNodeId, userId);	
		
		CopyDirectoryTask task = new CopyDirectoryTask(
				fromDir, toDir, replaceExisting, userId, secureTreeService, this, errorHandler);
		task.setTaskId(TaskIdGenerator.getNextTaskId());
		if(listener != null) {
			task.registerProgressListener(listener);
		}
		task.setName("Copy directory [copyDirNodeId=" + copyDirNodeId + ", destDirNodeId=" + destDirNodeId + 
				", replaceExisting=" + replaceExisting + "]");
		
		// CopyDirectoryTask contains child tasks which block. Since our task manager is a queue and
		// only runs one task a a time, the child tasks will never run because the parent task is waiting
		// for the child tasks to finish. Solution is to execute the parent task immediately in
		Executors.newSingleThreadExecutor().execute(task);		
		
	}
	
	/**
	 * Makes a copy of 'dirToCopy' under directory 'toDir'. If there already exists a directory under 'toDir' with the
	 * same name as directory 'dirToCopy' then the existing directory is returned. If not then a new directory is created.
	 * 
	 * @param dirToCopy
	 * @param toDir
	 * @param userId - id of user completing the action
	 * @param listener - a listener to track progress of the operation
	 * @return
	 * @throws Exception
	 */
	public DirectoryResource createCopyOfDirectory(
			DirectoryResource dirToCopy, 
			DirectoryResource toDir, 
			String userId,
			FileServiceTaskListener listener) throws ServiceException {
		
		// see if there already exists a child directory with the same name
		DirectoryResource existingChildDir = null;
		try {
			existingChildDir = getChildDirectoryResource(toDir.getNodeId(), dirToCopy.getPathName(), userId);
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
			DirectoryResource newCopy = addDirectory(toDir, dirToCopy.getPathName(), dirToCopy.getDesc(), 
					dirToCopy.getReadGroup1(), dirToCopy.getWriteGroup1(), dirToCopy.getExecuteGroup1(), userId, listener);
			
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
	 * @param listener - a listener to track progress of the operation
	 * @throws ServiceException
	 */
	@MethodTimer
	public void moveDirectory(Long moveDirId, Long destDirId, boolean replaceExisting, String userId, FileServiceTaskListener listener) throws ServiceException {
		
		// we can preserve file IDs but hard to preserve directory IDs...
		// if you eventually manage to preserve directory IDs, then you might have to worry about
		// updating the eas_store.node_id (root node) value.
		
		DirectoryResource dirToMove = getDirectory(moveDirId, userId);
		DirectoryResource destDir = getDirectory(destDirId, userId);
			
		MoveDirectoryTask task = new MoveDirectoryTask(
				dirToMove, destDir, replaceExisting, userId, secureTreeService, fileSystemRepository, this, errorHandler);
		task.setTaskId(TaskIdGenerator.getNextTaskId());
		if(listener != null) {
			task.registerProgressListener(listener);
		}
		task.setName("Movie directory [moveDirId=" + moveDirId + ", destDirId=" + destDirId + ", replaceExisting=" + replaceExisting + "]");
		
		// MoveDirectoryTask contains child tasks which block. Since our task manager is a queue and
		// only runs one task a a time, the child tasks will never run because the parent task is waiting
		// for the child tasks to finish. Solution is to execute the parent task immediately in
		Executors.newSingleThreadExecutor().execute(task);		
		
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
			String userId,
			FileServiceTaskListener listener) throws ServiceException {
		
		List<Path> filePaths = null;
		try {
			filePaths = FileUtil.listFilesToDepth(tempDir, 1);
		} catch (IOException e) {
			throw new ServiceException("Error listing files in temporary directory " + tempDir.toString());
		}
		
		filePaths.stream().forEach(
			(pathToFile) ->{
				try {
					addFile(dirNodeId, userId, pathToFile, replaceExisting, listener);
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
			String userId,
			FileServiceTaskListener listener) throws ServiceException{
	
		List<Path> filePaths = null;
		try {
			filePaths = FileUtil.listFilesToDepth(tempDir, 1);
		} catch (IOException e) {
			throw new ServiceException("Error listing files in temporary directory " + tempDir.toString());
		}
		
		filePaths.stream().forEach(
			(pathToFile) ->{
				try {
					addFile(storeName, dirRelPath, userId, pathToFile, replaceExisting, listener);
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
