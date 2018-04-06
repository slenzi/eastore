package org.eamrf.eastore.web.dto.map;

import java.util.ArrayList;
import java.util.List;

import org.eamrf.core.util.CollectionUtil;
import org.eamrf.eastore.web.dto.model.FileMetaResourceDto;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.FileMetaResource;

/**
 * 
 * @author slenzi
 *
 */
public class FileMetaResourceMapper {
	
	public FileMetaResourceMapper() {
		
	}
	
	/**
	 * Map file meta resource entity to file meta resource dto
	 * 
	 * @param resource
	 * @return
	 */
	public FileMetaResourceDto map(FileMetaResource resource) {
		
		return map(resource, new StoreMapper(), new DirectoryResourceMapper());
		
	}
	
	private FileMetaResourceDto map(FileMetaResource resource, StoreMapper storeMapper, DirectoryResourceMapper directoryMapper) {
		
		if(resource == null) {
			return null;
		}
		
		FileMetaResourceDto dto = new FileMetaResourceDto();
		
		// map node data
		dto.setNodeId(resource.getNodeId());
		dto.setParentNodeId(resource.getParentNodeId());
		dto.setChildNodeId(resource.getChildNodeId());
		dto.setNodeName(resource.getNodeName());
		dto.setDateCreated(resource.getDateCreated());
		dto.setDateUpdated(resource.getDateUpdated());
		
		// map path resource data
		dto.setStoreId(resource.getStoreId());
		dto.setResourceType(resource.getResourceType());
		dto.setRelativePath(resource.getRelativePath());
		dto.setStore(storeMapper.map(resource.getStore()));
		dto.setDesc(resource.getDesc());
		dto.setReadGroup1(resource.getReadGroup1());
		dto.setWriteGroup1(resource.getWriteGroup1());
		dto.setExecuteGroup1(resource.getExecuteGroup1());
		dto.setCanRead(resource.getCanRead());
		dto.setCanWrite(resource.getCanWrite());
		dto.setCanExecute(resource.getCanExecute());		
		
		// map file resource data
		dto.setFileSize(resource.getFileSize());
		dto.setMimeType(resource.getMimeType());
		dto.setIsBinaryInDatabase(resource.getIsBinaryInDatabase());
		dto.setDirectory(directoryMapper.map(resource.getDirectory()));
		
		return dto;		
		
	}
	
	/**
	 * Map list of file meta resource entities to list of file meta resource dtos
	 * 
	 * @param resources
	 * @return
	 */
	public List<FileMetaResourceDto> map(List<FileMetaResource> resources){
		
		if(CollectionUtil.isEmpty(resources)) {
			return null;
		}
		StoreMapper storeMapper = new StoreMapper();
		DirectoryResourceMapper directoryMapper = new DirectoryResourceMapper();		
		List<FileMetaResourceDto> dtoList = new ArrayList<FileMetaResourceDto>();
		for(FileMetaResource r : resources) {
			dtoList.add(map(r,storeMapper,directoryMapper));
		}
		return dtoList;
		
	}

}
