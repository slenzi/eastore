package org.eamrf.eastore.web.dto.map;

import java.util.ArrayList;
import java.util.List;

import org.eamrf.core.util.CollectionUtil;
import org.eamrf.eastore.web.dto.model.DirectoryResourceDto;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.DirectoryResource;

/**
 * 
 * @author slenzi
 *
 */
public class DirectoryResourceMapper {
	
	public DirectoryResourceMapper() {
		
	}
	
	/**
	 * Map directory resource entity to directory resource dto
	 * 
	 * @param resource
	 * @return
	 */
	public DirectoryResourceDto map(DirectoryResource resource) {
		
		return map(resource, new StoreMapper());
		
	}
	
	private DirectoryResourceDto map(DirectoryResource resource, StoreMapper storeMapper) {
		
		if(resource == null) {
			return null;
		}
		
		DirectoryResourceDto dto = new DirectoryResourceDto();
		
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
		
		// map directory resource data (currently not extra attributes in DirectoryResource)
		
		return dto;		
		
	}
	
	/**
	 * Map list of directory resource entities to list of directory resource dtos
	 * 
	 * @param resources
	 * @return
	 */
	public List<DirectoryResourceDto> map(List<DirectoryResource> resources){
		
		if(CollectionUtil.isEmpty(resources)) {
			return null;
		}
		List<DirectoryResourceDto> dtoList = new ArrayList<DirectoryResourceDto>();
		StoreMapper storeMapper = new StoreMapper();
		for(DirectoryResource r : resources) {
			dtoList.add(map(r,storeMapper));
		}
		return dtoList;
		
	}	

}
