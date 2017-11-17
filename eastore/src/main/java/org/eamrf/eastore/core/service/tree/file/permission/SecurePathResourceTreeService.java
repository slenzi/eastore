/**
 * 
 */
package org.eamrf.eastore.core.service.tree.file.permission;

import java.util.List;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.service.tree.file.FileSystemService;
import org.eamrf.eastore.core.tree.Tree;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.PathResource;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * A version of org.eamrf.eastore.core.service.tree.file.PathResourceTreeService, but in this version we 
 * evaluate user access permissions using Gatekeeper when building the trees.
 * 
 * @author slenzi
 */
@Service
public class SecurePathResourceTreeService {

    @InjectLogger
    private Logger logger;
    
    @Autowired
    private FileSystemService fileSystemService;
    
    @Autowired
    private SecurePathResourceTreeBuilder securePathResourceUtil;
	
	/**
	 * 
	 */
	public SecurePathResourceTreeService() {
		
	}
	
	/**
	 * Build a top-down (from root node to leaf nodes) tree of PathResource objects.
	 * 
	 * Access permissions will be evaluated for the user using the Gatekeeper groups assigned to
	 * the resources.
	 * 
	 * @param dirNodeId - Id of node which will become the root of the tree.
	 * @param userId - User ID used to evaluate access permissions (e.g. CTEP ID).
	 * @return
	 * @throws ServiceException
	 */
	public Tree<PathResource> buildPathResourceTree(Long dirNodeId, String userId) throws ServiceException {
		
		return buildPathResourceTree(dirNodeId, userId, Integer.MAX_VALUE);
		
	}
	
	/**
	 * Build a top-down (from root node to leaf nodes) tree of PathResource objects,
	 * but only include nodes up to a specified depth.
	 * 
	 * @param dirNodeId - Id of node which will become the root of the tree.
	 * @param userId - User ID used to evaluate access permissions (e.g. CTEP ID).
	 * @param depth - depth of child nodes to include.
	 * @return
	 * @throws ServiceException
	 */
	public Tree<PathResource> buildPathResourceTree(Long dirNodeId, String userId, int depth) throws ServiceException {
		
		List<PathResource> resources = fileSystemService.getPathResourceTree(dirNodeId, depth);
		
		if(resources == null || resources.size() == 0){
			throw new ServiceException("No top-down PathResource tree for directory node " + dirNodeId + 
					". Returned list was null or empty.");
		}
		
		return securePathResourceUtil.buildPathResourceTree(resources, userId, dirNodeId);		
		
	}
	
	/**
	 * Build a bottom-up (leaf node to root node) tree of PathResource objects.
	 * 
	 * @param nodeId - Id of the child node which will become the root of the tree.
	 * @param userId - User ID used to evaluate access permissions (e.g. CTEP ID).
	 * @return
	 * @throws ServiceException
	 */
	public Tree<PathResource> buildParentPathResourceTree(Long nodeId, String userId) throws ServiceException {
		
		return buildParentPathResourceTree(nodeId, userId, Integer.MAX_VALUE);
		
	}
	
	/**
	 * Build a bottom-up (leaf node to root node) tree of PathResource objects, but only up
	 * to a specified number of levels.
	 * 
	 * In order to properly evaluate the permissions we need ALL parent nodes, all the way
	 * to the root node for the store. This is because permission on resources are inherited
	 * from their parent directory resources.
	 * 
	 * @param nodeId - Id of the child node which will become the root of the tree
	 * @param userId - User ID used to evaluate access permissions (e.g. CTEP ID).
	 * @param levels - number of parent levels to include.
	 * @return
	 * @throws ServiceException
	 */
	private Tree<PathResource> buildParentPathResourceTree(Long nodeId, String userId, int levels) throws ServiceException {
		
		List<PathResource> resources = fileSystemService.getParentPathResourceTree(nodeId, levels);
		
		if(resources == null || resources.size() == 0){
			throw new ServiceException("No bottom-up PathResource tree for nodeId " + nodeId + 
					". Returned list was null or empty.");
		}
		
		return securePathResourceUtil.buildParentPathResourceTree(resources, userId);		
		
	}	

}
