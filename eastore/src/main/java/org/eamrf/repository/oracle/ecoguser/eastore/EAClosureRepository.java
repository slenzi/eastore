/**
 * 
 */
package org.eamrf.repository.oracle.ecoguser.eastore;

import java.sql.Timestamp;
import java.util.List;

import javax.sql.DataSource;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.DateUtil;
import org.eamrf.repository.oracle.ecoguser.eastore.model.ParentChildMapping;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.zaxxer.hikari.HikariDataSource;

/**
 * @author slenzi
 */
@Repository
public class EAClosureRepository {

    @InjectLogger
    private Logger logger;	
	
	// Spring Boot takes care of initializing the JdbcTemplate and DataSource, so we can simply autowire it! Magic!
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    // no need to autowire this. we simply autowire it so we can print some debug info
    @Autowired
    DataSource dataSource;
	
	public EAClosureRepository() {
		
	}
	
	private void debugDatasource(){
		
		logger.info("DataSource => " + dataSource);
		
        // If you want to check the HikariDataSource settings
        HikariDataSource hkDataSource = (HikariDataSource)dataSource;
        logger.info("HikariDataSource:");
        logger.info("Max pool size => " + hkDataSource.getMaximumPoolSize());
        logger.info("Connection timeout => " + hkDataSource.getConnectionTimeout());
        logger.info("Idle timeout => " + hkDataSource.getIdleTimeout());
        logger.info("JDBC URL => " + hkDataSource.getJdbcUrl());
		
	}
	
	/**
	 * Fetch parent-child mappings. This data can be used to build an in-memory tree representation.
	 * 
	 * @param nodeId
	 * @return
	 */
	public List<ParentChildMapping> getParentChildMappings(Long nodeId){
		
		debugDatasource();
		
		String sql = 
			"select " +
			"  n.parent_node_id, c.child_node_id, n.name " +
			"from " +
			"  eas_closure c " +
			"inner join " +
			"  eas_node n " +
			"on " +
			"  c.child_node_id = n.node_id " +
			"where " +
			"  c.parent_node_id = ? " +
			"order by " +
			"  c.depth, n.name";

		List<ParentChildMapping> mappings = jdbcTemplate.query(sql, new Object[] { nodeId },
				(rs, rowNum) -> new ParentChildMapping(rs.getLong("parent_node_id"), rs.getLong("child_node_id"),
						rs.getString("name")));

		return mappings;
		
	}

	/**
	 * Add a new node
	 * 
	 * @param parentNodeId - id of parent node under which new node will be added.
	 * @param name - name of the new node
	 * @return the id of the new node
	 */
	public Long addNode(Long parentNodeId, String name) {
		
		// TODO - make sure parent node doesn't already have a child node with the same name
		
		Timestamp dtNow = DateUtil.getCurrentTime();
		
		// get next id from eas_node_id_sequence
		Long newNodeId = jdbcTemplate.queryForObject(
				"select eas_node_id_sequence.nextval from dual", Long.class);
		
    	// add node to eas_node
		jdbcTemplate.update(
				"insert into eas_node (node_id, parent_node_id, name, creation_date, updated_date) " +
				"values (?, ?, ?, ?, ?)", newNodeId, parentNodeId, name, dtNow, dtNow);
    	
		// get next id from eas_link_id_sequence
		Long nextLinkId = jdbcTemplate.queryForObject(
				"select eas_link_id_sequence.nextval from dual", Long.class);		
		
    	// add depth-0 entry to eas_closure table
		jdbcTemplate.update(
				"insert into eas_closure (link_id, parent_node_id, child_node_id, depth) " +
				"values (?, ?, ?, ?)", nextLinkId, newNodeId, newNodeId, 0);		
    	
    	// execute make-parent query which inserts remaining rows in closure table
		// this only works if the depth-0 entry is already in the closure table
		String makeParentQuery =
			"insert into eas_closure (link_id, parent_node_id, child_node_id, depth) " +
			"select " +
			"	eas_link_id_sequence.nextval, p.parent_node_id, c.child_node_id, (p.depth + c.depth + 1) as depth " +
			"from " +
			"	eas_closure p, eas_closure c " +
			"where " +
			"	p.child_node_id = ? and c.parent_node_id = ?";
		jdbcTemplate.update(makeParentQuery, parentNodeId, newNodeId);
		
		return newNodeId;
		
	}

}
