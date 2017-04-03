package org.eamrf.eastore.core.service;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.repository.oracle.ecoguser.eastore.FileSystemRepository;
import org.eamrf.repository.oracle.ecoguser.eastore.model.DirectoryResource;
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
			fileNodeId = fileSystemRepository.addFileNode(dirNodeId, name);
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
			dirResource = fileSystemRepository.addDirectoryNode(dirNodeId, name);
		} catch (Exception e) {
			throw new ServiceException("Error adding new subdirectory to directory " + dirNodeId, e);
		}
		return dirResource;
		
	}	

}
