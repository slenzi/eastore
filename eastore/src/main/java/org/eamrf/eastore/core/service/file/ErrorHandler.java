package org.eamrf.eastore.core.service.file;

import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.DirectoryResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.FileMetaResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.PathResource;
import org.springframework.stereotype.Service;

/**
 * Util class for error handling
 * 
 * @author slenzi
 *
 */
@Service
public class ErrorHandler {

	public ErrorHandler() {

	}
	
	/**
	 * Helper method for processing permission related errors
	 * 
	 * @param error
	 * @param fileResource
	 * @param userId
	 * @throws ServiceException
	 */
	public void handlePermissionDenied(PermissionError error, PathResource resource, String userId) throws ServiceException {
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
	public void handlePermissionDenied(PermissionError error, FileMetaResource fileResource, String userId) throws ServiceException {
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
	public void handlePermissionDenied(PermissionError error, DirectoryResource directoryResource, String userId) throws ServiceException {
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
	public void handlePermissionDenied(PermissionError error, FileMetaResource fileResource, DirectoryResource directoryResource, String userId) throws ServiceException {
		// TODO create a new exception type for permission not allowed
		throw new ServiceException(error.toString() + ". User " + userId + " does not have permission to perform action on " + 
				" file resource [id=" + fileResource.getNodeId() + ", type=" + fileResource.getResourceType().getTypeString() + 
				", relPath=" + fileResource.getRelativePath() + ", store=" + fileResource.getStore().getName() + "] " +
				" under directory resource [id=" + directoryResource.getNodeId() + ", type=" + directoryResource.getResourceType().getTypeString() + 
				", relPath=" + directoryResource.getRelativePath() + ", store=" + directoryResource.getStore().getName() + "] "				
				);
	}	

}
