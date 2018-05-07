package org.eamrf.eastore.core.service.file.task;

import org.eamrf.concurrent.task.AbstractQueuedTask;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.socket.messaging.ResourceChangeService;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.FileSystemRepository;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.FileMetaResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task for removing a file
 * 
 * @author slenzi
 *
 */
public class RemoveFileTask extends AbstractQueuedTask<Void> {

	private Logger logger = LoggerFactory.getLogger(RemoveFileTask.class);
	
	private FileMetaResource fileMetaResource;
	private FileSystemRepository fileSystemRepository;
	private ResourceChangeService resChangeService;
	
	public RemoveFileTask(
			FileMetaResource fileMetaResource,
			FileSystemRepository fileSystemRepository,
			ResourceChangeService resChangeService) {
		
		this.fileMetaResource = fileMetaResource;
		this.fileSystemRepository = fileSystemRepository;
		this.resChangeService = resChangeService;
	}

	@Override
	public Void doWork() throws ServiceException {
		
		try {
			fileSystemRepository.removeFile(fileMetaResource);
		} catch (Exception e) {
			throw new ServiceException("Error removing file with node id => " + fileMetaResource.getNodeId() + ". " + e.getMessage(), e);
		}
		
		resChangeService.directoryContentsChanged(fileMetaResource.getDirectory().getNodeId());
		
		// TODO - remove from lucene index!
		
		return null;
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

}
