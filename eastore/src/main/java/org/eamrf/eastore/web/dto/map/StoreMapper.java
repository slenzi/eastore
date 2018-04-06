package org.eamrf.eastore.web.dto.map;

import java.util.ArrayList;
import java.util.List;

import org.eamrf.core.util.CollectionUtil;
import org.eamrf.eastore.web.dto.model.StoreDto;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;

/**
 * @author slenzi
 *
 */
public class StoreMapper {
	
	public StoreMapper() {
		
	}
	
	/**
	 * Map store entity to store dto
	 * 
	 * @param store
	 * @return
	 */
	public StoreDto map(Store store) {
		
		return map(store, new DirectoryResourceMapper());
		
	}
	
	private StoreDto map(Store store, DirectoryResourceMapper directoryMapper) {
		
		if(store == null) {
			return null;
		}
		
		StoreDto dto = new StoreDto();
		
		dto.setId(store.getId());
		dto.setName(store.getName());
		dto.setDescription(store.getDescription());
		dto.setPath(store.getPath());
		dto.setNodeId(store.getNodeId());
		dto.setMaxFileSizeBytes(store.getMaxFileSizeBytes());
		dto.setAccessRule(store.getAccessRule());
		dto.setDateCreated(store.getDateCreated());
		dto.setDateUpdated(store.getDateUpdated());
		dto.setRootDir(directoryMapper.map(store.getRootDir()));
		
		return dto;		
	
		
	}
	
	/**
	 * Map lsit of store entities to list of store dtos
	 * 
	 * @param stores
	 * @return
	 */
	public List<StoreDto> map(List<Store> stores){
		
		if(CollectionUtil.isEmpty(stores)) {
			return null;
		}
		List<StoreDto> dtoList = new ArrayList<StoreDto>();
		DirectoryResourceMapper directoryMapper = new DirectoryResourceMapper(); 
		for(Store s : stores) {
			dtoList.add(map(s,directoryMapper));
		}
		return dtoList;
		
	}	

}
