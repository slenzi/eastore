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
	
	public TreeService() {
		
	}
	
	/**
	 * Build a tree of parent-child mappings
	 * 
	 * @param nodeId - Id of the root node.
	 * @return
	 * @throws ServiceException
	 */
	public Tree<ParentChildMap> buildTree(Long nodeId) throws ServiceException {
		
		return buildTree(nodeId, Integer.MAX_VALUE);
		
	}
	
	/**
	 * Build a tree of parent-child mappings, but only include nodes up to a specified depth.
	 * 
	 * @param nodeId
	 * @param depth
	 * @return
	 * @throws ServiceException
	 */
	public Tree<ParentChildMap> buildTree(Long nodeId, int depth) throws ServiceException {
		
		//logger.info("Building parent-child mapping tree for node => " + nodeId);
		
		List<ParentChildMap> mappings = closureService.getMappings(nodeId, depth);
		
		if(mappings == null || mappings.size() == 0){
			throw new ServiceException("No parent-child mappings for node " + nodeId + 
					". Returned list was null or empty.");
		}
		
		ParentChildMap rootMapping = null;
		Map<Long,List<ParentChildMap>> map = new HashMap<Long,List<ParentChildMap>>();
		for(ParentChildMap pcm : mappings){
			//logger.info(pcm.toString());
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
		
		//logger.info("Root mapping = > " + ((rootMapping != null) ? rootMapping.toString() : "null"));
		//for(Long mapId : map.keySet()){
		//	logger.info("Map Node Id = > " + mapId);
		//	for(ParentChildMapping pcm : CollectionUtil.emptyIfNull( map.get(mapId)) ){
		//		logger.info("  " + pcm);
		//	}
		//}
		
		TreeNode<ParentChildMap> rootNode = new TreeNode<ParentChildMap>();
		rootNode.setData(rootMapping);
		
		addChildrenFromMap(rootNode, map);
		
		Tree<ParentChildMap> tree = new Tree<ParentChildMap>();
		tree.setRootNode(rootNode);
		
		//logger.info("\n" + tree.printTree());
		
		return tree;		
		
	}

	/**
	 * Recursively iterate over map to all all children until there are no more
	 * children to add.
	 * 
	 * @param rootNode
	 * @param map
	 */
	private void addChildrenFromMap(TreeNode<ParentChildMap> parentNode, Map<Long, List<ParentChildMap>> map) {
		
		TreeNode<ParentChildMap> childTreeNode = null;
		
		Long childNodeId = parentNode.getData().getChildId();
		//logger.info("Getting children for node => " + childNodeId);
		
		for( ParentChildMap pcm : CollectionUtil.emptyIfNull( map.get(childNodeId) ) ){
			
			childTreeNode = new TreeNode<ParentChildMap>();
			childTreeNode.setData(pcm);
			childTreeNode.setParent(parentNode);
			parentNode.addChildNode(childTreeNode);
			
			addChildrenFromMap(childTreeNode, map);
			
		}
		
	}
	
	/**
	 * logs the tree data (prints tree, plus pre-order and post-order traversals.)
	 * 
	 * @param tree
	 */
	public void logTree(Tree<ParentChildMap> tree){
		
    	logger.info("Tree:\n" + tree.printTree());
    	
    	logger.info("Pre-Order Traversal (top-down):\n");
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
    	
    	logger.info("Post-Order Traversal (bottom-up):\n");
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

}
