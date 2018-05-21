/**
 * 
 */
package org.eamrf.repository.jdbc.oracle.ecoguser.eastore;

import java.sql.Timestamp;
import java.util.List;

import javax.sql.DataSource;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.DateUtil;
import org.eamrf.eastore.core.aop.profiler.MethodTimer;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Node;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import com.zaxxer.hikari.HikariDataSource;

/**
 * @author slenzi
 */
@Repository
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Exception.class)
public class ClosureRepository {

    @InjectLogger
    private Logger logger;	
	
	// Spring Boot takes care of initializing the JdbcTemplate and DataSource, so we can simply autowire it! Magic!
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    // no need to autowire this. we simply autowire it so we can print some debug info
    @Autowired
    DataSource dataSource;
    
    private final RowMapper<Node> nodeRowMapper = (rs, rowNum) -> {
    	Node n = new Node();
    	n.setNodeId(rs.getLong("node_id"));
    	n.setParentNodeId(rs.getLong("parent_node_id"));
    	n.setChildNodeId(rs.getLong("child_node_id"));
    	n.setNodeName(rs.getString("node_name"));
    	n.setDateCreated(rs.getTimestamp("creation_date"));
    	n.setDateUpdated(rs.getTimestamp("updated_date"));
    	return n;
    };    
	
	public ClosureRepository() {
		
	}
	
	/**
	 * Print some debug info about our datasource, specifically make sure that we are using HikariCP.
	 */
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
	 * Fetch node by ID
	 * 
	 * @param nodeId
	 * @return
	 * @throws Exception
	 */
	public Node getNode(Long nodeId) throws Exception {
		
		List<Node> childResources = getChildMappings(nodeId, 0);
		if(childResources == null){
			throw new Exception("No node found for node id => " + nodeId);
		}else if(childResources.size() != 1){
			// should never get here...
			throw new Exception("Expected 1 node for node id => " + nodeId + ", but fetched " + childResources.size());
		}
		
		Node n = childResources.get(0);
		
		return n;
		
	}
	
	/**
	 * Fetch top-down parent-child mappings (root node to all child nodes).
	 * This data can be used to build an in-memory tree representation.
	 * 
	 * @param nodeId
	 * @return
	 */
	public List<Node> getChildMappings(Long nodeId) throws Exception {
		
		debugDatasource();
		
		String sql = 
			"select " +
			"  n.node_id, n.parent_node_id, c.child_node_id, n.node_name, n.creation_date, n.updated_date " +
			"from " +
			"  eas_closure c " +
			"inner join " +
			"  eas_node n " +
			"on " +
			"  c.child_node_id = n.node_id " +
			"where " +
			"  c.parent_node_id = ? " +
			"order by " +
			"  c.depth, n.node_name";

		List<Node> mappings = jdbcTemplate.query(
				sql, new Object[] { nodeId }, nodeRowMapper);

		return mappings;
		
	}
	
	/**
	 * Get top-down parent-child (root node to all child nodes), up to a specified depth.
	 * e.g., depth 1 will get a node node and it's first level children.
	 * 
	 * @param nodeId
	 * @param depth - number of levels down the tree, from the node, to include.
	 * @return
	 */
	public List<Node> getChildMappings(Long nodeId, int depth) throws Exception {
		
		String sql = 
			"select " +
			"  n.node_id, n.parent_node_id, c.child_node_id, n.node_name, n.creation_date, n.updated_date " +
			"from " +
			"  eas_closure c " +
			"inner join " +
			"  eas_node n " +
			"on " +
			"  c.child_node_id = n.node_id " +
			"where " +
			"  c.parent_node_id = ? " +
			"  and c.depth <= ? " +
			"order by " +
			"  c.depth, n.node_name";

		List<Node> mappings = jdbcTemplate.query(
				sql, new Object[] { nodeId, new Integer(depth) }, nodeRowMapper);

		return mappings;		
		
	}
	
	/**
	 * Fetch bottom-up (leaf node to root node), parent-child mappings. This can
	 * be used to build a tree (or more of a single path) from root to leaf.
	 * 
	 * @param nodeId - 
	 * @return
	 * @throws Exception
	 */
	public List<Node> getParentMappings(Long nodeId) throws Exception {
		
		String sql = 
			"select n2.node_id, n2.parent_node_id, n2.node_id as child_node_id, n2.node_name, n2.creation_date, n2.updated_date " +
			"from " +
			"  eas_node n2, " +
			"  ( " +
			"    select c.parent_node_id, c.depth " +
			"    from eas_closure c " +
			"    join eas_node n " +
			"    on c.child_node_id = n.node_id " +
			"    where c.child_node_id = ? " +
			"  ) nlist " +
			"where " +
			"  n2.node_id = nlist.parent_node_id " +
			"order by " +
			"  nlist.depth desc";

		List<Node> mappings = jdbcTemplate.query(
				sql, new Object[] { nodeId }, nodeRowMapper);

		return mappings;		
		
	}
	
	/**
	 * Fetch bottom-up (leaf node to root node), parent-child mappings, up to a specified levels up.
	 * This can be used to build a tree (or more of a single path) from root to leaf.
	 * 
	 * @param nodeId - 
	 * @param levels - number of levels up (towards root node), from the leaf node, to include.
	 * @return
	 * @throws Exception
	 */
	public List<Node> getParentMappings(Long nodeId, int levels) throws Exception {
		
		String sql = 
			"select n2.node_id, n2.parent_node_id, n2.node_id as child_node_id, n2.node_name, n2.creation_date, n2.updated_date " +
			"from " +
			"  eas_node n2, " +
			"  ( " +
			"    select c.parent_node_id, c.depth " +
			"    from eas_closure c " +
			"    join eas_node n " +
			"    on c.child_node_id = n.node_id " +
			"    where c.child_node_id = ? " +
			"    and c.depth <= ? " +
			"  ) nlist " +
			"where " +
			"  n2.node_id = nlist.parent_node_id " +
			"order by " +
			"  nlist.depth desc";

		List<Node> mappings = jdbcTemplate.query(
				sql, new Object[] { nodeId, new Integer(levels) }, nodeRowMapper);

		return mappings;		
		
	}	

	/**
	 * Add a new node
	 * 
	 * @param parentNodeId - id of parent node under which new node will be added.
	 * @param name - name of the new node
	 * @return the id of the new node
	 */
	@MethodTimer
	public Node addNode(Long parentNodeId, String name) throws Exception {
		
		// get next id from eas_node_id_sequence
		Long newNodeId = getNextNodeId();

		return addNode(newNodeId, parentNodeId, name);
				
	}
	
	/**
	 * Add a new node
	 * 
	 * @param newNodeId - id of the new node
	 * @param parentNodeId - id of the parent node
	 * @param name - node name
	 * @return
	 * @throws Exception
	 */
	@MethodTimer
	public Node addNode(Long newNodeId, Long parentNodeId, String name) throws Exception {
		
		Timestamp dtNow = DateUtil.getCurrentTime();
		
    	// add node to eas_node
		jdbcTemplate.update(
				"insert into eas_node (node_id, parent_node_id, node_name, creation_date, updated_date) " +
				"values (?, ?, ?, ?, ?)", newNodeId, parentNodeId, name, dtNow, dtNow);
    	
		// get next id from eas_link_id_sequence
		Long nextLinkId = getNextLinkId();		
		
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
		
		Node n = new Node();
		n.setNodeId(newNodeId);
		n.setNodeName(name);
		n.setParentNodeId(parentNodeId);
		n.setDateCreated(dtNow);
		n.setDateUpdated(dtNow);
		
		return n;		
		
	}
	
	/**
	 * Delete a node, along with everything under it.
	 * 
	 * @param nodeId
	 * @throws Exception
	 */
	@MethodTimer
	public void deleteNode(Long nodeId) throws Exception {
		
		// get next value from prune ID sequence
		Long newPruneId = getNextPruneId();
		
		// add IDs of all nodes to be deleted to the prune table
		// this includes the ID of the node itself, plus the IDs of ALL child nodes (the entire tree)
		String addToPruneQuery =
			"insert into eas_prune " +
			"select eas_prune_id_sequence.currval as prune_id, child_to_delete from ( " +
			"  select distinct c.child_node_id as child_to_delete " +
			"  from eas_closure c " +
			"  inner join eas_node n " +
			"  on c.child_node_id = n.node_id " + 
			"  where c.parent_node_id = ? " +
			")";
		jdbcTemplate.update(addToPruneQuery, nodeId);
		
		prune(newPruneId);
		
	}
	
	/**
	 * Delete all children under a node
	 * 
	 * @param nodeId
	 * @throws Exception
	 */
	@MethodTimer
	public void deleteChildren(Long nodeId) throws Exception {
		
		// get next value from prune ID sequence
		Long newPruneId = getNextPruneId();
		
		// add IDs of all nodes to be deleted to the prune table
		// this does not include the ID of the node itself, just the IDs of all the child nodes
		String addToPruneQuery =
			"insert into eas_prune " +
			"select eas_prune_id_sequence.currval as prune_id, child_to_delete from ( " +
			"  select distinct c.child_node_id as child_to_delete " +
			"  from eas_closure c " +
			"  inner join eas_node n " +
			"  on c.child_node_id = n.node_id " + 
			"  where c.parent_node_id = ? " +
			"  and c.depth > 0 " +
			")";
		jdbcTemplate.update(addToPruneQuery, nodeId);
		
		prune(newPruneId);
		
	}
	
	/*
	public void moveNode(Long moveNodeId, Long destNodeId) throws Exception {
		
		// TODO - can they move a root node?
		
		// TODO - finish
		
		if(moveNodeId.equals(destNodeId)){
			throw new Exception("Cannot move a node to itself. [moveNodeId=" + moveNodeId + ", destNodeId=" + destNodeId + "]");
		}
		
		// make sure node 'destNodeId' is not a child of 'moveNodeId'. You cannot
		// move a node to under itself, or under one of it's children.
		if(isChild(moveNodeId, destNodeId)){
			throw new Exception("Cannot move " + moveNodeId + " to under node " + destNodeId + 
					". Node " + destNodeId + " is a child of node " + moveNodeId + ". "
							+ "[moveNodeId=" + moveNodeId + ", destNodeId=" + destNodeId + "]");
		}
		
	}
	*/
	
	/**
	 * Returns true if node 'nodeIdB' is a child node of node 'nodeIdA'
	 * 
	 * @param nodeIdA
	 * @param nodeIdB
	 * @return
	 */
	public boolean isChild(Long nodeIdA, Long nodeIdB) throws Exception {
		
		List<Node> parentMappings = getParentMappings(nodeIdB);
		if(parentMappings == null || parentMappings.size() == 0){
			throw new Exception("No 'parent' node mappings for node id = > " + nodeIdB);
		}
		
		return parentMappings.stream()
	            .anyMatch(n -> n.getParentNodeId().equals(nodeIdA));		
		
	}
	
	/**
	 * Delete data from eas_node and eas_clusure linked to the prune_id
	 * 
	 * @param pruneId
	 * @throws Exception
	 */
	private void prune(Long pruneId) throws Exception {
		
		// delete nodes from node table
		String deleteNodeQuery =
			"delete from eas_node n where n.node_id in ( " +
			"	select p.node_id from eas_prune p where p.prune_id = ? " +
			")";
		jdbcTemplate.update(deleteNodeQuery, pruneId);
		
		// delete rows from closure table
		String deleteClosureQuery =
			"delete from " +
			"  eas_closure " +
			"where link_id in ( " +
			"  select " +
			"    distinct l.link_id " +
			"  from " +
			"    eas_closure p " +
			"  inner join " +
			"    eas_closure l " +
			"	on " +
			"    p.parent_node_id = l.parent_node_id " +
			"  inner join " +
			"    eas_closure c " +
			"	on " +
			"    c.child_node_id = l.child_node_id " +
			"  inner join " +
			"    eas_closure to_delete " +
			"  on " +
			"    p.child_node_id = to_delete.parent_node_id " +
			"    and c.parent_node_id = to_delete.child_node_id " +
			"    and to_delete.depth < 2 " +
			"  inner join " +
			"  ( " +
			/* select the IDs of the node we are deleting from our prune table */
			"		select p.node_id as child_to_delete " +
			"		from eas_prune p " +
			"		where p.prune_id = ? " +
			"  ) pruneTable " +
			"  on " +
			"	( " +
			/* for all nodes in the prune table, delete any parent node links and and child node links */
			"	  to_delete.parent_node_id = pruneTable.child_to_delete " +
			"	  or " +
			"	  to_delete.child_node_id = pruneTable.child_to_delete " +
			"	) " +
			")";
		jdbcTemplate.update(deleteClosureQuery, pruneId);		
		
	}
	
	/**
	 * Update node_name and updated_date in eas_node
	 * 
	 * @param n
	 * @throws Exception
	 */
	public void updateNodeMeta(Node n) throws Exception {
		
		jdbcTemplate.update("update eas_node set node_name = ?, updated_date = ? where node_id = ?",
				n.getNodeName(), n.getDateUpdated(), n.getNodeId());
		
	}
	
	/**
	 * Get next id from eas_node_id_sequence
	 * 
	 * @return
	 * @throws Exception
	 */
	public Long getNextNodeId() throws Exception {
		
		Long id = jdbcTemplate.queryForObject(
				"select eas_node_id_sequence.nextval from dual", Long.class);
		
		return id;
		
	}
	
	/**
	 * Get next id from eas_link_id_sequence
	 * 
	 * @return
	 * @throws Exception
	 */
	public Long getNextLinkId() throws Exception {
		
		Long id = jdbcTemplate.queryForObject(
				"select eas_link_id_sequence.nextval from dual", Long.class);
		
		return id;
		
	}
	
	/**
	 * Get next id from eas_prune_id_sequence
	 * 
	 * @return
	 * @throws Exception
	 */
	public Long getNextPruneId() throws Exception {
		
		Long id = jdbcTemplate.queryForObject(
				"select eas_prune_id_sequence.nextval from dual", Long.class);
		
		return id;
		
	}	

}
