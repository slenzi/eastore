package org.eamrf.eastore.core.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.CollectionUtil;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.tree.Tree;
import org.eamrf.eastore.core.tree.TreeNode;
import org.eamrf.eastore.core.tree.TreeNodeVisitException;
import org.eamrf.eastore.core.tree.Trees;
import org.eamrf.eastore.core.tree.Trees.WalkOption;
import org.eamrf.repository.oracle.ecoguser.eastore.model.Node;
import org.eamrf.repository.oracle.ecoguser.eastore.model.PathResource;
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
		
		PathResource rootResource = null;
		Map<Long,List<PathResource>> map = new HashMap<Long,List<PathResource>>();
		for(PathResource res : resources){
			if(res.getChildNodeId().equals(dirNodeId)){
				rootResource = res;
			}
			if(map.containsKey(res.getParentNodeId())){
				map.get(res.getParentNodeId()).add(res);
			}else{
				List<PathResource> children = new ArrayList<PathResource>();
				children.add(res);
				map.put(res.getParentNodeId(), children);
			}
		}
		
		TreeNode<PathResource> rootNode = new TreeNode<PathResource>();
		rootNode.setData(rootResource);
		
		addChildrenFromPathResourceMap(rootNode, map);
		
		Tree<PathResource> tree = new Tree<PathResource>();
		tree.setRootNode(rootNode);
		
		return tree;		
		
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
		
		PathResource rootResource = null;
		Map<Long,List<PathResource>> map = new HashMap<Long,List<PathResource>>();
		for(PathResource res : resources){
			if(rootResource == null){
				// will always be the first one
				rootResource = res;
			}
			if(map.containsKey(res.getParentNodeId())){
				map.get(res.getParentNodeId()).add(res);
			}else{
				List<PathResource> children = new ArrayList<PathResource>();
				children.add(res);
				map.put(res.getParentNodeId(), children);
			}
		}
		
		TreeNode<PathResource> rootNode = new TreeNode<PathResource>();
		rootNode.setData(rootResource);
		
		addChildrenFromPathResourceMap(rootNode, map);
		
		Tree<PathResource> tree = new Tree<PathResource>();
		tree.setRootNode(rootNode);
		
		return tree;		
		
	}
	
	/**
	 * Recursively iterate over map to all all children until there are no more
	 * children to add.
	 * 
	 * @param parentNode
	 * @param map
	 */
	private void addChildrenFromPathResourceMap(TreeNode<PathResource> parentNode, Map<Long, List<PathResource>> map) {
		
		TreeNode<PathResource> childTreeNode = null;
		
		Long childNodeId = parentNode.getData().getChildNodeId();
		
		for( PathResource res : CollectionUtil.emptyIfNull( map.get(childNodeId) ) ){
			
			childTreeNode = new TreeNode<PathResource>();
			childTreeNode.setData(res);
			childTreeNode.setParent(parentNode);
			parentNode.addChildNode(childTreeNode);
			
			addChildrenFromPathResourceMap(childTreeNode, map);
			
		}
		
	}
	
	/**
	 * logs the tree data (prints tree, plus pre-order and post-order traversals.)
	 * 
	 * @param tree
	 */
	public void logPathResourceTree(Tree<PathResource> tree){
		
    	logger.info("Tree:\n" + tree.printTree());
    	
    	logger.info("Pre-Order Traversal (top-down):");
    	try {
			Trees.walkTree(tree,
					(treeNode) -> {
						PathResource res = treeNode.getData();
						logger.info(res.toString());
					},
					WalkOption.PRE_ORDER_TRAVERSAL);
		} catch (TreeNodeVisitException e) {
			logger.error("Error walking tree in pre-order (top-down) traversal", e);
		}
    	logger.info("");
    	logger.info("Post-Order Traversal (bottom-up):");
    	try {
			Trees.walkTree(tree,
					(treeNode) -> {
						PathResource res = treeNode.getData();
						logger.info(res.toString());
					},
					WalkOption.POST_ORDER_TRAVERSAL);
		} catch (TreeNodeVisitException e) {
			logger.error("Error walking tree in post-order (bottom-up) traversal", e);
		}    	
		
	}	

}
