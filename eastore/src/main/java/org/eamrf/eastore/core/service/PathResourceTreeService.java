package org.eamrf.eastore.core.service;

import java.util.List;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.tree.ToString;
import org.eamrf.eastore.core.tree.Tree;
import org.eamrf.eastore.core.tree.TreeNodeVisitException;
import org.eamrf.eastore.core.tree.Trees;
import org.eamrf.eastore.core.tree.Trees.WalkOption;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.PathResource;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * @author slenzi
 *
 */
@Service
public class PathResourceTreeService {

    @InjectLogger
    private Logger logger;
    
    @Autowired
    private FileSystemService fileSystemService;
    
    @Autowired
    private PathResourceTreeUtil pathResourceUtil;
	
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
		
		return pathResourceUtil.buildPathResourceTree(resources, dirNodeId);		
		
	}
	
	/**
	 * Build a bottom-up (leaf node to root node) tree of PathResource objects.
	 * 
	 * @param dirNodeId - Id of the dir node
	 * @throws ServiceException
	 */
	public Tree<PathResource> buildParentPathResourceTree(Long dirNodeId) throws ServiceException {
		
		return buildParentPathResourceTree(dirNodeId, Integer.MAX_VALUE);
		
	}	
	
	/**
	 * Build a bottom-up (leaf node to root node) tree of PathResource objects, but only up
	 * to a specified number of levels.
	 * 
	 * @param nodeId
	 * @param levels
	 * @return
	 * @throws ServiceException
	 */
	public Tree<PathResource> buildParentPathResourceTree(Long dirNodeId, int levels) throws ServiceException {
		
		List<PathResource> resources = fileSystemService.getParentPathResourceTree(dirNodeId, levels);
		
		if(resources == null || resources.size() == 0){
			throw new ServiceException("No bottom-up PathResource tree for dirNodeId " + dirNodeId + 
					". Returned list was null or empty.");
		}
		
		return pathResourceUtil.buildParentPathResourceTree(resources);		
		
	}	

}
