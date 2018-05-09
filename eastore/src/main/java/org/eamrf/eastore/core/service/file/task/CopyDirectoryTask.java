/**
 * 
 */
package org.eamrf.eastore.core.service.file.task;

import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.service.file.ErrorHandler;
import org.eamrf.eastore.core.service.file.FileService;
import org.eamrf.eastore.core.service.file.PermissionError;
import org.eamrf.eastore.core.service.tree.file.secure.SecurePathResourceTreeService;
import org.eamrf.eastore.core.tree.Tree;
import org.eamrf.eastore.core.tree.TreeNode;
import org.eamrf.eastore.core.tree.TreeNodeVisitException;
import org.eamrf.eastore.core.tree.Trees;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.DirectoryResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.FileMetaResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.PathResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.ResourceType;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task for copying a directory
 * 
 * @author slenzi
 */
public class CopyDirectoryTask extends FileServiceTask<Void> {

	private Logger logger = LoggerFactory.getLogger(CopyDirectoryTask.class);
	
	private DirectoryResource fromDir;
	private DirectoryResource toDir;
	private boolean replaceExisting;
	private String userId;
	private SecurePathResourceTreeService secureTreeService;
	private FileService fileService;
	private ErrorHandler errorHandler;
	
	private Tree<PathResource> fromTree;
	
	private int jobCount = 0;
	
	/**
	 * 
	 */
	public CopyDirectoryTask(
			DirectoryResource fromDir, DirectoryResource toDir, boolean replaceExisting, String userId,
			SecurePathResourceTreeService secureTreeService, FileService fileService, ErrorHandler errorHandler) {
		
		this.fromDir = fromDir;
		this.toDir = toDir;
		this.replaceExisting = replaceExisting;
		this.userId = userId;
		this.secureTreeService = secureTreeService;
		this.fileService = fileService;
		this.errorHandler = errorHandler;
		
	}

	/* (non-Javadoc)
	 * @see org.eamrf.concurrent.task.AbstractQueuedTask#doWork()
	 */
	@Override
	public Void doWork() throws ServiceException {

		if(fromDir.getNodeId().equals(toDir.getNodeId())){
			throw new ServiceException("Source directory and destination directory are the same. "
					+ "You cannot copy a directory to itself. copyDirNodeId=" + fromDir.getNodeId() + 
					", destDirNodeId=" + toDir.getNodeId() + ", replaceExisting=" + replaceExisting);
		}
		
		final DirectoryResource fromDirParent = fileService.getParentDirectory(fromDir.getNodeId(), userId);
		if(fromDirParent != null) {
			// if the directory being copied has a parent directory, then the user must have read access
			// on that directory in order to perform copy.
			if(!fromDirParent.getCanRead()) {
				errorHandler.handlePermissionDenied(PermissionError.READ, fromDirParent, userId);
			}
		}
		
		final Store fromStore = fileService.getStore(fromDir, userId);
		final Store toStore = fileService.getStore(fromDir, userId);

		final Tree<PathResource> fromTree = secureTreeService.buildPathResourceTree(fromDir, userId);
		
		calculateJobCount(fromTree);
		
		copyDirectoryTraversal(fromStore, toStore, fromTree.getRootNode(), toDir, replaceExisting, userId);		
		
		return null;
		
	}
	
	/**
	 * Recursively walk the tree to copy all child path resources
	 * 
	 * @param fromStore - store under which the source directory resides
	 * @param toStore - store under which the destination directory resides
	 * @param pathResourceNode - root node for the source tree being copied
	 * @param toDir - the destination directory
	 * @param replaceExisting - 
	 * @param userId
	 * @throws ServiceException
	 */
	private void copyDirectoryTraversal(
			Store fromStore, 
			Store toStore, 
			TreeNode<PathResource> pathResourceNode, 
			DirectoryResource toDir, 
			boolean replaceExisting,
			String userId) throws ServiceException {		
		
		PathResource resourceToCopy = pathResourceNode.getData();
		
		if(resourceToCopy.getResourceType() == ResourceType.DIRECTORY){
			
			DirectoryResource dirToCopy = (DirectoryResource) resourceToCopy;
			
			// user needs read permission on directory to copy
			if(!dirToCopy.getCanRead()) {
				errorHandler.handlePermissionDenied(PermissionError.READ, dirToCopy, userId);
			}
			// user need write permission on destination directory
			if(!toDir.getCanWrite()) {
				errorHandler.handlePermissionDenied(PermissionError.WRITE, toDir, userId);
			}			
			
			// TODO - we perform a case insensitive match. If the directory names differ in case, do we want
			// to keep the directory that already exists (which we do now) or rename it to match exactly of
			// the one we are copying?
			DirectoryResource newToDir = fileService.createCopyOfDirectory(dirToCopy, toDir, userId, task -> {
				incrementJobsCompleted();
			});
			
			// copy over children of the directory (files and sub-directories)
			if(pathResourceNode.hasChildren()){
				for(TreeNode<PathResource> child : pathResourceNode.getChildren()){
					copyDirectoryTraversal(fromStore, toStore, child, newToDir, replaceExisting, userId);
				}
			}
			
		}else if(resourceToCopy.getResourceType() == ResourceType.FILE){
			
			fileService.copyFile( (FileMetaResource)resourceToCopy, toDir, replaceExisting, userId, task -> {
				incrementJobsCompleted();
			});
			
		}
		
	}
	
	private void calculateJobCount(Tree<PathResource> fromTree) throws ServiceException {
		
		int numDirToCopy = 0;
		int numFileToCopy = 0;
		
		try {
			numDirToCopy = Trees.nodeCount(fromTree, DirectoryResource.class);
			numFileToCopy = Trees.nodeCount(fromTree, FileMetaResource.class);
		} catch (TreeNodeVisitException e) {
			throw new ServiceException("Failed to get resource count for source directory, " + e.getMessage(), e);
		}	
	
		jobCount = numDirToCopy + (numFileToCopy * 3); // 3 operations for every file
		
	}	

	/* (non-Javadoc)
	 * @see org.eamrf.concurrent.task.AbstractQueuedTask#getLogger()
	 */
	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public int getJobCount() {
		return jobCount;
	}
	
	@Override
	public String getStatusMessage() {
		return "Copy directory task is " + Math.round(getProgress()) + "% complete (job " + this.getCompletedJobCount() + " of " + this.getJobCount() + " processed)";
	}	

}
