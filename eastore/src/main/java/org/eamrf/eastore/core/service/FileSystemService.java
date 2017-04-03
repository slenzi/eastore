package org.eamrf.eastore.core.service;

import java.util.List;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.repository.oracle.ecoguser.eastore.FileSystemRepository;
import org.eamrf.repository.oracle.ecoguser.eastore.model.DirectoryResource;
import org.eamrf.repository.oracle.ecoguser.eastore.model.PathResource;
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
	 * Add new file
	 * 
	 * @param dirNodeId
	 * @param name
	 * @return
	 * @throws ServiceException
	 */
	public Long addFile(Long dirNodeId, String name) throws ServiceException {
		
		Long fileNodeId = -1L;
		try {
			fileNodeId = fileSystemRepository.addFile(dirNodeId, name);
		} catch (Exception e) {
			throw new ServiceException("Error adding file to directory " + dirNodeId, e);
		}
		return fileNodeId;
		
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

}
