/**
 * 
 */
package org.eamrf.repository.oracle.ecoguser.eastore;

import java.util.List;

import javax.sql.DataSource;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.repository.oracle.ecoguser.eastore.model.ParentChildMapping;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.zaxxer.hikari.HikariDataSource;

/**
 * @author slenzi
 *
 */
@Repository
public class EAStoreRepository {

    @InjectLogger
    private Logger logger;	
	
	// Spring Boot takes care of initializing the JdbcTemplate and DataSource, so we can simply autowire it! Magic!
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    // no need to autowire this. we simply autowire it so we can print some debug info
    @Autowired
    DataSource dataSource;
	
	public EAStoreRepository() {
		
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

}
