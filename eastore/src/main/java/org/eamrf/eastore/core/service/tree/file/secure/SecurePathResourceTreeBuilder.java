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

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.CollectionUtil;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.service.security.GateKeeperClientProvider;
import org.eamrf.eastore.core.tree.Tree;
import org.eamrf.eastore.core.tree.TreeNode;
import org.eamrf.gatekeeper.web.service.jaxrs.client.GatekeeperRestClient;
import org.eamrf.gatekeeper.web.service.jaxws.model.Group;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.PathResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store.AccessRule;
import org.eamrf.web.rs.exception.WebServiceException;
import org.slf4j.Logger;
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

    @InjectLogger
    private Logger logger;
    
    @Autowired
    private GateKeeperClientProvider gatekeeperClientProvider;

	public SecurePathResourceTreeBuilder() {

	}
	
	/**
	 * Build a top-down tree
	 * 
	 * @param resources - all top-down PathResource data to build a tree. 
	 * @param userId - User ID used to evaluate access permissions (e.g. CTEP ID). 
	 * @param dirNodeId - the id of the root node of the tree.
	 * @return
	 * @throws ServiceException
	 */
	public Tree<PathResource> buildPathResourceTree(List<PathResource> resources, String userId, Long dirNodeId) throws ServiceException {
		
		Set<String> userGroupCodes = getUserGroupCodes(userId);

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
	 * @return
	 * @throws ServiceException
	 */
	public Tree<PathResource> buildParentPathResourceTree(List<PathResource> resources, String userId, boolean reverse) throws ServiceException {
		
		Set<String> userGroupCodes = getUserGroupCodes(userId);
		
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
		
		if(tree == null || tree.getRootNode() == null || tree.getRootNode().getChildCount() == 0) {
			return null;
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
		
		Tree<PathResource> reverseTree = new Tree<PathResource>();
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
	 * @param readGroups - The read groups from the tree node, or the ones from the last parent that had them
	 * @param writeGroups - The write groups from the tree node, or the ones from the last parent that had them
	 * @param executeGroups - The execute groups from the tree node, or the ones from the last parent that had them
	 * @param hashSet
	 * @param hashSet2
	 * @param map
	 */
	private void addChildrenFromPathResourceMap(
			AccessRule storeAccessRule,
			Set<String> userGroupCodes,
			TreeNode<PathResource> treeNode,
			HashSet<String> readGroups,
			HashSet<String> writeGroups,
			HashSet<String> executeGroups,
			Map<Long, List<PathResource>> map) {
		
		PathResource resource = treeNode.getData();
		
		//
		// set read bit
		//
		if(!CollectionUtil.isEmpty(resource.getReadGroups())) {
			readGroups = resource.getReadGroups();
			// !Collections.disjoint(userGroups, readGroups)
			if(readGroups.stream().anyMatch(userGroupCodes::contains)) {
				resource.setCanRead(true);
			}
		}else{
			if(storeAccessRule == AccessRule.ALLOW) {
				resource.setCanRead(true);
			}else{
				if(!CollectionUtil.isEmpty(readGroups) && readGroups.stream().anyMatch(userGroupCodes::contains)) {
					resource.setCanRead(true);
				}
			}
		}
		
		//
		// set write bit
		//
		if(!CollectionUtil.isEmpty(resource.getWriteGroups())) {
			writeGroups = resource.getWriteGroups();
			// !Collections.disjoint(userGroups, writeGroups)
			if(writeGroups.stream().anyMatch(userGroupCodes::contains)) {
				resource.setCanWrite(true);
			}
		}else{
			if(storeAccessRule == AccessRule.ALLOW) {
				resource.setCanWrite(true);
			}else{
				if(!CollectionUtil.isEmpty(writeGroups) && writeGroups.stream().anyMatch(userGroupCodes::contains)) {
					resource.setCanWrite(true);
				}
			}
		}
		
		//
		// set execute bit
		//
		if(!CollectionUtil.isEmpty(resource.getExecuteGroups())) {
			executeGroups = resource.getExecuteGroups();
			// !Collections.disjoint(userGroups, executeGroups)
			if(executeGroups.stream().anyMatch(userGroupCodes::contains)) {
				resource.setCanExecute(true);
			}
		}else{
			if(storeAccessRule == AccessRule.ALLOW) {
				resource.setCanExecute(true);
			}else{
				if(!CollectionUtil.isEmpty(executeGroups) && executeGroups.stream().anyMatch(userGroupCodes::contains)) {
					resource.setCanExecute(true);
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
			
			addChildrenFromPathResourceMap(storeAccessRule, userGroupCodes, childTreeNode, readGroups, writeGroups, executeGroups, map);
			
		}
		
	}
	
	/**
	 * Fetch users group codes
	 * 
	 * @param userId - user id (ctep id)
	 * @return A set of group codes
	 */
	private Set<String> getUserGroupCodes(String userId) throws ServiceException {
		
		List<Group> groupList = getGatekeeperGroups(userId);
		
		Set<String> groupSet = new HashSet<String>();
		if(!CollectionUtil.isEmpty(groupList)) {
			for(Group group : groupList) {
				groupSet.add(group.getGroupCode());
			}
		}
		
		return groupSet;
	}
	
	/**
	 * Fetch users gatekeeper groups
	 * 
	 * @param userId
	 * @return
	 * @throws ServiceException
	 */
	private List<Group> getGatekeeperGroups(String userId) throws ServiceException {
		
		GatekeeperRestClient client = gatekeeperClientProvider.getRestClient();
		
		List<Group> groupList = null;
		try {
			groupList = client.getGroupsForUser(userId);
		} catch (WebServiceException e) {
			throw new ServiceException("Error fetching Gatekeeper groups for user " + userId + ". " + e.getMessage(), e);
		}
		
		return groupList;
		
	}

}
