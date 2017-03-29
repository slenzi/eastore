/**
 * 
 */
package org.eamrf.repository.oracle.ecoguser.eastore;

import java.util.List;

import org.eamrf.repository.oracle.ecoguser.eastore.model.ParentChildMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author slenzi
 *
 */
@Repository
public class EAStoreRepository {

	// Spring Boot takes care of initializing the JdbcTemplate and DataSource, so we can simply autowire it! Magic!
    @Autowired
    private JdbcTemplate jdbcTemplate;	
	
	public EAStoreRepository() {
		
	}
	
	public List<ParentChildMapping> getParentChildMappings(Long nodeId){
		
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
