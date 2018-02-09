/**
 * 
 */
package org.eamrf.eastore.core.service.tree.file.secure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eamrf.core.util.CollectionUtil;
import org.eamrf.eastore.core.aop.profiler.MethodTimer;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.service.security.GatekeeperService;
import org.eamrf.eastore.core.tree.Tree;
import org.eamrf.eastore.core.tree.TreeNode;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.DirectoryResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.PathResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store.AccessRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * A version of org.eamrf.eastore.core.service.tree.file.PathResourceTreeBuilder, but in this version we 
 * evaluate user access permissions using Gatekeeper when building the trees.
 * 
 * @author slenzi
 */
@Service
public class SecurePathResourceTreeBuilder {
    
    @Autowired
    private GatekeeperService gatekeeperService;

	public SecurePathResourceTreeBuilder() {

	}
	
	/**
	 * Build a top-down tree
	 * 
	 * @param resources - all top-down PathResource data to build a tree. 
	 * @param userId - User ID used to evaluate access permissions (e.g. CTEP ID). 
	 * @param dirResource - the directory resource that is the root of the tree we are building. The directory resource
	 * should have its permissions evaluated (read, write, and execute bits) already set for the user. These bits are
	 * needed in order to properly evaluate the permissions for all the child resources.
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public Tree<PathResource> buildPathResourceTree(List<PathResource> resources, String userId, PathResource dirResource) throws ServiceException {
		
		Set<String> userGroupCodes = gatekeeperService.getUserGroupCodes(userId);
		//Set<String> userGroupCodes = new HashSet<String>();

		PathResource rootResource = null;
		Map<Long,List<PathResource>> map = new HashMap<Long,List<PathResource>>();
		for(PathResource res : resources){
			if(res.getChildNodeId().equals(dirResource.getNodeId())){
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
		
		AccessRule storeAccessRule = rootResource.getStore().getAccessRule();
		
		TreeNode<PathResource> rootNode = new TreeNode<PathResource>();
		rootNode.setData(rootResource);
		
		addChildrenFromPathResourceMap(
				storeAccessRule,
				userGroupCodes,
				rootNode,
				dirResource.getReadGroups(),
				dirResource.getWriteGroups(),
				dirResource.getExecuteGroups(),
				dirResource.getCanRead(),
				dirResource.getCanWrite(),
				dirResource.getCanExecute(),
				map);
		
		Tree<PathResource> tree = new Tree<PathResource>();
		tree.setRootNode(rootNode);
		
		return tree;		
		
	}
	
	/**
	 * Build a bottom-up (leaf node to root node) tree of PathResource objects, but only up
	 * to a specified number of levels.
	 * 
	 * @param resources - all bottom-up PathResource data for the tree
	 * @param userId - User ID used to evaluate access permissions (e.g. CTEP ID).
	 * @param reverese - pass true to return the tree in reverse order (leaf node becomes root,a nd root becomes leaf)
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public Tree<PathResource> buildParentPathResourceTree(List<PathResource> resources, String userId, boolean reverse) throws ServiceException {
		
		Set<String> userGroupCodes = gatekeeperService.getUserGroupCodes(userId);
		//Set<String> userGroupCodes = new HashSet<String>();
		
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
		
		AccessRule storeAccessRule = rootResource.getStore().getAccessRule();
		
		TreeNode<PathResource> rootNode = new TreeNode<PathResource>();
		rootNode.setData(rootResource);
		
		addChildrenFromPathResourceMap(
				storeAccessRule,
				userGroupCodes,
				rootNode,
				rootNode.getData().getReadGroups(),
				rootNode.getData().getWriteGroups(),
				rootNode.getData().getExecuteGroups(),
				false,
				false,
				false,
				map);
		
		Tree<PathResource> tree = new Tree<PathResource>();
		tree.setRootNode(rootNode);
		
		if(reverse) {
			return reverseSingleChildTree(tree);
		}
		
		return tree;		
		
	}
	
	/**
	 * Given a tree where each node has only one child, reverse the tree so the leaf node
	 * becomes the parent.
	 * 
	 * Essentially the same as reversing a doubly linked list
	 * 
	 * @param tree
	 * @return
	 */
	private Tree<PathResource> reverseSingleChildTree(Tree<PathResource> tree) throws ServiceException {
		
		if(tree == null || tree.getRootNode() == null) {
			return null;
		}
		
		Tree<PathResource> reverseTree = new Tree<PathResource>();
		if(!tree.getRootNode().hasChildren()) {
			reverseTree.setRootNode(tree.getRootNode());
			return reverseTree;
		}
		
		TreeNode<PathResource> temp = null;
		TreeNode<PathResource> curr = tree.getRootNode();
		while(curr != null) {
			if(curr.getChildCount() > 1) {
				throw new ServiceException("Cannot reverse tree because node " + curr.getData().getNodeId() + " has more than one child.");
			}
			temp = curr.getParent();
			curr.setParent(curr.getFirstChild());
			curr.setChildren(null);
			curr.addChildNode(temp);
			curr = curr.getParent();
		}
		
		
		reverseTree.setRootNode(temp.getParent());
		
		return reverseTree;
		
	}	
	
	/**
	 * Recursively iterate over map to all all children until there are no more children to add.
	 * 
	 * Also evaluate the access permissions using the provided set of user group codes, and set 
	 * the read, write, and execute bits on each resource.
	 * 
	 * @param storeAccessRule - Access rule for store
	 * @param userGroupCodes - Users groups codes used to evaluate read, write, and execute permissions
	 * @param treeNode - The current node in the tree from which we start
	 * @param lastReadGroups - The read groups from the tree node, or the ones from the last parent that had them
	 * @param lastWriteGroups - The write groups from the tree node, or the ones from the last parent that had them
	 * @param lastExecuteGroups - The execute groups from the tree node, or the ones from the last parent that had them
	 * @param lastReadBit - The last (possibly from parent) read bit
	 * @param lastWriteBit - The last (possibly from parent) write bit
	 * @param lastExecuteBit - The last (possibly from parent) execute bit
	 * @param map
	 */
	private void addChildrenFromPathResourceMap(
			AccessRule storeAccessRule,
			Set<String> userGroupCodes,
			TreeNode<PathResource> treeNode,
			HashSet<String> lastReadGroups,
			HashSet<String> lastWriteGroups,
			HashSet<String> lastExecuteGroups,
			Boolean lastReadBit,
			Boolean lastWriteBit,
			Boolean lastExecuteBit,
			Map<Long, List<PathResource>> map) {
		
		PathResource resource = treeNode.getData();
		
		// -----------------------------------------------------------------
		// set read bit
		// -----------------------------------------------------------------
		
		// use read groups from resource
		if(!CollectionUtil.isEmpty(resource.getReadGroups())) {
			lastReadGroups = resource.getReadGroups();
			// !Collections.disjoint(userGroups, readGroups)
			if(lastReadGroups.stream().anyMatch(userGroupCodes::contains)) {
				resource.setCanRead(true);
			}
		
		// otherwise use last read groups or last read bit to determine access
		}else{ 
			
			// automatically set read access to true of store access rule is set to 'allow'
			if(storeAccessRule == AccessRule.ALLOW) {
				resource.setCanRead(true);
				
			// store access rule is deny
			}else{
				
				// use last read groups to determine access if we have them
				if(!CollectionUtil.isEmpty(lastReadGroups) && lastReadGroups.stream().anyMatch(userGroupCodes::contains)) {
					resource.setCanRead(true);
					
				// otherwise use last read bit
				}else{
					if(lastReadBit){
						resource.setCanRead(true);
					}
				}
			}
		}
		
		// -----------------------------------------------------------------
		// set write bit
		// -----------------------------------------------------------------
		
		// use write groups from resource
		if(!CollectionUtil.isEmpty(resource.getWriteGroups())) {
			lastWriteGroups = resource.getWriteGroups();
			// !Collections.disjoint(userGroups, writeGroups)
			if(lastWriteGroups.stream().anyMatch(userGroupCodes::contains)) {
				resource.setCanWrite(true);
			}
			
		// otherwise use last write groups or last write bit to determine access
		}else{
			
			// automatically set write access to true of store access rule is set to 'allow'
			if(storeAccessRule == AccessRule.ALLOW) {
				resource.setCanWrite(true);
				
			// store access rule is deny	
			}else{
				
				// use last write groups to determine access if we have them
				if(!CollectionUtil.isEmpty(lastWriteGroups) && lastWriteGroups.stream().anyMatch(userGroupCodes::contains)) {
					resource.setCanWrite(true);
				
				// otherwise use last write bit
				}else{
					if(lastWriteBit){
						resource.setCanWrite(true);
					}					
				}
			}
		}
		
		// -----------------------------------------------------------------
		// set execute bit
		// -----------------------------------------------------------------
		
		// use execute groups from resource
		if(!CollectionUtil.isEmpty(resource.getExecuteGroups())) {
			lastExecuteGroups = resource.getExecuteGroups();
			// !Collections.disjoint(userGroups, executeGroups)
			if(lastExecuteGroups.stream().anyMatch(userGroupCodes::contains)) {
				resource.setCanExecute(true);
			}
			
		// otherwise use last execute groups or last execute bit to determine access
		}else{
			
			// automatically set execute access to true of store access rule is set to 'allow'
			if(storeAccessRule == AccessRule.ALLOW) {
				resource.setCanExecute(true);
				
			// store access rule is deny	
			}else{
				
				// use last execute groups to determine access if we have them
				if(!CollectionUtil.isEmpty(lastExecuteGroups) && lastExecuteGroups.stream().anyMatch(userGroupCodes::contains)) {
					resource.setCanExecute(true);
				
					// otherwise use last execute bit
					}else{
						if(lastExecuteBit){
							resource.setCanExecute(true);
						}					
					}
			}
		}		
		
		//
		// set read, write, execute bits for children
		//
		TreeNode<PathResource> childTreeNode = null;
		Long childNodeId = treeNode.getData().getChildNodeId();
		for( PathResource res : CollectionUtil.emptyIfNull( map.get(childNodeId) ) ){
			
			childTreeNode = new TreeNode<PathResource>();
			childTreeNode.setData(res);
			childTreeNode.setParent(treeNode);
			treeNode.addChildNode(childTreeNode);
			
			addChildrenFromPathResourceMap(
					storeAccessRule,
					userGroupCodes, 
					childTreeNode, 
					lastReadGroups,
					lastWriteGroups,
					lastExecuteGroups,
					resource.getCanRead(),
					resource.getCanWrite(),
					resource.getCanExecute(),
					map);
			
		}
		
	}

}
