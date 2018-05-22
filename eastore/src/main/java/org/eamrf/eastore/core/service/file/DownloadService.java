/**
 * 
 */
package org.eamrf.eastore.core.service.file;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.Executors;

import org.eamrf.concurrent.task.TaskIdGenerator;
import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.DateUtil;
import org.eamrf.core.util.FileUtil;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.properties.ManagedProperties;
import org.eamrf.eastore.core.service.file.task.TaskCompletionListener;
import org.eamrf.eastore.core.service.file.task.ZipTask;
import org.eamrf.eastore.core.service.tree.file.PathResourceUtil;
import org.eamrf.eastore.core.service.tree.file.secure.SecurePathResourceTreeService;
import org.eamrf.eastore.core.socket.messaging.FileServiceTaskMessageService;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.DownloadLogRepository;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.DownloadLogEntry;
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
    private ManagedProperties appProps;    
	
    @Autowired
    private FileService fileService;
    
	@Autowired
	private SecurePathResourceTreeService secureTreeService;    
	
    @Autowired
    private DownloadLogRepository downloadLogRepository;
    
    @Autowired
    private FileServiceTaskMessageService fileServiceTaskMessageService;    
	
	/**
	 * 
	 */
	public DownloadService() {

	}
	
	/**
	 * Fetch download log entry by id
	 * 
	 * @param downloadId - the unique download id
	 * @return
	 * @throws ServiceException
	 */
	public DownloadLogEntry getByDownloadId(Long downloadId) throws ServiceException {
		
		DownloadLogEntry entry = null;
		try {
			entry = downloadLogRepository.getById(downloadId);
		} catch (Exception e) {
			throw new ServiceException("Error fetching download log entry by id, downloadId=" + downloadId + 
					", " + e.getMessage(), e);
		}
		return entry;
		
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
		
		logDownload(fileMeta, userId);
		
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
		
		FileMetaResource fileMeta = null;
		try {
			fileMeta = fileService.getFileMetaResource(storeName, relPath, userId, true);
		} catch (ServiceException e) {
			throw new ServiceException("Error fetching file for download, storeName=" + storeName + ", relPath=" + relPath + 
					", userId=" + userId + ", " + e.getMessage(), e);
		}
		
		logDownload(fileMeta, userId);
		
		return fileMeta;
		
	}
	
	/**
	 * Trigger zip download event.
	 * 
	 * @param resourceIdList - ID of all path resources to zip for download
	 * @param userId - ID of user completing the action
	 * @param completionListener - listener to be notified when zip process has completed. Listener will provide access to the download ID
	 * 	which can be used to fetch the zip file.
	 * @throws ServiceException
	 */
	public void triggerZipDownload(List<Long> resourceIdList, String userId, TaskCompletionListener<Long> completionListener) throws ServiceException {
		
		final Timestamp dtNow = DateUtil.getCurrentTime();
		final String dateToken = DateUtil.formatDate(dtNow, "yyyy.MM.dd.HH.mm.ss.SSS").toLowerCase();
		final String miscDir = appProps.getProperty("temp.misc.directory");
		final String tempDir = dateToken + "_" + userId;
		final Path outputDir = Paths.get(miscDir, tempDir);
		try {
			FileUtil.createDirectory(outputDir, true);
		} catch (Exception e) {
			throw new ServiceException("Could not create temp directory for zip file at, " + outputDir.toString());
		}
		final String zipFileName = "ecog-acrin_file_manager_download_" + userId + "_" + dateToken + ".zip";
		final Path outZipPath = Paths.get(outputDir.toString(), zipFileName);
				
		ZipTask zipTask = new ZipTask(resourceIdList, userId, outZipPath, secureTreeService, fileService);
		zipTask.setTaskId(TaskIdGenerator.getNextTaskId());
		
		zipTask.registerProgressListener(task -> {
			
			logger.info(task.getStatusMessage());
			
			// broadcast task so clients can track progress
			fileServiceTaskMessageService.broadcast(task);
			
			// when task completes, notify completion listener
			Long percentComplete = Math.round(task.getProgress());
			if(percentComplete.equals(new Long(100))) {
				
				// log zip file in download table
				Long downloadId = this.logDownload(outZipPath, userId);
				
				// notify completion listener
				completionListener.onComplete(downloadId);
				
			}
			
		});
		
		Executors.newSingleThreadExecutor().execute(zipTask);
		
	}
	
	/**
	 * Log the download
	 * 
	 * @param file
	 * @param userId
	 */
	public Long logDownload(FileMetaResource file, String userId) {

		try {
			return downloadLogRepository.logDownload(file, userId);
		} catch (Exception e) {
			Path fullPath = PathResourceUtil.buildPath(file.getStore(), file);
			logger.warn("Faield to log download for userId=" + userId + ", fileMetaResource=" + fullPath.toString());
			return -1L;
		}		
		
	}
	
	/**
	 * Log the download
	 * 
	 * @param file
	 * @param userId
	 */
	public Long logDownload(Path pathToFile, String userId) {

		try {
			return downloadLogRepository.logDownload(pathToFile, userId);
		} catch (Exception e) {
			logger.warn("Faield to log download for userId=" + userId + ", pathToFile=" + pathToFile.toString());
			return -1L;
		}		
		
	}	

}
