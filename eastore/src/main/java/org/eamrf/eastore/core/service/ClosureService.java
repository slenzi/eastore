package org.eamrf.eastore.core.service;

import java.util.List;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.repository.oracle.ecoguser.eastore.EAClosureRepository;
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
    private EAClosureRepository closureRepository;

    /**
     * Fetch parent-child mappings. This data can be used to build an in-memory tree representation.
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
     * fetch first-level mappings for a node. This is the node and it's immediate 
     * children (not children's children, etc)
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
     * Add a new child node
     * 
     * @param parentNodeId - The parent node under which the new child node will be added
     * @param name - node name
     * @param type - node type
     * @throws ServiceException
     */
    public Long addNode(Long parentNodeId, String name, String type) throws ServiceException {
    	
    	// TODO - make sure parent node doesn't already have a child with the same name
    	
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
