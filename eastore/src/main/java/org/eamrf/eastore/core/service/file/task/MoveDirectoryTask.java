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
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.FileSystemRepository;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.DirectoryResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.FileMetaResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.PathResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.ResourceType;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task for moving a directory
 * 
 * @author slenzi
 */
public class MoveDirectoryTask extends FileServiceTask<Void> {

	private Logger logger = LoggerFactory.getLogger(MoveDirectoryTask.class);
	
	private DirectoryResource dirToMove;
	private DirectoryResource destDir;
	private boolean replaceExisting;
	private String userId;
	private SecurePathResourceTreeService secureTreeService;
	private FileSystemRepository fileSystemRepository;
	private FileService fileService;
	private ErrorHandler errorHandler;
	
	private int jobCount = 0;
	
	/**
	 * 
	 */
	public MoveDirectoryTask(
			DirectoryResource dirToMove, DirectoryResource destDir, boolean replaceExisting, String userId,
			SecurePathResourceTreeService secureTreeService, FileSystemRepository fileSystemRepository, 
			FileService fileService, ErrorHandler errorHandler) {
			
		this.dirToMove = dirToMove;
		this.destDir = destDir;
		this.replaceExisting = replaceExisting;
		this.userId = userId;
		this.secureTreeService = secureTreeService;
		this.fileSystemRepository = fileSystemRepository;
		this.fileService = fileService;
		this.errorHandler = errorHandler;
	}

	/* (non-Javadoc)
	 * @see org.eamrf.concurrent.task.AbstractQueuedTask#doWork()
	 */
	@Override
	public Void doWork() throws ServiceException {

		// make sure the user is not trying to move a root directory for a store
		if(dirToMove.getParentNodeId().equals(0L)){
			throw new ServiceException("You cannot move a root directory of a store. All stores require a root directory. "
					+ "moveDirId = " + dirToMove.getNodeId() + ", destDirId = " + destDir.getNodeId());
		}
		
		// user must have read and write on parent directory
		DirectoryResource parentDir = fileService.getParentDirectory(dirToMove.getNodeId(), userId);
		if(parentDir != null) {
			if(!parentDir.getCanRead()) {
				errorHandler.handlePermissionDenied(PermissionError.READ, parentDir, userId);
			}
			if(!parentDir.getCanWrite()) {
				errorHandler.handlePermissionDenied(PermissionError.WRITE, parentDir, userId);
			}			
		}
		
		// make sure destDirId is not a child node under moveDirId. Cannot move a directory to under itself.
		boolean isChild = false;
		try {
			isChild = fileSystemRepository.isChild(dirToMove.getNodeId(), destDir.getNodeId());
		} catch (Exception e) {
			throw new ServiceException("Error checking if directory " + destDir.getNodeId() + 
					" is a child directory (at any depth) of directory " + dirToMove.getNodeId(), e);
		}
		if(isChild){
			throw new ServiceException("Cannot move directory " + dirToMove.getNodeId() + " to under directory " + 
					destDir.getNodeId() + " because directory " + destDir.getNodeId() + " is a child of directory " + 
					dirToMove.getNodeId() + ".");
		}

		final Tree<PathResource> fromTree = secureTreeService.buildPathResourceTree(dirToMove, userId);
		
		calculateJobCount(fromTree);		
		
		//DirectoryResource toDir = this.getDirectory(destDirId, userId);
		final Store fromStore = fileService.getStore(dirToMove, userId);
		final Store toStore = fileService.getStore(destDir, userId);
		
		// walk the tree top-down and copy over directories one at a time, then use
		// existing moveFile method.
		moveDirectoryTraversal(fromStore, toStore, fromTree.getRootNode(), destDir, replaceExisting, userId);
		
		// remove from dir and all child directories
		fileService.removeDirectory(dirToMove.getNodeId(), userId, task -> {
			incrementJobsCompleted();
		});
		
		return null;
		
	}
	
	/**
	 * Helper method for moving a directory. This method is called recursively to move all child
	 * directories in the directory tree.
	 * 
	 * @param fromStore
	 * @param toStore
	 * @param pathResourceNode
	 * @param toDir
	 * @param replaceExisting
	 * @param userId
	 * @throws ServiceException
	 */
	private void moveDirectoryTraversal(
			Store fromStore,
			Store toStore, 
			TreeNode<PathResource> pathResourceNode, 
			DirectoryResource toDir, 
			boolean replaceExisting,
			String userId) throws ServiceException {
		
		PathResource resourceToMove = pathResourceNode.getData();
		
		if(resourceToMove.getResourceType() == ResourceType.DIRECTORY){
			
			DirectoryResource dirToMove = (DirectoryResource)resourceToMove;
			
			// user must have read & write access on directory to move
			if(!dirToMove.getCanRead()) {
				errorHandler.handlePermissionDenied(PermissionError.READ, dirToMove, userId);
			}
			if(!dirToMove.getCanWrite()) {
				errorHandler.handlePermissionDenied(PermissionError.WRITE, dirToMove, userId);
			}
			
			// user must have write access on destination directory
			if(!toDir.getCanWrite()) {
				errorHandler.handlePermissionDenied(PermissionError.WRITE, toDir, userId);
			}
			
			// TODO - we perform a case insensitive match. If the directory names differ in case, do we want
			// to keep the directory that already exists (which we do now) or rename it to match exactly of
			// the one we are copying?
			DirectoryResource newToDir = fileService.createCopyOfDirectory(dirToMove, toDir, userId, task -> {
				incrementJobsCompleted();
			});
			
			// move children of the directory (files and sub-directories)
			if(pathResourceNode.hasChildren()){
				for(TreeNode<PathResource> child : pathResourceNode.getChildren()){
					moveDirectoryTraversal(fromStore, toStore, child, newToDir, replaceExisting, userId);
				}
			}
			
		}else if(resourceToMove.getResourceType() == ResourceType.FILE){
			
			fileService.moveFile( (FileMetaResource)resourceToMove, toDir, replaceExisting, userId, task -> {
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
	
		jobCount = 
				(numDirToCopy * 2) + // x2, once for copying them all and once for deleting the sources
				numFileToCopy; // only 1 job per move file task
		
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
		return "Move directory task is " + Math.round(getProgress()) + "% complete (job " + this.getCompletedJobCount() + " of " + this.getJobCount() + " processed)";
	}	

	@Override
	public String getUserId() {
		return userId;
	}
	
}
