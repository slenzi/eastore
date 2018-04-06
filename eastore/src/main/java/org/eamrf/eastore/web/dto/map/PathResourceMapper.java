package org.eamrf.eastore.web.dto.map;

import java.util.ArrayList;
import java.util.List;

import org.eamrf.core.util.CollectionUtil;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.web.dto.model.PathResourceDto;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.DirectoryResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.FileMetaResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.PathResource;

/**
 * 
 * @author slenzi
 *
 */
public class PathResourceMapper {
	
	public PathResourceMapper() {

	}

	/**
	 * Map path resource entity to path resource dto.
	 * 
	 * @param resource
	 * @return
	 */
	public PathResourceDto map(PathResource resource) throws ServiceException {
		
		return map(resource, new StoreMapper());
		
	}
	
	private PathResourceDto map(PathResource resource, StoreMapper storeMapper) throws ServiceException {
		
		if(resource == null) {
			return null;
		}
		
		if(resource instanceof FileMetaResource) {
			
			FileMetaResourceMapper mapper = new FileMetaResourceMapper();
			return mapper.map((FileMetaResource) resource);
			
		}else if(resource instanceof DirectoryResource) {
			
			DirectoryResourceMapper mapper = new DirectoryResourceMapper();
			return mapper.map((DirectoryResource) resource);
			
		}else {
			throw new ServiceException("Don't know how to map PathResource entity of type " + 
					resource.getResourceType().getTypeString() + " to data transfer object.");
		}	
		
	}
	
	/**
	 * Map list of path resource entities to list of path resource dtos
	 * 
	 * @param resources
	 * @return
	 */
	public List<PathResourceDto> map(List<PathResource> resources) throws ServiceException {
		
		if(!CollectionUtil.isEmpty(resources)) {
			List<PathResourceDto> dtoList = new ArrayList<PathResourceDto>();
			StoreMapper storeMapper = new StoreMapper();
			for(PathResource r : resources) {
				dtoList.add(map(r,storeMapper));
			}
			return dtoList;
		}
		return null;
		
		
	}
	
}
