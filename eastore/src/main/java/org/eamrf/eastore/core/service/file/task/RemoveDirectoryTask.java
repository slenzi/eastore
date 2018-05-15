package org.eamrf.eastore.core.service.file.task;

import java.util.concurrent.atomic.AtomicInteger;

import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.service.file.ErrorHandler;
import org.eamrf.eastore.core.service.file.PermissionError;
import org.eamrf.eastore.core.service.tree.file.secure.SecurePathResourceTreeService;
import org.eamrf.eastore.core.socket.messaging.ResourceChangeService;
import org.eamrf.eastore.core.tree.Tree;
import org.eamrf.eastore.core.tree.TreeNodeVisitException;
import org.eamrf.eastore.core.tree.Trees;
import org.eamrf.eastore.core.tree.Trees.WalkOption;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.FileSystemRepository;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.DirectoryResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.FileMetaResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.PathResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task for deleting a directory
 * 
 * @author slenzi
 *
 */
public class RemoveDirectoryTask extends FileServiceTask<Void> {

	private Logger logger = LoggerFactory.getLogger(RemoveDirectoryTask.class);
	
	private DirectoryResource dirToDelete;
	private DirectoryResource parentDir;
	private String userId;
	private SecurePathResourceTreeService secureTreeService;
	private FileSystemRepository fileSystemRepository;
	private ResourceChangeService resChangeService;
	private ErrorHandler errorHandler;
	
	private int jobCount = -1;
	
	public RemoveDirectoryTask(
			DirectoryResource dirToDelete,
			DirectoryResource parentDir,
			String userId,
			SecurePathResourceTreeService secureTreeService,
			FileSystemRepository fileSystemRepository,
			ResourceChangeService resChangeService,
			ErrorHandler errorHandler) {

		this.dirToDelete = dirToDelete;
		this.parentDir = parentDir;
		this.userId = userId;
		this.secureTreeService = secureTreeService;
		this.fileSystemRepository = fileSystemRepository;
		this.resChangeService = resChangeService;
		this.errorHandler = errorHandler;
		
		//notifyChange();
		
	}
	
	private void calculateJobCount(Tree<PathResource> tree) throws ServiceException {
		
		// calculate number of nodes to delete.
		try {
			jobCount = Trees.nodeCount(tree);
		} catch (TreeNodeVisitException e) {
			throw new ServiceException("Failed to get resource count for source directory, " + e.getMessage(), e);
		}
		
		notifyChange();
		
	}

	@Override
	public Void doWork() throws ServiceException {

		getLogger().info("Deleting Tree:");
		
		Long dirNodeId = dirToDelete.getNodeId();
		
		final Tree<PathResource> tree = secureTreeService.buildPathResourceTree(dirToDelete, userId);
		
		DirectoryResource rootDirToDelete = (DirectoryResource)tree.getRootNode().getData();
		if(rootDirToDelete.getParentNodeId().equals(0L)){
			throw new ServiceException("Node id => " + rootDirToDelete.getNodeId() + " points to a root directory for a store. "
					+ "You cannot use this method to remove a root directory.");
		}				
		
		//pathResTreeLogger.logTree(tree);
		
		calculateJobCount(tree);
		
		
		AtomicInteger completedJobCount = new AtomicInteger(0);
		
		try {
			
			// walk tree, bottom-up, from leafs to root node.
			Trees.walkTree(tree,
				(treeNode) -> {
					
					try {
						if(treeNode.getData().getResourceType() == ResourceType.FILE){
							
							FileMetaResource fileToDelete = (FileMetaResource)treeNode.getData();
							// this works because files inherit permissions from their directory
							if(!fileToDelete.getCanWrite()) {
								errorHandler.handlePermissionDenied(PermissionError.WRITE, fileToDelete, userId);
							}
							fileSystemRepository.removeFile(fileToDelete);
							
							setCompletedJobCount(getTaskId(), completedJobCount.incrementAndGet());
							
							// broadcast resource change message
							if(treeNode.hasParent()) {
								DirectoryResource pdir = (DirectoryResource)treeNode.getParent().getData();
								if(pdir != null) {
									resChangeService.directoryContentsChanged(pdir.getNodeId(), userId);
								}
							}
							
						}else if(treeNode.getData().getResourceType() == ResourceType.DIRECTORY){
							
							// we walk the tree bottom up, so by the time we remove a directory it will be empty
							DirectoryResource nextDirToDelete = (DirectoryResource)treeNode.getData();
							if(!nextDirToDelete.getCanWrite()) {
								errorHandler.handlePermissionDenied(PermissionError.WRITE, nextDirToDelete, userId);
							}									
							fileSystemRepository.removeDirectory(nextDirToDelete);
							
							setCompletedJobCount(getTaskId(), completedJobCount.incrementAndGet());
							
							// broadcast resource change message
							if(treeNode.hasParent()) {
								DirectoryResource pdir = (DirectoryResource)treeNode.getParent().getData();
								if(pdir != null) {
									resChangeService.directoryContentsChanged(pdir.getNodeId(), userId);
								}
							}									
							
						}
					}catch(Exception e){
						
						PathResource presource = treeNode.getData();
						
						throw new TreeNodeVisitException("Error removing path resource with node id => " + 
								presource.getNodeId() + ", of resource type => " + 
								presource.getResourceType().getTypeString() +", " + e.getMessage(), e);
						
					}
					
				},
				WalkOption.POST_ORDER_TRAVERSAL);
			
			resChangeService.directoryContentsChanged(parentDir.getNodeId(), userId);
		
		}catch(TreeNodeVisitException e){
			throw new ServiceException("Encountered error when deleting directory with node id => " + 
					dirNodeId + ". " + e.getMessage(), e);
		}				
		
		return null;		
		
	}

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
		
		if(getJobCount() < 0) {
			return "Remove directory task pending...";
		}else{
			return "Remove directory task is " + Math.round(getProgress()) + "% complete (job " + this.getCompletedJobCount() + " of " + this.getJobCount() + " processed)";
		}
		
	}	

	@Override
	public String getUserId() {
		return userId;
	}
	
}
