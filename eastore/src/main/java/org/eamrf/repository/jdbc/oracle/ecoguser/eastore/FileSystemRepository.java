package org.eamrf.repository.jdbc.oracle.ecoguser.eastore;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.CodeTimer;
import org.eamrf.core.util.DateUtil;
//import org.eamrf.core.util.FileUtil;
import org.eamrf.eastore.core.aop.profiler.MethodTimer;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.service.io.FileIOService;
import org.eamrf.eastore.core.service.tree.file.PathResourceUtil;
import org.eamrf.eastore.core.service.tree.file.PathResourceTreeBuilder;
import org.eamrf.eastore.core.tree.Tree;
import org.eamrf.eastore.core.tree.Trees;
import org.eamrf.eastore.core.tree.Trees.WalkOption;
import org.eamrf.repository.jdbc.SpringJdbcUtil;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.BinaryResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.DirectoryResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.FileMetaResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Node;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.PathResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.ResourceType;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store.AccessRule;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.AbstractLobCreatingPreparedStatementCallback;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static java.lang.Math.toIntExact;

/**
 * Repository for manipulating a file system within a database, using our internal node & closure tables.
 * 
 * @author slenzi
 */
@Repository
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Exception.class)
public class FileSystemRepository {

    @InjectLogger
    private Logger logger;
    
	// Spring Boot takes care of initializing the JdbcTemplate and DataSource, so we can simply autowire it! Magic!
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private ClosureRepository closureRepository;
    
    //@Autowired
    //private PathResourceUtil pathResourceUtil;
    //private PathResourceUtil pathResourceUtil = new PathResourceUtil();
    
    @Autowired
    private PathResourceTreeBuilder pathResourceTreeBuilder;
    
    @Autowired
    private FileIOService fileService;
    
    // common query element used by several methods below
    private final String SQL_PATH_RESOURCE_COMMON =
			"select " +
			"n.node_id, n.parent_node_id, c.child_node_id, n.creation_date, n.updated_date, r.path_type, " +  
			"r.path_name, r.relative_path, r.store_id, r.path_desc, r.read_group_1, r.write_group_1, execute_group_1, " +
			"fmr.mime_type, fmr.file_size, fmr.is_file_data_in_db, " + 
			"s.store_id, s.store_name, s.store_description, s.store_path, s.node_id as store_root_node_id, " +
			"s.max_file_size_in_db, s.access_rule, s.creation_date as store_creation_date, s.updated_date as store_updated_date " +
			"from eas_closure c " +
			"inner join eas_node n on c.child_node_id = n.node_id " +  
			"inner join eas_path_resource r on n.node_id = r.node_id " +
			"inner join eas_store s on r.store_id = s.store_id " +
			"left join eas_directory_resource dr on r.node_id = dr.node_id " +  
			"left join eas_file_meta_resource fmr on r.node_id = fmr.node_id ";
    
    private final String SQL_STORES_COMMON =
    		"select " +
    		"s.store_id, s.store_name, s.store_description, s.store_path, s.node_id, " +
    		"s.max_file_size_in_db, s.access_rule, s.creation_date as store_creation_date, s.updated_date as store_updated_date, " +
    		"r.path_name, r.path_type, r.relative_path, r.path_desc, r.read_group_1, r.write_group_1, execute_group_1, n.node_name, " +
    		"n.creation_date as node_creation_date, n.updated_date as node_updated_date, n.parent_node_id " +
    		"from eas_store s " +
    		"inner join eas_path_resource r on r.node_id = s.node_id " +
    		"inner join eas_directory_resource dr on r.node_id = dr.node_id " +
    		"inner join eas_node n on n.node_id = r.node_id";  		
	
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
		r.setDesc(rs.getString("path_desc"));
		r.setReadGroup1(rs.getString("read_group_1"));
		r.setWriteGroup1(rs.getString("write_group_1"));
		r.setExecuteGroup1(rs.getString("execute_group_1"));

		
		Store s = new Store();
		s.setId(rs.getLong("store_id"));
		s.setName(rs.getString("store_name"));
		s.setDescription(rs.getString("store_description"));
		s.setMaxFileSizeBytes(rs.getLong("max_file_size_in_db"));
		s.setAccessRule(AccessRule.fromString(rs.getString("access_rule")));
		s.setPath(Paths.get(rs.getString("store_path")));
		s.setNodeId(rs.getLong("store_root_node_id"));
		s.setDateCreated(rs.getTimestamp("store_creation_date"));
		s.setDateUpdated(rs.getTimestamp("store_updated_date"));
		
		r.setStore(s);
		
		return r;
	};
	
    /**
     * Maps results from query to Store objects
     */
	private final RowMapper<Store> storeRowMapper = (rs, rowNum) -> {
		
		Store s = new Store();
		
		s.setId(rs.getLong("store_id"));
		s.setName(rs.getString("store_name"));
		s.setDescription(rs.getString("store_description"));
		s.setPath(Paths.get(rs.getString("store_path")));
		s.setNodeId(rs.getLong("node_id"));
		s.setMaxFileSizeBytes(rs.getLong("max_file_size_in_db"));
		s.setAccessRule(AccessRule.fromString(rs.getString("access_rule")));
		s.setDateCreated(rs.getTimestamp("store_creation_date"));
		s.setDateUpdated(rs.getTimestamp("store_updated_date"));
		
		ResourceType type = ResourceType.getFromString(rs.getString("path_type"));
		
		DirectoryResource r = new DirectoryResource();
		r.setNodeId(rs.getLong("node_id"));
		r.setParentNodeId(rs.getLong("parent_node_id"));
		//r.setChildNodeId(rs.getLong("child_node_id")); // don't have this value
		r.setDateCreated(rs.getTimestamp("node_creation_date"));
		r.setDateUpdated(rs.getTimestamp("Node_updated_date"));
		r.setPathName(rs.getString("path_name"));
		r.setRelativePath(rs.getString("relative_path"));
		r.setResourceType( type );
		r.setStoreId(rs.getLong("store_id"));
		r.setDesc(rs.getString("path_desc"));
		r.setReadGroup1(rs.getString("read_group_1"));
		r.setWriteGroup1(rs.getString("write_group_1"));
		r.setExecuteGroup1(rs.getString("execute_group_1"));
		
		s.setRootDir(r);
		
		return s;
		
	};	
    
	public FileSystemRepository() {
		
	}
	
	/**
	 * Fetch store by store id
	 * 
	 * @param storeId
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public Store getStoreById(Long storeId) throws Exception {
		
		String sql = SQL_STORES_COMMON + " where s.store_id = ?";
		
		final ResultSetExtractor<Store> storeResultExtractor = SpringJdbcUtil.getSingletonExtractor(storeRowMapper);
		
		return jdbcTemplate.query(sql, storeResultExtractor, new Object[] { storeId });		
		
	}
	
	/**
	 * Fetch store by name
	 * 
	 * @param storeName
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public Store getStoreByName(String storeName) throws Exception {
		
		String sql = SQL_STORES_COMMON + " where lower(s.store_name) = ?";
		
		final ResultSetExtractor<Store> storeResultExtractor = SpringJdbcUtil.getSingletonExtractor(storeRowMapper);
		
		return jdbcTemplate.query(sql, storeResultExtractor, new Object[] { storeName });	
		
	}
	
	/**
	 * Fetch the store for the resource, first from the resource object itself, but if it doens't
	 * exists then fetch it from the database.
	 * 
	 * @param resource
	 * @return
	 * @throws Exception
	 */
	public Store getStoreForResource(PathResource resource) throws Exception {
		if(resource == null) {
			return null;
		}
		Store store = resource.getStore();
		if(store != null) {
			return store;
		}
		Long storeId = resource.getStoreId();
		if(storeId == null) {
			return null;
		}
		store = this.getStoreById(storeId);
		resource.setStore(store);
		return store;
	}
	
	/**
	 * Fetch all stores
	 * 
	 * @return
	 * @throws Exception
	 */
	public List<Store> getStores() throws Exception {
		
		String sql = SQL_STORES_COMMON;
		
		List<Store> stores = jdbcTemplate.query(sql, storeRowMapper);			
		
		return stores;
		
	}
	
	/**
	 * Update store name & desc.
	 * 
	 * @param storeToEdit - the store to edit
	 * @param storeName - new name
	 * @param storeDesc - new desc
	 */
	public void updateStore(Store storeToEdit, String storeName, String storeDesc) {

		// TODO - add update for store path, max file size db, and access rule
		
		jdbcTemplate.update("update eas_store set store_name = ?, store_description = ? where store_id = ?",
				storeName, storeDesc, storeToEdit.getId());			
		
	}	
	
	/**
	 * Get a list of PathResource starting at the specified dirNodeId. With this information
	 * you can build a tree. This will not contain the binary data for files.
	 * 
	 * This is functionally equivalent to ClosureRepository.getChildMappings(Long nodeId)
	 * 
	 * @param nodeId
	 * @return
	 * @throws Exception
	 */
	//@MethodTimer
	public List<PathResource> getPathResourceTree(Long nodeId) throws Exception {
		
		// functionally equivalent to ClosureRepository.getChildMappings(Long nodeId)	
		
		String sql =
			SQL_PATH_RESOURCE_COMMON +
			"where c.parent_node_id = ? " +
			"order by c.depth, n.node_name";
		
		List<PathResource> resources = jdbcTemplate.query(
				sql, new Object[] { nodeId }, resourcePathRowMapper);		
		
		return resources;
		
	}
	
	/**
	 * Get a list of PathResource starting at the specified dirNodeId, but only up to a specified depth.
	 * With this information you can build a tree. This will not contain the binary data for files.
	 * 
	 * This is functionally equivalent to ClosureRepository.getChildMappings(Long nodeId)
	 * 
	 * @param nodeId
	 * @return
	 * @throws Exception
	 */
	//@MethodTimer
	public List<PathResource> getPathResourceTree(Long nodeId, int depth) throws Exception {
		
		// functionally equivalent to ClosureRepository.getChildMappings(Long nodeId, int depth)

		String sql =
			SQL_PATH_RESOURCE_COMMON +
			"where c.parent_node_id = ? and c.depth <= ? " +
			"order by c.depth, n.node_name";
		
		List<PathResource> resources = jdbcTemplate.query(
				sql, new Object[] { nodeId, new Integer(depth) }, resourcePathRowMapper);		
		
		return resources;
		
	}
	
	/**
	 * Fetch bottom-up (leaf node to root node), PathResource list. This can
	 * be used to build a tree (or more of a single path) from root to leaf.
	 * 
	 * functionally equivalent to ClosureRepository.getParentMappings(Long nodeId)
	 * 
	 * @param nodeId
	 * @return
	 * @throws Exception
	 */
	//@MethodTimer
	public List<PathResource> getParentPathResourceTree(Long nodeId) throws Exception {
		
		// functionally equivalent to ClosureRepository.getParentMappings(Long nodeId)	
		
		String sql =
			"select " +
			"  n2.node_id, n2.parent_node_id, n2.node_id as child_node_id, n2.node_name, n2.creation_date, n2.updated_date, " + 
			"  r.path_type, r.path_name, r.relative_path, r.store_id, r.path_desc, r.read_group_1, r.write_group_1, execute_group_1, " +
			"  fmr.mime_type, fmr.file_size, fmr.is_file_data_in_db, " +
			"  s.store_id, s.store_name, s.store_description, s.store_path, s.node_id as store_root_node_id, " +
			"  s.max_file_size_in_db, s.access_rule, s.creation_date as store_creation_date, s.updated_date as store_updated_date " +  
			"from " +
			"  eas_node n2 inner join " +  
			"  (  " +
			"	select c.parent_node_id, c.depth " + 
			"	from eas_closure c  " +
			"	join eas_node n  " +
			"	on c.child_node_id = n.node_id " + 
			"	where c.child_node_id = ?  " +
			"  ) nlist on (n2.node_id = nlist.parent_node_id) " + 
			"inner join eas_path_resource r on n2.node_id = r.node_id " +
			"inner join eas_store s on r.store_id = s.store_id " +
			"left join eas_directory_resource dr on r.node_id = dr.node_id " + 
			"left join eas_file_meta_resource fmr on r.node_id = fmr.node_id  " +
			"order by  " +
			"  nlist.depth desc";
		
		List<PathResource> resources = jdbcTemplate.query(
				sql, new Object[] { nodeId }, resourcePathRowMapper);		
		
		return resources;		
		
	}
	
	/**
	 * Fetch bottom-up (leaf node to root node) PathResource list, up to a specified levels up. This can
	 * be used to build a tree (or more of a single path) from root to leaf.
	 * 
	 * functionally equivalent to ClosureRepository.getParentMappings(Long nodeId, int levels)
	 * 
	 * @param nodeId
	 * @param levels
	 * @return
	 * @throws Exception
	 */
	@MethodTimer
	public List<PathResource> getParentPathResourceTree(Long nodeId, int levels) throws Exception {
		
		// functionally equivalent to ClosureRepository.getParentMappings(Long nodeId, int levels)	
		
		String sql =
			"select " +
			"  n2.node_id, n2.parent_node_id, n2.node_id as child_node_id, n2.node_name, n2.creation_date, n2.updated_date, " + 
			"  r.path_type, r.path_name, r.relative_path, r.store_id, r.path_desc, r.read_group_1, r.write_group_1, execute_group_1, " +
			"  fmr.mime_type, fmr.file_size, fmr.is_file_data_in_db, " +
			"  s.store_id, s.store_name, s.store_description, s.store_path, s.node_id as store_root_node_id, " +
			"  s.max_file_size_in_db, s.access_rule, s.creation_date as store_creation_date, s.updated_date as store_updated_date " +  
			"from " +
			"  eas_node n2 inner join " +  
			"  (  " +
			"	select c.parent_node_id, c.depth " + 
			"	from eas_closure c  " +
			"	join eas_node n  " +
			"	on c.child_node_id = n.node_id " + 
			"	where c.child_node_id = ? and c.depth <= ? " +
			"  ) nlist on (n2.node_id = nlist.parent_node_id) " + 
			"inner join eas_path_resource r on n2.node_id = r.node_id " +
			"inner join eas_store s on r.store_id = s.store_id " +
			"left join eas_directory_resource dr on r.node_id = dr.node_id " + 
			"left join eas_file_meta_resource fmr on r.node_id = fmr.node_id  " +
			"order by  " +
			"  nlist.depth desc";
		
		List<PathResource> resources = jdbcTemplate.query(
				sql, new Object[] { nodeId, new Integer(levels) }, resourcePathRowMapper);		
		
		return resources;
		
	}
	
	/**
	 * Fetch a path resource. Every resource has a unique node id.
	 * 
	 * Will not include data from eas_binary_resource for FileMetaResource objects.
	 * 
	 * @param nodeId
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	//@MethodTimer
	public PathResource getPathResource(Long nodeId) throws Exception {
		
		//List<PathResource> childResources = getPathResourceTree(nodeId, 0);
		//if(childResources == null){
		//	throw new Exception("No PathResource found for node id => " + nodeId);
		//}else if(childResources.size() != 1){
		//	// should never get here...
		//	throw new Exception("Expected 1 PathResource for node id => " + nodeId + ", but fetched " + childResources.size());
		//}
		//return childResources.get(0);
		
		String sql =
				SQL_PATH_RESOURCE_COMMON +
				"where c.parent_node_id = ? and c.depth <= ? " +
				"order by c.depth, n.node_name";
		
		final ResultSetExtractor<PathResource> pathResultExtractor = SpringJdbcUtil.getSingletonExtractor(resourcePathRowMapper);
		
		return jdbcTemplate.query(sql, pathResultExtractor, new Object[] { nodeId, new Integer(0) });
		
	}
	
	/**
	 * Fetch a path resource by store and relative path.
	 * 
	 * @param storeName - the store name
	 * @param relativePath - the relative path within the store
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public PathResource getPathResource(String storeName, String relativePath) throws Exception {
		
		String sql =
			SQL_PATH_RESOURCE_COMMON +
			"where lower(s.store_name) = ? and lower(r.relative_path) = ? and c.depth = 0 " +
			"order by c.depth, n.node_name";
			
		//return jdbcTemplate.queryForObject(sql, resourcePathRowMapper,
		//		new Object[] { storeName.toLowerCase(), relativePath.toLowerCase() });
		
		final ResultSetExtractor<PathResource> pathResultExtractor = SpringJdbcUtil.getSingletonExtractor(resourcePathRowMapper);
		
		return jdbcTemplate.query(sql, pathResultExtractor, new Object[] { storeName.toLowerCase(), relativePath.toLowerCase() });			
		
	}
	
	/**
	 * Fetch the parent path resource for the specified node. If the node is a root node, and
	 * has no parent, then null will be returned.
	 * 
	 * @param nodeId
	 * @throws Exception
	 */
	public PathResource getParentPathResource(Long nodeId) throws Exception {
		
		List<PathResource> resources = getParentPathResourceTree(nodeId, 1);
		
		if(resources == null || resources.size() == 0){
			throw new ServiceException("No bottom-up PathResource tree for nodeId=" + nodeId + 
					". Returned list was null or empty.");
		}
		
		Tree<PathResource> tree = pathResourceTreeBuilder.buildParentPathResourceTree(resources);
		
		PathResource resource = tree.getRootNode().getData();
		if(resource.getNodeId().equals(nodeId) && resource.getParentNodeId().equals(0L)){
			// this is a root node with no parent
			return null;
		}
		
		return resource;		
		
	}
	
	/**
	 * Fetch the first level children for the path resource.
	 * 
	 * @param nodeId - id of the resource. All first level children will be returned
	 * @return All the first-level children, or an empty list of the node has no children
	 */	
	public List<PathResource> getChildPathResource(Long nodeId) throws Exception {
		
		List<PathResource> resources = getPathResourceTree(nodeId, 1);
		
		Tree<PathResource> tree = pathResourceTreeBuilder.buildPathResourceTree(resources, nodeId);
		
		if(tree.getRootNode().hasChildren()){
			List<PathResource> children = tree.getRootNode().getChildren().stream()
					.map(n -> n.getData())
					.collect(Collectors.toCollection(ArrayList::new));
			return children;
		}else{
			return new ArrayList<PathResource>();
		}
		
	}
	
	/**
	 * Fetch the child resource for the directory (first level only) with the matching name, of the specified type.
	 * 
	 * @param dirNodeId
	 * @param name
	 * @param type
	 * @return
	 * @throws Exception
	 */
	//@MethodTimer
	public PathResource getChildPathResource(Long dirNodeId, String name, ResourceType type) throws Exception {
		
		List<PathResource> childResources = getPathResourceTree(dirNodeId, 1);
		if(childResources != null && childResources.size() > 0){
			for(PathResource pr : childResources){
				if(pr.getParentNodeId().equals(dirNodeId)
						&& pr.getResourceType() == type
						&& pr.getPathName().toLowerCase().equals(name.toLowerCase())){
					
					return pr;
					
				}
			}
		}
		
		return null;
		
	}
	
	/**
	 * Fetch a DirectoryResource
	 * 
	 * @param nodeId
	 * @return
	 * @throws Exception
	 */
	//@MethodTimer
	public DirectoryResource getDirectory(Long nodeId) throws Exception {
		
		PathResource resource = getPathResource(nodeId);
		if(resource == null){
			throw new Exception("Failed to get directory by id, no path resource for nodeId=" + nodeId + ". Returned object was null.");
		}
		if(resource.getResourceType() == ResourceType.DIRECTORY){
			return (DirectoryResource)resource;
		}else{
			throw new Exception("Error fetching directory resource, nodeId => " + nodeId + " is not a directory resource.");
		}
		
	}
	
	/**
	 * Fetch a DirectoryResource
	 * 
	 * @param storeName - the store name
	 * @param relativePath - the relative path within the store
	 * @return
	 * @throws Exception
	 */
	public DirectoryResource getDirectory(String storeName, String relativePath) throws Exception {
		
		PathResource resource = getPathResource(storeName, relativePath);
		if(resource == null){
			throw new Exception("Failed to get directory by store name and resource relative path, "
					+ "returned object was null. storeName=" + storeName + ", relativePath=" + relativePath);
		}
		if(resource.getResourceType() == ResourceType.DIRECTORY){
			return (DirectoryResource)resource;
		}else{
			throw new Exception("Error fetching directory resource, storeName=" + storeName + 
					", relativePath=" + relativePath + ", is not a directory resource.");
		}
		
	}	
	
	/**
	 * Fetch a FileMetaResource
	 * 
	 * @param nodeId - file node Id
	 * @param includeBinary - pass true to include the binary data for the file, pass false not to.
	 * @return
	 * @throws Exception
	 */
	//@MethodTimer
	public FileMetaResource getFileMetaResource(Long nodeId, boolean includeBinary) throws Exception {
		
		PathResource resource = getPathResource(nodeId);
		if(resource == null){
			throw new Exception("Failed to get file meta resource by node id, returned object was null. "
					+ "nodeId=" + nodeId + ", includeBinary=" + includeBinary);
		}
		if(resource.getResourceType() == ResourceType.FILE){
			FileMetaResource fileMeta = (FileMetaResource)resource;
			if(includeBinary){
				fileMeta = populateWithBinaryData(fileMeta);
			}
			return fileMeta;
		}else{
			throw new Exception("Error fetching file meta resource for nodeId=" + nodeId + 
					". Path resource is not a file meta resource.");
		}
		
	}
	
	/**
	 * Fetch a FileMetaResource
	 * 
	 * @param storeName - the store name
	 * @param relativePath - the relative path within the store
	 * @param includeBinary - pass true to include the binary data for the file, pass false not to.
	 * @return
	 * @throws Exception
	 */
	//@MethodTimer
	public FileMetaResource getFileMetaResource(String storeName, String relativePath, boolean includeBinary) throws Exception {
		
		PathResource resource = getPathResource(storeName, relativePath);
		if(resource == null){
			throw new Exception("Failed to get file meta resource by store name and resource relative path, returned object was null. "
					+ "storeName=" + storeName + ", relativePath=" + relativePath + ", includeBinary=" + includeBinary);
		}
		if(resource.getResourceType() == ResourceType.FILE){
			FileMetaResource fileMeta = (FileMetaResource)resource;
			if(includeBinary){
				fileMeta = populateWithBinaryData(fileMeta);
			}
			return fileMeta;
		}else{
			throw new Exception("Error fetching file meta resource, storeName=" + storeName + 
					", relativePath=" + relativePath + ", includeBinary=" + includeBinary + 
					". Path resource is not a file meta resource.");
		}
		
	}	
	
	/**
	 * Create a new store
	 * 
	 * @param storeName
	 * @param storeDesc
	 * @param storePath
	 * @param rootDirName
	 * @param rootDirDesc
	 * @param maxFileSizeDb
	 * @param readGroup
	 * @param writeGroup
	 * @param executeGroup
	 * @param rule
	 * @return
	 * @throws Exception
	 */
	//@MethodTimer
	public Store addStore(
			String storeName, 
			String storeDesc, 
			Path storePath, 
			String rootDirName, 
			String rootDirDesc, 
			Long maxFileSizeDb,
			String readGroup,
			String writeGroup,
			String executeGroup,
			AccessRule rule) throws Exception {
		
		Long storeId = getNextStoreId();
		Long rootNodeId = closureRepository.getNextNodeId();
		
		Timestamp dtNow = DateUtil.getCurrentTime();
		
		String storePathString = PathResourceUtil.cleanFullPath(storePath.toString());
				
		// add entry to eas_store
		jdbcTemplate.update(
				"insert into eas_store (store_id, store_name, store_description, store_path, "
				+ "node_id, max_file_size_in_db, access_rule, creation_date, updated_date) " +
				"values (?, ?, ?, ?, ?, ?, ?, ?, ?)", storeId, storeName, storeDesc,
				storePathString, rootNodeId, maxFileSizeDb, rule.toString(), dtNow, dtNow);
		
		// create store directory on local file system
		try {
			fileService.createDirectory(storePath, true);
		} catch (Exception e) {
			throw new Exception("Failed to create store directory => " + storePathString + ". " + e.getMessage(), e);
		}		
		
		// add root directory for store
		DirectoryResource rootDir = addRootDirectory(storeId, storePath, rootNodeId, rootDirName, rootDirDesc, readGroup, writeGroup, executeGroup);
		
		Store store = new Store();
		store.setId(storeId);
		store.setName(storeName);
		store.setDescription(storeDesc);
		store.setNodeId(rootNodeId);
		store.setPath(storePath);
		store.setDateCreated(dtNow);
		store.setDateUpdated(dtNow);
		store.setMaxFileSizeBytes(maxFileSizeDb);
		store.setRootDir(rootDir);
		// TODO - pass in access rule. store defaults to DENY
		
		return store;
		
	}
	
	/**
	 * Renames the path resource. If the path resource is a FileMetaResource then we simply
	 * rename the file. If the path resource is a DirectoryResource then we rename the directory,
	 * and update the relative path data for all resources under the directory.
	 * 
	 * @param resource - the resource to rename
	 * @param newName - new name for the resource
	 * @throws Exception
	 */
	@MethodTimer
	private void renamePathResource(PathResource resource, String newName) throws Exception {
		
		if(resource.getResourceType() == ResourceType.FILE){
			
			logger.info("Peforming path resource rename on file");
			
			// get parent directory and check if file resource with the new name already exists
			PathResource parentDir = getParentPathResource(resource.getNodeId());
			if(hasChildPathResource(parentDir.getNodeId(), newName, ResourceType.FILE, resource.getNodeId()) ){
				throw new Exception("Cannot rename file resource " + resource.getNodeId() + " to " + newName + 
						", the directory in which the resource exists already contains a file with that name.");
			}
			
			_renameFileResource((FileMetaResource)resource, newName);
			
		}else if(resource.getResourceType() == ResourceType.DIRECTORY){
			
			logger.info("Peforming path resource rename on directory");
			
			// get parent directory and check if a child directory resource with the new name already exists
			PathResource parentDir = getParentPathResource(resource.getNodeId());
			if(parentDir != null) {
				if(hasChildPathResource(parentDir.getNodeId(), newName, ResourceType.DIRECTORY, resource.getNodeId()) ){
					throw new Exception("Cannot rename directory resource " + resource.getNodeId() + " to " + newName + 
							", the directory in which the resource exists already contains a child directory with that name.");
				}				
			} // else it's a root directory for a store			
			
			_renameDirectory((DirectoryResource)resource, newName);
			
		}else{
			throw new Exception("Cannot rename resource, unknown resource type '" + 
					resource.getResourceType().getTypeString() + "'");
		}
		
	}
	
	/**
	 * Helper method for renaming file
	 * 
	 * @param resource
	 * @param newName
	 * @throws Exception
	 */
	private void _renameFileResource(FileMetaResource resource, String newName) throws Exception {
		
		String oldName = resource.getPathName();
		String oldRelPath = resource.getRelativePath();
		String newRelPath = oldRelPath.substring(0, oldRelPath.lastIndexOf(oldName));
		newRelPath = newRelPath + newName;
		
		// rename data in EAS_PATH_RESOURCE
		jdbcTemplate.update(
				"update eas_path_resource set path_name = ?, relative_path = ? where node_id = ?", 
				newName, newRelPath, resource.getNodeId());
		
		// update EAS_NODE
		jdbcTemplate.update(
				"update eas_node set node_name = ?, updated_date = ? where node_id = ?", 
				newName, DateUtil.getCurrentTime(), resource.getNodeId());
		
		// rename file on local file system
		Store store = this.getStoreForResource(resource);
		Path oldPath = Paths.get(store.getPath() + resource.getRelativePath());
		Path newPath = Paths.get(store.getPath() + newRelPath);			
		fileService.moveFile(oldPath, newPath);		
		
	}
	
	/**
	 * Helper method for renaming a directory
	 * 
	 * @param resource
	 * @param newName
	 * @throws Exception
	 */
	private void _renameDirectory(DirectoryResource resource, String newName) throws Exception {
		
		List<PathResource> resTree = getPathResourceTree(resource.getNodeId());
		Tree<PathResource> tree = pathResourceTreeBuilder.buildPathResourceTree(resTree, resource.getNodeId());
		
		//TreeNode<PathResource> rootNode = tree.getRootNode();
		
		//logger.info("before rename");
		//pathResTreeUtil.logPreOrderTraversal(tree);	
		
		// walk tree and update in-memory values on our models
		Trees.walkTree(tree, (treeNode) ->{
			
			if(!treeNode.hasParent()){
				
				// rename root node, and update relative path
				PathResource resourceToRename = treeNode.getData();
				String oldName = resourceToRename.getPathName();
				String oldRelPath = resourceToRename.getRelativePath();
				String newRelPath = oldRelPath.substring(0, oldRelPath.lastIndexOf(oldName));
				newRelPath = newRelPath + newName;
				resourceToRename.setPathName(newName);
				resourceToRename.setRelativePath(newRelPath);
				resourceToRename.setDateUpdated(DateUtil.getCurrentTime());
				
			}else{
				
				// update relative paths of child nodes
				PathResource resToUpdate = treeNode.getData();
				PathResource parentResource = treeNode.getParent().getData();
				String parentRelPath = parentResource.getRelativePath();
				String resourceName = resToUpdate.getPathName();
				String newRelPath = parentRelPath + "/" + resourceName;
				resToUpdate.setRelativePath(newRelPath);
				
			}
			
		}, WalkOption.PRE_ORDER_TRAVERSAL);
		
		// walk tree and update database
		Trees.walkTree(tree, (treeNode) ->{
			
			PathResource resourceToUpdate = treeNode.getData();
			
			// rename data in EAS_PATH_RESOURCE
			jdbcTemplate.update(
					"update eas_path_resource set path_name = ?, relative_path = ? where node_id = ?", 
					resourceToUpdate.getPathName(), resourceToUpdate.getRelativePath(), resourceToUpdate.getNodeId());
			
			
			if(!treeNode.hasParent()){
				
				// update EAS_NODE, and make sure to update the updated date for the root node
				jdbcTemplate.update(
						"update eas_node set node_name = ?, updated_date = ? where node_id = ?", 
						resourceToUpdate.getPathName(), DateUtil.getCurrentTime(), resourceToUpdate.getNodeId());
				
			}else{
				
				// no need to update the updated date for any of the children
				jdbcTemplate.update(
						"update eas_node set node_name = ? where node_id = ?", 
						resourceToUpdate.getPathName(), resourceToUpdate.getNodeId());				
				
			}
			
		}, WalkOption.PRE_ORDER_TRAVERSAL);
		
		//logger.info("after rename");
		//pathResTreeUtil.logPreOrderTraversal(tree);		
		
		// rename directory on local file system
		//Store store = resource.getStore();
		Store store = this.getStoreForResource(resource);
		Path oldPath = Paths.get(store.getPath() + resource.getRelativePath());
		Path newPath = Paths.get(store.getPath() + tree.getRootNode().getData().getRelativePath());		
		fileService.moveFile(oldPath, newPath);
		
	}
	
	/**
	 * Refreshes the data in eas_binary_resource (or adds a new entry) for the file
	 * 
	 * @param fileNodeId - the id of the file meta resource 
	 * @return
	 * @throws Exception
	 */
	@MethodTimer
	public FileMetaResource refreshBinaryDataInDatabase(final Long fileNodeId) throws Exception {
		
		//
		// rather than pass in the FileMetaResource object we pass in the ID for the file meta object
		// and re-fetch it from the database. We do this in case the location of the file has changed
		// by the time this method has been executed.
		//
		
		CodeTimer timer = new CodeTimer();
		timer.start();
		
		final FileMetaResource fileMetaResource = this.getFileMetaResource(fileNodeId, false);
		final Store store = this.getStoreForResource(fileMetaResource);
		//final Long nodeId = fileMetaResource.getNodeId();
		final Path filePath = PathResourceUtil.buildPath(store, fileMetaResource);
		final boolean haveBinaryInDb = fileMetaResource.getIsBinaryInDatabase();
		long lFileSize = Files.size(filePath);
		final int fileSize = toIntExact(lFileSize);
		
		if(!Files.exists(filePath)){
			timer.stop();
			throw new Exception("Cannot refresh binary data in DB for FileMetaResource with node id => " + 
					fileMetaResource.getNodeId() + ". File does not exists on disk => " + filePath);
		}
		InputStream inStream = Files.newInputStream(filePath);
		
		
		if(haveBinaryInDb){
			
			// update existing eas_binary_resource entry
			LobHandler lobHandler = new DefaultLobHandler(); 
			jdbcTemplate.execute(
					"update eas_binary_resource r set r.file_data = ? where r.node_id = ?",
					new AbstractLobCreatingPreparedStatementCallback(lobHandler) {

						@Override
						protected void setValues(PreparedStatement statement, LobCreator lobCreator)
								throws SQLException, DataAccessException {
							
							lobCreator.setBlobAsBinaryStream(statement, 1, inStream, fileSize);
							statement.setLong(2, fileNodeId);
							
						}
						
					});
			
		}else{
			
			// add new entry to eas_binary_resource
			LobHandler lobHandler = new DefaultLobHandler(); 
			jdbcTemplate.execute(
					"insert into eas_binary_resource (node_id, file_data) values (?, ?)",
					new AbstractLobCreatingPreparedStatementCallback(lobHandler) {

						@Override
						protected void setValues(PreparedStatement statement, LobCreator lobCreator)
								throws SQLException, DataAccessException {
							
							statement.setLong(1, fileNodeId);
							lobCreator.setBlobAsBinaryStream(statement, 2, inStream, fileSize);
							
						}
						
					});
			
			// Update eas_file_meta_resource.is_binary_data_in_db to 'Y'
			jdbcTemplate.update("update eas_file_meta_resource set is_file_data_in_db = 'Y' where node_id = ?", fileNodeId);			
		
			fileMetaResource.setIsBinaryInDatabase(true);
			
		}
		
		inStream.close();
		
		timer.stop();
		logger.info("refreshBinaryDataInDatabase completed in " + timer.getElapsedTime() + " for file " + filePath.toString());			
		
		return fileMetaResource;
		
	}
	
	/**
	 * Internal helper method for adding new files (never to be used to replace an existing file.)
	 * 
	 * This method adds a new FileMetaResource, but does not add data to eas_binary_resource
	 * 
	 * @param store - the store that 'directory' param is under
	 * @param directory - directory where file will be added
	 * @param filePath - temp path to source file on local file system
	 * @return
	 * @throws Exception
	 */
	@MethodTimer
	public FileMetaResource _addNewFileWithoutBinary(Store store, DirectoryResource directory, Path filePath) throws Exception {	
		
		String fileName = filePath.getFileName().toString();
		Long fileSizeBytes = fileService.getSize(filePath);
		String fileMimeType = fileService.getMimeType(filePath);
		Boolean isBinaryInDb = false;

		// add entry to eas_node and eas_closure
		Node newNode = null;
		try {
			newNode = closureRepository.addNode(directory.getNodeId(), fileName);
		} catch (Exception e) {
			throw new Exception("Error adding directory node", e);
		}
		
		//Store store = getStoreById(dirResource.getStoreId());
		
		String fileRelPathString = PathResourceUtil.buildRelativePath(directory, fileName);
		
		PathResource newFileResource = new FileMetaResource();
		newFileResource.setNodeId(newNode.getNodeId());
		newFileResource.setResourceType(ResourceType.FILE);
		newFileResource.setDateCreated(newNode.getDateCreated());
		newFileResource.setDateUpdated(newNode.getDateUpdated());
		newFileResource.setPathName(fileName);
		newFileResource.setParentNodeId(newNode.getParentNodeId());
		newFileResource.setRelativePath(fileRelPathString);
		newFileResource.setDesc(null); // TODO - change method to pass in optional description?
		newFileResource.setStoreId(store.getId());
		newFileResource.setStore(store);
		((FileMetaResource)newFileResource).setFileSize(fileSizeBytes);
		((FileMetaResource)newFileResource).setMimeType(fileMimeType);
		((FileMetaResource)newFileResource).setIsBinaryInDatabase(isBinaryInDb);
		
		// add entry to eas_path_resource
		jdbcTemplate.update(
				"insert into eas_path_resource (node_id, store_id, path_name, path_type, relative_path, path_desc) " +
				"values (?, ?, ?, ?, ?, ?)", newFileResource.getNodeId(), newFileResource.getStoreId(), newFileResource.getPathName(),
				newFileResource.getResourceType().getTypeString(), newFileResource.getRelativePath(), newFileResource.getDesc());
		
		// add entry to eas_file_meta_resource
		jdbcTemplate.update(
				"insert into eas_file_meta_resource (node_id, file_size, mime_type, is_file_data_in_db) values (?, ?, ?, ?)",
					newFileResource.getNodeId(), fileSizeBytes, fileMimeType, ((isBinaryInDb) ? "Y" : "N"));
		
		// copy file to directory in the tree
		Path newFilePath = PathResourceUtil.buildPath(store, newFileResource);
		try {
			fileService.copyFile(filePath, newFilePath);
		} catch (Exception e) {
			throw new Exception("Failed to copy file from => " + filePath.toString() + 
					" to " + newFilePath.toString() + ". " + e.getMessage(), e);
		}	
		
		return (FileMetaResource)newFileResource;
		
	}
	
	/**
	 * Updates the physical file on disk, then removes the old binary data from the database.
	 * 
	 * @param dirResource - The directory where the new file will go
	 * @param srcFilePath - The new file to add. The old binary data in the database will be removed.
	 * @param currFileRes - The current file meta resource being updated
	 * @return
	 * @throws Exception
	 */
	@MethodTimer
	public FileMetaResource _updateFileDiscardOldBinary(DirectoryResource dirResource, Path srcFilePath, FileMetaResource currFileRes) throws Exception {
		
		final String newFileName = srcFilePath.getFileName().toString();
		final Store store = this.getStoreForResource(dirResource);
		final Long newFileSizeBytes = fileService.getSize(srcFilePath);
		final String newFileMimeType = fileService.getMimeType(srcFilePath);
		final String newRelFilePath = PathResourceUtil.buildRelativePath(dirResource, newFileName);
		
		// old/current tree path for the old physical file
		Path oldFilePath = PathResourceUtil.buildPath(store, currFileRes);
		// new tree path for the new physical file
		Path newFilePath = PathResourceUtil.buildPath(store, newRelFilePath);
		
		// check if we have existing binary data in the database, we might need to remove it.
		if(currFileRes.getIsBinaryInDatabase()){
			
			// remove existing binary data in database (it's the old file)
			jdbcTemplate.update("delete from eas_binary_resource where node_id = ?", currFileRes.getNodeId());
			
			currFileRes.setIsBinaryInDatabase(false);
			
		}
		
		currFileRes.setMimeType(newFileMimeType);
		currFileRes.setFileSize(newFileSizeBytes);
		currFileRes.setBinaryResource(null);
		currFileRes.setDateUpdated(DateUtil.getCurrentTime());
		currFileRes.setPathName(newFileName);
		currFileRes.setRelativePath(newRelFilePath);
		//currFileRes.setStoreId(store.getId());
		//currFileRes.setStore(store);
			
		// update eas_file_meta_resource
		jdbcTemplate.update(
				"update eas_file_meta_resource set is_file_data_in_db = 'N', file_size = ?, mime_type = ? "
				+ " where node_id = ?",
				currFileRes.getFileSize(), currFileRes.getMimeType(), currFileRes.getNodeId());
			
		// eas_path_resource to account for differences in uppercase/lowercase of file name
		jdbcTemplate.update("update eas_path_resource set path_name = ?, relative_path = ?, path_desc = ? where node_id = ?",
				currFileRes.getPathName(), currFileRes.getRelativePath(), currFileRes.getDesc(), currFileRes.getNodeId());			
		
		// update eas_node
		closureRepository.updateNodeMeta(currFileRes);
		
		// delete old file on local disk
		try {
			fileService.deletePath(oldFilePath);
		} catch (Exception e) {
			throw new Exception("Failed to remove old file at => " + oldFilePath.toString() + 
					". " + e.getMessage(), e);
		}
		
		// copy new file to directory in the tree
		try {
			fileService.copyFile(srcFilePath, newFilePath);
		} catch (Exception e) {
			throw new Exception("Failed to copy new file from => " + srcFilePath.toString() + 
					" to " + newFilePath.toString() + ". " + e.getMessage(), e);
		}
		
		// NOTE - do not delete file at srcFilePath
		
		return currFileRes;
		
	}
	
	/**
	 * Add a directory node.
	 * 
	 * @param parentDir - the directory under which a new child directory will be added
	 * @param name - name for new directory node.
	 * @param desc - description for new directory
	 * @param readGroup1 - optional read group
	 * @param writeGroup1 - optional write group
	 * @param executeGroup1 - optional execute group
	 * @return
	 * @throws Exception
	 */
	@MethodTimer
	public DirectoryResource addDirectory(
			DirectoryResource parentDir, 
			String name, 
			String desc, 
			String readGroup1, 
			String writeGroup1, 
			String executeGroup1) throws Exception {
		
		// make sure directory doesn't already contain a sub-directory with the same name	
		if(hasChildPathResource(parentDir.getNodeId(), name, ResourceType.DIRECTORY, null)){
			throw new Exception("Directory with dirNodeId " + parentDir.getNodeId() + 
					" already contains a sub-directory with the name '" + name + "'");			
		}
		
		// add entry to eas_node and eas_closure
		Node newNode = null;
		try {
			newNode = closureRepository.addNode(parentDir.getNodeId(), name);
		} catch (Exception e) {
			throw new Exception("Error adding directory node", e);
		}
		
		Store store = getStoreForResource(parentDir);
		
		String dirRelPathString = PathResourceUtil.buildRelativePath(parentDir, name);
		
		PathResource resource = new DirectoryResource();
		resource.setNodeId(newNode.getNodeId());
		resource.setResourceType(ResourceType.DIRECTORY);
		resource.setDateCreated(newNode.getDateCreated());
		resource.setDateUpdated(newNode.getDateUpdated());
		resource.setPathName(name);
		resource.setParentNodeId(newNode.getParentNodeId());
		resource.setRelativePath(dirRelPathString);
		resource.setDesc(desc);
		resource.setStoreId(store.getId());
		resource.setStore(store);
		resource.setReadGroup1(readGroup1);
		resource.setWriteGroup1(writeGroup1);
		resource.setExecuteGroup1(executeGroup1);
		
		// add entry to eas_path_resource
		jdbcTemplate.update(
				"insert into eas_path_resource (node_id, store_id, path_name, path_type, relative_path, path_desc, read_group_1, write_group_1, execute_group_1) " +
				"values (?, ?, ?, ?, ?, ?, ?, ?, ?)", resource.getNodeId(), resource.getStoreId(), resource.getPathName(),
				resource.getResourceType().getTypeString(), resource.getRelativePath(), resource.getDesc(), resource.getReadGroup1(), resource.getWriteGroup1(), resource.getExecuteGroup1());		
		
		// add entry to eas_directory_resource
		jdbcTemplate.update(
				"insert into eas_directory_resource (node_id) values (?)", resource.getNodeId());

		// create directory on local file system. If there is any error throw a RuntimeException,
		// or update the @Transactional annotation to rollback for any exception type, i.e.,
		// @Transactional(rollbackFor=Exception.class)
		Path newDirectoryPath = PathResourceUtil.buildPath(store, resource);
		try {
			fileService.createDirectory(newDirectoryPath, true);
		} catch (Exception e) {
			throw new Exception("Failed to create directory => " + newDirectoryPath.toString().replace("\\", "/") + ". " + e.getMessage(), e);
		}	
		
		return (DirectoryResource)resource;
		
	}
	
	/**
	 * Adds a root directory. This is a directory with no parent, and is the top most
	 * directory for a store (the parent directory is the store directory.)
	 * 
	 * @param storeId - store id
	 * @param storePath - path for the store
	 * @param rootNodeId - id of store's root directory node
	 * @param name - path name of store's root directory node
	 * @param desc - description for directory
	 * @param readGroup
	 * @param writeGroup
	 * @param executeGroup
	 * @return
	 * @throws Exception
	 */
	@MethodTimer
	private DirectoryResource addRootDirectory(
			Long storeId, 
			Path storePath, 
			Long rootNodeId, 
			String name, 
			String desc,
			String readGroup,
			String writeGroup,
			String executeGroup) throws Exception {
		
		// add entry to eas_node and eas_closure
		Node newNode = null;
		try {
			newNode = closureRepository.addNode(rootNodeId, 0L, name); // parent id set to 0 for all root nodes.
		} catch (Exception e) {
			throw new Exception("Error adding directory node", e);
		}
		
		String dirRelPathString = PathResourceUtil.cleanRelativePath(name);
		
		PathResource dirResource = new DirectoryResource();
		dirResource.setNodeId(newNode.getNodeId());
		dirResource.setResourceType(ResourceType.DIRECTORY);
		dirResource.setDateCreated(newNode.getDateCreated());
		dirResource.setDateUpdated(newNode.getDateUpdated());
		dirResource.setPathName(name);
		dirResource.setParentNodeId(newNode.getParentNodeId());
		dirResource.setRelativePath(dirRelPathString);
		dirResource.setDesc(desc);
		dirResource.setStoreId(storeId);
		dirResource.setReadGroup1(readGroup);
		dirResource.setWriteGroup1(writeGroup);
		dirResource.setExecuteGroup1(executeGroup);
		
		Store store = getStoreForResource(dirResource);
		dirResource.setStore(store);
		
		// add entry to eas_path_resource
		jdbcTemplate.update(
				"insert into eas_path_resource (node_id, store_id, path_name, path_type, relative_path, path_desc, read_group_1, write_group_1, execute_group_1) " +
				"values (?, ?, ?, ?, ?, ?, ?, ?, ?)", dirResource.getNodeId(), dirResource.getStoreId(), dirResource.getPathName(),
				dirResource.getResourceType().getTypeString(), dirResource.getRelativePath(), dirResource.getDesc(), 
				dirResource.getReadGroup1(), dirResource.getWriteGroup1(), dirResource.getExecuteGroup1());		
		
		// add entry to eas_directory_resource
		jdbcTemplate.update(
				"insert into eas_directory_resource (node_id) values (?)", dirResource.getNodeId());		
		
		// create directory on local file system. If there is any error throw a RuntimeException,
		// or update the @Transactional annotation to rollback for any exception type, i.e.,
		// @Transactional(rollbackFor=Exception.class)
		Path newDirectoryPath = PathResourceUtil.buildPath(storePath, dirResource);
		try {
			fileService.createDirectory(newDirectoryPath, true);
		} catch (Exception e) {
			throw new Exception("Failed to create directory => " + newDirectoryPath.toString().replace("\\", "/") + ". " + e.getMessage(), e);
		}
		
		return (DirectoryResource)dirResource;		
		
	}
	
	/**
	 * Checks if the directory already contains a child resource (first level child)
	 * of the specified type, with the same name (case insensitive.)
	 * 
	 * @param dirNodeId - the id of the directory node
	 * @param name - the name of the child path resource. Will match on name.toLowerCase()
	 * @param type - the type of child path resource to check for.
	 * @param ignoreChildId - Id of child resource to ignore when performing check. Useful when renaming resources as we don't
	 * 	watch to match the file or directory we're renaming against itself.
	 * @return
	 */
	//@MethodTimer
	public boolean hasChildPathResource(Long dirNodeId, String name, ResourceType type, Long ignoreChildId) throws Exception {
		
		List<PathResource> childResources = getPathResourceTree(dirNodeId, 1);
		if(childResources != null && childResources.size() > 0){
			for(PathResource pr : childResources){
				if(ignoreChildId != null && pr.getNodeId().equals(ignoreChildId)) {
					// ignore this child
					continue;
				}
				if(pr.getParentNodeId().equals(dirNodeId)
						&& pr.getResourceType() == type
						&& pr.getPathName().toLowerCase().equals(name.toLowerCase())){
					
					return true;
					
				}
			}
		}		
		
		return false;
	}
	
	/**
	 * Returns true if node 'dirNodeB' is a child node (at any depth) of node 'dirNodeA'
	 * 
	 * @param dirNodeA
	 * @param dirNodeB
	 * @return
	 */
	//@MethodTimer
	public boolean isChild(Long dirNodeA, Long dirNodeB) throws Exception {
		
		List<PathResource> parentResources = getParentPathResourceTree(dirNodeB);
		if(parentResources == null || parentResources.size() == 0){
			throw new Exception("No 'parent' PathResource mappings for dir node id = > " + dirNodeB);
		}
		
		return parentResources.stream()
	            .anyMatch(r -> r.getParentNodeId().equals(dirNodeA));		
		
	}	
	
	/**
	 * Adds a BinaryResource object to the FileMetaResource with either the byte[] data from
	 * the database (if it exists) or from the local file system.
	 * 
	 * @param resource
	 * @return
	 * @throws Exception
	 */
	private FileMetaResource populateWithBinaryData(FileMetaResource resource) throws Exception {
		
		// error checking
		if(resource == null){
			throw new Exception("FileMetaResource object is null. Cannot populate FileMetaResource with binary data.");
		}else if(resource.getNodeId() == null){
			throw new Exception("FileMetaResource nodeId value is null. Cannot populate FileMetaResource with binary data.");
		}
		
		// get binary data from database
		if(resource.getIsBinaryInDatabase()){
			
			BinaryResource binRes = jdbcTemplate.queryForObject(
					"select node_id, file_data from eas_binary_resource where node_id = ?",
					new Object[]{ resource.getNodeId() }, (rs, rowNum) -> {
						BinaryResource br = new BinaryResource();
						br.setNodeId(rs.getLong("node_id"));
						br.setFileData(rs.getBytes(2));
						return br;
					});
			resource.setBinaryResource(binRes);
			return resource;
		
		// else get data from local file system
		}else{
		
			Store store = getStoreForResource(resource);
			if(store == null) {
				throw new Exception("Cannot populate file meta resource with binary data from database because the store could not be fetched.");
			}
			Path pathToFile = PathResourceUtil.buildPath(store, resource);
			if(!Files.exists(pathToFile)){
				throw new Exception("Error, file on local file system does not exist for FileMetaResource "
						+ "with file node id => " + resource.getNodeId() + ", path => " + pathToFile.toString() +
						". Cannot populate FileMetaResource with binary data.");				
			}
			byte[] fileBytes = Files.readAllBytes(pathToFile);
			BinaryResource br = new BinaryResource();
			br.setNodeId(resource.getNodeId());
			br.setFileData(fileBytes);
			resource.setBinaryResource(br);
			
			return resource;
			
		}
		
	}	
	
	/**
	 * Remove a file
	 * 
	 * @param resource
	 * @throws Exception
	 */
	@MethodTimer
	public void removeFile(FileMetaResource resource) throws Exception {
		
		Path filePath = PathResourceUtil.buildPath(getStoreForResource(resource), resource);
		
		// delete from eas_binary_resource
		if(resource.getIsBinaryInDatabase()){
			jdbcTemplate.update("delete from eas_binary_resource where node_id = ?", resource.getNodeId());
		}
		
		// delete from eas_file_meta_resource
		jdbcTemplate.update("delete from eas_file_meta_resource where node_id = ?", resource.getNodeId());
		
		// delete from eas_path_resource
		jdbcTemplate.update("delete from eas_path_resource where node_id = ?", resource.getNodeId());
		
		// delete closure data and node
		closureRepository.deleteNode(resource.getNodeId());
		
		// remove file for local file system
		if(Files.exists(filePath)) {
			fileService.deletePath(filePath);
		}else {
			logger.warn("Removing file " + "[nodeId=" + resource.getNodeId() + ", name=" + resource.getNodeName() + ", relPath=" + resource.getRelativePath() + "] but physical file does not exists at " + filePath.toString());
		}
		
	}
	
	/**
	 * Removing a directory. THE DIRECTORY MUST BE EMPTY!
	 * 
	 * @param resource
	 * @throws Exception
	 */
	@MethodTimer
	public void removeDirectory(DirectoryResource resource) throws Exception {
		
		Path dirPath = PathResourceUtil.buildPath(getStoreForResource(resource), resource);
		
		// delete from eas_directory_resource
		jdbcTemplate.update("delete from eas_directory_resource where node_id = ?", resource.getNodeId());
		
		// delete from eas_path_resource
		jdbcTemplate.update("delete from eas_path_resource where node_id = ?", resource.getNodeId());
		
		// delete closure data and node
		closureRepository.deleteNode(resource.getNodeId());
		
		// remove file for local file system
		fileService.deletePath(dirPath);		
		
	}
	
	/**
	 * Get next id from eas_store_id_sequence
	 * 
	 * @return
	 * @throws Exception
	 */
	private Long getNextStoreId() throws Exception {
		
		Long id = jdbcTemplate.queryForObject(
				"select eas_store_id_sequence.nextval from dual", Long.class);
		
		return id;
		
	}

	/**
	 * Move a file
	 * 
	 * @param fileToMove - the file to move
	 * @param destDir - the directory to move it to.
	 * @param replaceExisting
	 */
	@MethodTimer
	public void moveFile(FileMetaResource fileToMove, DirectoryResource destDir, boolean replaceExisting) throws Exception {
		
		//
		// TODO - Consider breaking up the deletion of the existing file in the destination directory as a separate
		// task manager task (queued)
		//
		
		// check if user is trying to move file to the same directory it's already in
		if(fileToMove.getParentNodeId().equals(destDir.getNodeId())){
			throw new ServiceException("Cannot move file to directory that it's already in. [fileNodeId=" + fileToMove.getNodeId() + 
					", dirNodeId=" + destDir.getNodeId() + ", replaceExisting=" + replaceExisting + "]");
		}		
		
		// check if destination directory already contains file with the same name.
		String fileName = fileToMove.getPathName();
		PathResource existingFile = getChildPathResource(destDir.getNodeId(), fileName, ResourceType.FILE);
		boolean hasExisting = (existingFile != null) ? true : false;
		
		// TODO - check for write permission on resource being overwritten
		
		// replace existing file
		if(hasExisting && replaceExisting){
			
			Store sourceStore = this.getStoreForResource(fileToMove);
			Store destinationStore = this.getStoreForResource(destDir);			
			
			// current/old path to file on local file system
			Path oldFullPath = PathResourceUtil.buildPath(sourceStore, fileToMove);
			// new path to file on local file system
			Path newFullPath = PathResourceUtil.buildPath(destinationStore, destDir, fileName);
			// new relative path for eas_path_resource
			String newRelativePath = PathResourceUtil.buildRelativePath(destDir, fileName);
			
			// delete existing file in destination directory
			removeFile((FileMetaResource)existingFile);
			
			// remove existing closure data for 'fileToMove'
			// TODO - will this work if we have foreign key constraints? 
			closureRepository.deleteNode(fileToMove.getNodeId());
			
			// add new closure data for 'fileToMove'
			closureRepository.addNode(fileToMove.getNodeId(), destDir.getNodeId(), fileName);
			
			// nothing to update in eas_file_meta_resource
			
			// update data in eas_path_resource
			jdbcTemplate.update(
					"update eas_path_resource set store_id = ?, relative_path = ? where node_id = ?", 
					destinationStore.getId(), newRelativePath, fileToMove.getNodeId());			
			
			// move physical file on disk
			try {
				fileService.moveFile(oldFullPath, newFullPath);
			} catch (Exception e) {
				throw new Exception("Failed to move file " + fileToMove.getNodeId() + " to directory " + destDir.getNodeId() + 
						". oldFullPath => " + oldFullPath.toString() + ", newFullPath => " + newFullPath.toString() + 
						". " + e.getMessage(), e);
			}
			
		}else if(hasExisting && !replaceExisting){
			
			throw new Exception("Cannot move file " + fileToMove.getNodeId() + " to directory " + destDir.getNodeId() + 
					" because destination directory already contains a file with the same name, and "
					+ "replaceExisting => " + replaceExisting);
			
		}else{
			
			Store sourceStore = this.getStoreForResource(fileToMove);
			Store destinationStore = this.getStoreForResource(destDir);			
			
			// current/old path to file on local file system
			Path oldFullPath = PathResourceUtil.buildPath(sourceStore, fileToMove);
			// new path to file on local file system
			Path newFullPath = PathResourceUtil.buildPath(destinationStore, destDir, fileName);
			// new relative path for eas_path_resource
			String newRelativePath = PathResourceUtil.buildRelativePath(destDir, fileName);
			
			// remove existing closure data for 'fileToMove'
			// TODO - will this work if we have foreign key constraints? 
			closureRepository.deleteNode(fileToMove.getNodeId());
			
			// add new closure data for 'fileToMove'
			closureRepository.addNode(fileToMove.getNodeId(), destDir.getNodeId(), fileName);
			
			// nothing to update in eas_file_meta_resource
			
			// update data in eas_path_resource
			jdbcTemplate.update(
					"update eas_path_resource set store_id = ?, relative_path = ? where node_id = ?", 
					destinationStore.getId(), newRelativePath, fileToMove.getNodeId());			
			
			// move physical file on disk
			try {
				fileService.moveFile(oldFullPath, newFullPath);
			} catch (Exception e) {
				throw new Exception("Failed to move file " + fileToMove.getNodeId() + " to directory " + destDir.getNodeId() + 
						". oldFullPath => " + oldFullPath.toString() + ", newFullPath => " + newFullPath.toString() + 
						". " + e.getMessage(), e);
			}
			
		}
		
	}

	/**
	 * Updates the directory. This will properly rename the directory and update the relative paths of all child resources.
	 * 
	 * @param dir - the directory to update
	 * @param name - new name
	 * @param desc - new description
	 * @param readGroup1 - new optional read group
	 * @param writeGroup1 - new optional write group
	 * @param executeGroup1 new optional execute group
	 * @throws Exception
	 */
	@MethodTimer
	public void updateDirectory(DirectoryResource dir, String name, String desc, String readGroup1, String writeGroup1, String executeGroup1) throws Exception {
		
		// perform recursive rename, if name is actually different
		//if(!dir.getPathName().equals(name)) {
		this.renamePathResource(dir, name);
		//}
		
		// update other fields
		jdbcTemplate.update("update eas_path_resource set path_desc = ?, read_group_1 = ?, write_group_1 = ?, execute_group_1 = ? where node_id = ?",
				desc, readGroup1, writeGroup1, executeGroup1, dir.getNodeId());		
		
	}

	/**
	 * Updates the file (name and description)
	 * 
	 * @param file - the file to update
	 * @param newName - the new name value
	 * @param newDesc - the new description value
	 */
	@MethodTimer
	public void updateFile(FileMetaResource file, String newName, String newDesc) throws Exception {
		
		// perform rename, if name is actually different
		//if(!file.getPathName().equals(newName)) {
		this.renamePathResource(file, newName);
		//}
		
		// update other fields
		jdbcTemplate.update("update eas_path_resource set path_desc = ? where node_id = ?",
				newDesc, file.getNodeId());			
		
	}

}
