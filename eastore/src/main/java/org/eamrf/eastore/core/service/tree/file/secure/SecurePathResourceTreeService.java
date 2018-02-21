/**
 * 
 */
package org.eamrf.eastore.core.service.tree.file.secure;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.aop.profiler.MethodTimer;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.tree.Tree;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.FileSystemRepository;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.DirectoryResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.PathResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.ResourceType;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service class for dealing with PathResources. Uses SecurePathResourceTreeBuilder to set
 * permission bits on resources.
 * 
 * @author slenzi
 */
@Service
public class SecurePathResourceTreeService {

    @InjectLogger
    private Logger logger;  
    
    @Autowired
    private FileSystemRepository fileSystemRepository; 
    
    @Autowired
    private SecurePathResourceTreeBuilder securePathResourceTreeBuilder; 
	
	public SecurePathResourceTreeService() {
		
	}
	
	/**
	 * Build a top-down (from root node to leaf nodes) tree of PathResource objects.
	 * 
	 * Access permissions will be evaluated for the user using the Gatekeeper groups assigned to
	 * the resources.
	 * 
	 * @param nodeId - Id of node which will become the root of the tree.
	 * @param userId - User ID used to evaluate access permissions (e.g. CTEP ID).
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public Tree<PathResource> buildPathResourceTree(Long nodeId, String userId) throws ServiceException {
		
		//DirectoryResource dirResource = this.getDirectory(dirNodeId, userId);
		PathResource resource = getPathResource(nodeId, userId);
		
		return this.buildPathResourceTree(resource, userId, Integer.MAX_VALUE);
		
	}
	
	/**
	 * Build a top-down (from root node to leaf nodes) tree of PathResource objects.
	 * 
	 * Access permissions will be evaluated for the user using the Gatekeeper groups assigned to
	 * the resources.
	 * 
	 * @param dirResource - directory which will become the root of the tree
	 * @param userId - User ID used to evaluate access permissions (e.g. CTEP ID).
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public Tree<PathResource> buildPathResourceTree(DirectoryResource dirResource, String userId) throws ServiceException {
		
		return this.buildPathResourceTree(dirResource, userId, Integer.MAX_VALUE);
		
	}
	
	/**
	 * Build a top-down (from root node to leaf nodes) tree of PathResource objects.
	 * 
	 * Access permissions will be evaluated for the user using the Gatekeeper groups assigned to
	 * the resources.
	 * 
	 * @param nodeId - Id of node which will become the root of the tree.
	 * @param userId - User ID used to evaluate access permissions (e.g. CTEP ID).
	 * @param depth - depth of child nodes to include.
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public Tree<PathResource> buildPathResourceTree(Long nodeId, String userId, int depth) throws ServiceException {
		
		// get the directory node
		PathResource resource = getPathResource(nodeId, userId);
		
		// get the children up to the specified depth
		return this.buildPathResourceTree(resource, userId, depth);	// TODO - was Integer.MAX_VALUE. A bug I think.
		
	}	
	
	/**
	 * Build a top-down (from root node to leaf nodes) tree of PathResource objects,
	 * but only include nodes up to a specified depth.
	 * 
	 * @param dirResource - directory which will become the root of the tree
	 * @param userId - User ID used to evaluate access permissions (e.g. CTEP ID).
	 * @param depth - depth of child nodes to include.
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public Tree<PathResource> buildPathResourceTree(PathResource resource, String userId, int depth) throws ServiceException {
		
		List<PathResource> resources = this.getPathResourceForTree(resource.getNodeId(), depth);
		
		if(resources == null || resources.size() == 0){
			throw new ServiceException("No top-down PathResource tree for resource node " + resource.getNodeId() + 
					". Returned list was null or empty.");
		}
		
		return securePathResourceTreeBuilder.buildPathResourceTree(resources, userId, resource);		
		
	}
	
	/**
	 * Build a bottom-up (leaf node to root node) tree of PathResource objects.
	 * 
	 * @param nodeId - Id of the child node which will become the root of the tree.
	 * @param userId - User ID used to evaluate access permissions (e.g. CTEP ID).
	 * @param reverse - get the tree in reverse order (leaf node becomes root node)
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public Tree<PathResource> buildParentPathResourceTree(Long nodeId, String userId, boolean reverse) throws ServiceException {
		
		return buildParentPathResourceTree(nodeId, userId, Integer.MAX_VALUE, reverse);
		
	}
	
	/**
	 * Build a bottom-up (leaf node to root node) tree of PathResource objects, but only up
	 * to a specified number of levels.
	 * 
	 * In order to properly evaluate the permissions we need ALL parent nodes, all the way
	 * to the root node for the store. This is because permission on resources are inherited
	 * from their parent directory resources.
	 * 
	 * @param nodeId - Id of the child node which will become the root of the tree
	 * @param userId - User ID used to evaluate access permissions (e.g. CTEP ID).
	 * @param levels - number of parent levels to include.
	 * @param reverse - get the tree in reverse order (leaf node becomes root node)
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	private Tree<PathResource> buildParentPathResourceTree(Long nodeId, String userId, int levels, boolean reverse) throws ServiceException {
		
		List<PathResource> resources = getParentPathResourceForTree(nodeId, levels);
		
		if(resources == null || resources.size() == 0){
			throw new ServiceException("No bottom-up PathResource tree for nodeId " + nodeId + 
					". Returned list was null or empty.");
		}
		
		return securePathResourceTreeBuilder.buildParentPathResourceTree(resources, userId, reverse);		
		
	}

	/**
	 * Build a bottom-up (leaf node to root node) tree of PathResource objects.
	 * 
	 * @param storeName - name of the store
	 * @param relativePath - path of resource relative to it's tore
	 * @param userId - User ID used to evaluate access permissions (e.g. CTEP ID).
	 * @param reverse - get the tree in reverse order (leaf node becomes root node)
	 * @return
	 */
	@MethodTimer
	public Tree<PathResource> buildParentPathResourceTree(String storeName, String relativePath, String userId, boolean reverse) throws ServiceException {
		
		PathResource resource = null;
		try {
			resource = fileSystemRepository.getPathResource(storeName, relativePath);
		} catch (Exception e) {
			throw new ServiceException("Error fetching PathResource for storeName " + storeName + 
					" and relativePath " + relativePath + ", " + e.getMessage(), e);
		}
		
		return buildParentPathResourceTree(resource.getNodeId(), userId, reverse);
		
	}
	
	/**
	 * Get a list of PathResource starting at the specified dirNodeId, but only up to a specified depth.
	 * With this information you can build a tree. This will not contain the binary data for files.
	 * 
	 * This is functionally equivalent to ClosureService.getChildMappings(Long nodeId, int depth)
	 * 
	 * @param nodeId
	 * @param depth
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	private List<PathResource> getPathResourceForTree(Long nodeId, int depth) throws ServiceException {
		
		List<PathResource> resources = null;
		try {
			resources = fileSystemRepository.getPathResourceTree(nodeId, depth);
		} catch (Exception e) {
			throw new ServiceException("Error getting PathResource tree for node " + 
					nodeId + ". " + e.getMessage(), e);
		}
		return resources;		
		
	}
	
	/**
	 * Fetch bottom-up (leaf node to root node) PathResource list, up to a specified levels up. This can
	 * be used to build a tree (or more of a single path) from root to leaf.
	 * 
	 * This is functionally equivalent to ClosureService.getParentMappings(Long nodeId, int depth)
	 * 
	 * @param nodeId
	 * @param levels
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	private List<PathResource> getParentPathResourceForTree(Long nodeId, int levels) throws ServiceException {
		
		List<PathResource> resources = null;
		try {
			resources = fileSystemRepository.getParentPathResourceTree(nodeId, levels);
		} catch (Exception e) {
			throw new ServiceException("Error getting PathResource tree for node " + 
					nodeId + ". " + e.getMessage(), e);
		}
		return resources;		
		
	}
	
	/**
	 * Fetch a PathResource by Id, and evaluate the permissions.
	 * 
	 * In order to properly evaluate the permissions we have to fetch the entire parent tree,
	 * since resource inherit permissions from their parent (directory) resources.
	 * 
	 * @param nodeId
	 * @param userId
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public PathResource getPathResource(Long nodeId, String userId) throws ServiceException {
		
		Tree<PathResource> tree = this.buildParentPathResourceTree(nodeId, userId, true);
		
		return tree.getRootNode().getData();
		
	}
	
	/**
	 * Fetch a PathResource by store name and relative path of resource, and evaluate the permissions.
	 * 
	 * In order to properly evaluate the permissions we have to fetch the entire parent tree,
	 * since resource inherit permissions from their parent (directory) resources.
	 * 
	 * @param storeName
	 * @param relativePath
	 * @param userId
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public PathResource getPathResource(String storeName, String relativePath, String userId) throws ServiceException {
		
		Tree<PathResource> tree = this.buildParentPathResourceTree(storeName, relativePath, userId, true);
		
		return tree.getRootNode().getData();
		
	}
	
	/**
	 * TODO - Have to test to make sure this works.
	 * 
	 * Fetch the parent path resource for the specified node. If the node is a root node, and
	 * has no parent, then null will be returned.
	 * 
	 * @param nodeId
	 * @param userId
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public PathResource getParentPathResource(Long nodeId, String userId) throws ServiceException {
		
		Tree<PathResource> tree = this.buildParentPathResourceTree(nodeId, userId, true);
		
		PathResource resource = tree.getRootNode().getData();
		if(resource.getNodeId().equals(nodeId) && resource.getParentNodeId().equals(0L)){
			// this is a root node with no parent
			return null;
		}else{
			// tree is in reverse order, so root node is the child and child is the parent :-)
			return tree.getRootNode().getFirstChild().getData();
		}
		
	}
	
	/**
	 * TODO - Have to test to make sure this works.
	 * 
	 * Fetch the parent path resource for the specified node. If the node is a root node, and
	 * has no parent, then null will be returned.
	 * 
	 * @param storeName
	 * @param relativePath
	 * @param userId
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public PathResource getParentPathResource(String storeName, String relativePath, String userId) throws ServiceException {
		
		Tree<PathResource> tree = this.buildParentPathResourceTree(storeName, relativePath, userId, true);
		
		PathResource resource = tree.getRootNode().getData();
		if(resource.getRelativePath().equals(relativePath) && resource.getParentNodeId().equals(0L)){
			// this is a root node with no parent
			return null;
		}else{
			// tree is in reverse order, so root node is the child and child is the parent :-)
			return tree.getRootNode().getFirstChild().getData();
		}
		
	}
	
	/**
	 * TODO - have to test to make sure this works
	 * 
	 * Fetch the first level children for the path resource. This is only applicable for
	 * directory resources
	 * 
	 * @param dirId - id of directory node
	 * @param userId - id of user completing the action
	 * @return
	 * @throws ServiceException
	 */
	@MethodTimer
	public List<PathResource> getChildPathResources(Long dirId, String userId) throws ServiceException {
		
		// this function will properly evaluate the permissions for the parent directory
		// by fetching the entire parent tree and evaluating all parent permissions.
		Tree<PathResource> tree = this.buildPathResourceTree(dirId, userId, 1);
		
		// collect the first-level children in a list
		if(tree.getRootNode().hasChildren()){
			return tree.getRootNode().getChildren()
				.stream()
				.map((treeNode) -> { return treeNode.getData(); } )
				.collect(Collectors.toList());
		}
		
		return new ArrayList<PathResource>();
		
	}
	
	/**
	 * Fetch first-level resource, by name (case insensitive), from the directory, provided one exists.
	 * 
	 * @param dirId - id of the directory
	 * @param name - name of the file to fetch
	 * @param userId - ID of user completing the action
	 * @return
	 * @throws ServiceException
	 */
	public PathResource getChildResource(Long dirId, String name, ResourceType type, String userId) throws ServiceException {
		
		List<PathResource> childResources = this.getChildPathResources(dirId, userId);
		if(childResources == null || childResources.size() == 0) {
			return null;
		}
		// find the child resource with the specified name
		for(PathResource pr : childResources){
			if(pr.getParentNodeId().equals(dirId)
					&& pr.getResourceType() == type
					&& pr.getPathName().toLowerCase().equals(name.toLowerCase())){
				
				return pr;
				
			}
		}
		return null;
		
	}

}
