/**
 * 
 */
package org.eamrf.eastore.web.jaxrs.core.rs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.List;

import javax.activation.DataHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.CollectionUtil;
import org.eamrf.core.util.StringUtil;
import org.eamrf.eastore.core.aop.profiler.MethodTimer;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.service.file.FileService;
import org.eamrf.eastore.core.service.tree.file.PathResourceUtil;
import org.eamrf.eastore.core.service.upload.UploadPipeline;
import org.eamrf.eastore.web.dto.map.DirectoryResourceMapper;
import org.eamrf.eastore.web.dto.map.StoreMapper;
import org.eamrf.eastore.web.dto.model.DirectoryResourceDto;
import org.eamrf.eastore.web.dto.model.StoreDto;
import org.eamrf.eastore.web.jaxrs.BaseResourceHandler;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.DirectoryResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.FileMetaResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Store.AccessRule;
import org.eamrf.web.rs.exception.WebServiceException;
import org.eamrf.web.rs.exception.WebServiceException.WebExceptionType;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * JAX-RS resource for modifying our file system (edit files and directories, etc)
 * 
 * @author slenzi
 */
@Path("/fsys/action")
@Service("fileSystemActionResource")
public class FileSystemActionResource extends BaseResourceHandler {

    @InjectLogger
    private Logger logger;
    
    @Autowired
    private UploadPipeline uploadPipeline;
    
    @Autowired
    private FileService fileService;  
    
    @Autowired
    private HttpSession session;
    
    @Autowired
    private HttpServletRequest request;   
    
	public FileSystemActionResource() {

	}

	/* (non-Javadoc)
	 * @see org.eamrf.eastore.web.jaxrs.BaseResourceHandler#getLogger()
	 */
	@Override
	public Logger getLogger() {
		return logger;
	}
	
	/**
	 * Download a file by it's file node ID.
	 * 
	 * @param fileId
	 * @param userId - id of user completing action
	 * @return
	 * @throws WebServiceException
	 */
	@GET
	@Path("/download/userId/{userId}/id/{fileId}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadFile(
			@PathParam("fileId") Long fileId, @PathParam("userId") String userId) throws WebServiceException {
		
		validateUserId(userId);
		
		if(fileId == null){
			handleError("Missing fileId path param", WebExceptionType.CODE_IO_ERROR);
		}
		
		FileMetaResource fileMeta = null;
		try {
			fileMeta = fileService.getFileMetaResource(fileId, userId, true);
		} catch (ServiceException e) {
			handleError("Error downloading file, failed to get file resource with binary data, " + 
					e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
		
		if(fileMeta == null){
			handleError("Returned FileMetaResource object was null. fileId=" + fileId, WebExceptionType.CODE_IO_ERROR);
		}		
		
		return writeFileToResponse(fileMeta);
		
	}
	
	/**
	 * Download a file by store name and relative path value within the store.
	 * 
	 * @param storeName
	 * @param list
	 * @param userId - id of user completing action
	 * @return
	 * @throws WebServiceException
	 */
	@GET
	@Path("/download/userId/{userId}/{storeName}/{relPath:.+}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadFile(
			@PathParam("storeName") String storeName,
			@PathParam("relPath") List<PathSegment> list,
			@PathParam("userId") String userId) throws WebServiceException {
		
		validateUserId(userId);
		
		if(StringUtil.isNullEmpty(storeName) || list == null || list.size() == 0){
			handleError("Missing storeName, and/or relPath segment parameters", WebExceptionType.CODE_IO_ERROR);
		}	
		storeName = storeName.trim();
		String relPath = buildRelativePathSegment(list);
		logger.info("File Download: storeName=" + storeName + ", relPath=" + relPath);
		
		FileMetaResource fileMeta = null;
		try {
			fileMeta = fileService.getFileMetaResource(storeName, relPath, userId, true);
		} catch (ServiceException e) {
			handleError("Error downloading file, failed to get file resource with binary data, " + 
					e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
		
		if(fileMeta == null){
			handleError("Returned FileMetaResource object was null. storeName=" + storeName + 
					", relPath=" + relPath, WebExceptionType.CODE_IO_ERROR);
		}
		
		return writeFileToResponse(fileMeta);
		
	}
	
	/**
	 * Uses request dispatcher to 'load' the resource
	 * 
	 * @param storeName
	 * @param list
	 * @return
	 * @throws WebServiceException
	 */
	@GET
	@Path("/dispatch/{storeName}/{relPath:.+}")
	public void dispatchFile(
			@PathParam("storeName") String storeName,
			@PathParam("relPath") List<PathSegment> list) throws WebServiceException {
		
		handleError("Feature not supported", WebExceptionType.CODE_IO_ERROR);
		
		// TODO - Cannot use request dispatcher on resources that live outside the
		// current servlet context. For a CMS site you should put the file store (all resources)
		// under the /WEB-INF/jsp directory. Perhaps something like /WEB-INF/jsp/cms/sites/{storeName}
		
		/*
		
		if(StringUtil.isNullEmpty(storeName) || list == null || list.size() == 0){
			handleError("Missing storeName, and/or relPath segment parameters", WebExceptionType.CODE_IO_ERROR);
		}	
		storeName = storeName.trim();
		String relPath = buildRelativePathSegment(list);
		logger.info("File Download: storeName=" + storeName + ", relPath=" + relPath);
		
		FileMetaResource fileMeta = null;
		try {
			fileMeta = fileSystemService.getFileMetaResource(storeName, relPath, false);
		} catch (ServiceException e) {
			handleError("Error downloading file, failed to get file resource with binary data, " + 
					e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
		Store store = fileMeta.getStore();
		java.nio.file.Path pathToFile = fileSystemUtil.buildPath(store, fileMeta);
		
		//RequestDispatcher requestDispatcher = request.getRequestDispatcher(forwardPath);
		
		*/
		
	}
	
	private String buildRelativePathSegment(List<PathSegment> list){
		
		StringBuffer relativePath = new StringBuffer();
		for(PathSegment ps : list){
			relativePath.append(File.separator + ps.getPath().trim());
		}
		
		return PathResourceUtil.cleanRelativePath(relativePath.toString());
		
	}
	
	/**
	 * Processes multipart/form-data uploads. Allows user to add a file.
	 * 
	 * Request must contain the following parameter:
	 * 'file_0': The file that was uploaded (multipart/form-data attachment)
	 * 
	 * In addition to the 'file_0' parameter the request must contain either the ID
	 * of the directory where the file is being uploaded, or, the store name +
	 * the relative path value of a directory resource.
	 * 
	 * 'dirId': Directory node ID where the file will be added (text/plain)
	 * 
	 * or,
	 * 
	 * 'dirRelPath': Relative path for the directory where the file will be added.
	 * 'storeName': Name of the store for the directory.
	 * 
	 * In a nutshell, you need 'file_0' and ('dirId' or ('dirRelPath' plus 'storeName'))
	 * 
	 * @return
	 * @throws WebServiceException
	 */
	@MethodTimer
    @POST
    @Path("/uploadFile")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response addFile(MultipartBody body) throws WebServiceException {
    	
    	// See -> http://stackoverflow.com/questions/25797650/fileupload-with-jax-rs
    	
		//@Consumes(MediaType.MULTIPART_FORM_DATA)
		//public Response uploadFile(@Multipart(value = "vendor") String vendor,
        //@Multipart(value = "uploadedFile") Attachment attr) {
    	
    	List<Attachment> attachments = body.getAllAttachments();
    	Attachment rootAttachement = body.getRootAttachment();
    	
    	// check if we actually have a multipart/form-data attachment for param 'file_0'
    	Attachment fileAttachment = body.getAttachment("file_0");
    	if(fileAttachment == null){
    		handleError("Error, no 'file_0' multipart/form-data attachement found in request",
    				WebExceptionType.CODE_IO_ERROR);
    	}
    	ContentDisposition cd = fileAttachment.getContentDisposition();
    	if (cd == null || cd.getParameter("filename") == null) {
    		handleError("Could not pull file name from content disposition", WebExceptionType.CODE_IO_ERROR);
    	}
    	String fileName = cd.getParameter("filename");
    	DataHandler dataHandler = fileAttachment.getDataHandler();    	
    	
    	// check for additional upload parameters
    	String userId = getStringValue("userId", body);
    	String dirId = getStringValue("dirId", body);
    	String storeName = getStringValue("storeName", body);
    	String dirRelPath = getStringValue("dirRelPath", body);
    	
    	validateUserId(userId);
    	
    	// if user provided a 'dirId' value then use that
    	if(!StringUtil.isNullEmpty(dirId)){
    		
        	Long longDirId = -1L;
        	try {
    			longDirId = Long.valueOf(dirId);
    		} catch (NumberFormatException e) {
    			handleError("Error parsing dirId '" + dirId +"' to long", WebExceptionType.CODE_IO_ERROR, e);
    		}
        	
        	try {
    			uploadPipeline.processUpload(longDirId, fileName, dataHandler, true, userId);
    		} catch (ServiceException e) {
    			handleError("Upload pipeline error", WebExceptionType.CODE_IO_ERROR, e);
    		}        	
    	
        // use store name and directory relative path value
    	}else if(!StringUtil.isNullEmpty(storeName) && !StringUtil.isNullEmpty(dirRelPath)){
    		
        	try {
    			uploadPipeline.processUpload(storeName, dirRelPath, fileName, dataHandler, true, userId);
    		} catch (ServiceException e) {
    			handleError("Upload pipeline error", WebExceptionType.CODE_IO_ERROR, e);
    		}    		
    		
    	}else{
    		
    		handleError("Error processig upload. Need either 'dirId' parameter, or 'storeName' + 'dirRelPath' parameters",
    				WebExceptionType.CODE_IO_ERROR);
    		
    	}
    	
    	return Response.ok("File processed").build(); 
    	
    }
    
    /**
     * Update a directory
     * 
     * @param fileNodeId - the id of the file to update
     * @param name - new name for the directory
     * @param desc - new description for the directory
     * @param userId - id of user completing action
     * @return
     * @throws WebServiceException
     */
    @POST
    @Path("/updateFile")
    @Produces(MediaType.APPLICATION_JSON)    
    public Response updateFile(
    		@QueryParam("fileNodeId") Long fileNodeId,
    		@QueryParam("name") String name,
    		@QueryParam("desc") String desc,
    		@QueryParam("userId") String userId)  throws WebServiceException {
    	
    	validateUserId(userId);
    	
    	if(fileNodeId == null || StringUtil.isNullEmpty(name) || StringUtil.isNullEmpty(desc)){
    		handleError("Missing 'fileNodeId', 'name', and/or 'desc' params.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	try {
			fileService.updateFile(fileNodeId, name, desc, userId, task -> {
				logger.info("Update file progress at " + Math.round(task.getProgress()) + "%, job " + task.getCompletedJobCount() + " of " + task.getJobCount() + " completed"
						+ " {fileNodeId : " + fileNodeId + ", user : " + userId + " }");
			});
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	logger.info("Update of file complete.");
    	
    	return Response.ok(buildJsonOK(), MediaType.APPLICATION_JSON).build();
    	
    }    
    
    /**
     * Add a directory
     * 
     * @param dirNodeId - id of parent directory. New directory will be created under the parent.
     * @param name - name for new directory
     * @param desc - description for new directory
     * @param readGroup1 - optional read group
     * @param writeGroup1 - optional write group
     * @param executeGroup1 - optional execute group
     * @param userId - id of user completing action
     * @return
     * @throws WebServiceException
     */
    @GET
    @Path("/addDirectory")
    @Produces(MediaType.APPLICATION_JSON)
    public DirectoryResourceDto addDirectory(
    		@QueryParam("dirNodeId") Long dirNodeId,
    		@QueryParam("name") String name,
    		@QueryParam("desc") String desc,
    		@QueryParam("readGroup1") String readGroup1,
    		@QueryParam("writeGroup1") String writeGroup1,
    		@QueryParam("executeGroup1") String executeGroup1,
    		@QueryParam("userId") String userId) throws WebServiceException {
    	
    	validateUserId(userId);
    	
    	if(dirNodeId == null || StringUtil.isNullEmpty(name) || StringUtil.isNullEmpty(desc)){
    		handleError("Missing 'dirNodeId', 'name', and/or 'desc' params.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	DirectoryResource dirResource = null;
    	try {
    		dirResource = fileService.addDirectory(dirNodeId, name, desc, readGroup1, writeGroup1, executeGroup1, userId, task -> {
				logger.info("Add directory progress at " + Math.round(task.getProgress()) + "%, job " + task.getCompletedJobCount() + " of " + task.getJobCount() + " completed"
						+ " {dirNodeId (parent) : " + dirNodeId + ", name : " + name + ", user : " + userId + " }");
			});
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	DirectoryResourceMapper mapper = new DirectoryResourceMapper();
    	
    	//return Response.ok(buildJsonOK(), MediaType.APPLICATION_JSON).build();
    	return mapper.map(dirResource);
    	
    }
    
    /**
     * Update a directory
     * 
     * @param dirNodeId - the id of the directory to update
     * @param name - new name for the directory
     * @param desc - new description for the directory
     * @param readGroup1 - new read group
     * @param writeGroup1 - new write group
     * @param executeGroup1 - new execute group
     * @param userId - id of user completing action
     * @return
     * @throws WebServiceException
     */
    @POST
    @Path("/updateDirectory")
    @Produces(MediaType.APPLICATION_JSON)    
    public Response updateDirectory(
    		@QueryParam("dirNodeId") Long dirNodeId,
    		@QueryParam("name") String name,
    		@QueryParam("desc") String desc,
    		@QueryParam("readGroup1") String readGroup1,
    		@QueryParam("writeGroup1") String writeGroup1,
    		@QueryParam("executeGroup1") String executeGroup1,
    		@QueryParam("userId") String userId)  throws WebServiceException {
    	
    	validateUserId(userId);
    	
    	if(dirNodeId == null || StringUtil.isNullEmpty(name) || StringUtil.isNullEmpty(desc)){
    		handleError("Missing 'dirNodeId', 'name', and/or 'desc' params.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	try {
			fileService.updateDirectory(dirNodeId, name, desc, readGroup1, writeGroup1, executeGroup1, userId, task -> {
				logger.info("Update directory progress at " + Math.round(task.getProgress()) + "%, job " + task.getCompletedJobCount() + " of " + task.getJobCount() + " completed"
						+ " {dirNodeId : " + dirNodeId + ", new name : " + name + ", user : " + userId + " }");
			});
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	logger.info("Update of directory complete.");
    	
    	return Response.ok(buildJsonOK(), MediaType.APPLICATION_JSON).build();
    	
    }
        
    /**
     * Create a new store
     * 
     * @param storeName - store name must be unique. an exception will be thrown if a store with
     * the provided name already exists.
     * @param storeDesc - store description
     * @param storePath - store path on the local file system. This application must have read/write
     * permission to create the directory.
     * @param maxFileSizeBytes - max file size in bytes allowed by the store for file storage in the
     * database in blob format (file will still be saved to the local file system.)
     * @param rootDirName - directory name for the root directory for the store.
     * @param rootDirDesc - description for the root directory
     * @param readGroup1
     * @param writeGroup1
     * @param executeGroup1
     * @return
     * @throws WebServiceException
     */
    @POST
    @Path("/addStore")
    @Produces(MediaType.APPLICATION_JSON)
    public StoreDto addStore(
    		@QueryParam("storeName") String storeName,
    		@QueryParam("storeDesc") String storeDesc,
    		@QueryParam("storePath") String storePath,
    		@QueryParam("maxFileSizeBytes") Long maxFileSizeBytes,
    		@QueryParam("rootDirName") String rootDirName,
    		@QueryParam("rootDirDesc") String rootDirDesc,
    		@QueryParam("readGroup1") String readGroup1,
    		@QueryParam("writeGroup1") String writeGroup1,
    		@QueryParam("executeGroup1") String executeGroup1) throws WebServiceException {
    	
    	if(maxFileSizeBytes == null || StringUtil.isNullEmpty(storeName) || StringUtil.isNullEmpty(storeDesc)
    			|| StringUtil.isNullEmpty(storePath) || StringUtil.isNullEmpty(rootDirName) ||
    			StringUtil.isNullEmpty(rootDirDesc) || StringUtil.isNullEmpty(readGroup1) ||
    			StringUtil.isNullEmpty(writeGroup1) || StringUtil.isNullEmpty(executeGroup1)){
    		
    		handleError("Missing required params. Please check, storeName, storeDesc, storePath, "
    				+ "maxFileSizeBytes, rootDirName, and/or rootDirDesc values.", WebExceptionType.CODE_IO_ERROR);
    		
    	}
    	
    	// needs to be lowercase
    	//storePath = storePath.toLowerCase();
    	
    	java.nio.file.Path storeFilePath = null;
    	try {
    	storeFilePath = Paths.get(storePath);
    	}catch(InvalidPathException e) {
			handleError("Error creating new store, store path '" + storePath + "' is not valid. " + 
					e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);    		
    	}
    	
    	Store store = null;
    	try {
			store = fileService.addStore(storeName, storeDesc, storeFilePath, rootDirName, rootDirDesc, maxFileSizeBytes, 
					readGroup1, writeGroup1, executeGroup1, AccessRule.DENY);
		} catch (ServiceException e) {
			handleError("Error creating new store, name=" + storeName + ", " + 
					e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	StoreMapper mapper = new StoreMapper();
    	
    	return mapper.map(store);
    	
    }
    
    /**
     * Update a store
     * 
     * @param storeId
     * @param storeName
     * @param storeDesc
     * @param rootDirName
     * @param rootDirDesc
     * @param rootDirReadGroup1
     * @param rootDirWriteGroup1
     * @param rootDirExecuteGroup1
     * @param userId
     * @return
     * @throws WebServiceException
     */
    @POST
    @Path("/updateStore")
    @Produces(MediaType.APPLICATION_JSON)    
	public Response updateStore(
			@QueryParam("storeId") Long storeId,
			@QueryParam("storeName") String storeName,
			@QueryParam("storeDesc") String storeDesc,
			@QueryParam("rootDirName") String rootDirName, 
			@QueryParam("rootDirDesc") String rootDirDesc, 
    		@QueryParam("rootDirReadGroup1") String rootDirReadGroup1,
    		@QueryParam("rootDirWriteGroup1") String rootDirWriteGroup1,
    		@QueryParam("rootDirExecuteGroup1") String rootDirExecuteGroup1,
			@QueryParam("userId") String userId) throws WebServiceException {
		
		if(storeId == null || StringUtil.isAnyNullEmpty(storeName, storeDesc, rootDirName, rootDirDesc, 
				rootDirReadGroup1, rootDirWriteGroup1, rootDirExecuteGroup1, userId)) {
			
    		handleError("Missing required params to update store", WebExceptionType.CODE_IO_ERROR);			
			
		}
		
		try {
			fileService.updateStore(storeId, storeName, storeDesc, rootDirName, rootDirDesc, 
					rootDirReadGroup1, rootDirWriteGroup1, rootDirExecuteGroup1, userId, task -> {
						logger.info("Update store progress at " + Math.round(task.getProgress()) + "%, job " + task.getCompletedJobCount() + " of " + task.getJobCount() + " completed"
								+ " {storeId : " + storeId + ", user : " + userId + " }");
					});
		} catch (ServiceException e) {
			handleError("Error updating store, id=" + storeId + ", " + 
					e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
		
		return Response.ok(buildJsonOK(), MediaType.APPLICATION_JSON).build();		
		
	}
  
    /**
     * Delete a file
     * 
     * @param fileNodeId
     * @param userId - id of user completing action
     * @return
     * @throws WebServiceException
     */
    @POST
    @Path("/removeFile")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeFile(
    		@QueryParam("fileNodeId") Long fileNodeId, @QueryParam("userId") String userId) throws WebServiceException {
    	
    	validateUserId(userId);
    	
    	if(fileNodeId == null){
    		handleError("Missing fileNodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	try {
    		fileService.removeFile(fileNodeId, userId, task -> {
				logger.info("Remove file progress at " + Math.round(task.getProgress()) + "%, job " + task.getCompletedJobCount() + " of " + task.getJobCount() + " completed"
						+ " {fileNodeId : " + fileNodeId + ", user : " + userId + " }");
			});
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	return Response.ok(buildJsonOK(), MediaType.APPLICATION_JSON).build();
    	
    }
    
    /**
     * 
     * @param dirNodeId - id of directory to delete
     * @param userId - id of user completing action
     * @return
     * @throws WebServiceException
     */
    @POST
    @Path("/removeDirectory")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeDirectory(@QueryParam("dirNodeId") Long dirNodeId, @QueryParam("userId") String userId) throws WebServiceException {
    	
    	validateUserId(userId);
    	
    	if(dirNodeId == null){
    		handleError("Missing dirNodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	try {
			fileService.removeDirectory(dirNodeId, userId, task -> {
				logger.info("Remove directory progress at " + Math.round(task.getProgress()) + "%, job " + task.getCompletedJobCount() + " of " + task.getJobCount() + " completed"
						+ " {dirNodeId : " + dirNodeId + ", user : " + userId + " }");
			});
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	return Response.ok(buildJsonOK(), MediaType.APPLICATION_JSON).build();
    	
    }
    
    /**
     * Copy a file
     * 
     * @param fileNodeId - id of file to copy
     * @param dirNodeId - id of directory where file will be copied to
     * @param replaceExisting - pass true to replace any existing file with the same name (case insensitive match) in
     * the target directory. Pass false not to replace. If you pass false and a file does already exist, then
     * an exception will be thrown.
     * @param userId - id of user completing action
     * @return
     * @throws WebServiceException
     */
    @POST
    @Path("/copyFile")
    @Produces(MediaType.APPLICATION_JSON)
    public Response copyFile(
    		@QueryParam("fileNodeId") Long fileNodeId,
    		@QueryParam("dirNodeId") Long dirNodeId,
    		@QueryParam("replaceExisting") Boolean replaceExisting,
    		@QueryParam("userId") String userId) throws WebServiceException {
    	
    	validateUserId(userId);
    	
    	if(fileNodeId == null || dirNodeId == null || replaceExisting == null){
    		handleError("Cannot copy file, missing fileNodeId, dirNodeId, and/or replaceExisting params.", 
    				WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	try {
			fileService.copyFile(fileNodeId, dirNodeId, replaceExisting.booleanValue(), userId, task -> {
				logger.info("Copy file progress at " + Math.round(task.getProgress()) + "%, job " + task.getCompletedJobCount() + " of " + task.getJobCount() + " completed"
						+ " {fileNodeId : " + fileNodeId + ", dirNodeId : " + dirNodeId + ", user : " + userId + " }");
			});
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	return Response.ok(buildJsonOK(), MediaType.APPLICATION_JSON).build();
    	
    }

    /**
     * Copy a directory
     * 
     * @param copyDirNodeId
     * @param destDirNodeId
     * @param replaceExisting
     * @param userId - id of user completing action
     * @return
     * @throws WebServiceException
     */
    @POST
    @Path("/copyDirectory")
    @Produces(MediaType.APPLICATION_JSON)
    public Response copyDirectory(
    		@QueryParam("copyDirNodeId") Long copyDirNodeId,
    		@QueryParam("destDirNodeId") Long destDirNodeId,
    		@QueryParam("replaceExisting") Boolean replaceExisting,
    		@QueryParam("userId") String userId) throws WebServiceException {
    	
    	validateUserId(userId);
    	
    	if(copyDirNodeId == null || destDirNodeId == null || replaceExisting == null){
    		handleError("Missing copyDirNodeId, destDirNodeId, and/or replaceExisting params.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	try {
			fileService.copyDirectory(copyDirNodeId, destDirNodeId, replaceExisting.booleanValue(), userId, task -> {
				logger.info("Copy directory progress at " + Math.round(task.getProgress()) + "%, job " + task.getCompletedJobCount() + " of " + task.getJobCount() + " completed"
						+ " {copyDirNodeId : " + copyDirNodeId + ", destDirNodeId : " + destDirNodeId + ", user : " + userId + " }");
			});
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	return Response.ok(buildJsonOK(), MediaType.APPLICATION_JSON).build();
    	
    }
    
    /**
     * Move a file
     * 
     * @param fileNodeId - id of file to move
     * @param dirNodeId - id of directory where file will be moved to
     * @param replaceExisting - pass true to replace any existing file with the same name (case insensitive match) in
     * the target directory. Pass false not to replace. If you pass false and a file does already exist, then
     * an exception will be thrown.
     * @param userId - id of user completing action
     * @return
     * @throws WebServiceException
     */
    @POST
    @Path("/moveFile")
    @Produces(MediaType.APPLICATION_JSON)
    public Response moveFile(
    		@QueryParam("fileNodeId") Long fileNodeId,
    		@QueryParam("dirNodeId") Long dirNodeId,
    		@QueryParam("replaceExisting") Boolean replaceExisting,
    		@QueryParam("userId") String userId) throws WebServiceException {
    	
    	validateUserId(userId);
    	
    	if(fileNodeId == null || dirNodeId == null || replaceExisting == null){
    		handleError("Cannot move file, missing fileNodeId, dirNodeId, and/or replaceExisting params.", 
    				WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	try {
			fileService.moveFile(fileNodeId, dirNodeId, replaceExisting.booleanValue(), userId, task -> {
				logger.info("Move file progress at " + Math.round(task.getProgress()) + "%, job " + task.getCompletedJobCount() + " of " + task.getJobCount() + " completed"
						+ " {fileNodeId : " + fileNodeId + ", dirNodeId : " + dirNodeId + ", user : " + userId + " }");
			});
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	return Response.ok(buildJsonOK(), MediaType.APPLICATION_JSON).build();
    	
    }
    
    /**
     * Move a directory
     * 
     * @param copyDirNodeId
     * @param destDirNodeId
     * @param replaceExisting
     * @param userId - id of user completing action
     * @return
     * @throws WebServiceException
     */
    @POST
    @Path("/moveDirectory")
    @Produces(MediaType.APPLICATION_JSON)
    public Response moveDirectory(
    		@QueryParam("moveDirNodeId") Long moveDirNodeId,
    		@QueryParam("destDirNodeId") Long destDirNodeId,
    		@QueryParam("replaceExisting") Boolean replaceExisting,
    		@QueryParam("userId") String userId) throws WebServiceException {
    	
    	validateUserId(userId);
    	
    	if(moveDirNodeId == null || destDirNodeId == null || replaceExisting == null){
    		handleError("Missing moveDirNodeId, destDirNodeId, and/or replaceExisting params.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	try {
			fileService.moveDirectory(moveDirNodeId, destDirNodeId, replaceExisting.booleanValue(), userId, task -> {
				logger.info("Move directory progress at " + Math.round(task.getProgress()) + "%, job " + task.getCompletedJobCount() + " of " + task.getJobCount() + " completed"
						+ " {moveDirNodeId : " + moveDirNodeId + ", destDirNodeId : " + destDirNodeId + ", user : " + userId + " }");
			});
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	return Response.ok(buildJsonOK(), MediaType.APPLICATION_JSON).build();
    	
    }
    
    /*
    @POST
    @Path("/rename")
    @Produces(MediaType.APPLICATION_JSON)
    public Response renameResource(
    		@QueryParam("nodeId") Long nodeId,
    		@QueryParam("newName") String newName,
    		@QueryParam("userId") String userId) throws WebServiceException {
    	
    	validateUserId(userId);
    	
    	if(nodeId == null || newName == null){
    		handleError("Missing nodeId, and/or newName params.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	try {
			fileService.renamePathResource(nodeId, newName, userId);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	return Response.ok(buildJsonOK(), MediaType.APPLICATION_JSON).build();
    	
    }
    */   
    
    /**
     * Create the test store
     * 
     * @return
     * @throws WebServiceException
     */
    @GET
    @Path("/createTestStore")
    @Produces(MediaType.APPLICATION_JSON)    
    public StoreDto createTestStore() throws WebServiceException {
    	
    	Store testStore = null;
    	try {
			testStore = fileService.createTestStore();
		} catch (ServiceException e) {
			handleError("Error creating test store, " + e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	StoreMapper mapper = new StoreMapper();
    	
    	return mapper.map(testStore);
    	
    }
    
    /**
     * Trigger the process to rebuild the store search (Lucene) index.
     * 
     * @param storeId - ID of the store.
     * @param userId - ID of user triggering the process.
     * @return A JSON OK response.
     * @throws WebServiceException
     */
    @POST
    @Path("/store/reindex")
    @Produces(MediaType.APPLICATION_JSON)     
    public Response rebuildStoreIndex(@QueryParam("storeId") Long storeId, @QueryParam("userId") String userId) throws WebServiceException {
    	
    	validateUserId(userId);
    	
    	if(storeId == null) {
    		handleError("Missing storeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	try {
			fileService.rebuildStoreSearchIndex(storeId, userId);
		} catch (ServiceException e) {
			handleError("Error creating test store, " + e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	return Response.ok(buildJsonOK(), MediaType.APPLICATION_JSON).build();
    	
    }
    
    /**
     * Fetch a string value from the multipart body
     * 
     * @param key - the attribute name from the request
     * @param body - the multipartbody
     * @return
     */
    private String getStringValue(String key, MultipartBody body) {
    	
    	Attachment att = body.getAttachment(key);
    	if(att == null){
    		return null;
    	}
    	return att.getObject(String.class);
    	
    }
    
    /**
     * Log debug info for attachment object
     * 
     * @param attach
     */
    private void logAttachement(Attachment attach){
    	
    	logger.info("Content ID => " + attach.getContentId());
    	
    	MultivaluedMap<String, String> headers = attach.getHeaders();
    	for(String key : headers.keySet()){
    		List<String> values = headers.get(key);
    		for(String val : CollectionUtil.emptyIfNull(values)){
    			logger.info("Key => " + key + ", Values => " + val);
    		}
    	}
    	
    	logger.info("Content type => " + attach.getContentType().toString());
    	
    }
    
	/**
	 * Writes the file binary data to the response
	 * 
	 * @param fileMeta
	 * @return
	 */
	private Response writeFileToResponse(FileMetaResource fileMeta) {
		
		//
		// Write data to output/response
		//
		ByteArrayInputStream bis = new ByteArrayInputStream(fileMeta.getBinaryResource().getFileData());
		
		//ContentDisposition contentDisposition = ContentDisposition.type("attachment")
		//	    .fileName("filename.csv").creationDate(new Date()).build();
		//ContentDisposition contentDisposition = new ContentDisposition("attachment; filename=image.jpg");
		
		return Response.ok(
			new StreamingOutput() {
				@Override
				public void write(OutputStream out) throws IOException, WebApplicationException {
					byte[] buffer = new byte[4 * 1024];
					int bytesRead;
					while ((bytesRead = bis.read(buffer)) != -1) {
						out.write(buffer, 0, bytesRead);
					}
					out.flush();
					out.close();
					bis.close();
				}
			}
		).header("Content-Disposition", "attachment; filename=" + fileMeta.getPathName()).build();		
		
	}    

}
