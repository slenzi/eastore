package org.eamrf.repository.oracle.ecoguser.eastore;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.List;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.DateUtil;
import org.eamrf.core.util.FileUtil;
import org.eamrf.repository.oracle.ecoguser.eastore.model.DirectoryResource;
import org.eamrf.repository.oracle.ecoguser.eastore.model.FileMetaResource;
import org.eamrf.repository.oracle.ecoguser.eastore.model.Node;
import org.eamrf.repository.oracle.ecoguser.eastore.model.PathResource;
import org.eamrf.repository.oracle.ecoguser.eastore.model.ResourceType;
import org.eamrf.repository.oracle.ecoguser.eastore.model.Store;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
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
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Exception.class)
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
			+ "where store_id = ?";
		
		return (Store)jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
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
		}, new Object[] { storeId });		
		
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
		
		return (DirectoryResource)jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
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
		}, new Object[] { nodeId });		
		
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
		
		// functionally equivalent to ClosureRepository.getChildMappings(Long nodeId)
		
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
		
		// functionally equivalent to ClosureRepository.getChildMappings(Long nodeId, int depth)
		
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
	 * Fetch bottom-up (leaf node to root node), PathResource list. This can
	 * be used to build a tree (or more of a single path) from root to leaf.
	 * 
	 * functionally equivalent to ClosureRepository.getParentMappings(Long nodeId)
	 * 
	 * @param dirNodeId
	 * @return
	 * @throws Exception
	 */
	public List<PathResource> getParentPathResourceTree(Long dirNodeId) throws Exception {
		
		// functionally equivalent to ClosureRepository.getParentMappings(Long nodeId)
		
		String sql =
			"select " +
			"  n2.node_id, n2.parent_node_id, n2.node_id as child_node_id, n2.node_name, n2.creation_date, n2.updated_date, " +
			"  r.path_type, r.path_name, r.relative_path, r.store_id, fmr.mime_type, fmr.file_size, fmr.is_file_data_in_db " +
			"from " +
			"  eas_node n2 inner join " + 
			"  ( " +
			"    select c.parent_node_id, c.depth " +
			"    from eas_closure c " +
			"    join eas_node n " +
			"    on c.child_node_id = n.node_id " +
			"    where c.child_node_id = ? " +
			"  ) nlist on (n2.node_id = nlist.parent_node_id) " +
			"inner join eas_path_resource r on n2.node_id = r.node_id " +
			"left join eas_directory_resource dr on r.node_id = dr.node_id " +
			"left join eas_file_meta_resource fmr on r.node_id = fmr.node_id " +
			"order by " +
			"  nlist.depth desc";
		
		List<PathResource> resources = jdbcTemplate.query(
				sql, new Object[] { dirNodeId }, resourcePathRowMapper);		
		
		return resources;		
		
	}
	
	/**
	 * Fetch bottom-up (leaf node to root node) PathResource list, up to a specified levels up. This can
	 * be used to build a tree (or more of a single path) from root to leaf.
	 * 
	 * functionally equivalent to ClosureRepository.getParentMappings(Long nodeId, int levels)
	 * 
	 * @param dirNodeId
	 * @param levels
	 * @return
	 * @throws Exception
	 */
	public List<PathResource> getParentPathResourceTree(Long dirNodeId, int levels) throws Exception {
		
		// functionally equivalent to ClosureRepository.getParentMappings(Long nodeId, int levels)
		
		String sql =
			"select " +
			"  n2.node_id, n2.parent_node_id, n2.node_id as child_node_id, n2.node_name, n2.creation_date, n2.updated_date, " +
			"  r.path_type, r.path_name, r.relative_path, r.store_id, fmr.mime_type, fmr.file_size, fmr.is_file_data_in_db " +
			"from " +
			"  eas_node n2 inner join " + 
			"  ( " +
			"    select c.parent_node_id, c.depth " +
			"    from eas_closure c " +
			"    join eas_node n " +
			"    on c.child_node_id = n.node_id " +
			"    where c.child_node_id = ? and c.depth <= ? " +
			"  ) nlist on (n2.node_id = nlist.parent_node_id) " +
			"inner join eas_path_resource r on n2.node_id = r.node_id " +
			"left join eas_directory_resource dr on r.node_id = dr.node_id " +
			"left join eas_file_meta_resource fmr on r.node_id = fmr.node_id " +
			"order by " +
			"  nlist.depth desc";
		
		List<PathResource> resources = jdbcTemplate.query(
				sql, new Object[] { dirNodeId, new Integer(levels) }, resourcePathRowMapper);		
		
		return resources;
		
	}
	
	/**
	 * Create a new store
	 * 
	 * @param storeName
	 * @param storeDesc
	 * @param storePath
	 * @param rootDirName
	 * @param maxFileSizeDb
	 * @return
	 * @throws Exception
	 */
	public Store addStore(String storeName, String storeDesc, Path storePath, String rootDirName, Long maxFileSizeDb) throws Exception {
	
		Long storeId = getNextStoreId();
		Long rootNodeId = closureRepository.getNextNodeId();
		
		Timestamp dtNow = DateUtil.getCurrentTime();
		
		String storePathString = storePath.toString().replace("\\", "/");
				
		//
		// add entry to eas_store
		//
		jdbcTemplate.update(
				"insert into eas_store (store_id, store_name, store_description, store_path, "
				+ "node_id, max_file_size_in_db, creation_date, updated_date) " +
				"values (?, ?, ?, ?, ?, ?, ?, ?)", storeId, storeName, storeDesc,
				storePathString, rootNodeId, maxFileSizeDb, dtNow, dtNow);
		//
		// create store directory on local file system
		//
		try {
			FileUtil.createDirectory(storePath, true);
		} catch (Exception e) {
			throw new Exception("Failed to create store directory => " + storePathString + ". " + e.getMessage(), e);
		}		
		
		//
		// add root directory for store
		//
		addRootDirectory(storeId, storePath, rootNodeId, rootDirName);
		
		Store store = new Store();
		store.setId(storeId);
		store.setName(storeName);
		store.setDescription(storeDesc);
		store.setNodeId(rootNodeId);
		store.setPath(storePath);
		store.setDateCreated(dtNow);
		store.setDateUpdated(dtNow);
		store.setMaxFileSizeBytes(maxFileSizeDb);
		
		return store;
		
	}
	
	/**
	 * Adds a new file, but does not add the binary data to eas_file_binary_resource
	 * 
	 * @param dirNodeId
	 * @param filePath
	 * @param replaceExisting
	 * @return
	 * @throws Exception
	 */
	public FileMetaResource addFile(Long dirNodeId, Path filePath, boolean replaceExisting) throws Exception {
		
		//
		// make sure parentDirNodeId is actually of a directory
		//
		DirectoryResource parentDirectory = getDirectoryById(dirNodeId);
		if(parentDirectory.getResourceType() != ResourceType.DIRECTORY){
			throw new Exception("Node ID " + dirNodeId + " is not a directory node. "
					+ "Cannot add file to a non-directory node.");
		}
		
		String fileName = filePath.getFileName().toString();
		
		//
		// check if directory already contains a file with the same name (case insensitive)
		//
		boolean hasExisting = hasChildPathResource(dirNodeId, fileName, ResourceType.FILE);
		
		if(hasExisting && replaceExisting){
			
			return _updateFile(parentDirectory, filePath);
		
		}else if(hasExisting && !replaceExisting){
			
			throw new Exception("Directory with dirNodeId " + dirNodeId + 
					" already contains a file with the name '" + fileName + "', and 'replaceExisting' param is set to false.");
			
		}else{
			
			return _addNewFile(parentDirectory, filePath);
			
		}
		
		
	}
	
	/**
	 * Internal helper method for adding new files (never to be used to replace an existin file.)
	 * 
	 * @param dirResource
	 * @param filePath
	 * @return
	 * @throws Exception
	 */
	private FileMetaResource _addNewFile(DirectoryResource dirResource, Path filePath) throws Exception {
		
		String fileName = filePath.getFileName().toString();
		Long fileSizeBytes = FileUtil.getFileSize(filePath);
		String fileMimeType = FileUtil.detectMimeType(filePath);
		Boolean isBinaryInDb = false;
		
		//
		// add entry to eas_node and eas_closure
		//
		Node newNode = null;
		try {
			newNode = closureRepository.addNode(dirResource.getNodeId(), fileName);
		} catch (Exception e) {
			throw new Exception("Error adding directory node", e);
		}
		
		//
		// get store
		//
		Store store = getStoreById(dirResource.getStoreId());
		
		String fileRelPathString = (dirResource.getRelativePath() + File.separator + fileName).replace("\\", "/");
		
		PathResource resource = new FileMetaResource();
		resource.setNodeId(newNode.getNodeId());
		resource.setResourceType(ResourceType.FILE);
		resource.setDateCreated(newNode.getDateCreated());
		resource.setDateUpdated(newNode.getDateUpdated());
		resource.setPathName(fileName);
		resource.setParentNodeId(newNode.getParentNodeId());
		resource.setRelativePath(fileRelPathString);
		resource.setStoreId(store.getId());
		((FileMetaResource)resource).setFileSize(fileSizeBytes);
		((FileMetaResource)resource).setMimeType(fileMimeType);
		((FileMetaResource)resource).setIsBinaryInDatabase(isBinaryInDb);
		
		//
		// add entry to eas_path_resource
		//
		jdbcTemplate.update(
				"insert into eas_path_resource (node_id, store_id, path_name, path_type, relative_path) " +
				"values (?, ?, ?, ?, ?)", resource.getNodeId(), resource.getStoreId(), resource.getPathName(),
				resource.getResourceType().getTypeString(), resource.getRelativePath());
		
		//
		// add entry to eas_file_meta_resource
		//
		jdbcTemplate.update(
				"insert into eas_file_meta_resource (node_id, file_size, mime_type, is_file_data_in_db) values (?, ?, ?, ?)",
					resource.getNodeId(), fileSizeBytes, fileMimeType, ((isBinaryInDb) ? "Y" : "N"));
		
		//
		// copy file to directory in the tree
		//
		Path newFilePath = Paths.get(store.getPath() + resource.getRelativePath());
		try {
			FileUtil.copyFile(filePath, newFilePath);
		} catch (Exception e) {
			throw new Exception("Failed to copy file from => " + filePath.toString() + " to " + newFilePath.toString() + ". " + e.getMessage(), e);
		}
		
		return (FileMetaResource)resource;
		
	}
	
	/**
	 * Updates the existing file.
	 * 
	 * @param dirResource
	 * @param filePath
	 * @return
	 * @throws Exception
	 */
	private FileMetaResource _updateFile(DirectoryResource dirResource, Path filePath) throws Exception {
		
		// TODO - add code for replacing existing file
		
		String fileName = filePath.getFileName().toString();
		
		throw new Exception("Directory with dirNodeId " + dirResource.getNodeId() + " already contains a file with the name '" + 
				fileName + "'. The 'replaceExisting' param is set to true, but this feature is not suppored yet :(");
		
	}
	
	/**
	 * Add a directory node.
	 * 
	 * @param parentDirNodeId - Id of parent directory node.
	 * @param name - name of new directory node.
	 * @return
	 * @throws Exception
	 */
	public DirectoryResource addDirectory(Long parentDirNodeId, String name) throws Exception {
		
		//
		// make sure parentDirNodeId is actually of a directory path resource type
		//
		DirectoryResource parentDirectory = getDirectoryById(parentDirNodeId);
		if(parentDirectory.getResourceType() != ResourceType.DIRECTORY){
			throw new Exception("Node ID " + parentDirNodeId + " is not a directory node. "
					+ "Cannot add sub directory to a non-directory node.");
		}
		
		//
		// make sure directory doesn't already contain a sub-directory with the same name
		//		
		if(hasChildPathResource(parentDirNodeId, name, ResourceType.DIRECTORY)){
			throw new Exception("Directory with dirNodeId " + parentDirNodeId + 
					" already contains a sub-directory with the name '" + name + "'");			
		}
		
		//
		// add entry to eas_node and eas_closure
		//
		Node newNode = null;
		try {
			newNode = closureRepository.addNode(parentDirNodeId, name);
		} catch (Exception e) {
			throw new Exception("Error adding directory node", e);
		}
		
		//
		// get store
		//
		Store store = getStoreById(parentDirectory.getStoreId());
		
		String dirRelPathString = (parentDirectory.getRelativePath() + File.separator + name).replace("\\", "/");
		
		PathResource resource = new DirectoryResource();
		resource.setNodeId(newNode.getNodeId());
		resource.setResourceType(ResourceType.DIRECTORY);
		resource.setDateCreated(newNode.getDateCreated());
		resource.setDateUpdated(newNode.getDateUpdated());
		resource.setPathName(name);
		resource.setParentNodeId(newNode.getParentNodeId());
		resource.setRelativePath(dirRelPathString);
		resource.setStoreId(store.getId());
		
		//
		// add entry to eas_path_resource
		//
		jdbcTemplate.update(
				"insert into eas_path_resource (node_id, store_id, path_name, path_type, relative_path) " +
				"values (?, ?, ?, ?, ?)", resource.getNodeId(), resource.getStoreId(), resource.getPathName(),
				resource.getResourceType().getTypeString(), resource.getRelativePath());		
		
		//
		// add entry to eas_directory resource
		//
		jdbcTemplate.update(
				"insert into eas_directory_resource (node_id) values (?)", resource.getNodeId());		
		
		//
		// create directory on local file system. If there is any error throw a RuntimeException,
		// or update the @Transactional annotation to rollback for any exception type, i.e.,
		// @Transactional(rollbackFor=Exception.class)
		//
		Path newDirectoryPath = Paths.get(store.getPath() + resource.getRelativePath());
		try {
			FileUtil.createDirectory(newDirectoryPath, true);
		} catch (Exception e) {
			throw new Exception("Failed to create directory => " + newDirectoryPath.toString().replace("\\", "/") + ". " + e.getMessage(), e);
		}
		
		return (DirectoryResource)resource;
		
	}
	
	/**
	 * Adds a root directory. This is a directory with no parent, and are the top most
	 * directory for a store (the parent directory is the store directory.)
	 * 
	 * @param storeId - store id
	 * @param storePath - path for the store
	 * @param rootNodeId - id of store's root directory node
	 * @param name - path name of store's root directory node
	 * @return
	 * @throws Exception
	 */
	private DirectoryResource addRootDirectory(Long storeId, Path storePath, Long rootNodeId, String name) throws Exception {
		
		String storePathString = storePath.toString().replace("\\", "/");
		
		//
		// add entry to eas_node and eas_closure
		//
		Node newNode = null;
		try {
			newNode = closureRepository.addNode(rootNodeId, 0L, name); // parent id set to 0 for all root nodes.
		} catch (Exception e) {
			throw new Exception("Error adding directory node", e);
		}
		
		String dirRelPathString = (File.separator + name).replace("\\", "/");
		
		PathResource dirResource = new DirectoryResource();
		dirResource.setNodeId(newNode.getNodeId());
		dirResource.setResourceType(ResourceType.DIRECTORY);
		dirResource.setDateCreated(newNode.getDateCreated());
		dirResource.setDateUpdated(newNode.getDateUpdated());
		dirResource.setPathName(name);
		dirResource.setParentNodeId(newNode.getParentNodeId());
		dirResource.setRelativePath(dirRelPathString);
		dirResource.setStoreId(storeId);
		
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
		
		//
		// create directory on local file system. If there is any error throw a RuntimeException,
		// or update the @Transactional annotation to rollback for any exception type, i.e.,
		// @Transactional(rollbackFor=Exception.class)
		//
		Path newDirectoryPath = Paths.get(storePath + dirResource.getRelativePath());
		try {
			FileUtil.createDirectory(newDirectoryPath, true);
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
	 * @return
	 */
	private boolean hasChildPathResource(Long dirNodeId, String name, ResourceType type) throws Exception {
		
		List<PathResource> childResources = getPathResourceTree(dirNodeId, 1);
		if(childResources != null && childResources.size() > 0){
			for(PathResource pr : childResources){
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

}
