package org.eamrf.eastore.core.service;

import java.util.List;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.repository.oracle.ecoguser.eastore.EAStoreRepository;
import org.eamrf.repository.oracle.ecoguser.eastore.model.ParentChildMapping;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Main service class for interacting with stores in the database
 * 
 * @author slenzi
 */
@Service
public class StoreService {

    @InjectLogger
    private Logger logger;
    
    @Autowired
    private EAStoreRepository storeRepository;

    public List<ParentChildMapping> getParentChildMappings(Long nodeId) throws ServiceException {
    	
    	List<ParentChildMapping> mappings = null;
    	mappings = storeRepository.getParentChildMappings(nodeId);
    	return mappings;
    	
    }
    
}
