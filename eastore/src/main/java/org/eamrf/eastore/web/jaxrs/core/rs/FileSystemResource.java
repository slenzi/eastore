/**
 * 
 */
package org.eamrf.eastore.web.jaxrs.core.rs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.List;

import javax.activation.DataHandler;
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
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.service.FileSystemService;
import org.eamrf.eastore.core.service.UploadPipeline;
import org.eamrf.eastore.web.jaxrs.BaseResourceHandler;
import org.eamrf.repository.oracle.ecoguser.eastore.model.DirectoryResource;
import org.eamrf.repository.oracle.ecoguser.eastore.model.FileMetaResource;
import org.eamrf.repository.oracle.ecoguser.eastore.model.Store;
import org.eamrf.web.rs.exception.WebServiceException;
import org.eamrf.web.rs.exception.WebServiceException.WebExceptionType;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author slenzi
 *
 */
@Path("/fsys")
@Service("eaFileSystemResource")
public class FileSystemResource extends BaseResourceHandler {

    @InjectLogger
    private Logger logger;    
	
    @Autowired
    private FileSystemService fileSystemService;
    
    @Autowired
    private UploadPipeline uploadPipeline;
    
	public FileSystemResource() {

	}

	/* (non-Javadoc)
	 * @see org.eamrf.eastore.web.jaxrs.BaseResourceHandler#getLogger()
	 */
	@Override
	public Logger getLogger() {
		return logger;
	}
	
	/**
	 * Create a new store
	 * 
	 * @param dirNodeId
	 * @param name
	 * @return
	 * @throws WebServiceException
	 */
	@GET
	@Path("/addStore")
	@Produces(MediaType.APPLICATION_JSON)
    public Store addStore(
    		@QueryParam("storeName") String storeName,
    		@QueryParam("storeDesc") String storeDesc,
    		@QueryParam("storePath") String storePath,
    		@QueryParam("rootDirName") String rootDirName,
    		@QueryParam("maxFileSizeDb") Long maxFileSizeDb) throws WebServiceException {
    	
		if(StringUtil.isNullEmpty(storeName) || StringUtil.isNullEmpty(storeDesc) || StringUtil.isNullEmpty(storePath) ||
				StringUtil.isNullEmpty(rootDirName) || maxFileSizeDb == null || maxFileSizeDb < 0){
			
			handleError("Missing required params. Plese check values for storeName, storeDesc, "
					+ "storePath, rootDirName, and maxFileSizeDb", WebExceptionType.CODE_IO_ERROR);
			
		}
		
		java.nio.file.Path newStorePath = null;
		try {
			newStorePath = Paths.get(storePath);
		} catch (Exception e) {
			handleError("Error converting String '" + storePath + "' to java.nio.file.Path: " + e.getMessage(), 
					WebExceptionType.CODE_IO_ERROR, e);
		}
		
		Store store = null;
    	try {
			store = fileSystemService.addStore(storeName, storeDesc, newStorePath, rootDirName, maxFileSizeDb);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	return store;
    	
    }
	
	/**
	 * Download a file by it's file node ID.
	 * 
	 * @return
	 * @throws WebServiceException
	 */
	@GET
	@Path("/download/id/{fileId}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getFile(@PathParam("fileId") Long fileId) throws WebServiceException {
		
		if(fileId == null){
			handleError("Missing fileId path param", WebExceptionType.CODE_IO_ERROR);
		}
		
		FileMetaResource fileMeta = null;
		try {
			fileMeta = fileSystemService.getFileMetaResource(fileId, true);
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
	 * @param list
	 * @return
	 * @throws WebServiceException
	 */
	@GET
	@Path("/download/{storeName}/{relPath:.+}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getFile(
			@PathParam("storeName") String storeName,
			@PathParam("relPath") List<PathSegment> list) throws WebServiceException {
		
		if(StringUtil.isNullEmpty(storeName) || list == null || list.size() == 0){
			handleError("Missing storeName, and/or relPath segment parameters", WebExceptionType.CODE_IO_ERROR);
		}
		StringBuffer relativePath = new StringBuffer();
		for(PathSegment ps : list){
			relativePath.append(File.separator + ps.getPath().trim());
		}
		
		storeName = storeName.trim();
		String relPath = relativePath.toString().replace("\\", "/");
		
		logger.info("File Download: storeName=" + storeName + ", relPath=" + relPath);
		
		FileMetaResource fileMeta = null;
		try {
			fileMeta = fileSystemService.getFileMetaResource(storeName, relPath, true);
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
	
	/**
	 * Processes http multipart for data uploads. Allows user to add a file.
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
    @POST
    @Path("/uploadFile")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response processUpload(MultipartBody body) throws WebServiceException {
    	
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
    	String dirId = getStringValue("dirId", body);
    	String storeName = getStringValue("storeName", body);
    	String dirRelPath = getStringValue("dirRelPath", body);
    	
    	// if user provided a 'dirId' value then use that
    	if(!StringUtil.isNullEmpty(dirId)){
    		
        	Long longDirId = -1L;
        	try {
    			longDirId = Long.valueOf(dirId);
    		} catch (NumberFormatException e) {
    			handleError("Error parsing dirId '" + dirId +"' to long", WebExceptionType.CODE_IO_ERROR, e);
    		}
        	
        	try {
    			uploadPipeline.processUpload(longDirId, fileName, dataHandler, true);
    		} catch (ServiceException e) {
    			handleError("Upload pipeline error", WebExceptionType.CODE_IO_ERROR, e);
    		}        	
    	
        // use store name and directory relative patn value
    	}else if(!StringUtil.isNullEmpty(storeName) && !StringUtil.isNullEmpty(dirRelPath)){
    		
        	try {
    			uploadPipeline.processUpload(storeName, dirRelPath, fileName, dataHandler, true);
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
	 * Add a file...
	 * 
	 * @return
	 * @throws WebServiceException
	 */
    @GET
    @Path("/addDirectory/{dirNodeId}/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public DirectoryResource addDirectory(
    		@PathParam("dirNodeId") Long dirNodeId,
    		@PathParam("name") String name) throws WebServiceException {
    	
    	if(dirNodeId == null || StringUtil.isNullEmpty(name)){
    		handleError("Missing dirNodeId, and/or name params.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	DirectoryResource dirResource = null;
    	try {
    		dirResource = fileSystemService.addDirectory(dirNodeId, name);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	//return Response.ok(buildJsonOK(), MediaType.APPLICATION_JSON).build(); 
    	return dirResource;
    	
    }
    
    /**
     * Delete a file
     * 
     * @param fileNodeId
     * @return
     * @throws WebServiceException
     */
    @POST
    @Path("/removeFile")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeFile(@QueryParam("fileNodeId") Long fileNodeId) throws WebServiceException {
    	
    	if(fileNodeId == null){
    		handleError("Missing fileNodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	try {
			fileSystemService.removeFile(fileNodeId);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	return Response.ok(buildJsonOK(), MediaType.APPLICATION_JSON).build();
    	
    }
    
    /**
     * Delete a directory
     * 
     * @param dirNodeId
     * @return
     * @throws WebServiceException
     */
    @POST
    @Path("/removeDirectory")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeDirectory(@QueryParam("dirNodeId") Long dirNodeId) throws WebServiceException {
    	
    	if(dirNodeId == null){
    		handleError("Missing dirNodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	try {
			fileSystemService.removeDirectory(dirNodeId);
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
     * @return
     * @throws WebServiceException
     */
    @POST
    @Path("/copyFile")
    @Produces(MediaType.APPLICATION_JSON)
    public Response copyFile(
    		@QueryParam("fileNodeId") Long fileNodeId,
    		@QueryParam("dirNodeId") Long dirNodeId,
    		@QueryParam("replaceExisting") Boolean replaceExisting) throws WebServiceException {
    	
    	if(fileNodeId == null || dirNodeId == null || replaceExisting == null){
    		handleError("Cannot copy file, missing fileNodeId, dirNodeId, and/or replaceExisting params.", 
    				WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	try {
			fileSystemService.copyFile(fileNodeId, dirNodeId, replaceExisting.booleanValue());
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
     * @return
     * @throws WebServiceException
     */
    @POST
    @Path("/copyDirectory")
    @Produces(MediaType.APPLICATION_JSON)
    public Response copyDirectory(
    		@QueryParam("copyDirNodeId") Long copyDirNodeId,
    		@QueryParam("destDirNodeId") Long destDirNodeId,
    		@QueryParam("replaceExisting") Boolean replaceExisting) throws WebServiceException {
    	
    	if(copyDirNodeId == null || destDirNodeId == null || replaceExisting == null){
    		handleError("Missing copyDirNodeId, destDirNodeId, and/or replaceExisting params.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	try {
			fileSystemService.copyDirectory(copyDirNodeId, destDirNodeId, replaceExisting.booleanValue());
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
     * @return
     * @throws WebServiceException
     */
    @POST
    @Path("/moveFile")
    @Produces(MediaType.APPLICATION_JSON)
    public Response moveFile(
    		@QueryParam("fileNodeId") Long fileNodeId,
    		@QueryParam("dirNodeId") Long dirNodeId,
    		@QueryParam("replaceExisting") Boolean replaceExisting) throws WebServiceException {
    	
    	if(fileNodeId == null || dirNodeId == null || replaceExisting == null){
    		handleError("Cannot move file, missing fileNodeId, dirNodeId, and/or replaceExisting params.", 
    				WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	try {
			fileSystemService.moveFile(fileNodeId, dirNodeId, replaceExisting.booleanValue());
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
     * @return
     * @throws WebServiceException
     */
    @POST
    @Path("/moveDirectory")
    @Produces(MediaType.APPLICATION_JSON)
    public Response moveDirectory(
    		@QueryParam("moveDirNodeId") Long moveDirNodeId,
    		@QueryParam("destDirNodeId") Long destDirNodeId,
    		@QueryParam("replaceExisting") Boolean replaceExisting) throws WebServiceException {
    	
    	if(moveDirNodeId == null || destDirNodeId == null || replaceExisting == null){
    		handleError("Missing moveDirNodeId, destDirNodeId, and/or replaceExisting params.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	try {
			fileSystemService.moveDirectory(moveDirNodeId, destDirNodeId, replaceExisting.booleanValue());
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	return Response.ok(buildJsonOK(), MediaType.APPLICATION_JSON).build();
    	
    }
    
    /**
     * Create the test store
     * 
     * @return
     * @throws WebServiceException
     */
    @GET
    @Path("/createTestStore")
    @Produces(MediaType.APPLICATION_JSON)    
    public Store createTestStore() throws WebServiceException {
    	
    	Store testStore = null;
    	try {
			testStore = fileSystemService.createTestStore();
		} catch (ServiceException e) {
			handleError("Error creating test store, " + e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	return testStore;
    	
    }

}
