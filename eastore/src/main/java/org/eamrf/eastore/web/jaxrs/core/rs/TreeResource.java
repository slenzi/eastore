/**
 * 
 */
package org.eamrf.eastore.web.jaxrs.core.rs;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.DateUtil;
import org.eamrf.core.util.FileUtil;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.properties.ManagedProperties;
import org.eamrf.eastore.core.service.FileSystemService;
import org.eamrf.eastore.core.service.NodeTreeService;
import org.eamrf.eastore.core.service.PathResourceTreeService;
import org.eamrf.eastore.core.tree.ToString;
import org.eamrf.eastore.core.tree.Tree;
import org.eamrf.eastore.core.tree.TreeNode;
import org.eamrf.eastore.core.tree.TreeNodeVisitException;
import org.eamrf.eastore.core.tree.Trees;
import org.eamrf.eastore.core.tree.Trees.WalkOption;
import org.eamrf.eastore.web.jaxrs.BaseResourceHandler;
import org.eamrf.repository.oracle.ecoguser.eastore.model.Node;
import org.eamrf.repository.oracle.ecoguser.eastore.model.PathResource;
import org.eamrf.repository.oracle.ecoguser.eastore.model.ResourceType;
import org.eamrf.repository.oracle.ecoguser.eastore.model.Store;
import org.eamrf.repository.oracle.ecoguser.eastore.model.FileMetaResource;
import org.eamrf.web.rs.exception.WebServiceException;
import org.eamrf.web.rs.exception.WebServiceException.WebExceptionType;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * JAX-RS resource for fetching tree data. Useful for debugging purposes.
 * 
 * @author slenzi
 */
@Path("/tree")
@Service("treeResource")
public class TreeResource extends BaseResourceHandler {

    @InjectLogger
    private Logger logger;
    
    @Autowired
    private ManagedProperties appProps;    
    
    @Autowired
    private NodeTreeService nodeTreeService;
    
    @Autowired
    private PathResourceTreeService pathResourceTreeService;
    
    @Autowired
    private FileSystemService fileSystemService;
    
    // basic toString() call on the Node
	private class NodeToString implements ToString<Node>{
		@Override
		public String toString(Node node) {
			return node.toString();
		}
	}    
    
	// basic toString() call on the PathResource
	private class PathResourceToString implements ToString<PathResource>{
		@Override
		public String toString(PathResource resource) {
			return resource.toString();
		}
	}
	
	// Creates an ahref download URL for all FileMetaResources
	// File size, update date, and mime type are also displayed
	// DirectoryResources are simple bold text
	private class PathResourceHtmlAhrefDownloadToString implements ToString<PathResource>{
		@Override
		public String toString(PathResource resource) {
			
			if(resource.getResourceType() == ResourceType.DIRECTORY){
				
				return "<span style=\"font-weight: bold;\">" + resource.getPathName() + "</span> - " +
						"[id=" + resource.getNodeId() + "]";
				
			}else if(resource.getResourceType() == ResourceType.FILE){
				
				String appContext = appProps.getProperty("server.contextPath");
				String downloadUrlPrefix = appContext + "/services/easapi/v1/fsys/download";
				String storeName = resource.getStore().getName();
				String relPath = resource.getRelativePath();
				String downloadUrl = downloadUrlPrefix + "/" + storeName + relPath;
				
				String fileByteFormat = FileUtil.humanReadableByteCount( ((FileMetaResource)resource).getFileSize(), true);
				String fileMimeType = ((FileMetaResource)resource).getMimeType();
				String fileUpdateDate = DateUtil.defaultDateFormat(resource.getDateUpdated());
				
				return "<a href=\"" + downloadUrl + "\">" + resource.getPathName() + "</a>" +
					" (" + fileMimeType + ", " + fileByteFormat + ", " + fileUpdateDate + ") - " +
					"[id=" + resource.getNodeId() + "]";
				
			}
			
			return "";
			
		}
	}
	
	// sort tree children so files appear before directories, and if resources
	// are the same resource type then sort alphabetically by name
	private Comparator<TreeNode<PathResource>> nodePathResourceCompare = (TreeNode<PathResource> n1, TreeNode<PathResource> n2) -> {
		
		if(n1.getData().getResourceType() == n2.getData().getResourceType()){
			// same type, so compare on resource name
			return n1.getData().getPathName().compareTo(n2.getData().getPathName());
		}else if(n1.getData().getResourceType() == ResourceType.FILE){
			return -1;
		}else{
			return 1;
		}
		
	};	
    
	public TreeResource() {

	}
	
	/**
	 * Fetch Node top-down (root to all leafs) tree in HTML representation
	 * 
	 * @param nodeId
	 * @return
	 * @throws WebServiceException
	 */
    @GET
    @Path("/node/html/{nodeId}")
    @Produces(MediaType.TEXT_HTML)
    public Response getNodeTree(@PathParam("nodeId") Long nodeId) throws WebServiceException {
    	
    	if(nodeId == null){
    		handleError("Missing nodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	Tree<Node> tree = null;
    	try {
    		tree = nodeTreeService.buildNodeTree(nodeId);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for node => " + nodeId, WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	StringBuffer buf = new StringBuffer();
    	buf.append( tree.printHtmlTree(new NodeToString()) );
    	
    	nodeTreeService.logTree(tree);

    	return Response.ok(buf.toString(), MediaType.TEXT_HTML).build();
    	
    }
    
    /**
     * Fetch Node top-down (root to all leafs) tree in HTML representation, but only include nodes up to a specified depth.
     * 
     * @param nodeId
     * @param depth
     * @return
     * @throws WebServiceException
     */
    @GET
    @Path("/node/html/{nodeId}/depth/{depth}")
    @Produces(MediaType.TEXT_HTML)
    public Response getNodeTree(@PathParam("nodeId") Long nodeId, @PathParam("depth") int depth) throws WebServiceException {
    	
    	if(nodeId == null){
    		handleError("Missing nodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	if(depth < 0){
    		handleError("Depth param must be positive.", WebExceptionType.CODE_IO_ERROR);
    	}    	
    	
    	Tree<Node> tree = null;
    	try {
    		tree = nodeTreeService.buildNodeTree(nodeId, depth);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for node => " + nodeId, WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	StringBuffer buf = new StringBuffer();
    	buf.append( tree.printHtmlTree(new NodeToString()) );

    	nodeTreeService.logTree(tree);
    	
    	return Response.ok(buf.toString(), MediaType.TEXT_HTML).build();
    	
    }
    
	/**
	 * Fetch Node bottom-up tree (leaf to root) in HTML representation
	 * 
	 * @param nodeId - some leaf node
	 * @return
	 * @throws WebServiceException
	 */
    @GET
    @Path("/node/parent/html/{nodeId}")
    @Produces(MediaType.TEXT_HTML)
    public Response getNodeParentTree(@PathParam("nodeId") Long nodeId) throws WebServiceException {
    	
    	if(nodeId == null){
    		handleError("Missing nodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	Tree<Node> tree = null;
    	try {
    		tree = nodeTreeService.buildParentNodeTree(nodeId);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for node => " + nodeId, WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	StringBuffer buf = new StringBuffer();
    	buf.append( tree.printHtmlTree(new NodeToString()) );
    	
    	nodeTreeService.logTree(tree);

    	return Response.ok(buf.toString(), MediaType.TEXT_HTML).build();
    	
    }
    
	/**
	 * Fetch bottom-up tree (leaf to root) in HTML representation, but only include so many levels up (towards the root node)
	 * 
	 * @param nodeId - some leaf node
	 * @return
	 * @throws WebServiceException
	 */
    @GET
    @Path("/node/parent/html/{nodeId}/levels/{levels}")
    @Produces(MediaType.TEXT_HTML)
    public Response getNodeParentTree(@PathParam("nodeId") Long nodeId, @PathParam("levels") int levels) throws WebServiceException {
    	
    	if(nodeId == null){
    		handleError("Missing nodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	if(levels < 0){
    		handleError("Levels param must be positive.", WebExceptionType.CODE_IO_ERROR);
    	}    	
    	
    	Tree<Node> tree = null;
    	try {
    		tree = nodeTreeService.buildParentNodeTree(nodeId, levels);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for node => " + nodeId, WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	StringBuffer buf = new StringBuffer();
    	buf.append( tree.printHtmlTree(new NodeToString()) );
    	
    	nodeTreeService.logTree(tree);

    	return Response.ok(buf.toString(), MediaType.TEXT_HTML).build();
    	
    }
    
	/**
	 * Fetch PathResource top-down (root to all leafs) tree in HTML representation
	 * 
	 * @param dirNodeId
	 * @return
	 * @throws WebServiceException
	 */
    @GET
    @Path("/pathresource/html/{dirNodeId}")
    @Produces(MediaType.TEXT_HTML)
    public Response getPathResourceTree(@PathParam("dirNodeId") Long dirNodeId) throws WebServiceException {
    	
    	if(dirNodeId == null){
    		handleError("Missing dirNodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	Tree<PathResource> tree = null;
    	try {
    		tree = pathResourceTreeService.buildPathResourceTree(dirNodeId);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for dirNodeId => " + dirNodeId, WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	String htmlTree = buildPathResourceTreeHtml(tree);

    	return Response.ok(htmlTree, MediaType.TEXT_HTML).build();
    	
    }
    
    /**
     * Fetch PathResource top-down (root to all leafs) tree in HTML representation, but only include nodes up to a specified depth.
     * 
     * @param dirNodeId
     * @param depth
     * @return
     * @throws WebServiceException
     */
    @GET
    @Path("/pathresource/html/{dirNodeId}/depth/{depth}")
    @Produces(MediaType.TEXT_HTML)
    public Response getPathResourceTree(@PathParam("dirNodeId") Long dirNodeId, @PathParam("depth") int depth) throws WebServiceException {
    	
    	if(dirNodeId == null){
    		handleError("Missing dirNodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	if(depth < 0){
    		handleError("Depth param must be positive.", WebExceptionType.CODE_IO_ERROR);
    	}    	
    	
    	Tree<PathResource> tree = null;
    	try {
    		tree = pathResourceTreeService.buildPathResourceTree(dirNodeId, depth);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for dirNodeId => " + dirNodeId, WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	String htmlTree = buildPathResourceTreeHtml(tree);
    	
    	return Response.ok(htmlTree, MediaType.TEXT_HTML).build();
    	
    }
    
	/**
	 * Fetch PathResource bottom-up tree (leaf to root) in HTML representation
	 * 
	 * @param dirNodeId
	 * @return
	 * @throws WebServiceException
	 */
    @GET
    @Path("/pathresource/parent/html/{dirNodeId}")
    @Produces(MediaType.TEXT_HTML)
    public Response getPathResourceParentTree(@PathParam("dirNodeId") Long dirNodeId) throws WebServiceException {
    	
    	if(dirNodeId == null){
    		handleError("Missing dirNodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	Tree<PathResource> tree = null;
    	try {
    		tree = pathResourceTreeService.buildParentPathResourceTree(dirNodeId);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for dirNodeId => " + dirNodeId, WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	String htmlTree = buildPathResourceTreeHtml(tree);

    	return Response.ok(htmlTree, MediaType.TEXT_HTML).build();
    	
    }
    
	/**
	 * Fetch bottom-up tree (leaf to root) in HTML representation, but only include so many levels up (towards the root node)
	 * 
	 * @param nodeId - some leaf node
	 * @return
	 * @throws WebServiceException
	 */
    @GET
    @Path("/pathresource/parent/html/{dirNodeId}/levels/{levels}")
    @Produces(MediaType.TEXT_HTML)
    public Response getPathResourceParentTree(@PathParam("dirNodeId") Long dirNodeId, @PathParam("levels") int levels) throws WebServiceException {
    	
    	if(dirNodeId == null){
    		handleError("Missing dirNodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	if(levels < 0){
    		handleError("Levels param must be positive.", WebExceptionType.CODE_IO_ERROR);
    	}    	
    	
    	Tree<PathResource> tree = null;
    	try {
    		tree = pathResourceTreeService.buildParentPathResourceTree(dirNodeId, levels);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for node => " + dirNodeId, WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	String htmlTree = buildPathResourceTreeHtml(tree);

    	return Response.ok(htmlTree, MediaType.TEXT_HTML).build();
    	
    }
    
	/**
	 * Fetch PathResource top-down (root to all leafs) tree in HTML representation, with ahref download links
	 * for all FileMetaResources
	 * 
	 * @param dirNodeId
	 * @return
	 * @throws WebServiceException
	 */
    @GET
    @Path("/pathresource/download/{dirNodeId}")
    @Produces(MediaType.TEXT_HTML)
    public Response getPathResourceDownloadTree(@PathParam("dirNodeId") Long dirNodeId) throws WebServiceException {
    	
    	if(dirNodeId == null){
    		handleError("Missing dirNodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	Tree<PathResource> tree = null;
    	try {
    		tree = pathResourceTreeService.buildPathResourceTree(dirNodeId);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	if(tree == null){
    		handleError("Tree object was null for dirNodeId => " + dirNodeId, WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	String htmlTree = buildPathResourceTreeDownload(tree);
    	
    	StringBuffer htmlResponse = new StringBuffer();
    	
    	htmlResponse.append("<html>\n");
    	htmlResponse.append("<head>\n");
    	htmlResponse.append("<style type=\"text/css\">\n");
    	htmlResponse.append("body {font-size: 75%; line-height:1 !important; }\n");
    	htmlResponse.append("</style>\n");
    	htmlResponse.append("</head>\n");
    	htmlResponse.append("<body>\n");
    	htmlResponse.append(htmlTree + "\n");
    	htmlResponse.append("</body>\n");
    	htmlResponse.append("</html>\n");
    	
    	return Response.ok(htmlResponse.toString(), MediaType.TEXT_HTML).build();
    	
    }    
    
    /**
     * Prints the tree, and it's store, in HTML, and returns that HTML content as a String.
     * 
     * This method also logs the tree.
     * 
     * @param tree
     * @return
     * @throws WebServiceException
     */
    private String buildPathResourceTreeHtml(Tree<PathResource> tree) throws WebServiceException {
    	
    	if(tree == null){
    		return "null tree";
    	}
    	
    	PathResource rootNode = tree.getRootNode().getData();
    	
    	Store store = null;
    	try {
			store = fileSystemService.getStore(rootNode);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	// sort by resource type so files appear before directories, then sort by resource name
    	Trees.sortChildren(tree.getRootNode(), nodePathResourceCompare);
    	
    	StringBuffer buf = new StringBuffer();
    	buf.append(store.toString() + "<br>");
    	if(!rootNode.getParentNodeId().equals(0L)){
    		// the root node of the tree is not a root node of a store (there are some other directories in-between)
    		buf.append("[...other directories here...]<br>");
    	}    	
    	
    	buf.append( tree.printHtmlTree(new PathResourceToString()) );
    	
    	//pathResourceTreeService.logTree(tree);
    	
    	return buf.toString();
    	
    }
    
    /**
     * Prints the tree, and it's store, in HTML, with ahref download links for all FileMetaResource,
     * and returns that HTML content as a String.
     * 
     * This method also logs the tree.
     * 
     * @param tree
     * @return
     * @throws WebServiceException
     */
    private String buildPathResourceTreeDownload(Tree<PathResource> tree) throws WebServiceException {
    	
    	if(tree == null){
    		return "null tree";
    	}
    	
    	PathResource rootNode = tree.getRootNode().getData();
    	
    	Store store = null;
    	try {
			store = fileSystemService.getStore(rootNode);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	// sort by resource type so files appear before directories, then sort by resource name
    	Trees.sortChildren(tree.getRootNode(), nodePathResourceCompare);    	
    	
    	// get total size of all files in the store
    	AtomicLong totalSize = new AtomicLong();
    	try {
			Trees.walkTree(tree, (treeNode) -> {
				PathResource r = treeNode.getData();
				if(r.getResourceType() == ResourceType.FILE){
					totalSize.addAndGet(((FileMetaResource)r).getFileSize());
				}
			}, WalkOption.PRE_ORDER_TRAVERSAL);
		} catch (TreeNodeVisitException e) {
			handleError("Error computing total file size of all files in store. " + e.getMessage(),
					WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	StringBuffer buf = new StringBuffer();
    	buf.append("<span style=\"font-weight: bold;\">" + store.getName() + "</span>");
    	buf.append("[id=" + store.getId() + ", path=" + store.getPath() + ", diskUsage=" + 
    			FileUtil.humanReadableByteCount(totalSize.get(), true)+ "]<br>");
    	buf.append("[<br>");
    	buf.append(store.getDescription() + "<br>");
    	buf.append("]<br>");
    	if(!rootNode.getParentNodeId().equals(0L)){
    		// the root node of the tree is not a root node of a store (there are some other directories in-between)
    		buf.append("[...other directories here...]<br>");
    	} 
    	buf.append( tree.printHtmlTree(new PathResourceHtmlAhrefDownloadToString()) );
    	
    	//pathResourceTreeService.logTree(tree);
    	
    	return buf.toString();
    	
    }    

	/* (non-Javadoc)
	 * @see org.eamrf.eastore.web.jaxrs.BaseResourceHandler#getLogger()
	 */
	@Override
	public Logger getLogger() {
		return logger;
	}

}
