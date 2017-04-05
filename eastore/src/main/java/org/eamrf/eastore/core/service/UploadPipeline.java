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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.activation.DataHandler;
import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.CodeTimer;
import org.eamrf.core.util.FileUtil;
import org.eamrf.eastore.core.concurrent.task.AbstractQueuedTask;
import org.eamrf.eastore.core.concurrent.task.QueuedTaskManager;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.properties.ManagedProperties;
import org.eamrf.repository.oracle.ecoguser.eastore.model.FileMetaResource;
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
	private QueuedTaskManager taskManager;
	
	// used in conjunction with the QueuedTaskManager
	private ExecutorService taskExecutorService = null;
	
	@Autowired
	private FileSystemService fileSystemService;
	
	public UploadPipeline() {
		
	}
	
	/**
	 * Start the QueuedTaskManager when this bean (UploadPipeline) is created.
	 */
	@PostConstruct
	public void init(){
		
		if(taskManager != null){
			
			logger.info("Starting queued task manager...");
			
			taskExecutorService = Executors.newSingleThreadExecutor();
			
			taskManager.setManagerName(UploadPipeline.class.getName() + " Service");
			
			taskManager.startTaskManager(taskExecutorService);
			
			logger.info("Queued task manager startup complete.");
			
		}else{
			
			logger.error("Cannot start " + UploadPipeline.class.getName() + " Service. The " + 
					QueuedTaskManager.class.getName() + " object is null.");
			
		}
		
	}
	
	/**
	 * Process file through upload pipeline
	 * 
	 * @param dirNodeId - directory where file will be added
	 * @param fileName - name of file
	 * @param dataHandler - interface to the binary data for the file
	 * @param replaceExisting - pass true to replace exiting file, or pass false. If you pass false and
	 * and file with the same name already exists, then a service exception will be thrown.
	 * @throws ServiceException
	 */
	public void processUpload(Long dirNodeId, String fileName, DataHandler dataHandler, boolean replaceExisting) throws ServiceException {
		
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
		
		processToDirectory(dirNodeId, tempDir, replaceExisting);
		
	}
	
	/**
	 * Process the incoming file and adds it to the database tree, under the specified node.
	 * 
	 * @param dirNodeId
	 * @param tempDir
	 * @param replaceExisting
	 * @throws ServiceException
	 */
	private void processToDirectory(Long dirNodeId, Path tempDir, boolean replaceExisting) throws ServiceException {
		
		// TODO - Eventually we'll want the ID of the user who uploaded. The JAX-RS service will have to use (possibly)
		// the AuthWorld session key to identify users logged into the portal. That would give us access to the CTEP ID.
		// We'll also need to add a field to eas_path_resource to store the user's ID.
		
		// Option 1: Add data to eas_node, eas_closure, eas_path_resource, and eas_file_meta_resource, then copy physical
		// file to tree directory on the local file system. Block until operation is complete, then add a task on a separate
		// thread which goes back and adds the binary data to eas_binary_resource table, and updates the 'IS_FILE_DATA_IN_DB'
		// to 'Y'. Do it on a separate thread so control can be released back to the calling process (users who uploaded.)
		
		List<Path> filePaths = null;
		try {
			filePaths = FileUtil.listFilesToDepth(tempDir, 1);
		} catch (IOException e) {
			throw new ServiceException("Error listing files in temporary directory " + tempDir.toString());
		}
		
		filePaths.stream().forEach(
			(pathToFile) ->{
				
				// add file meta
				FileMetaResource fileMetaResource = null;
				try {
					fileMetaResource = addFileWithoutBinary(dirNodeId, pathToFile, replaceExisting);
				} catch (ServiceException e) {
					throw new RuntimeException("Error adding file '" + pathToFile.toString() + "' to directory with id '" + dirNodeId + "'.", e);
				}
				
				// go back and add binary to db
				try {
					updateFileWithBinary(fileMetaResource);
				} catch (ServiceException e) {
					throw new RuntimeException("Error adding binary data to db for FileMetaResource with node id => " + fileMetaResource.getNodeId(), e);
				}
					
			});
		
	}
	
	/**
	 * Adds the file to the directory, but does not add the binary data to eas_binary_resource.
	 * 
	 * @param dirNodeId
	 * @param filePath
	 * @param replaceExisting
	 * @throws ServiceException
	 */
	private FileMetaResource addFileWithoutBinary(Long dirNodeId, Path filePath, boolean replaceExisting) throws ServiceException {
		
		class AddFileTask extends AbstractQueuedTask<FileMetaResource> {

			@Override
			public FileMetaResource doWork() throws ServiceException {
				
				CodeTimer timer = new CodeTimer();
				timer.start();
				
				logger.info(">> Process to directory (start) => " + filePath.toString());
				
				FileMetaResource fileMetaResource = null;
				
				fileMetaResource = fileSystemService.addFileWithoutBinary(dirNodeId, filePath, replaceExisting);
	
				timer.stop();
				
				logger.info(">> Process to directory (end) => " + filePath.toString() + ", elapsed time => " + timer.getElapsedTime());
				
				return fileMetaResource;
				
			}

			@Override
			public Logger getLogger() {
				return logger;
			}
			
		};
		
		AddFileTask task = new AddFileTask();
		task.setName("Upd file => " + filePath.toString() + ", to directory node => " + 
				dirNodeId + ", replaceExisting => " + replaceExisting);
		taskManager.addTask(task);
		
		FileMetaResource fileMetaResource = task.get(); // blocks until complete
		
		return fileMetaResource;
		
	}
	
	/**
	 * Updated the FileMetaResource by adding the binary data to eas_binary_resource, then sets the
	 * EAS_FILE_META_RESOURCE.IS_FILE_DATA_IN_DB field to 'Y'
	 * 
	 * @param fileMetaResource
	 * @throws ServiceException
	 */
	private void updateFileWithBinary(FileMetaResource fileMetaResource) throws ServiceException {
		
		class UpdateFileTask extends AbstractQueuedTask<Void> {

			@Override
			public Void doWork() throws ServiceException {
				
				CodeTimer timer = new CodeTimer();
				timer.start();
				
				logger.info(">> Update with binary data (start): Node ID => " + 
						fileMetaResource.getNodeId() + ", relPath => " + fileMetaResource.getRelativePath());
				
				fileSystemService.updateFileBinary(fileMetaResource);
				
				timer.stop();
				
				logger.info(">> Update with binary data (end): Node ID => " + 
						fileMetaResource.getNodeId() + ", relPath => " + fileMetaResource.getRelativePath());
				
				return null;
				
			}

			@Override
			public Logger getLogger() {
				return logger;
			}
			
		};
		
		UpdateFileTask task = new UpdateFileTask();
		task.setName("Adding binary data to database for FileMetaResource with node id => " + fileMetaResource.getNodeId());
		taskManager.addTask(task);
		
		// do not block for completion of task
		
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
