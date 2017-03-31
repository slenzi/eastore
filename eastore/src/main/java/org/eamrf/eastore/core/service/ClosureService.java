package org.eamrf.eastore.core.service;

import java.util.List;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.repository.oracle.ecoguser.eastore.ClosureRepository;
import org.eamrf.repository.oracle.ecoguser.eastore.model.ParentChildMap;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Main service class for interacting with our closure repository.
 * 
 * @author slenzi
 */
@Service
public class ClosureService {

    @InjectLogger
    private Logger logger;
    
    @Autowired
    private ClosureRepository closureRepository;

    /**
     * Fetch top-down parent-child mappings (root node to all child nodes)
     * 
     * This data can be used to build an in-memory tree representation.
     * 
     * @param nodeId
     * @return
     * @throws ServiceException
     */
    public List<ParentChildMap> getMappings(Long nodeId) throws ServiceException {
    	
    	List<ParentChildMap> mappings = null;
    	try {
			mappings = closureRepository.getMappings(nodeId);
		} catch (Exception e) {
			throw new ServiceException(
					"Error getting mappings for node " + nodeId + ". " + e.getMessage(),e);
		}
    	return mappings;
    	
    }
    
    /**
	 * Get top-down parent-child (root node to all child nodes), up to a specified depth.
	 * e.g., depth 1 will get a node node and it's first level children.
	 * 
	 * This data can be used to build an in-memory tree representation.
     * 
     * @param nodeId
     * @return
     * @throws ServiceException
     */
    public List<ParentChildMap> getMappings(Long nodeId, int depth) throws ServiceException {
    	
    	List<ParentChildMap> mappings = null;
    	try {
			mappings = closureRepository.getMappings(nodeId, depth);
		} catch (Exception e) {
			throw new ServiceException(
					"Error getting mappings for node " + nodeId + ", to depth " + depth + ". " + e.getMessage(), e);
		}
    	return mappings;
    	
    }
    
    /**
	 * Fetch bottom-up (leaf node to root node), parent-child mappings. This can
	 * be used to build a tree (or more of a single path) from root to leaf.. 
     * 
     * @param nodeId
     * @return
     * @throws ServiceException
     */
    public List<ParentChildMap> getParentMappings(Long nodeId) throws ServiceException {
    	
    	List<ParentChildMap> mappings = null;
    	try {
			mappings = closureRepository.getParentMappings(nodeId);
		} catch (Exception e) {
			throw new ServiceException(
					"Error getting parent mappings for node " + nodeId + ". " + e.getMessage(), e);
		}
    	return mappings;
    	
    }    
    
    /**
     * Add a new child node
     * 
     * @param parentNodeId - The parent node under which the new child node will be added
     * @param name - node name
     * @param type - node type
     * @throws ServiceException
     */
    public Long addNode(Long parentNodeId, String name, String type) throws ServiceException {
    	
    	Long newNodeId = -1L;
    	try {
			newNodeId = closureRepository.addNode(parentNodeId, name, type);
		} catch (Exception e) {
			throw new ServiceException(
					"Error adding new node under parent node " + parentNodeId + ". " + e.getMessage(), e);
		}
    	return newNodeId;
    }
    
    /**
     * Delete a node, and all children underneath 
     * 
     * @param nodeId
     * @throws ServiceException
     */
    public void deleteNode(Long nodeId) throws ServiceException {
    	
    	try {
			closureRepository.deleteNode(nodeId);
		} catch (Exception e) {
			throw new ServiceException(
					"Error deleting node " + nodeId + ". " + e.getMessage(), e);
		}    	
    	
    }
    
    /**
     * Delete all children under the node, but not the node itself
     * 
     * @param nodeId
     * @throws ServiceException
     */
    public void deleteChildren(Long nodeId) throws ServiceException {
    	
    	try {
			closureRepository.deleteChildren(nodeId);
		} catch (Exception e) {
			throw new ServiceException(
					"Error deleting children for node " + nodeId + ". " + e.getMessage(), e);
		}    	
    	
    }    
    
}
