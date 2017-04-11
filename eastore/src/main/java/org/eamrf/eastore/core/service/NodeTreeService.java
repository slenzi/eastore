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
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * @author slenzi
 */
@Service
public class NodeTreeService {

    @InjectLogger
    private Logger logger;
    
    @Autowired
    private ClosureService closureService;	
	
	public NodeTreeService() {
		
	}

	/**
	 * Build a top-down (from root node to leaf nodes) tree of parent-child mappings
	 * 
	 * @param nodeId - Id of the root node.
	 * @return
	 * @throws ServiceException
	 */
	public Tree<Node> buildNodeTree(Long nodeId) throws ServiceException {
		
		return buildNodeTree(nodeId, Integer.MAX_VALUE);
		
	}
	
	/**
	 * Build a top-down (from root node to leaf nodes) tree of parent-child mappings,
	 * but only include nodes up to a specified depth.
	 * 
	 * @param nodeId
	 * @param depth
	 * @return
	 * @throws ServiceException
	 */
	public Tree<Node> buildNodeTree(Long nodeId, int depth) throws ServiceException {
		
		List<Node> mappings = closureService.getChildMappings(nodeId, depth);
		
		if(mappings == null || mappings.size() == 0){
			throw new ServiceException("No top-down parent-child mappings for node " + nodeId + 
					". Returned list was null or empty.");
		}
		
		Node rootMapping = null;
		Map<Long,List<Node>> map = new HashMap<Long,List<Node>>();
		for(Node n : mappings){
			if(n.getChildNodeId().equals(nodeId)){
				rootMapping = n;
			}
			if(map.containsKey(n.getParentNodeId())){
				map.get(n.getParentNodeId()).add(n);
			}else{
				List<Node> children = new ArrayList<Node>();
				children.add(n);
				map.put(n.getParentNodeId(), children);
			}
		}
		
		TreeNode<Node> rootNode = new TreeNode<Node>();
		rootNode.setData(rootMapping);
		
		addChildrenFromNodeMapping(rootNode, map);
		
		Tree<Node> tree = new Tree<Node>();
		tree.setRootNode(rootNode);
		
		return tree;		
		
	}
	
	/**
	 * Build a bottom-up (leaf node to root node) tree of parent-child mappings
	 * 
	 * @param nodeId
	 * @param levels
	 * @return
	 * @throws ServiceException
	 */
	public Tree<Node> buildParentNodeTree(Long nodeId) throws ServiceException {
		
		return buildParentNodeTree(nodeId, Integer.MAX_VALUE);
		
	}
	
	/**
	 * Build a bottom-up (leaf node to root node) tree of parent-child mappings, but only up
	 * to a specified number of levels.
	 * 
	 * @param nodeId
	 * @param levels
	 * @return
	 * @throws ServiceException
	 */
	public Tree<Node> buildParentNodeTree(Long nodeId, int levels) throws ServiceException {
		
		List<Node> mappings = closureService.getParentMappings(nodeId, levels);
		
		if(mappings == null || mappings.size() == 0){
			throw new ServiceException("No bottom-up node mappings for node " + nodeId + 
					". Returned list was null or empty.");
		}		
		
		Node rootMapping = null;
		Map<Long,List<Node>> map = new HashMap<Long,List<Node>>();
		for(Node n : mappings){
			if(rootMapping == null){
				// will always be the first one
				rootMapping = n;
			}
			if(map.containsKey(n.getParentNodeId())){
				map.get(n.getParentNodeId()).add(n);
			}else{
				List<Node> children = new ArrayList<Node>();
				children.add(n);
				map.put(n.getParentNodeId(), children);
			}
		}
		
		TreeNode<Node> rootNode = new TreeNode<Node>();
		rootNode.setData(rootMapping);
		
		addChildrenFromNodeMapping(rootNode, map);
		
		Tree<Node> tree = new Tree<Node>();
		tree.setRootNode(rootNode);
		
		return tree;		
		
	}
	
	/**
	 * Recursively iterate over map to all all children until there are no more
	 * children to add.
	 * 
	 * @param rootNode
	 * @param map
	 */
	private void addChildrenFromNodeMapping(TreeNode<Node> parentNode, Map<Long, List<Node>> map) {
		
		TreeNode<Node> childTreeNode = null;
		
		Long childNodeId = parentNode.getData().getChildNodeId();
		
		for( Node pcm : CollectionUtil.emptyIfNull( map.get(childNodeId) ) ){
			
			childTreeNode = new TreeNode<Node>();
			childTreeNode.setData(pcm);
			childTreeNode.setParent(parentNode);
			parentNode.addChildNode(childTreeNode);
			
			addChildrenFromNodeMapping(childTreeNode, map);
			
		}
		
	}
	
	/**
	 * logs the tree data (prints tree, plus pre-order and post-order traversals.)
	 * 
	 * @param tree
	 */
	public void logNodeTree(Tree<Node> tree){
		
    	logger.info("Tree:\n" + tree.printTree());
    	
    	logger.info("Pre-Order Traversal (top-down):");
    	try {
			Trees.walkTree(tree,
					(treeNode) -> {
						Node node = treeNode.getData();
						logger.info(node.toString());
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
						Node node = treeNode.getData();
						logger.info(node.toString());
					},
					WalkOption.POST_ORDER_TRAVERSAL);
		} catch (TreeNodeVisitException e) {
			logger.error("Error walking tree in post-order (bottom-up) traversal", e);
		}    	
		
	}	
	
}
