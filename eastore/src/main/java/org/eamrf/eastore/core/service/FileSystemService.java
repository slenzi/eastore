package org.eamrf.eastore.core.service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import org.eamrf.concurrent.task.AbstractQueuedTask;
import org.eamrf.concurrent.task.QueuedTaskManager;
import org.eamrf.concurrent.task.TaskManagerProvider;
import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.FileUtil;
import org.eamrf.eastore.core.aop.profiler.MethodTimer;
import org.eamrf.eastore.core.concurrent.StoreTaskManagerMap;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.properties.ManagedProperties;
import org.eamrf.eastore.core.tree.Tree;
import org.eamrf.eastore.core.tree.TreeNode;
import org.eamrf.eastore.core.tree.TreeNodeVisitException;
import org.eamrf.eastore.core.tree.Trees;
import org.eamrf.eastore.core.tree.Trees.WalkOption;
import org.eamrf.repository.oracle.ecoguser.eastore.FileSystemRepository;
import org.eamrf.repository.oracle.ecoguser.eastore.model.DirectoryResource;
import org.eamrf.repository.oracle.ecoguser.eastore.model.FileMetaResource;
import org.eamrf.repository.oracle.ecoguser.eastore.model.PathResource;
import org.eamrf.repository.oracle.ecoguser.eastore.model.ResourceType;
import org.eamrf.repository.oracle.ecoguser.eastore.model.Store;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Main service class for interacting with our file system.
 * 
 * All operations which perform updates to the file system are added to a queue to be run one at a time.
 * 
 * @author slenzi
 */
@Service
public class FileSystemService {

    @InjectLogger
    private Logger logger;
    
    @Autowired
    private ManagedProperties appProps;    
	
    @Autowired
    private FileSystemRepository fileSystemRepository;
    
    @Autowired
    private PathResourceTreeService treeService;
    
    @Autowired
    private TaskManagerProvider taskManagerProvider;
    
    // maps all stores to their task manager
    private Map<Store,StoreTaskManagerMap> storeTaskManagerMap = new HashMap<Store,StoreTaskManagerMap>();
    
	public FileSystemService() {
		
	}
	
	/**
	 * Create a QueuedTaskManager for each store. Any SQL update operations are queued per store.
	 */
	@PostConstruct
	public void init(){
		
		List<Store> stores = null;
		
		try {
			stores = getStores();
		} catch (ServiceException e) {
			logger.error("Failed to fetch list of stores. Cannot initialize task managers.");
		}
		
		for(Store store : stores){
			
			logger.info("Creating queued task manager for store [storeId=" + store.getId() + ", storeName=" + store.getName() + "]");
			
			QueuedTaskManager manager = taskManagerProvider.createQueuedTaskManager();
			manager.setManagerName("Task Manager [storeId=" + store.getId() + ", storeName=" + store.getName() + "]");
			ExecutorService executor = Executors.newSingleThreadExecutor();
			manager.startTaskManager(executor);
			StoreTaskManagerMap mapEntry = new StoreTaskManagerMap(store, manager);
			storeTaskManagerMap.put(store, mapEntry);
			
		}
		
	}	
	
	/**
	 * Get a list of PathResource starting at the specified dirNodeId. With this information
	 * you can build a tree. This will not contain the binary data for files.
	 * 
	 * This is functionally equivalent to ClosureService.getChildMappings(Long nodeId)
	 * 
	 * @param dirNodeId
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public List<PathResource> getPathResourceTree(Long dirNodeId) throws ServiceException {
		
		List<PathResource> resources = null;
		try {
			resources = fileSystemRepository.getPathResourceTree(dirNodeId);
		} catch (Exception e) {
			throw new ServiceException("Error getting PathResource tree for directory node node " + 
					dirNodeId + ". " + e.getMessage(), e);
		}
		return resources;
		
	}
	
	/**
	 * Get a list of PathResource starting at the specified dirNodeId, but only up to a specified depth.
	 * With this information you can build a tree. This will not contain the binary data for files.
	 * 
	 * This is functionally equivalent to ClosureService.getChildMappings(Long nodeId, int depth)
	 * 
	 * @param dirNodeId
	 * @param depth
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public List<PathResource> getPathResourceTree(Long dirNodeId, int depth) throws ServiceException {
		
		List<PathResource> resources = null;
		try {
			resources = fileSystemRepository.getPathResourceTree(dirNodeId, depth);
		} catch (Exception e) {
			throw new ServiceException("Error getting PathResource tree for directory node node " + 
					dirNodeId + ". " + e.getMessage(), e);
		}
		return resources;		
		
	}
	
	/**
	 * Fetch bottom-up (leaf node to root node), PathResource list. This can
	 * be used to build a tree (or more of a single path) from root to leaf.
	 * 
	 * This is functionally equivalent to ClosureService.getParentMappings(Long nodeId)
	 * 
	 * @param dirNodeId
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public List<PathResource> getParentPathResourceTree(Long dirNodeId) throws ServiceException {
		
		List<PathResource> resources = null;
		try {
			resources = fileSystemRepository.getParentPathResourceTree(dirNodeId);
		} catch (Exception e) {
			throw new ServiceException("Error getting PathResource tree for directory node node " + 
					dirNodeId + ". " + e.getMessage(), e);
		}
		return resources;
		
	}
	
	/**
	 * Fetch bottom-up (leaf node to root node) PathResource list, up to a specified levels up. This can
	 * be used to build a tree (or more of a single path) from root to leaf.
	 * 
	 * This is functionally equivalent to ClosureService.getParentMappings(Long nodeId, int depth)
	 * 
	 * @param dirNodeId
	 * @param levels
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public List<PathResource> getParentPathResourceTree(Long dirNodeId, int levels) throws ServiceException {
		
		List<PathResource> resources = null;
		try {
			resources = fileSystemRepository.getParentPathResourceTree(dirNodeId, levels);
		} catch (Exception e) {
			throw new ServiceException("Error getting PathResource tree for directory node node " + 
					dirNodeId + ". " + e.getMessage(), e);
		}
		return resources;		
		
	}
	
	/**
	 * Create a new store, and create it's queued task manager
	 * 
	 * @param storeName
	 * @param storeDesc
	 * @param storePath
	 * @param rootDirName
	 * @param maxFileSizeDb
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public Store addStore(String storeName, String storeDesc, Path storePath, String rootDirName, Long maxFileSizeDb) throws ServiceException {
		
		Store store = null;
		try {
			store = fileSystemRepository.addStore(storeName, storeDesc, storePath, rootDirName, maxFileSizeDb);
		} catch (Exception e) {
			throw new ServiceException("Error creating new store '" + storeName + "' at " + storePath.toString(), e);
		}
		
		QueuedTaskManager manager = taskManagerProvider.createQueuedTaskManager();
		manager.setManagerName("Task Manager [storeId=" + store.getId() + ", storeName=" + store.getName() + "]");
		ExecutorService executor = Executors.newSingleThreadExecutor();
		manager.startTaskManager(executor);
		StoreTaskManagerMap mapEntry = new StoreTaskManagerMap(store, manager);
		storeTaskManagerMap.put(store, mapEntry);		
		
		return store;
		
	}
	
	/**
	 * Fetch the queued task manager for the store;
	 * 
	 * @param store
	 * @return
	 */
	private QueuedTaskManager getTaskManagerForStore(Store store){
		
		StoreTaskManagerMap map = storeTaskManagerMap.get(store);
		return map.getTaskManager();
		
	}
	
	/**
	 * Adds a new file, but does not add the binary data to eas_binary_resource
	 * 
	 * @param dirNodeId - directory where file will be added
	 * @param filePath - path to file on local file system
	 * @param replaceExisting - pass true to overwrite any file with the same name that's already in the directory tree. If you
	 * pass false, and there exists a file with the same name (case insensitive) then an ServiceException will be thrown. 
	 * @return
	 */
	@MethodTimer
	public FileMetaResource addFileWithoutBinary(Long dirNodeId, Path filePath, boolean replaceExisting) throws ServiceException {
		
		final DirectoryResource dirRes = getDirectoryResource(dirNodeId);

		return addFileWithoutBinary(dirRes, filePath, replaceExisting);
		
	}
	
	/**
	 * Adds a new file, but does not add the binary data to eas_binary_resource
	 * 
	 * @param dirRes - directory where file will be added
	 * @param filePath - path to file on local file system
	 * @param replaceExisting - pass true to overwrite any file with the same name that's already in the directory tree. If you
	 * pass false, and there exists a file with the same name (case insensitive) then an ServiceException will be thrown.
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public FileMetaResource addFileWithoutBinary(DirectoryResource dirRes, Path filePath, boolean replaceExisting) throws ServiceException {
		
		final Store store = getStore(dirRes);
		final QueuedTaskManager taskManager = getTaskManagerForStore(store);
		
		class Task extends AbstractQueuedTask<FileMetaResource> {

			@Override
			public FileMetaResource doWork() throws ServiceException {

				FileMetaResource fileMetaResource = null;
				try {
					fileMetaResource = fileSystemRepository.addFileWithoutBinary(dirRes, filePath, replaceExisting);
				} catch (Exception e) {
					throw new ServiceException("Error adding new file => " + filePath.toString() + 
							", to directory => " + dirRes.getNodeId() + ", replaceExisting => " + replaceExisting, e);
				}
				return fileMetaResource;				
				
			}

			@Override
			public Logger getLogger() {
				return logger;
			}
			
		}
		
		Task task = new Task();
		task.setName("Add File Without Binary [dirNodeId=" + dirRes.getNodeId() + ", filePath=" + filePath + 
				", replaceExisting=" + replaceExisting + "]");
		taskManager.addTask(task);
		
		FileMetaResource fileMetaResource = task.get(); // block until finished
		
		return fileMetaResource;		
		
	}	
	
	/**
	 * Refreshes the binary data in the database (data from the file on disk is copied to eas_binary_resource)
	 * 
	 * @param fileMetaResource
	 * @throws ServiceException
	 */
	@MethodTimer
	public void refreshBinaryDataInDatabase(FileMetaResource fileMetaResource) throws ServiceException {
		
		final Store store = getStore(fileMetaResource);
		final QueuedTaskManager taskManager = getTaskManagerForStore(store);		
		
		class Task extends AbstractQueuedTask<Void> {

			@Override
			public Void doWork() throws ServiceException {

				try {
					fileSystemRepository.refreshBinaryDataInDatabase(fileMetaResource);
				} catch (Exception e) {
					throw new ServiceException("Error refreshing (or adding) binary data in database (eas_binary_resource) "
							+ "for file resource node => " + fileMetaResource.getNodeId(), e);
				}
				return null;				
				
			}

			@Override
			public Logger getLogger() {
				return logger;
			}
			
		}
		
		Task task = new Task();
		task.setName("Refresh binary data in DB [" + fileMetaResource.toString() + "]");
		taskManager.addTask(task);
		
		// TODO - do we actually want to wait for this to finish? It definitely needs to be queued!
		
		//FileMetaResource updatedResource = task.get(); // block until finished

	}
	
	/**
	 * Remove the file, from database and disk. No undo.
	 * 
	 * @param fileNodeId - id of the FileMetaResource
	 * @throws ServiceException
	 */
	@MethodTimer
	public void removeFile(Long fileNodeId) throws ServiceException {
		
		final FileMetaResource fileMetaResource = getFileMetaResource(fileNodeId);
		final Store store = getStore(fileMetaResource);
		final QueuedTaskManager taskManager = getTaskManagerForStore(store);		
		
		class Task extends AbstractQueuedTask<Void> {

			@Override
			public Void doWork() throws ServiceException {
	
				try {
					fileSystemRepository.removeFile(store, fileMetaResource);
				} catch (Exception e) {
					throw new ServiceException("Error removing file with node id => " + fileNodeId + ". " + e.getMessage(), e);
				}
				
				return null;
				
			}

			@Override
			public Logger getLogger() {
				return logger;
			}
			
		}
		
		Task task = new Task();
		task.setName("Remove file [fileNodeId=" + fileNodeId + "]");
		taskManager.addTask(task);
		
		task.waitComplete(); // block until finished
		
	}
	
	/**
	 * Remove a directory. Walks the tree in POST_ORDER_TRAVERSAL, from leafs to root node.
	 * 
	 * @param dirNodeId
	 * @throws ServiceException
	 */
	@MethodTimer
	public void removeDirectory(Long dirNodeId) throws ServiceException {
		
		// this function call makes sure the dirNodeId points to an actual directory
		final DirectoryResource dirResource = getDirectoryResource(dirNodeId);
		
		// make sure user is not trying to delete a root directory for a store
		if(dirResource.getParentNodeId().equals(0L)){
			throw new ServiceException("Node id => " + dirNodeId + " points to a root directory for a store. "
					+ "You cannot use this method to remove a root directory.");
		}
		
		final Store store = getStore(dirResource);
		final QueuedTaskManager taskManager = getTaskManagerForStore(store);
		
		class Task extends AbstractQueuedTask<Void> {

			@Override
			public Void doWork() throws ServiceException {
				
				getLogger().info("Deleting Tree:");
				
				// build a tree that we can walk
				final Tree<PathResource> tree = treeService.buildPathResourceTree(dirResource.getNodeId());				
				
				treeService.logTree(tree);
				
				try {
					
					// walk tree, bottom-up, from leafs to root node.
					Trees.walkTree(tree,
						(treeNode) -> {
							
							try {
								if(treeNode.getData().getResourceType() == ResourceType.FILE){
									
									fileSystemRepository.removeFile(store, (FileMetaResource)treeNode.getData());
									
								}else if(treeNode.getData().getResourceType() == ResourceType.DIRECTORY){
									
									// we walk the tree bottom up, so by the time we remove a directory it will be empty
									fileSystemRepository.removeDirectory(store, (DirectoryResource)treeNode.getData());
									
								}
							}catch(Exception e){
								
								PathResource presource = treeNode.getData();
								
								throw new TreeNodeVisitException("Error removing path resource with node id => " + 
										presource.getNodeId() + ", of resource type => " + 
										presource.getResourceType().getTypeString(),e);
								
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
	 * fetch a store
	 * 
	 * @param storeId
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public Store getStore(Long storeId) throws ServiceException {
		
		Store store = null;
		try {
			store = fileSystemRepository.getStoreById(storeId);
		} catch (Exception e) {
			throw new ServiceException("Failed to get store for store id => " + storeId);
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
		return getStore(r.getStoreId());
		
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
	 * fetch a directory
	 * 
	 * @param dirNodeId
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public FileMetaResource getFileMetaResource(Long fileNodeId) throws ServiceException {
		
		FileMetaResource resource = null;
		try {
			resource = fileSystemRepository.getFileMetaResource(fileNodeId);
		} catch (Exception e) {
			throw new ServiceException("Failed to get file meta resource for node id => " + fileNodeId);
		}
		return resource;
	}	
	
	/**
	 * fetch a directory
	 * 
	 * @param dirNodeId
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public DirectoryResource getDirectoryResource(Long dirNodeId) throws ServiceException {
		
		DirectoryResource resource = null;
		try {
			resource = fileSystemRepository.getDirectory(dirNodeId);
		} catch (Exception e) {
			throw new ServiceException("Failed to get directory for node id => " + dirNodeId);
		}
		return resource;
	}
	
	/**
	 * Add new directory
	 * 
	 * @param dirNodeId - id of parent directory
	 * @param name - Name of new directory which will be created under the parent directory
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public DirectoryResource addDirectory(Long dirNodeId, String name) throws ServiceException {
		
		final DirectoryResource dirRes = getDirectoryResource(dirNodeId);
		
		return addDirectory(dirRes, name);
		
	}
	
	/**
	 * Add new directory.
	 * 
	 * @param parentDir - The parent directory
	 * @param name - Name of new directory which will be created under the parent directory
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public DirectoryResource addDirectory(DirectoryResource parentDir, String name) throws ServiceException {
		
		final Store store = getStore(parentDir);
		final QueuedTaskManager taskManager = getTaskManagerForStore(store);		
		
		class Task extends AbstractQueuedTask<DirectoryResource> {

			@Override
			public DirectoryResource doWork() throws ServiceException {

				DirectoryResource dirResource = null;
				try {
					// TODO, pass parentDir rather than just it's node ID.
					dirResource = fileSystemRepository.addDirectory(parentDir.getNodeId(), name);
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
	 * Copy file to another directory (could be in another store)
	 * 
	 * @param fileNodeId - the file to copy
	 * @param dirNodeId - the destination directory
	 * @param replaceExisting - pass true to replace any existing file in the destination directory with
	 * same name. If you pass false, and a file already exists, then an exception will be thrown.
	 * @throws ServiceException
	 */
	@MethodTimer
	public void copyFile(Long fileNodeId, Long dirNodeId, boolean replaceExisting) throws ServiceException {
		
		FileMetaResource sourceFile = getFileMetaResource(fileNodeId);
		DirectoryResource destitationDir = getDirectoryResource(dirNodeId);
		
		copyFile(sourceFile, destitationDir, replaceExisting);
		
	}
	
	private void copyFile(FileMetaResource fileToCopy, DirectoryResource toDir, boolean replaceExisting) throws ServiceException {
		
		Store soureStore = getStore(fileToCopy);
		Path sourceFilePath = Paths.get(soureStore.getPath() + fileToCopy.getRelativePath());
		
		// can't copy a file to the directory it's already in
		if(fileToCopy.getParentNodeId().equals(toDir.getNodeId())){
			throw new ServiceException("You cannot copy a file to the directory that it's already in. "
					+ "fileNodeId => " + fileToCopy.getNodeId() + ", dirNodeId => " + toDir.getNodeId());
		}
		
		FileMetaResource fileMeta = addFileWithoutBinary(toDir, sourceFilePath, replaceExisting);
		
		refreshBinaryDataInDatabase(fileMeta); // TODO - currently does not block
		
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
	 * @throws ServiceException
	 */
	@MethodTimer
	public void copyDirectory(Long copyDirNodeId, Long destDirNodeId, boolean replaceExisting) throws ServiceException {
		
		// TODO - wrap in queued task
		
		if(copyDirNodeId.equals(destDirNodeId)){
			throw new ServiceException("Source directory and destination directory are the same. "
					+ "You cannot copy a directory to itself. copyDirNodeId=" + copyDirNodeId + 
					", destDirNodeId=" + destDirNodeId + ", replaceExisting=" + replaceExisting);
		}
		
		final DirectoryResource fromDir = getDirectoryResource(copyDirNodeId);
		final DirectoryResource toDir = getDirectoryResource(destDirNodeId);
		final Store fromStore = getStore(fromDir);
		final Store toStore = getStore(fromDir);

		final Tree<PathResource> fromTree = treeService.buildPathResourceTree(copyDirNodeId);
		
		copyDirectoryTraversal(fromStore, toStore, fromTree.getRootNode(), toDir, replaceExisting);
		
	}
	
	/**
	 * Recursively walk the tree to copy all child path resources
	 * 
	 * @param fromStore
	 * @param toStore
	 * @param pathResourceNode
	 * @param toDir
	 * @param replaceExisting
	 * @throws ServiceException
	 */
	private void copyDirectoryTraversal(
			Store fromStore, Store toStore, TreeNode<PathResource> pathResourceNode, 
			DirectoryResource toDir, boolean replaceExisting) throws ServiceException {
		
		PathResource resourceToCopy = pathResourceNode.getData();
		
		if(resourceToCopy.getResourceType() == ResourceType.DIRECTORY){
			
			// copy the directory
			
			// TODO - we perform a case insensitive match. If the directory names differ in case, do we want
			// to keep the directory that already exists (which we do now) or rename it to match exactly of
			// the one we are copying?
			DirectoryResource newToDir = _createCopyOfDirectory((DirectoryResource) resourceToCopy, toDir);
			
			// copy over children of the directory (files and subdirectories)
			if(pathResourceNode.hasChildren()){
				for(TreeNode<PathResource> child : pathResourceNode.getChildren()){
					
					copyDirectoryTraversal(fromStore, toStore, child, newToDir, replaceExisting);
					
				}
			}
			
		}else if(resourceToCopy.getResourceType() == ResourceType.FILE){
			
			copyFile( (FileMetaResource)resourceToCopy, toDir, replaceExisting);
			
		}
		
	}
	
	/**
	 * Makes a copy of 'dirToCopy' under directory 'toDir'. If there already exists a directory under 'toDir' with the
	 * same name as directory 'dirToCopy' then the existing directory is returned. If not then a new directory is created.
	 * 
	 * @param dirToCopy
	 * @param toDir
	 * @return
	 * @throws Exception
	 */
	private DirectoryResource _createCopyOfDirectory(DirectoryResource dirToCopy, DirectoryResource toDir) throws ServiceException {
		
		// see if there already exists a child directory with the same name
		PathResource existingChildDir = null;
		try {
			existingChildDir = fileSystemRepository.getChildPathResource(
					toDir.getNodeId(), dirToCopy.getPathName(), ResourceType.DIRECTORY);
		} catch (Exception e) {
			throw new ServiceException("Failed to check for existing child directory with name '" + 
					dirToCopy.getPathName() + "' under directory node " + toDir.getNodeId(), e);
		}
		
		if(existingChildDir != null){
			// directory with the same name already exists in the 'toDir'
			// TODO - directory names might differ by case (uppercase/lowercase etc). Do we want to change
			// the name of the existing directory to exactly match the one being copied?
			return (DirectoryResource) existingChildDir;
		}else{
			// create new directory
			return addDirectory(toDir, dirToCopy.getPathName());
		}
		
	}

	/**
	 * Move a file, preserving same node id.
	 * 
	 * @param fileNodeId - id of the file to move
	 * @param dirNodeId - id of the directory where the file will be moved to
	 * @param replaceExisting
	 * @throws ServiceException
	 */
	@MethodTimer
	public void moveFile(Long fileNodeId, Long dirNodeId, boolean replaceExisting) throws ServiceException {
		
		final FileMetaResource fileToMove = getFileMetaResource(fileNodeId);
		final DirectoryResource destDir = getDirectoryResource(dirNodeId);
				
		moveFile(fileToMove, destDir, replaceExisting);
		
	}
	
	/**
	 * Move a file, preserving same node id.
	 * 
	 * @param fileNodeId - the file to move
	 * @param dirNodeId - the directory where the file will be moved to
	 * @param replaceExisting
	 * @throws ServiceException
	 */
	private void moveFile(FileMetaResource fileToMove, DirectoryResource destDir, boolean replaceExisting) throws ServiceException {
		
		final Store store = getStore(destDir);
		final QueuedTaskManager taskManager = getTaskManagerForStore(store);
		
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
		task.setName("Move file [fileNodeId=" + fileToMove.getNodeId() + ", dirNodeId=" + destDir.getNodeId() + 
				", replaceExisting=" + replaceExisting + "]");
		taskManager.addTask(task);
		
		task.waitComplete(); // block until finished	
		
	}
	
	/**
	 * Move a directory (does not preserve node IDs for directories, but does for files.)
	 * 
	 * @param moveDirId - the directory to move
	 * @param destDirId - the directory where 'moveDirId' will be moved to (under). 
	 * @param replaceExisting
	 * @throws ServiceException
	 */
	@MethodTimer
	public void moveDirectory(Long moveDirId, Long destDirId, boolean replaceExisting) throws ServiceException {
		
		// TODO - wrap in queued task
		
		// we can preserve file IDs but hard to preserve directory IDs...
		// if you eventually manage to preserv directory IDs, then you might have to worry about
		// updating the eas_store.node_id (root node) value.
		
		DirectoryResource dirToMove = getDirectoryResource(moveDirId);
		
		// make sure the user is not trying to move a root directory for a store
		if(dirToMove.getParentNodeId().equals(0L)){
			throw new ServiceException("You cannot move a root directory of a store. All stores require a root directory. "
					+ "moveDirId = " + moveDirId + ", destDirId = " + destDirId);
		}
		
		// make sure destDirId is not a child node under moveDirId. Cannot move a directory
		// to under itself.
		boolean isChild = false;
		try {
			isChild = fileSystemRepository.isChild(moveDirId, destDirId);
		} catch (Exception e) {
			throw new ServiceException("Error checking if directory " + destDirId + 
					" is a child directory (at any depth) of directory " + moveDirId);
		}
		if(isChild){
			throw new ServiceException("Cannot move directory " + moveDirId + " to under directory " + 
					destDirId + " because directory " + destDirId + " is a child of directory " + moveDirId + ".");
		}

		final Tree<PathResource> fromTree = treeService.buildPathResourceTree(dirToMove.getNodeId());
		
		DirectoryResource toDir = getDirectoryResource(destDirId);
		final Store fromStore = getStore(dirToMove);
		final Store toStore = getStore(toDir);
		
		// walk the tree top-down and copy over directories one at a time, then use
		// existing moveFile method.
		moveDirectoryTraversal(fromStore, toStore, fromTree.getRootNode(), toDir, replaceExisting);
		
		// remove from dir and all child directories
		removeDirectory(dirToMove.getNodeId());
		
	}
	
	private void moveDirectoryTraversal(
			Store fromStore, Store toStore, TreeNode<PathResource> pathResourceNode, 
			DirectoryResource toDir, boolean replaceExisting) throws ServiceException {
		
		PathResource resourceToMove = pathResourceNode.getData();
		
		if(resourceToMove.getResourceType() == ResourceType.DIRECTORY){
			
			// move the directory
			
			// TODO - we perform a case insensitive match. If the directory names differ in case, do we want
			// to keep the directory that already exists (which we do now) or rename it to match exactly of
			// the one we are copying?
			DirectoryResource newToDir = _createCopyOfDirectory((DirectoryResource) resourceToMove, toDir);
			
			// move children of the directory (files and sub-directories)
			if(pathResourceNode.hasChildren()){
				for(TreeNode<PathResource> child : pathResourceNode.getChildren()){
					
					moveDirectoryTraversal(fromStore, toStore, child, newToDir, replaceExisting);
					
				}
			}
			
		}else if(resourceToMove.getResourceType() == ResourceType.FILE){
			
			moveFile( (FileMetaResource)resourceToMove, toDir, replaceExisting);
			
		}
		
	}

	/**
	 * All files in 'tempDir' will be added to directory 'dirNodeId'
	 * 
	 * @param dirNodeId
	 * @param tempDir
	 * @param replaceExisting
	 * @throws ServiceException
	 */
	@MethodTimer
	public void processToDirectory(Long dirNodeId, Path tempDir, boolean replaceExisting) throws ServiceException {
		
		// TODO - Eventually we'll want the ID of the user who uploaded. The JAX-RS service will have to use (possibly)
		// the AuthWorld session key to identify users logged into the portal. That would give us access to the CTEP ID.
		// We'll also need to add a field to eas_path_resource to store the user's ID.
		
		List<Path> filePaths = null;
		try {
			filePaths = FileUtil.listFilesToDepth(tempDir, 1);
		} catch (IOException e) {
			throw new ServiceException("Error listing files in temporary directory " + tempDir.toString());
		}
		
		filePaths.stream().forEach(
			(pathToFile) ->{
				
				// add file meta
				FileMetaResource fileMetaResource = null;
				try {
					fileMetaResource = addFileWithoutBinary(dirNodeId, pathToFile, replaceExisting);
				} catch (ServiceException e) {
					throw new RuntimeException("Error adding file '" + pathToFile.toString() + "' to directory with id '" + dirNodeId + "'.", e);
				}
				
				// go back and add binary to db
				try {
					refreshBinaryDataInDatabase(fileMetaResource);
				} catch (ServiceException e) {
					throw new RuntimeException("Error adding binary data to db for FileMetaResource with node id => " + fileMetaResource.getNodeId(), e);
				}
					
			});
		
	}	
	
	/**
	 * Create sample store for testing, with some sub-directories.
	 * 
	 * @throws ServiceException
	 */
	@MethodTimer
	public Store createTestStore() throws ServiceException {
		
		String testStoreName = appProps.getProperty("store.test.name");
		String testStoreDesc = appProps.getProperty("store.test.desc");
		String testStorePath = appProps.getProperty("store.test.path").replace("\\", "/");
		String testStoreMaxFileSizeBytes = appProps.getProperty("store.test.max.file.size.bytes");
		String testStoreRootDirName = appProps.getProperty("store.test.root.dir.name");
		
		Long maxBytes = 0L;
		try {
			maxBytes = Long.valueOf(testStoreMaxFileSizeBytes);
		} catch (NumberFormatException e) {
			throw new ServiceException("Error parsing store.test.max.file.size.bytes to long. " + e.getMessage(), e);
		}
		
		Store store = addStore(testStoreName, testStoreDesc, Paths.get(testStorePath), testStoreRootDirName, maxBytes);
		
		DirectoryResource dirMore  = addDirectory(store.getNodeId(), "more");
		DirectoryResource dirOther = addDirectory(store.getNodeId(), "other");
			DirectoryResource dirThings = addDirectory(dirOther, "things");
			DirectoryResource dirFoo = addDirectory(dirOther, "foo");
				DirectoryResource dirCats = addDirectory(dirFoo, "cats");
				DirectoryResource dirDogs = addDirectory(dirFoo, "dogs");
					DirectoryResource dirBig = addDirectory(dirDogs, "big");
					DirectoryResource dirSmall = addDirectory(dirDogs, "small");
						DirectoryResource dirPics = addDirectory(dirSmall, "pics");		
		
		return store;
	}

}
