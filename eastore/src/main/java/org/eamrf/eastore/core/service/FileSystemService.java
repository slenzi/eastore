package org.eamrf.eastore.core.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.properties.ManagedProperties;
import org.eamrf.repository.oracle.ecoguser.eastore.FileSystemRepository;
import org.eamrf.repository.oracle.ecoguser.eastore.model.DirectoryResource;
import org.eamrf.repository.oracle.ecoguser.eastore.model.FileMetaResource;
import org.eamrf.repository.oracle.ecoguser.eastore.model.PathResource;
import org.eamrf.repository.oracle.ecoguser.eastore.model.Store;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Main service class for interacting with our file system.
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
    
	public FileSystemService() {
		
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
	 * Create a new store
	 * 
	 * @param storeName
	 * @param storeDesc
	 * @param storePath
	 * @param rootDirName
	 * @param maxFileSizeDb
	 * @return
	 * @throws ServiceException
	 */
	public Store addStore(String storeName, String storeDesc, Path storePath, String rootDirName, Long maxFileSizeDb) throws ServiceException {
		
		Store store = null;
		try {
			store = fileSystemRepository.addStore(storeName, storeDesc, storePath, rootDirName, maxFileSizeDb);
		} catch (Exception e) {
			throw new ServiceException("Error creating new store '" + storeName + "' at " + storePath.toString(), e);
		}
		return store;
		
	}
	
	/**
	 * Adds a new file, but does not add the binary data to eas_file_binary_resource
	 * 
	 * @param dirNodeId - directory where file will be added
	 * @param filePath - current temp path where file resides on disk
	 * @param replaceExisting - pass true to overwrite any file with the same name that's already in the directory tree. If you
	 * pass false, and there exists a file with the same name (case insensitive) then an ServiceException will be thrown. 
	 * @return
	 */
	public FileMetaResource addFile(Long dirNodeId, Path filePath, boolean replaceExisting) throws ServiceException {
		
		FileMetaResource fileMetaResource = null;
		try {
			fileMetaResource = fileSystemRepository.addFile(dirNodeId, filePath, replaceExisting);
		} catch (Exception e) {
			throw new ServiceException("Error adding new file => " + filePath.toString() + 
					", to directory => " + dirNodeId + ", replaceExisting => " + replaceExisting, e);
		}
		return fileMetaResource;
	}
	
	/**
	 * 
	 * 
	 * @param fileMetaResource
	 * @throws ServiceException
	 */
	public void updateFileBinary(FileMetaResource fileMetaResource) throws ServiceException {
		
		// TODO - add code to update/insert the row into eas_file_binary_resource
		
	}
	
	/**
	 * Add new directory
	 * 
	 * @param dirNodeId
	 * @param name
	 * @return
	 * @throws ServiceException
	 */
	public DirectoryResource addDirectory(Long dirNodeId, String name) throws ServiceException {
		
		DirectoryResource dirResource = null;
		try {
			dirResource = fileSystemRepository.addDirectory(dirNodeId, name);
		} catch (Exception e) {
			throw new ServiceException("Error adding new subdirectory to directory " + dirNodeId, e);
		}
		return dirResource;
		
	}
	
	/**
	 * Create sample store for testing, with some sub-directories.
	 * 
	 * @throws ServiceException
	 */
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
			DirectoryResource dirThings = addDirectory(dirOther.getNodeId(), "things");
			DirectoryResource dirFoo = addDirectory(dirOther.getNodeId(), "foo");
				DirectoryResource dirCats = addDirectory(dirFoo.getNodeId(), "cats");
				DirectoryResource dirDogs = addDirectory(dirFoo.getNodeId(), "dogs");
					DirectoryResource dirBig = addDirectory(dirDogs.getNodeId(), "big");
					DirectoryResource dirSmall = addDirectory(dirDogs.getNodeId(), "small");
						DirectoryResource dirPics = addDirectory(dirSmall.getNodeId(), "pics");
		
		return store;
	}

}
