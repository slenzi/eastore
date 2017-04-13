package org.eamrf.core.util;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MimeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FileUtil {

	private static Logger logger = LoggerFactory.getLogger(FileUtil.class);
	
	private static final Detector DETECTOR = new DefaultDetector(MimeTypes.getDefaultMimeTypes());

	/**
	 * Pretty print byte count into human readable format
	 * 
	 * @param bytes
	 * @param si
	 * @return
	 */
	public static String humanReadableByteCount(long bytes, boolean si) {
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}	
	
	/**
	 * Get file size in bytes.
	 * 
	 * @param file
	 * @return
	 */
	public static Long getFileSize(final Path file){
		if(file == null){
			return 0L;
		}
		return file.toFile().length();
	}
	
	/**
	 * Get file mime type
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static String detectMimeType(final Path file) throws IOException {
	    TikaInputStream tikaIS = null;
	    try {
	        //tikaIS = TikaInputStream.get(file.toFile());
	        tikaIS = TikaInputStream.get(file);
	        /*
	         * You might not want to provide the file's name. If you provide an Excel
	         * document with a .xls extension, it will get it correct right away; but
	         * if you provide an Excel document with .doc extension, it will guess it
	         * to be a Word document
	         */
	        final Metadata metadata = new Metadata();
	        // metadata.set(Metadata.RESOURCE_NAME_KEY, file.getName());

	        return DETECTOR.detect(tikaIS, metadata).toString();
	        
	    } finally {
	        if (tikaIS != null) {
	            tikaIS.close();
	        }
	    }
	}
	
	/**
	 * Get file mime type
	 * 
	 * @param fileBytes
	 * @return
	 * @throws IOException
	 */
	public static String detectMimeType(final byte[] fileBytes) throws IOException {
	    TikaInputStream tikaIS = null;
	    try {
	        tikaIS = TikaInputStream.get(fileBytes);
	        /*
	         * You might not want to provide the file's name. If you provide an Excel
	         * document with a .xls extension, it will get it correct right away; but
	         * if you provide an Excel document with .doc extension, it will guess it
	         * to be a Word document
	         */
	        final Metadata metadata = new Metadata();
	        // metadata.set(Metadata.RESOURCE_NAME_KEY, file.getName());

	        return DETECTOR.detect(tikaIS, metadata).toString();
	        
	    } finally {
	        if (tikaIS != null) {
	            tikaIS.close();
	        }
	    }
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
	public static void copyFile(Path source, Path target, CopyOption... options) throws IOException, SecurityException {
		
		try {
			
			Files.copy(source, target, options);
			
		} catch (IOException e) {
			throw new IOException("Error copying " + source.toString() + " to " + target.toString() + ". " + e.getMessage(), e);
		} catch (SecurityException e) {
			throw new SecurityException("Error copying " + source.toString() + " to " + target.toString() + ". " + e.getMessage(), e);
		}
		
	}
	
	/**
	 * Move a file
	 * 
	 * @param source
	 * @param target
	 * @param options
	 * @throws IOException
	 * @throws SecurityException
	 */
	public static void moveFile(Path source, Path target, CopyOption... options) throws IOException, SecurityException {
		
		try {
			
			Files.move(source, target, options);
			
		} catch (IOException e) {
			throw new IOException("Error copying " + source.toString() + " to " + target.toString() + ". " + e.getMessage(), e);
		} catch (SecurityException e) {
			throw new SecurityException("Error copying " + source.toString() + " to " + target.toString() + ". " + e.getMessage(), e);
		}
		
	}
	
	/**
	 * Check if a directory is empty.
	 * 
	 * @param directory
	 * @return
	 * @throws IOException
	 */
	public static boolean isDirectoryEmpty(final Path directory) throws IOException {
	    try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
	        return !dirStream.iterator().hasNext();
	    }
	}
	
	/**
	 * List files in a directory, and all sub-directories
	 * 
	 * @param directory
	 * @return
	 * @throws IOException
	 */
	public static List<Path> listFiles(final Path directory) throws IOException {
		return listFilesToDepth(directory, Integer.MAX_VALUE);
	}
	
	/**
	 * List files in a directory, up to a limited number of sub directories.
	 * 
	 * @param directory - the directory to start at
	 * @param maxDepth - the maximum number of directory levels to visit
	 * @return
	 * @throws IOException
	 */
	public static List<Path> listFilesToDepth(final Path directory, int maxDepth) throws IOException {
		
		return Files.walk(directory, maxDepth)
			.filter(path -> Files.isRegularFile(path))
			.collect(Collectors.toList());
	}
	
	/**
	 * Create a directory
	 * 
	 * @param directory - path of directory to create
	 * @param clearIfNotEmpty - pass true to delete any files in the directory if it already exists and contains files.
	 * @throws IOException - if there is any error creating the directory, and if directory already
	 * 	exists, and it contains files, and 'clearIfExists' is false.
	 */
	public static void createDirectory(final Path directory, boolean clearIfNotEmpty) throws Exception {
		
		boolean dirExists = Files.exists(directory);
		
		if(dirExists){
			
			logger.info(directory.toString() + " already exists.");
			
			boolean isEmpty = false;
			try {
				isEmpty = FileUtil.isDirectoryEmpty(directory);
			} catch (IOException e) {
				throw new IOException("Error checking if " + directory.toString() + " exists and contains files.", e);
			}
			
			logger.info(directory.toString() + " is not empty.");
			
			if(!isEmpty && !clearIfNotEmpty){
				throw new IOException("Cannot create new file store. Path " + directory.toString() + " already exists and contains files");
			}
			if(!isEmpty && clearIfNotEmpty){
				logger.info("Deleting " + directory.toString());
				try {
					deletePath(directory);
					//Files.delete(directory);
				} catch (IOException e) {
					throw new IOException("Error removing existing directory " + directory.toString());
				}
				try {
					logger.info("Re-creating " + directory.toString());
					Files.createDirectories(directory);
				} catch (IOException e) {
					throw new IOException("Failed to re-create directory " + directory.toString(), e);
				}
			}
		}else{
			
			logger.info(directory.toString() + " does not exist yet. Need to create it.");
			logger.info("Creating " + directory.toString());
			
			try {
				Files.createDirectories(directory);
			} catch (IOException e) {
				throw new IOException("Failed to create directory " + directory.toString(), e);
			}	
		}
		
		// verify it exists
		if(!Files.exists(directory)){
			throw new IOException("Failed to created directory on file system for unknown reason. No exception was thrown during create operation, but directory does not exist.");
		}
		
	}
	
	/**
	 * Delete a path. If the path references a file then the file is deleted. If the path references a
	 * directory then the directory and everything under it is deleted.
	 * 
	 * @param path
	 * @throws IOException
	 */
	public static void deletePath(final Path path) throws IOException {
		
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

			// delete the file
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			// delete the directory after all child elements have been visited, and deleted
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}

		});		
		
	}

}
