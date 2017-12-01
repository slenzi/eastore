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

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.FileUtil;
import org.eamrf.eastore.core.aop.profiler.MethodTimer;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.properties.ManagedProperties;
import org.eamrf.eastore.core.service.tree.file.secure.SecurePathResourceTreeService;
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
    private SecurePathResourceTreeService pathResourceTreeService;    
	
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
			String userId) throws ServiceException {
		
		Path tempDir = saveIncomingDataToTempDir(fileName, dataHandler);
		
		pathResourceTreeService.processToDirectory(dirNodeId, tempDir, replaceExisting, userId);
		
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
			String userId) throws ServiceException {	
		
		Path tempDir = saveIncomingDataToTempDir(fileName, dataHandler);
		
		pathResourceTreeService.processToDirectory(storeName, dirRelPath, tempDir, replaceExisting, userId);
		
	}
	
	/**
	 * Saves the file data to a new temp directory
	 * 
	 * @param fileName
	 * @param dataHandler
	 * @return
	 * @throws ServiceException
	 */
	private Path saveIncomingDataToTempDir(String fileName, DataHandler dataHandler) throws ServiceException {
		
		Path tempDir = createTempDir();
		
		InputStream inStream = null;
		try {
			inStream = dataHandler.getInputStream();
		} catch (IOException e) {
			throw new ServiceException("Upload pipeline error, failed to get InputStream from DataHandler, " + e.getMessage(), e);
		}
		
		Path destFile = Paths.get(tempDir.toString() + File.separator + fileName);
		
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