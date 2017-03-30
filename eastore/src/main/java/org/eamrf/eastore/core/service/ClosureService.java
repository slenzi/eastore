package org.eamrf.eastore.core.service;

import java.util.List;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.repository.oracle.ecoguser.eastore.EAClosureRepository;
import org.eamrf.repository.oracle.ecoguser.eastore.model.ParentChildMapping;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Main service class for interacting with our closure table.
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
    public List<ParentChildMapping> getParentChildMappings(Long nodeId) throws ServiceException {
    	
    	List<ParentChildMapping> mappings = null;
    	try {
			mappings = closureRepository.getParentChildMappings(nodeId);
		} catch (Exception e) {
			throw new ServiceException(e.getMessage(),e);
		}
    	return mappings;
    	
    }
    
    /**
     * Add a new child node
     * 
     * @param parentNodeId - The parent node under which the new child node will be added
     * @param name
     * @throws ServiceException
     */
    public Long addNode(Long parentNodeId, String name) throws ServiceException {
    	
    	// TODO - make sure parent node doesn't already have a child with the same name
    	
    	return closureRepository.addNode(parentNodeId, name);
    	
    }
    
}
