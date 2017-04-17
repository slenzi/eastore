package org.eamrf.repository.oracle.ecoguser.eastore;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.DateUtil;
import org.eamrf.core.util.FileUtil;
import org.eamrf.eastore.core.aop.profiler.MethodTimer;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.service.FileSystemUtil;
import org.eamrf.repository.oracle.ecoguser.eastore.model.BinaryResource;
import org.eamrf.repository.oracle.ecoguser.eastore.model.DirectoryResource;
import org.eamrf.repository.oracle.ecoguser.eastore.model.FileMetaResource;
import org.eamrf.repository.oracle.ecoguser.eastore.model.Node;
import org.eamrf.repository.oracle.ecoguser.eastore.model.PathResource;
import org.eamrf.repository.oracle.ecoguser.eastore.model.ResourceType;
import org.eamrf.repository.oracle.ecoguser.eastore.model.Store;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
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
    
    @Autowired
    private FileSystemUtil fileSystemUtil;    
    
    // common query element used by several methods below
    private final String SQL_PATH_RESOURCE_COMMON =
			"select " +
			"n.node_id, n.parent_node_id, c.child_node_id, n.creation_date, n.updated_date, r.path_type, " +  
			"r.path_name, r.relative_path, r.store_id, fmr.mime_type, fmr.file_size, fmr.is_file_data_in_db, " + 
			"s.store_id, s.store_name, s.store_description, s.store_path, s.node_id as store_root_node_id, " +
			"s.max_file_size_in_db, s.creation_date as store_creation_date, s.updated_date as store_updated_date " +
			"from eas_closure c " +
			"inner join eas_node n on c.child_node_id = n.node_id " +  
			"inner join eas_path_resource r on n.node_id = r.node_id " +
			"inner join eas_store s on r.store_id = s.store_id " +
			"left join eas_directory_resource dr on r.node_id = dr.node_id " +  
			"left join eas_file_meta_resource fmr on r.node_id = fmr.node_id ";    		
	
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

		Store s = new Store();
		s.setId(rs.getLong("store_id"));
		s.setName(rs.getString("store_name"));
		s.setDescription(rs.getString("store_description"));
		s.setMaxFileSizeBytes(rs.getLong("max_file_size_in_db"));
		s.setPath(Paths.get(rs.getString("store_path")));
		s.setNodeId(rs.getLong("store_root_node_id"));
		s.setDateCreated(rs.getTimestamp("store_creation_date"));
		s.setDateUpdated(rs.getTimestamp("store_updated_date"));
		
		r.setStore(s);
		
		return r;
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
	@MethodTimer
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
	 * Fetch store by name
	 * 
	 * @param storeName
	 * @return
	 * @throws Exception
	 */
	@MethodTimer
	public Store getStoreByName(String storeName) throws Exception {
		
		String sql =
			"select store_id, store_name, store_description, store_path, node_id, "
			+ "max_file_size_in_db, creation_date, updated_date from eas_store "
			+ "where lower(store_name) = ?";
		
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
		}, new Object[] { storeName });		
		
	}	
	
	/**
	 * Fetch all stores
	 * 
	 * @return
	 * @throws Exception
	 */
	@MethodTimer
	public List<Store> getStores() throws Exception {
		
		String sql =
			"select store_id, store_name, store_description, store_path, node_id, "
			+ "max_file_size_in_db, creation_date, updated_date from eas_store";
		
		List<Store> stores = jdbcTemplate.query(
			sql, (rs, rowNum) -> {
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
		
		return stores;
		
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
	@MethodTimer
	public List<PathResource> getPathResourceTree(Long nodeId) throws Exception {
		
		// functionally equivalent to ClosureRepository.getChildMappings(Long nodeId)	
		
		String sql =
			/*
			"select " +
			"n.node_id, n.parent_node_id, c.child_node_id, n.creation_date, n.updated_date, r.path_type, " + 
			"r.path_name, r.relative_path, r.store_id, fmr.mime_type, fmr.file_size, fmr.is_file_data_in_db, " +
			"s.store_id, s.store_name, s.store_description, s.store_path, s.node_id as store_root_node_id, " +
			"s.max_file_size_in_db, s.creation_date as store_creation_date, s.updated_date as store_updated_date " +
			"from eas_closure c " +
			"inner join eas_node n on c.child_node_id = n.node_id " + 
			"inner join eas_path_resource r on n.node_id = r.node_id " +
			"inner join eas_store s on r.store_id = s.store_id " +
			"left join eas_directory_resource dr on r.node_id = dr.node_id " + 
			"left join eas_file_meta_resource fmr on r.node_id = fmr.node_id " +
			*/
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
	@MethodTimer
	public List<PathResource> getPathResourceTree(Long nodeId, int depth) throws Exception {
		
		// functionally equivalent to ClosureRepository.getChildMappings(Long nodeId, int depth)

		String sql =
			/*
			"select " +
			"n.node_id, n.parent_node_id, c.child_node_id, n.creation_date, n.updated_date, r.path_type, " + 
			"r.path_name, r.relative_path, r.store_id, fmr.mime_type, fmr.file_size, fmr.is_file_data_in_db, " +
			"s.store_id, s.store_name, s.store_description, s.store_path, s.node_id as store_root_node_id, " +
			"s.max_file_size_in_db, s.creation_date as store_creation_date, s.updated_date as store_updated_date " +
			"from eas_closure c " +
			"inner join eas_node n on c.child_node_id = n.node_id " +
			"inner join eas_path_resource r on n.node_id = r.node_id " +
			"inner join eas_store s on r.store_id = s.store_id " +
			"left join eas_directory_resource dr on r.node_id = dr.node_id " +
			"left join eas_file_meta_resource fmr on r.node_id = fmr.node_id " +
			*/
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
	@MethodTimer
	public List<PathResource> getParentPathResourceTree(Long nodeId) throws Exception {
		
		// functionally equivalent to ClosureRepository.getParentMappings(Long nodeId)	
		
		String sql =
			"select " +
			"  n2.node_id, n2.parent_node_id, n2.node_id as child_node_id, n2.node_name, n2.creation_date, n2.updated_date, " + 
			"  r.path_type, r.path_name, r.relative_path, r.store_id, fmr.mime_type, fmr.file_size, fmr.is_file_data_in_db, " +
			"  s.store_id, s.store_name, s.store_description, s.store_path, s.node_id as store_root_node_id, " +
			"  s.max_file_size_in_db, s.creation_date as store_creation_date, s.updated_date as store_updated_date " +  
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
			"  r.path_type, r.path_name, r.relative_path, r.store_id, fmr.mime_type, fmr.file_size, fmr.is_file_data_in_db, " +
			"  s.store_id, s.store_name, s.store_description, s.store_path, s.node_id as store_root_node_id, " +
			"  s.max_file_size_in_db, s.creation_date as store_creation_date, s.updated_date as store_updated_date " +  
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
	@MethodTimer
	public Store addStore(String storeName, String storeDesc, Path storePath, String rootDirName, Long maxFileSizeDb) throws Exception {
	
		Long storeId = getNextStoreId();
		Long rootNodeId = closureRepository.getNextNodeId();
		
		Timestamp dtNow = DateUtil.getCurrentTime();
		
		String storePathString = fileSystemUtil.cleanFullPath(storePath.toString());
				
		// add entry to eas_store
		jdbcTemplate.update(
				"insert into eas_store (store_id, store_name, store_description, store_path, "
				+ "node_id, max_file_size_in_db, creation_date, updated_date) " +
				"values (?, ?, ?, ?, ?, ?, ?, ?)", storeId, storeName, storeDesc,
				storePathString, rootNodeId, maxFileSizeDb, dtNow, dtNow);
		
		// create store directory on local file system
		try {
			FileUtil.createDirectory(storePath, true);
		} catch (Exception e) {
			throw new Exception("Failed to create store directory => " + storePathString + ". " + e.getMessage(), e);
		}		
		
		// add root directory for store
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
	 * Adds a new file, but does not add the binary data to eas_binary_resource.
	 * 
	 * If a file with the same name already exists, then the file on disk is updated, and the old binary
	 * data in eas_binary_resource is removed.
	 * 
	 * @param parentDirectory
	 * @param srcFilePath
	 * @param replaceExisting
	 * @return
	 * @throws Exception
	 */
	@MethodTimer
	public FileMetaResource addFileWithoutBinary(
			DirectoryResource parentDirectory, Path srcFilePath, boolean replaceExisting) throws Exception {
			
		String fileName = srcFilePath.getFileName().toString();
		
		// check if directory already contains a file with the same name (case insensitive)
		boolean hasExisting = hasChildPathResource(parentDirectory.getNodeId(), fileName, ResourceType.FILE);
		
		if(hasExisting && replaceExisting){
			
			return _updateFileDiscardOldBinary(parentDirectory, srcFilePath);
		
		}else if(hasExisting && !replaceExisting){
			
			throw new Exception("Directory with dirNodeId " + parentDirectory.getNodeId() + 
					" already contains a file with the name '" + fileName + "', and 'replaceExisting' param is set to false.");
			
		}else{
			
			return _addNewFileWithoutBinary(parentDirectory, srcFilePath);
			
		}
		
	}
	
	/**
	 * Refreshes the data in eas_binary_resource (or adds a new entry) for the file
	 * 
	 * @param fileMetaResource
	 * @return
	 * @throws Exception
	 */
	@MethodTimer
	public FileMetaResource refreshBinaryDataInDatabase(FileMetaResource fileMetaResource) throws Exception {
		
		final Store store = getStoreById(fileMetaResource.getStoreId());
		final Long nodeId = fileMetaResource.getNodeId();
		final Path filePath = fileSystemUtil.buildPath(store, fileMetaResource);
		final boolean haveBinaryInDb = fileMetaResource.getIsBinaryInDatabase();
		long lFileSize = Files.size(filePath);
		final int fileSize = toIntExact(lFileSize);
		
		if(!Files.exists(filePath)){
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
							statement.setLong(2, nodeId);
							
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
							
							statement.setLong(1, nodeId);
							lobCreator.setBlobAsBinaryStream(statement, 2, inStream, fileSize);
							
						}
						
					});
			
			// Update eas_file_meta_resource.is_binary_data_in_db to 'Y'
			jdbcTemplate.update("update eas_file_meta_resource set is_file_data_in_db = 'Y' where node_id = ?", nodeId);			
		
			fileMetaResource.setIsBinaryInDatabase(true);
			
		}
		
		inStream.close();
		
		return fileMetaResource;
		
	}
	
	/**
	 * Internal helper method for adding new files (never to be used to replace an existing file.)
	 * 
	 * This method adds a new FileMetaResource, but does not add data to eas_binary_resource
	 * 
	 * @param dirResource
	 * @param filePath
	 * @return
	 * @throws Exception
	 */
	private FileMetaResource _addNewFileWithoutBinary(DirectoryResource dirResource, Path filePath) throws Exception {
		
		String fileName = filePath.getFileName().toString();
		Long fileSizeBytes = FileUtil.getFileSize(filePath);
		String fileMimeType = FileUtil.detectMimeType(filePath);
		Boolean isBinaryInDb = false;

		// add entry to eas_node and eas_closure
		Node newNode = null;
		try {
			newNode = closureRepository.addNode(dirResource.getNodeId(), fileName);
		} catch (Exception e) {
			throw new Exception("Error adding directory node", e);
		}
		
		Store store = getStoreById(dirResource.getStoreId());
		
		String fileRelPathString = fileSystemUtil.buildRelativePath(dirResource, fileName);
		
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
		
		// add entry to eas_path_resource
		jdbcTemplate.update(
				"insert into eas_path_resource (node_id, store_id, path_name, path_type, relative_path) " +
				"values (?, ?, ?, ?, ?)", resource.getNodeId(), resource.getStoreId(), resource.getPathName(),
				resource.getResourceType().getTypeString(), resource.getRelativePath());
		
		// add entry to eas_file_meta_resource
		jdbcTemplate.update(
				"insert into eas_file_meta_resource (node_id, file_size, mime_type, is_file_data_in_db) values (?, ?, ?, ?)",
					resource.getNodeId(), fileSizeBytes, fileMimeType, ((isBinaryInDb) ? "Y" : "N"));
		
		// copy file to directory in the tree
		Path newFilePath = fileSystemUtil.buildPath(store, resource);
		try {
			FileUtil.copyFile(filePath, newFilePath);
		} catch (Exception e) {
			throw new Exception("Failed to copy file from => " + filePath.toString() + 
					" to " + newFilePath.toString() + ". " + e.getMessage(), e);
		}
		
		return (FileMetaResource)resource;
		
	}
	
	/**
	 * Updates the physical file on disk, then removes the old binary data from the database.
	 * 
	 * @param dirResource - The directory where the new file will go
	 * @param srcFilePath - The new file to add. The old binary data in the database will be removed.
	 * @return
	 * @throws Exception
	 */
	private FileMetaResource _updateFileDiscardOldBinary(DirectoryResource dirResource, Path srcFilePath) throws Exception {
		
		String newFileName = srcFilePath.getFileName().toString();
		
		// get current (file) PathResource (case-insensitive search, so we can use the new file name)
		PathResource currPathRes = getChildPathResource(dirResource.getNodeId(), newFileName, ResourceType.FILE);
		if(currPathRes == null){
			throw new Exception("Cannot update file " + newFileName + " in directory node => " + 
					dirResource.getNodeId() + ", failed to fetch child (file) resource, return object was null.");
		}		
		
		FileMetaResource currFileRes = (FileMetaResource)currPathRes;
		Store store = getStoreById(dirResource.getStoreId());
		
		Long newFileSizeBytes = FileUtil.getFileSize(srcFilePath);
		String newFileMimeType = FileUtil.detectMimeType(srcFilePath);
		String newRelFilePath = fileSystemUtil.buildRelativePath(dirResource, newFileName);
		
		// old/current tree path for the old physical file
		Path oldFilePath = fileSystemUtil.buildPath(store, currFileRes);
		// new tree path for the new physical file
		Path newFilePath = fileSystemUtil.buildPath(store, newRelFilePath);
		
		// check if we have existing binary data in the database, we might need to remove it.
		if(currFileRes.getIsBinaryInDatabase()){
			
			// remove existing binary data in database (it's the old file)
			jdbcTemplate.update("delete from eas_binary_resource where node_id = ?", currPathRes.getNodeId());
			
			currFileRes.setIsBinaryInDatabase(false);
			
		}
		
		currFileRes.setMimeType(newFileMimeType);
		currFileRes.setFileSize(newFileSizeBytes);
		currFileRes.setBinaryResource(null);
		currFileRes.setDateUpdated(DateUtil.getCurrentTime());
		currFileRes.setPathName(newFileName);
		currFileRes.setRelativePath(newRelFilePath);
			
		// update eas_file_meta_resource
		jdbcTemplate.update(
				"update eas_file_meta_resource set is_file_data_in_db = 'N', file_size = ?, mime_type = ? "
				+ " where node_id = ?",
				currFileRes.getFileSize(), currFileRes.getMimeType(), currFileRes.getNodeId());
			
		// eas_path_resource to account for differences in uppercase/lowercase of file name
		jdbcTemplate.update("update eas_path_resource set path_name = ?, relative_path = ? where node_id = ?",
				currFileRes.getPathName(), currFileRes.getRelativePath(), currFileRes.getNodeId());			
		
		// update eas_node
		closureRepository.updateNodeMeta(currFileRes);
		
		// delete old file on local disk
		try {
			FileUtil.deletePath(oldFilePath);
		} catch (Exception e) {
			throw new Exception("Failed to remove old file at => " + oldFilePath.toString() + 
					". " + e.getMessage(), e);
		}
		
		// copy new file to directory in the tree
		try {
			FileUtil.copyFile(srcFilePath, newFilePath);
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
	 * @param parentDirNodeId - Id of parent directory node.
	 * @param name - name of new directory node.
	 * @return
	 * @throws Exception
	 */
	@MethodTimer
	public DirectoryResource addDirectory(Long parentDirNodeId, String name) throws Exception {
		
		// make sure parentDirNodeId is actually of a directory path resource type
		DirectoryResource parentDirectory = getDirectory(parentDirNodeId);
		
		// make sure directory doesn't already contain a sub-directory with the same name	
		if(hasChildPathResource(parentDirNodeId, name, ResourceType.DIRECTORY)){
			throw new Exception("Directory with dirNodeId " + parentDirNodeId + 
					" already contains a sub-directory with the name '" + name + "'");			
		}
		
		// add entry to eas_node and eas_closure
		Node newNode = null;
		try {
			newNode = closureRepository.addNode(parentDirNodeId, name);
		} catch (Exception e) {
			throw new Exception("Error adding directory node", e);
		}
		
		Store store = getStoreById(parentDirectory.getStoreId());
		
		String dirRelPathString = fileSystemUtil.buildRelativePath(parentDirectory, name);
		
		PathResource resource = new DirectoryResource();
		resource.setNodeId(newNode.getNodeId());
		resource.setResourceType(ResourceType.DIRECTORY);
		resource.setDateCreated(newNode.getDateCreated());
		resource.setDateUpdated(newNode.getDateUpdated());
		resource.setPathName(name);
		resource.setParentNodeId(newNode.getParentNodeId());
		resource.setRelativePath(dirRelPathString);
		resource.setStoreId(store.getId());
		
		// add entry to eas_path_resource
		jdbcTemplate.update(
				"insert into eas_path_resource (node_id, store_id, path_name, path_type, relative_path) " +
				"values (?, ?, ?, ?, ?)", resource.getNodeId(), resource.getStoreId(), resource.getPathName(),
				resource.getResourceType().getTypeString(), resource.getRelativePath());		
		
		// add entry to eas_directory_resource
		jdbcTemplate.update(
				"insert into eas_directory_resource (node_id) values (?)", resource.getNodeId());		
		
		// create directory on local file system. If there is any error throw a RuntimeException,
		// or update the @Transactional annotation to rollback for any exception type, i.e.,
		// @Transactional(rollbackFor=Exception.class)
		Path newDirectoryPath = fileSystemUtil.buildPath(store, resource);
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
		
		// add entry to eas_node and eas_closure
		Node newNode = null;
		try {
			newNode = closureRepository.addNode(rootNodeId, 0L, name); // parent id set to 0 for all root nodes.
		} catch (Exception e) {
			throw new Exception("Error adding directory node", e);
		}
		
		String dirRelPathString = fileSystemUtil.cleanRelativePath(name);
		
		PathResource dirResource = new DirectoryResource();
		dirResource.setNodeId(newNode.getNodeId());
		dirResource.setResourceType(ResourceType.DIRECTORY);
		dirResource.setDateCreated(newNode.getDateCreated());
		dirResource.setDateUpdated(newNode.getDateUpdated());
		dirResource.setPathName(name);
		dirResource.setParentNodeId(newNode.getParentNodeId());
		dirResource.setRelativePath(dirRelPathString);
		dirResource.setStoreId(storeId);
		
		// add entry to eas_path_resource
		jdbcTemplate.update(
				"insert into eas_path_resource (node_id, store_id, path_name, path_type, relative_path) " +
				"values (?, ?, ?, ?, ?)", dirResource.getNodeId(), dirResource.getStoreId(), dirResource.getPathName(),
				dirResource.getResourceType().getTypeString(), dirResource.getRelativePath());		
		
		// add entry to eas_directory_resource
		jdbcTemplate.update(
				"insert into eas_directory_resource (node_id) values (?)", dirResource.getNodeId());		
		
		// create directory on local file system. If there is any error throw a RuntimeException,
		// or update the @Transactional annotation to rollback for any exception type, i.e.,
		// @Transactional(rollbackFor=Exception.class)
		Path newDirectoryPath = fileSystemUtil.buildPath(storePath, dirResource);
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
	@MethodTimer
	public boolean hasChildPathResource(Long dirNodeId, String name, ResourceType type) throws Exception {
		
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
	 * Fetch the child resource for the directory (first level only) with the matching name, of the specified type.
	 * 
	 * @param dirNodeId
	 * @param name
	 * @param type
	 * @return
	 * @throws Exception
	 */
	@MethodTimer
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
	 * Returns true if node 'dirNodeB' is a child node (at any depth) of node 'dirNodeA'
	 * 
	 * @param dirNodeA
	 * @param dirNodeB
	 * @return
	 */
	@MethodTimer
	public boolean isChild(Long dirNodeA, Long dirNodeB) throws Exception {
		
		List<PathResource> parentResources = getParentPathResourceTree(dirNodeB);
		if(parentResources == null || parentResources.size() == 0){
			throw new Exception("No 'parent' PathResource mappings for dir node id = > " + dirNodeB);
		}
		
		return parentResources.stream()
	            .anyMatch(r -> r.getParentNodeId().equals(dirNodeA));		
		
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
	@MethodTimer
	public PathResource getPathResource(Long nodeId) throws Exception {
		
		List<PathResource> childResources = getPathResourceTree(nodeId, 0);
		if(childResources == null){
			throw new Exception("No PathResource found for node id => " + nodeId);
		}else if(childResources.size() != 1){
			// should never get here...
			throw new Exception("Expected 1 PathResource for node id => " + nodeId + ", but fetched " + childResources.size());
		}
		
		return childResources.get(0);
		
	}
	
	/**
	 * Fetch a path resource by store and relative path.
	 * 
	 * @param storeName - the store name
	 * @param relativePath - the relative path within the store
	 * @return
	 * @throws Exception
	 */
	public PathResource getPathResource(String storeName, String relativePath) throws Exception {
		
		String sql =
			/*
			"select " +
			"n.node_id, n.parent_node_id, c.child_node_id, n.creation_date, n.updated_date, r.path_type, " +  
			"r.path_name, r.relative_path, r.store_id, fmr.mime_type, fmr.file_size, fmr.is_file_data_in_db, " + 
			"s.store_id, s.store_name, s.store_description, s.store_path, s.node_id as store_root_node_id, " +
			"s.max_file_size_in_db, s.creation_date as store_creation_date, s.updated_date as store_updated_date, c.depth " +
			"from eas_closure c " +
			"inner join eas_node n on c.child_node_id = n.node_id " +  
			"inner join eas_path_resource r on n.node_id = r.node_id " +
			"inner join eas_store s on r.store_id = s.store_id " +
			"left join eas_directory_resource dr on r.node_id = dr.node_id " +  
			"left join eas_file_meta_resource fmr on r.node_id = fmr.node_id " +
			*/
			SQL_PATH_RESOURCE_COMMON +
			"where lower(s.store_name) = ? and lower(r.relative_path) = ? and c.depth = 0 " +
			"order by c.depth, n.node_name";
			
			return jdbcTemplate.queryForObject(sql, resourcePathRowMapper,
					new Object[] { storeName.toLowerCase(), relativePath.toLowerCase() });		
		
	}	
	
	/**
	 * Fetch a FileMetaResource
	 * 
	 * @param nodeId - file node Id
	 * @param includeBinary - pass true to include the binary data for the file, pass false not to.
	 * @return
	 * @throws Exception
	 */
	@MethodTimer
	public FileMetaResource getFileMetaResource(Long nodeId, boolean includeBinary) throws Exception {
		
		PathResource resource = getPathResource(nodeId);
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
	@MethodTimer
	public FileMetaResource getFileMetaResource(String storeName, String relativePath, boolean includeBinary) throws Exception {
		
		PathResource resource = getPathResource(storeName, relativePath);
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
		
			Store store = resource.getStore();
			if(store == null){
				Long storeId = resource.getStoreId();
				if(storeId == null){
					throw new Exception("FileMetaResource storeId value is null. Need store path information to read file "
							+ "data from local file system. Cannot populate FileMetaResource with binary data.");
				}
				store = getStoreById(storeId);
				if(store == null){
					throw new Exception("Failed to fetch store from DB for FileMetaResource, returned store object "
							+ "was null, storeId => " + storeId + " Cannot populate FileMetaResource with binary data.");
				}
				resource.setStore(store);
			}
			Path pathToFile = fileSystemUtil.buildPath(store, resource);
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
	 * Fetch a DirectoryResource
	 * 
	 * @param nodeId
	 * @return
	 * @throws Exception
	 */
	@MethodTimer
	public DirectoryResource getDirectory(Long nodeId) throws Exception {
		
		PathResource resource = getPathResource(nodeId);
		if(resource.getResourceType() == ResourceType.DIRECTORY){
			return (DirectoryResource)resource;
		}else{
			throw new Exception("Error fetching directory resource. Node id => " + nodeId + " is not a directory resource.");
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
		if(resource.getResourceType() == ResourceType.DIRECTORY){
			return (DirectoryResource)resource;
		}else{
			throw new Exception("Error fetching directory resource, storeName=" + storeName + 
					", relativePath=" + relativePath + ", is not a directory resource.");
		}
		
	}	
	
	/**
	 * Remove a file
	 * 
	 * @param store
	 * @param resource
	 * @throws Exception
	 */
	@MethodTimer
	public void removeFile(Store store, FileMetaResource resource) throws Exception {
		
		Path filePath = fileSystemUtil.buildPath(store, resource);
		
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
		FileUtil.deletePath(filePath);		
		
	}
	
	/**
	 * Removing a directory. THE DIRECTORY MUST BE EMPTY!
	 * 
	 * @param store
	 * @param resource
	 * @throws Exception
	 */
	@MethodTimer
	public void removeDirectory(Store store, DirectoryResource resource) throws Exception {
		
		Path dirPath = fileSystemUtil.buildPath(store, resource);
		
		// delete from eas_directory_resource
		jdbcTemplate.update("delete from eas_directory_resource where node_id = ?", resource.getNodeId());
		
		// delete from eas_path_resource
		jdbcTemplate.update("delete from eas_path_resource where node_id = ?", resource.getNodeId());
		
		// delete closure data and node
		closureRepository.deleteNode(resource.getNodeId());
		
		// remove file for local file system
		FileUtil.deletePath(dirPath);		
		
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
		
		// replace existing file
		if(hasExisting && replaceExisting){
			
			Store sourceStore = getStoreById(fileToMove.getStoreId());
			Store destinationStore = getStoreById(destDir.getStoreId());
			
			// current/old path to file on local file system
			Path oldFullPath = fileSystemUtil.buildPath(sourceStore, fileToMove);
			// new path to file on local file system
			Path newFullPath = fileSystemUtil.buildPath(destinationStore, destDir, fileName);
			// new relative path for eas_path_resource
			String newRelativePath = fileSystemUtil.buildRelativePath(destDir, fileName);
			
			// delete existing file in destination directory
			removeFile(destinationStore, (FileMetaResource)existingFile);
			
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
				FileUtil.moveFile(oldFullPath, newFullPath);
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
			
			Store sourceStore = getStoreById(fileToMove.getStoreId());
			Store destinationStore = getStoreById(destDir.getStoreId());
			
			// current/old path to file on local file system
			Path oldFullPath = fileSystemUtil.buildPath(sourceStore, fileToMove);
			// new path to file on local file system
			Path newFullPath = fileSystemUtil.buildPath(destinationStore, destDir, fileName);
			// new relative path for eas_path_resource
			String newRelativePath = fileSystemUtil.buildRelativePath(destDir, fileName);
			
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
				FileUtil.moveFile(oldFullPath, newFullPath);
			} catch (Exception e) {
				throw new Exception("Failed to move file " + fileToMove.getNodeId() + " to directory " + destDir.getNodeId() + 
						". oldFullPath => " + oldFullPath.toString() + ", newFullPath => " + newFullPath.toString() + 
						". " + e.getMessage(), e);
			}
			
		}
		
	}

}
