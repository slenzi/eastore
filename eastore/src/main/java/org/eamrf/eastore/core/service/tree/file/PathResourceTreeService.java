package org.eamrf.eastore.core.service.tree.file;

import java.util.List;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.tree.Tree;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.PathResource;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for building trees of PathResource objects.
 * 
 * @author slenzi
 * 
 * @deprecated - Gatekeeper security was added. Please use org.eamrf.eastore.core.service.tree.file.secure.SecurePathResourceTreeService
 */
@Service
public class PathResourceTreeService {

    @InjectLogger
    private Logger logger;
    
    @Autowired
    private FileSystemService fileSystemService;
    
    @Autowired
    private PathResourceTreeBuilder pathResourceTreeBuilder;
	
	public PathResourceTreeService() {
		
	}
	
	/**
	 * Build a top-down (from root node to leaf nodes) tree of PathResource objects.
	 * 
	 * @param nodeId - Id of the root node.
	 * @return
	 * @throws ServiceException
	 */
	public Tree<PathResource> buildPathResourceTree(Long nodeId) throws ServiceException {
		
		return buildPathResourceTree(nodeId, Integer.MAX_VALUE);
		
	}	
	
	/**
	 * Build a top-down (from root node to leaf nodes) tree of PathResource objects,
	 * but only include nodes up to a specified depth.
	 * 
	 * @param dirNodeId
	 * @param depth
	 * @return
	 * @throws ServiceException
	 */
	public Tree<PathResource> buildPathResourceTree(Long dirNodeId, int depth) throws ServiceException {
		
		List<PathResource> resources = fileSystemService.getPathResourceTree(dirNodeId, depth);
		
		if(resources == null || resources.size() == 0){
			throw new ServiceException("No top-down PathResource tree for directory node " + dirNodeId + 
					". Returned list was null or empty.");
		}
		
		return pathResourceTreeBuilder.buildPathResourceTree(resources, dirNodeId);		
		
	}
	
	/**
	 * Build a bottom-up (leaf node to root node) tree of PathResource objects.
	 * 
	 * @param nodeId - Id of the child node
	 * @throws ServiceException
	 */
	public Tree<PathResource> buildParentPathResourceTree(Long nodeId) throws ServiceException {
		
		return buildParentPathResourceTree(nodeId, Integer.MAX_VALUE);
		
	}	
	
	/**
	 * Build a bottom-up (leaf node to root node) tree of PathResource objects, but only up
	 * to a specified number of levels.
	 * 
	 * @param nodeId - Id of the child node
	 * @param levels - number of parent levels to include
	 * @return
	 * @throws ServiceException
	 */
	public Tree<PathResource> buildParentPathResourceTree(Long nodeId, int levels) throws ServiceException {
		
		List<PathResource> resources = fileSystemService.getParentPathResourceTree(nodeId, levels);
		
		if(resources == null || resources.size() == 0){
			throw new ServiceException("No bottom-up PathResource tree for nodeId " + nodeId + 
					". Returned list was null or empty.");
		}
		
		return pathResourceTreeBuilder.buildParentPathResourceTree(resources);		
		
	}	

}
