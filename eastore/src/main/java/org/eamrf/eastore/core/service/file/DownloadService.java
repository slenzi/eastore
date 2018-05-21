/**
 * 
 */
package org.eamrf.eastore.core.service.file;

import java.nio.file.Path;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.service.tree.file.PathResourceUtil;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.DownloadLogRepository;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.FileMetaResource;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Main service class for downloading files from eastore
 * 
 * @author slenzi
 */
@Service
public class DownloadService {

    @InjectLogger
    private Logger logger;		
	
    @Autowired
    private FileService fileService;	
	
    @Autowired
    private DownloadLogRepository downloadLogRepository;	
	
	/**
	 * 
	 */
	public DownloadService() {

	}
	
	/**
	 * Download the file for eastore
	 * 
	 * @param fileId - unique id of the file
	 * @param userId - user id of the user downloading the file
	 * @return
	 * @throws ServiceException
	 */
	public FileMetaResource downloadFile(Long fileId, String userId) throws ServiceException {
		
		FileMetaResource fileMeta = null;
		try {
			fileMeta = fileService.getFileMetaResource(fileId, userId, true);
		} catch (ServiceException e) {
			throw new ServiceException("Error fetching file for download, fileId=" + fileId + 
					", userId=" + userId + ", " + e.getMessage(), e);
		}
		
		try {
			downloadLogRepository.logDownload(fileMeta, userId);
		} catch (Exception e) {
			Path fullPath = PathResourceUtil.buildPath(fileMeta.getStore(), fileMeta);
			logger.warn("Faield to log download for userId=" + userId + ", file=" + fullPath.toString());
		}
		
		return fileMeta;
		
	}	
	
	/**
	 * Download the file for eastore
	 * 
	 * @param storeName - name of the store under which the file resides
	 * @param relPath - path to the file relative to the store path
	 * @param userId - user id of the user downloading the file
	 * @return
	 * @throws ServiceException
	 */
	public FileMetaResource downloadFile(String storeName, String relPath, String userId) throws ServiceException {
		
		// fetch the file, with binary data
		FileMetaResource fileMeta = null;
		try {
			fileMeta = fileService.getFileMetaResource(storeName, relPath, userId, true);
		} catch (ServiceException e) {
			throw new ServiceException("Error fetching file for download, storeName=" + storeName + ", relPath=" + relPath + 
					", userId=" + userId + ", " + e.getMessage(), e);
		}
		
		// log the download
		try {
			downloadLogRepository.logDownload(fileMeta, userId);
		} catch (Exception e) {
			Path fullPath = PathResourceUtil.buildPath(fileMeta.getStore(), fileMeta);
			logger.warn("Faield to log download for userId=" + userId + ", file=" + fullPath.toString());
		}
		
		return fileMeta;
		
	}

}
