package org.eamrf.eastore.core.service.io;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Path;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.FileUtil;
import org.eamrf.eastore.core.aop.profiler.MethodTimer;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

/**
 * Helper service for various file related actions.
 * 
 * @author slenzi
 *
 */
@Service
public class FileIOService {

    @InjectLogger
    private Logger logger;	
	
	public FileIOService() {
	
	}
	
	/**
	 * Get size of file
	 * 
	 * @param pathToFile - path to a file
	 * @return
	 */
	@MethodTimer
	public Long getSize(Path pathToFile) {
		return FileUtil.getFileSize(pathToFile);
	}
	
	/**
	 * Get MIME type of file
	 * 
	 * @param pathToFile - path to a file
	 * @return
	 * @throws IOException
	 */
	@MethodTimer
	public String getMimeType(Path pathToFile) throws IOException {
		return FileUtil.detectMimeType(pathToFile);
	}
	
	/**
	 * Create a directory
	 * 
	 * @param directory
	 * @param clearIfNotEmpty
	 * @throws Exception
	 */
	@MethodTimer
	public void createDirectory(final Path directory, boolean clearIfNotEmpty) throws Exception {
		FileUtil.createDirectory(directory, clearIfNotEmpty);
	}
	
	/**
	 * Move a file
	 * 
	 * @param source
	 * @param target
	 * @param options
	 * @throws SecurityException
	 * @throws IOException
	 */
	@MethodTimer
	public void moveFile(Path source, Path target, CopyOption... options) throws SecurityException, IOException {
		FileUtil.moveFile(source, target, options);
	}
	
	/**
	 * Copy a file
	 * 
	 * @param source
	 * @param target
	 * @param options
	 * @throws IOException
	 * @throws SecurityException
	 */
	@MethodTimer
	public void copyFile(Path source, Path target, CopyOption... options) throws IOException, SecurityException {
		FileUtil.copyFile(source, target, options);
	}
	
	/**
	 * Delete a resource at the provided path
	 * 
	 * @param path
	 * @throws IOException
	 */
	@MethodTimer
	public void deletePath(final Path path) throws IOException {
		FileUtil.deletePath(path);
	}

}
