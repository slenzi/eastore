package org.eamrf.repository.oracle.ecoguser.eastore;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.repository.oracle.ecoguser.eastore.model.ResourceType;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for manipulating a file system within a database, using out internal node & closure tables.
 * 
 * @author slenzi
 */
@Repository
@Transactional(propagation=Propagation.REQUIRED)
public class FileSystemRepository {

    @InjectLogger
    private Logger logger;
    
	// Spring Boot takes care of initializing the JdbcTemplate and DataSource, so we can simply autowire it! Magic!
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private ClosureRepository closureRepository;    
	
	public FileSystemRepository() {
		
	}
	
	/**
	 * Add a directory node.
	 * 
	 * @param parentDirNodeId - Id of parent directory node.
	 * @param name - name of new directory node.
	 * @return
	 * @throws Exception
	 */
	public Long addFileNode(Long parentDirNodeId, String name) throws Exception {
		
		// TODO - make sure parentDirNodeId is actually of a directory
		
		// TODO - make sure directory doesn't already contain a file with the same name
		
		Long dirNodeId = -10L;
		try {
			dirNodeId = closureRepository.addNode(parentDirNodeId, name, ResourceType.FILE.getTypeString());
		} catch (Exception e) {
			throw new Exception("Error adding directory node", e);
		}
		return dirNodeId;
		
	}	
	
	/**
	 * Add a directory node.
	 * 
	 * @param parentDirNodeId - Id of parent directory node.
	 * @param name - name of new directory node.
	 * @return
	 * @throws Exception
	 */
	public Long addDirectoryNode(Long parentDirNodeId, String name) throws Exception {
		
		// TODO - make sure parentDirNodeId is actually of a directory
		
		// TODO - make sure directory doesn't already contain a subdirectory with the same name
		
		Long dirNodeId = -10L;
		try {
			dirNodeId = closureRepository.addNode(parentDirNodeId, name, ResourceType.DIRECTORY.getTypeString());
		} catch (Exception e) {
			throw new Exception("Error adding directory node", e);
		}
		return dirNodeId;
		
	}

}
