package org.eamrf.repository.oracle.ecoguser.eastore;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.repository.oracle.ecoguser.eastore.model.DirectoryResource;
import org.eamrf.repository.oracle.ecoguser.eastore.model.FileMetaResource;
import org.eamrf.repository.oracle.ecoguser.eastore.model.Node;
import org.eamrf.repository.oracle.ecoguser.eastore.model.PathResource;
import org.eamrf.repository.oracle.ecoguser.eastore.model.ResourceType;
import org.eamrf.repository.oracle.ecoguser.eastore.model.Store;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
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
	
    /**
     * Maps results from query to PathResource objects
     */
	private final RowMapper<PathResource> resourcePathRowMapper = (rs, rowNum) -> {
		PathResource r = null;
		ResourceType type = ResourceType.getFromString(rs.getString("path_type"));
		if(type == ResourceType.DIRECTORY){
			r = new DirectoryResource();
		}else if(type == ResourceType.FILE){
			r = new FileMetaResource();
			((FileMetaResource)r).setMimeType(rs.getString("mime_type"));
			((FileMetaResource)r).setFileSize(rs.getLong("file_size"));
			String yn = rs.getString("is_file_data_in_db");
			if(yn.toLowerCase().equals("y")){
				((FileMetaResource)r).setIsBinaryInDatabase(true);
			}else{
				((FileMetaResource)r).setIsBinaryInDatabase(false);
			}
		}
		r.setNodeId(rs.getLong("node_id"));
		r.setParentNodeId(rs.getLong("parent_node_id"));
		r.setChildNodeId(rs.getLong("child_node_id"));
		r.setDateCreated(rs.getTimestamp("creation_date"));
		r.setDateUpdated(rs.getTimestamp("updated_date"));
		r.setPathName(rs.getString("path_name"));
		r.setRelativePath(rs.getString("relative_path"));
		r.setResourceType( type );
		r.setStoreId(rs.getLong("store_id"));
		return r;
	};    
    
	public FileSystemRepository() {
		
	}
	
	/**
	 * Get store by its ID.
	 * 
	 * @param storeId
	 * @return
	 * @throws Exception
	 */
	public Store getStoreById(Long storeId) throws Exception {
		
		String sql =
				"select store_id, store_name, store_description, store_path, node_id, "
				+ "max_file_size_in_db, creation_date, updated_date from eas_store "
				+ "where store_id = 1";
		
		return jdbcTemplate.query(sql, new Object[] { storeId },
				(rs) -> {
					Store s = new Store();
					s.setId(rs.getLong("store_id"));
					s.setName(rs.getString("store_name"));
					s.setDescription(rs.getString("store_description"));
					s.setPath(Paths.get(rs.getString("store_path")));
					s.setNodeId(rs.getLong("node_id"));
					s.setMaxFileSizeBytes(rs.getLong("max_file_size_in_db"));
					s.setDateCreated(rs.getTimestamp("creation_date"));
					s.setDateUpdated(rs.getTimestamp("updated_date"));
					return s;
				});		
		
	}
	
	/**
	 * Fetch directory by its node id.
	 * 
	 * @param nodeId
	 * @return
	 * @throws Exception
	 */
	public DirectoryResource getDirectoryById(Long nodeId) throws Exception {
		
		// left joins on eas_directory so we always pull data from eas_node & eas_path_resource
		// in cases where 'nodeId' doesn't actually point to a directory.
		String sql =
				"select " +
				"n.node_id, n.parent_node_id, n.creation_date, n.updated_date, r.store_id, " +
				"r.path_name, r.path_type, r.relative_path " +
				"from eas_node n " +
				"inner join eas_path_resource r " +
				"on n.node_id = r.node_id " +
				"left join eas_directory_resource d " +
				"on r.node_id = d.node_id " +
				"where n.node_id = ?";
		
		return jdbcTemplate.query(sql, new Object[] { nodeId },
				(rs) -> {
					DirectoryResource r = new DirectoryResource();
					r.setNodeId(rs.getLong("node_id"));
					r.setParentNodeId(rs.getLong("parent_node_id"));
					r.setDateCreated(rs.getTimestamp("creation_date"));
					r.setDateUpdated(rs.getTimestamp("updated_date"));
					r.setPathName(rs.getString("path_name"));
					r.setRelativePath(rs.getString("relative_path"));
					r.setResourceType( ResourceType.getFromString(rs.getString("path_type")) );
					r.setStoreId(rs.getLong("store_id"));
					return r;
				});			
		
	}
	
	/**
	 * Get a list of PathResource starting at the specified dirNodeId. With this information
	 * you can build a tree. This will not contain the binary data for files.
	 * 
	 * This is functionally equivalent to ClosureRepository.getChildMappings(Long nodeId)
	 * 
	 * @param dirNodeId
	 * @return
	 * @throws Exception
	 */
	public List<PathResource> getPathResourceTree(Long dirNodeId) throws Exception {
		
		// functionally equivalent to getChildMappings(Long nodeId)
		
		String sql =
			"select " +
			"n.node_id, n.parent_node_id, c.child_node_id, n.creation_date, n.updated_date, r.path_type, " +
			"r.path_name, r.relative_path, r.store_id, fmr.mime_type, fmr.file_size, fmr.is_file_data_in_db " +
			"from eas_closure c " +
			"inner join eas_node n on c.child_node_id = n.node_id " +
			"inner join eas_path_resource r on n.node_id = r.node_id " +
			"left join eas_directory_resource dr on r.node_id = dr.node_id " +
			"left join eas_file_meta_resource fmr on r.node_id = fmr.node_id " +
			"where c.parent_node_id = ? " +
			"order by c.depth, n.node_name";
		
		List<PathResource> resources = jdbcTemplate.query(
				sql, new Object[] { dirNodeId }, resourcePathRowMapper);		
		
		return resources;
		
	}
	
	/**
	 * Get a list of PathResource starting at the specified dirNodeId, but only up to a specified depth.
	 * With this information you can build a tree. This will not contain the binary data for files.
	 * 
	 * This is functionally equivalent to ClosureRepository.getChildMappings(Long nodeId)
	 * 
	 * @param dirNodeId
	 * @return
	 * @throws Exception
	 */
	public List<PathResource> getPathResourceTree(Long dirNodeId, int depth) throws Exception {
		
		// functionally equivalent to getChildMappings(Long nodeId, int depth)
		
		String sql =
			"select " +
			"n.node_id, n.parent_node_id, c.child_node_id, n.creation_date, n.updated_date, r.path_type, " +
			"r.path_name, r.relative_path, r.store_id, fmr.mime_type, fmr.file_size, fmr.is_file_data_in_db " +
			"from eas_closure c " +
			"inner join eas_node n on c.child_node_id = n.node_id " +
			"inner join eas_path_resource r on n.node_id = r.node_id " +
			"left join eas_directory_resource dr on r.node_id = dr.node_id " +
			"left join eas_file_meta_resource fmr on r.node_id = fmr.node_id " +
			"where c.parent_node_id = ? and c.depth <= ? " +
			"order by c.depth, n.node_name";
		
		List<PathResource> resources = jdbcTemplate.query(
				sql, new Object[] { dirNodeId, new Integer(depth) }, resourcePathRowMapper);		
		
		return resources;
		
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
		
		//
		// make sure parentDirNodeId is actually of a directory
		//
		DirectoryResource parentDirectory = getDirectoryById(parentDirNodeId);
		if(parentDirectory.getResourceType() != ResourceType.DIRECTORY){
			throw new Exception("Node ID " + parentDirNodeId + " is not a directory node. "
					+ "Cannot add file to a non-directory node.");
		}
		
		// TODO - make sure directory doesn't already contain a file with the same name
		
		Node newNode = null;
		try {
			newNode = closureRepository.addNode(parentDirNodeId, name);
		} catch (Exception e) {
			throw new Exception("Error adding directory node", e);
		}
		return newNode.getNodeId();
		
	}	
	
	/**
	 * Add a directory node.
	 * 
	 * @param parentDirNodeId - Id of parent directory node.
	 * @param name - name of new directory node.
	 * @return
	 * @throws Exception
	 */
	public DirectoryResource addDirectoryNode(Long parentDirNodeId, String name) throws Exception {
		
		//
		// make sure parentDirNodeId is actually of a directory path resource type
		//
		DirectoryResource parentDirectory = getDirectoryById(parentDirNodeId);
		if(parentDirectory.getResourceType() != ResourceType.DIRECTORY){
			throw new Exception("Node ID " + parentDirNodeId + " is not a directory node. "
					+ "Cannot add sub directory to a non-directory node.");
		}
		
		// TODO - make sure directory doesn't already contain a sub-directory with the same name
		
		//
		// get store
		//
		Store store = getStoreById(parentDirectory.getStoreId());
		
		//
		// add entry to eas_node and eas_closure
		//
		Node newNode = null;
		try {
			newNode = closureRepository.addNode(parentDirNodeId, name);
		} catch (Exception e) {
			throw new Exception("Error adding directory node", e);
		}
		
		PathResource dirResource = new DirectoryResource();
		dirResource.setNodeId(newNode.getNodeId());
		dirResource.setResourceType(ResourceType.DIRECTORY);
		dirResource.setDateCreated(newNode.getDateCreated());
		dirResource.setDateUpdated(newNode.getDateUpdated());
		dirResource.setPathName(name);
		dirResource.setParentNodeId(newNode.getParentNodeId());
		dirResource.setRelativePath(parentDirectory.getRelativePath() + File.separator + name);
		dirResource.setStoreId(store.getId());
		
		//
		// add entry to eas_path_resource
		//
    	// add node to eas_node
		jdbcTemplate.update(
				"insert into eas_path_resource (node_id, store_id, path_name, path_type, relative_path) " +
				"values (?, ?, ?, ?, ?)", dirResource.getNodeId(), dirResource.getStoreId(), dirResource.getPathName(),
				dirResource.getResourceType().getTypeString(), dirResource.getRelativePath());		
		
		//
		// add entry to eas_directory resource
		//
		jdbcTemplate.update(
				"insert into eas_directory_resource (node_id) values (?)", dirResource.getNodeId());		
		
		return (DirectoryResource)dirResource;
		
	}

}
