/**
 * 
 */
package org.eamrf.eastore.core.service.upload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import javax.activation.DataHandler;

import org.apache.commons.io.FilenameUtils;
import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.FileUtil;
import org.eamrf.eastore.core.aop.profiler.MethodTimer;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.properties.ManagedProperties;
import org.eamrf.eastore.core.service.file.FileService;
import org.eamrf.eastore.core.service.file.task.FileServiceTaskListener;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * @author slenzi
 *
 */
@Service
@Scope("singleton")
public class UploadPipeline {

    @InjectLogger
    private Logger logger;
    
    @Autowired
    private ManagedProperties appProps;    
	
    @Autowired
    private FileService fileService;
    
	public UploadPipeline() {
		
	}
	
	/**
	 * Process file through upload pipeline
	 * 
	 * @param dirNodeId - directory where file will be added
	 * @param fileName - name of file
	 * @param dataHandler - interface to the binary data for the file
	 * @param replaceExisting - pass true to replace exiting file, or pass false. If you pass false and
	 * and file with the same name already exists, then a service exception will be thrown.
	 * @param userId - id of user performing action
	 * @throws ServiceException
	 */
	@MethodTimer
	public void processUpload(
			Long dirNodeId,
			String fileName,
			DataHandler dataHandler, 
			boolean replaceExisting,
			String userId,
			FileServiceTaskListener listener) throws ServiceException {
		
		Path tempDir = saveIncomingDataToTempDir(fileName, dataHandler, true);
		
		fileService.processToDirectory(dirNodeId, tempDir, replaceExisting, userId, listener);
		
	}
	
	/**
	 * Process file through upload pipeline
	 * 
	 * @param storeName - name of the store
	 * @param dirRelPath - relative path of the directory within the store
	 * @param fileName - name of file
	 * @param dataHandler - interface to the binary data for the file
	 * @param replaceExisting - pass true to replace exiting file, or pass false. If you pass false and
	 * and file with the same name already exists, then a service exception will be thrown.
	 * @param userId - id of user performing action
	 * @throws ServiceException
	 */
	@MethodTimer
	public void processUpload(
			String storeName, 
			String dirRelPath, 
			String fileName, 
			DataHandler dataHandler, 
			boolean replaceExisting,
			String userId,
			FileServiceTaskListener listener) throws ServiceException {	
		
		Path tempDir = saveIncomingDataToTempDir(fileName, dataHandler, true);
		
		fileService.processToDirectory(storeName, dirRelPath, tempDir, replaceExisting, userId, listener);
		
	}
	
	/**
	 * Saves the file data to a new temp directory
	 * 
	 * @param fileName
	 * @param dataHandler
	 * @param cleanFileName - Pass true to clean the file name by removing non-alphanumeric characters, 
	 * 	preserving period, underscore, and dash (._-). Pass false not to clean the file name.
	 * @return
	 * @throws ServiceException
	 */
	private Path saveIncomingDataToTempDir(String fileName, DataHandler dataHandler, boolean cleanFileName) throws ServiceException {
		
		Path tempDir = createTempDir();
		
		InputStream inStream = null;
		try {
			inStream = dataHandler.getInputStream();
		} catch (IOException e) {
			throw new ServiceException("Upload pipeline error, failed to get InputStream from DataHandler, " + e.getMessage(), e);
		}
		
		String fileExtension = FilenameUtils.getExtension(fileName);
		String fileBaseName = FilenameUtils.getBaseName(fileName);
		String cleanBaseName = fileBaseName.replaceAll("[^a-zA-Z0-9._-]", " ").replaceAll(" +", " ");
		String cleanName = cleanBaseName + "." + fileExtension;
		
		Path destFile = Paths.get(tempDir.toString() + File.separator + cleanName);
		
		try {
			Files.copy(inStream, destFile);
		} catch (IOException e) {
			throw new ServiceException("Error saving incoming file to => " + destFile.toString() + ", " + e.getMessage(), e);
		}
		
		return tempDir;
		
	}
	
	/**
	 * Creates a temporary directory to store the incoming files.
	 * 
	 * @return
	 * @throws ServiceException
	 */
	private Path createTempDir() throws ServiceException {
		
		String parentTempDir = appProps.getProperty("temp.upload.directory");
		
		long epochMilli = System.currentTimeMillis();
		String uuid = UUID.randomUUID().toString();
		
		String tempDir = parentTempDir + File.separator + String.valueOf(epochMilli) + "." + uuid;
		
		Path tempPath = Paths.get(tempDir);
		
		try {
			FileUtil.createDirectory(tempPath, true);
		} catch (Exception e) {
			throw new ServiceException("Failed to create temporary directory at " + tempDir + ". " + e.getMessage(), e);
		}		
		
		return tempPath;
		
	}

	

}
