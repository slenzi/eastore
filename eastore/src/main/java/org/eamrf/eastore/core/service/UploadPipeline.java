/**
 * 
 */
package org.eamrf.eastore.core.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import javax.activation.DataHandler;

import org.apache.commons.io.IOUtils;
import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.FileUtil;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.properties.ManagedProperties;
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
	
	public UploadPipeline() {
		
	}
	
	/**
	 * Process file through upload pipeline
	 * 
	 * @param dirNodeId - directory where file will be added
	 * @param fileName - name of file
	 * @param dataHandler - interface to the binary data for the file
	 * @throws ServiceException
	 */
	public void processUpload(Long dirNodeId, String fileName, DataHandler dataHandler) throws ServiceException {
		
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
		
		/*
		byte[] bytes = null;
		try {
			bytes = IOUtils.toByteArray(inStream);
		} catch (IOException e) {
			throw new ServiceException("Upload pipeline error, failed to get InputStream from DataHandler, " + e.getMessage(), e);
		}
		*/
		
	}
	
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
