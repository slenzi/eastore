package org.eamrf.eastore.core.service.tree.file;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.CollectionUtil;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.tree.Tree;
import org.eamrf.eastore.core.tree.TreeNode;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.PathResource;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

/**
 * Builds Tree objects from collections of PathResources.
 * 
 * @author slenzi
 */
@Service
public class PathResourceTreeBuilder {

    @InjectLogger
    private Logger logger;	
	
	public PathResourceTreeBuilder() {
		
	}
	
	/**
	 * Build a top-down tree
	 * 
	 * @param resources - all top-down PathResource data to build a tree. 
	 * @param dirNodeId - the id of the root node of the tree.
	 * @return
	 * @throws ServiceException
	 */
	public Tree<PathResource> buildPathResourceTree(List<PathResource> resources, Long dirNodeId) throws ServiceException {
		
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
	 * Build a bottom-up (leaf node to root node) tree of PathResource objects, but only up
	 * to a specified number of levels.
	 * 
	 * @param resources - all bottom-up PathResource data for the tree
	 * @return
	 * @throws ServiceException
	 */
	public Tree<PathResource> buildParentPathResourceTree(List<PathResource> resources) throws ServiceException {
		
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

}
