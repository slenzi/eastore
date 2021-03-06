package org.eamrf.eastore.core.service.tree.file;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.DirectoryResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.PathResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;
import org.springframework.stereotype.Service;

/**
 * Helper methods for working with our file system.
 * 
 * @author slenzi
 */
//@Service
public abstract class PathResourceUtil {

	public static Path buildPath(Path path, PathResource resource){
		return Paths.get(path + resource.getRelativePath());
	}
	
	public static Path buildPath(Store store, String relativePath){
		return Paths.get(store.getPath() + cleanRelativePath(relativePath));
	}	
	
	public static Path buildPath(Store store, PathResource resource){
		return Paths.get(store.getPath() + resource.getRelativePath());
	}
	
	public static Path buildPath(Store store, DirectoryResource dirResource, String fileName){
		return Paths.get(store.getPath() + dirResource.getRelativePath() + File.separator + fileName);
	}	
	
	public static String buildRelativePath(DirectoryResource dirResource, String fileName){
		return (dirResource.getRelativePath() + File.separator + fileName).replace("\\", "/");
	}
	
	public static String cleanFullPath(String path){
		if(path == null){
			return null;
		}
		path = path.trim();
		if(path.equals("")){
			return path;
		}
		path = path.replace("\\", "/");
		return path;
	}	
	
	public static String cleanRelativePath(String path){
		if(path == null){
			return null;
		}
		path = path.trim();
		if(path.equals("")){
			return path;
		}
		path = path.replace("\\", "/");
		if(!path.startsWith("/")){
			path = "/" + path;
		}
		return path;
	}

}
