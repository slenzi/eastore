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
import org.eamrf.repository.oracle.ecoguser.eastore.model.ParentChildMapping;
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
    private StoreService storeService;
	
	public TreeService() {
		
	}
	
	/**
	 * Build a tree of parent-child mappings
	 * 
	 * @param nodeId - Id of the root node.
	 * @return
	 * @throws ServiceException
	 */
	public Tree<ParentChildMapping> buildTree(Long nodeId) throws ServiceException {
		
		//logger.info("Building parent-child mapping tree for node => " + nodeId);
		
		List<ParentChildMapping> mappings = storeService.getParentChildMappings(nodeId);
		
		if(mappings == null || mappings.size() == 0){
			throw new ServiceException("No parent-child mappings for node " + nodeId + 
					". Returned list was null or empty.");
		}
		
		ParentChildMapping rootMapping = null;
		Map<Long,List<ParentChildMapping>> map = new HashMap<Long,List<ParentChildMapping>>();
		for(ParentChildMapping pcm : mappings){
			//logger.info(pcm.toString());
			if(pcm.getChildId().equals(nodeId)){
				rootMapping = pcm;
			}
			if(map.containsKey(pcm.getParentId())){
				map.get(pcm.getParentId()).add(pcm);
			}else{
				List<ParentChildMapping> children = new ArrayList<ParentChildMapping>();
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
		
		TreeNode<ParentChildMapping> rootNode = new TreeNode<ParentChildMapping>();
		rootNode.setData(rootMapping);
		
		addChildrenFromMap(rootNode, map);
		
		Tree<ParentChildMapping> tree = new Tree<ParentChildMapping>();
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
	private void addChildrenFromMap(TreeNode<ParentChildMapping> parentNode, Map<Long, List<ParentChildMapping>> map) {
		
		TreeNode<ParentChildMapping> childTreeNode = null;
		
		Long childNodeId = parentNode.getData().getChildId();
		//logger.info("Getting children for node => " + childNodeId);
		
		for( ParentChildMapping pcm : CollectionUtil.emptyIfNull( map.get(childNodeId) ) ){
			
			childTreeNode = new TreeNode<ParentChildMapping>();
			childTreeNode.setData(pcm);
			childTreeNode.setParent(parentNode);
			parentNode.addChildNode(childTreeNode);
			
			addChildrenFromMap(childTreeNode, map);
			
		}
		
	}

}
