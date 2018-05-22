/**
 * 
 */
package org.eamrf.eastore.core.service.file.task;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eamrf.core.util.CollectionUtil;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.service.file.DownloadService;
import org.eamrf.eastore.core.service.file.FileService;
import org.eamrf.eastore.core.service.tree.file.PathResourceUtil;
import org.eamrf.eastore.core.service.tree.file.secure.SecurePathResourceTreeService;
import org.eamrf.eastore.core.tree.Tree;
import org.eamrf.eastore.core.tree.TreeNode;
import org.eamrf.eastore.core.tree.TreeNodeVisitException;
import org.eamrf.eastore.core.tree.Trees;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.FileMetaResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.PathResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.ResourceType;
import org.slf4j.Logger;

/**
 * A task for handling a user action to zip files/directories for download
 * 
 * @author slenzi
 */
public class ZipTask extends FileServiceTask<Void> {

	private List<Long> resourcesToZip;
	private String userId;
	private Path pathToZip;
	private SecurePathResourceTreeService secureTreeService;
	private FileService fileService;
	
	private List<FileMetaResource> filesToZip = new ArrayList<FileMetaResource>();
	private List<Tree<PathResource>> directoriesToZip = new ArrayList<Tree<PathResource>>();
	
	private int jobCount = 0;
	
	// the id of the entry in the download log for the zip file
	private Long downloadId = null;
	
	public ZipTask(
			List<Long> resourcesToZip,
			String userId,
			Path pathToZip,
			SecurePathResourceTreeService secureTreeService, 
			FileService fileService) {
		
		this.resourcesToZip = resourcesToZip;
		this.userId = userId;
		this.pathToZip = pathToZip;
		this.secureTreeService = secureTreeService;
		this.fileService = fileService;
		
	}

	/* (non-Javadoc)
	 * @see org.eamrf.eastore.core.service.file.task.FileServiceTask#getJobCount()
	 */
	@Override
	public int getJobCount() {
		return jobCount;
	}

	/* (non-Javadoc)
	 * @see org.eamrf.eastore.core.service.file.task.FileServiceTask#getStatusMessage()
	 */
	@Override
	public String getStatusMessage() {

		if(getJobCount() <= 0) {
			return "Zip task pending...";
		}else{
			return "Zip task is " + Math.round(getProgress()) + "% complete (job " + this.getCompletedJobCount() + " of " + this.getJobCount() + " processed)";
		}		
		
	}

	/* (non-Javadoc)
	 * @see org.eamrf.eastore.core.service.file.task.FileServiceTask#getUserId()
	 */
	@Override
	public String getUserId() {
		return userId;
	}
	
	/**
	 * Get id of download for the zip file. This will only be set after the zip process
	 * has completed (i.e. the task progress is 100%.)
	 * 
	 * @return
	 */
	public Long getDownloadId() {
		return downloadId;
	}
	
	/**
	 * Fetch all files to zip, and also calculate the job count (simply the number of files to zip)
	 * 
	 * @throws ServiceException
	 */
	private void fetchFiles() throws ServiceException {
		
		PathResource pathResource = null;
		Tree<PathResource> tree = null;
		int numFiles = 0;
		
		for(Long resourceId : CollectionUtil.emptyIfNull(resourcesToZip)) {
			
			pathResource = fileService.getPathResource(resourceId, userId);
			
			// add file, and count it
			if(pathResource.getResourceType() == ResourceType.FILE) {
				filesToZip.add((FileMetaResource) pathResource);
				numFiles++;
				
			// add directory, and count all files in it (recursively)
			}else if(pathResource.getResourceType() == ResourceType.DIRECTORY) {
				tree = secureTreeService.buildPathResourceTree(resourceId, userId);
				directoriesToZip.add(tree);
				try {
					numFiles += Trees.nodeCount(tree, FileMetaResource.class);
				} catch (TreeNodeVisitException e) {
					throw new ServiceException("Failed to count the number of files under directory " + resourceId);
				}
			}
			
		}
		
		// set job count
		jobCount = numFiles; 
		
		notifyChange();
		
	}

	/* (non-Javadoc)
	 * @see org.eamrf.concurrent.task.AbstractQueuedTask#doWork()
	 */
	@Override
	public Void doWork() throws ServiceException {
		
		fetchFiles();
		
		// open zip output stream
        FileOutputStream fos;
		try {
			fos = new FileOutputStream(pathToZip.toFile());
		} catch (FileNotFoundException e) {
			throw new ServiceException("Can't create file at " + pathToZip.toString() + ", " + e.getMessage(), e);
		}
        ZipOutputStream zipOut = new ZipOutputStream(fos);			
		
        // add all files to zip file
		Path filePath = null;
		for(FileMetaResource file : CollectionUtil.emptyIfNull(filesToZip)) {
			filePath = PathResourceUtil.buildPath(file.getStore(), file);
			try {
				zipFile(file, file.getPathName(), zipOut);
			} catch (IOException e) {
				throw new ServiceException("Error adding file " + filePath.toString() + " to zip file " + pathToZip.toString());
			}	
		}
		
		// add all directories, and all files under the directories, to the zip file 
		Path dirPathPath = null;
		TreeNode<PathResource> treeNode = null;
		for(Tree<PathResource> tree : CollectionUtil.emptyIfNull(directoriesToZip)) {
			treeNode = tree.getRootNode();
			dirPathPath = PathResourceUtil.buildPath(treeNode.getData().getStore(), treeNode.getData());
			try {
				zipFile(treeNode, treeNode.getData().getPathName(), zipOut);
			} catch (IOException e) {
				throw new ServiceException("Error adding all files under directory " + dirPathPath.toString() + " to zip file " + pathToZip.toString());
			}
		}
		
		// close output streams
		try {
			zipOut.close();
			fos.close();
		} catch (IOException e) {
			logger.warn("Error closing ZipOutputStream and/or FileOutputStream for zip file " + pathToZip.toString());
			// eat it..
		}
		
		return null;
		
	}
	
	/**
	 * Add file to the zip output stream
	 * 
	 * @param file
	 * @param fileName
	 * @param zipOut
	 * @throws IOException
	 */
	private void zipFile(FileMetaResource file, String fileName, ZipOutputStream zipOut) throws IOException {
        
		FileInputStream fis = new FileInputStream(PathResourceUtil.buildPath(file.getStore(), file).toFile());
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
        
        setCompletedJobCount(this, getCompletedJobCount() + 1);
        
    }
	
	/**
	 * Recursively iterator over tree representing directory and add all files to zip output stream
	 * 
	 * @param node
	 * @param pathName
	 * @param zipOut
	 * @throws IOException
	 */
	private void zipFile(TreeNode<PathResource> node, String pathName, ZipOutputStream zipOut) throws IOException {
		PathResource resource = node.getData();
		if(resource.getResourceType() == ResourceType.FILE) {
			zipFile((FileMetaResource)resource, pathName, zipOut);
		}else if(resource.getResourceType() == ResourceType.DIRECTORY) {
			List<TreeNode<PathResource>> children = node.getChildren();
			for(TreeNode<PathResource> childNode : CollectionUtil.emptyIfNull(children)) {
				zipFile(childNode, pathName + "/" + childNode.getData().getPathName(), zipOut);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eamrf.concurrent.task.AbstractQueuedTask#getLogger()
	 */
	@Override
	public Logger getLogger() {
		return logger;
	}

}
