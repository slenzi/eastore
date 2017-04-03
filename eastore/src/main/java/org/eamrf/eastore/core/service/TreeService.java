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
import org.eamrf.repository.oracle.ecoguser.eastore.model.ParentChildMap;
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
public class TreeService {

    @InjectLogger
    private Logger logger;
    
    @Autowired
    private ClosureService closureService;
    
    @Autowired
    private FileSystemService fileSystemService;    
	
	public TreeService() {
		
	}
	
	/**
	 * Build a top-down (from root node to leaf nodes) tree of parent-child mappings
	 * 
	 * @param nodeId - Id of the root node.
	 * @return
	 * @throws ServiceException
	 */
	public Tree<ParentChildMap> buildPCMTree(Long nodeId) throws ServiceException {
		
		return buildPCMTree(nodeId, Integer.MAX_VALUE);
		
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
	public Tree<ParentChildMap> buildPCMTree(Long nodeId, int depth) throws ServiceException {
		
		List<ParentChildMap> mappings = closureService.getChildMappings(nodeId, depth);
		
		if(mappings == null || mappings.size() == 0){
			throw new ServiceException("No top-down parent-child mappings for node " + nodeId + 
					". Returned list was null or empty.");
		}
		
		ParentChildMap rootMapping = null;
		Map<Long,List<ParentChildMap>> map = new HashMap<Long,List<ParentChildMap>>();
		for(ParentChildMap pcm : mappings){
			if(pcm.getChildId().equals(nodeId)){
				rootMapping = pcm;
			}
			if(map.containsKey(pcm.getParentId())){
				map.get(pcm.getParentId()).add(pcm);
			}else{
				List<ParentChildMap> children = new ArrayList<ParentChildMap>();
				children.add(pcm);
				map.put(pcm.getParentId(), children);
			}
		}
		
		TreeNode<ParentChildMap> rootNode = new TreeNode<ParentChildMap>();
		rootNode.setData(rootMapping);
		
		addChildrenFromParentChildMap(rootNode, map);
		
		Tree<ParentChildMap> tree = new Tree<ParentChildMap>();
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
	public Tree<ParentChildMap> buildParentPCMTree(Long nodeId) throws ServiceException {
		
		return buildParentPCMTree(nodeId, Integer.MAX_VALUE);
		
	}
	
	/**
	 * Build a bottom-up (leaf node to root node) tree of parent-child mappings
	 * 
	 * @param nodeId
	 * @param levels
	 * @return
	 * @throws ServiceException
	 */
	public Tree<ParentChildMap> buildParentPCMTree(Long nodeId, int levels) throws ServiceException {
		
		List<ParentChildMap> mappings = closureService.getParentMappings(nodeId, levels);
		
		if(mappings == null || mappings.size() == 0){
			throw new ServiceException("No bottom-up parent-child mappings for node " + nodeId + 
					". Returned list was null or empty.");
		}		
		
		ParentChildMap rootMapping = null;
		Map<Long,List<ParentChildMap>> map = new HashMap<Long,List<ParentChildMap>>();
		for(ParentChildMap pcm : mappings){
			if(rootMapping == null){
				// will always be the first one
				rootMapping = pcm;
			}
			if(map.containsKey(pcm.getParentId())){
				map.get(pcm.getParentId()).add(pcm);
			}else{
				List<ParentChildMap> children = new ArrayList<ParentChildMap>();
				children.add(pcm);
				map.put(pcm.getParentId(), children);
			}
		}
		
		TreeNode<ParentChildMap> rootNode = new TreeNode<ParentChildMap>();
		rootNode.setData(rootMapping);
		
		addChildrenFromParentChildMap(rootNode, map);
		
		Tree<ParentChildMap> tree = new Tree<ParentChildMap>();
		tree.setRootNode(rootNode);
		
		return tree;		
		
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
	 * Recursively iterate over map to all all children until there are no more
	 * children to add.
	 * 
	 * @param rootNode
	 * @param map
	 */
	private void addChildrenFromParentChildMap(TreeNode<ParentChildMap> parentNode, Map<Long, List<ParentChildMap>> map) {
		
		TreeNode<ParentChildMap> childTreeNode = null;
		
		Long childNodeId = parentNode.getData().getChildId();
		
		for( ParentChildMap pcm : CollectionUtil.emptyIfNull( map.get(childNodeId) ) ){
			
			childTreeNode = new TreeNode<ParentChildMap>();
			childTreeNode.setData(pcm);
			childTreeNode.setParent(parentNode);
			parentNode.addChildNode(childTreeNode);
			
			addChildrenFromParentChildMap(childTreeNode, map);
			
		}
		
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
	public void logPCMTree(Tree<ParentChildMap> tree){
		
    	logger.info("Tree:\n" + tree.printTree());
    	
    	logger.info("Pre-Order Traversal (top-down):");
    	try {
			Trees.walkTree(tree,
					(treeNode) -> {
						ParentChildMap pcm = treeNode.getData();
						logger.info(pcm.toString());
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
						ParentChildMap pcm = treeNode.getData();
						logger.info(pcm.toString());
					},
					WalkOption.POST_ORDER_TRAVERSAL);
		} catch (TreeNodeVisitException e) {
			logger.error("Error walking tree in post-order (bottom-up) traversal", e);
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
