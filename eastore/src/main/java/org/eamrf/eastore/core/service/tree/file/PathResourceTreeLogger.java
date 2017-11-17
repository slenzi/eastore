/**
 * 
 */
package org.eamrf.eastore.core.service.tree.file;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.tree.ToString;
import org.eamrf.eastore.core.tree.Tree;
import org.eamrf.eastore.core.tree.TreeNodeVisitException;
import org.eamrf.eastore.core.tree.Trees;
import org.eamrf.eastore.core.tree.Trees.WalkOption;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.PathResource;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

/**
 * Some utility log methods for logging tree data.
 * 
 * @author slenzi
 */
@Service
public class PathResourceTreeLogger {

    @InjectLogger
    private Logger logger;	
    
	public PathResourceTreeLogger() {

	}
	
	/**
	 * logs the tree data (prints tree, plus pre-order and post-order traversals.)
	 * 
	 * @param tree
	 */
	public void logTree(Tree<PathResource> tree){
		
		class PathResourceToString implements ToString<PathResource>{
			@Override
			public String toString(PathResource resource) {
				return resource.toString();
			}
		}		
		
    	logger.info("Tree:\n" + tree.printTree(new PathResourceToString()));
		
	}
	
	/**
	 * Logs pre-order traversal (top-down) order of nodes in the tree
	 * 
	 * @param tree
	 */	
	public void logPreOrderTraversal(Tree<PathResource> tree) {
		
    	logger.info("Pre-Order Traversal (top-down):");
    	try {
			Trees.walkTree(tree,
					(treeNode) -> {
						PathResource res = treeNode.getData();
						logger.info(res.toString());
					},
					WalkOption.PRE_ORDER_TRAVERSAL);
		} catch (TreeNodeVisitException e) {
			logger.error("Error walking tree in pre-order (top-down) traversal", e);
		}		
		
	}
	
	/**
	 * Logs post-order traversal (bottom-up) order of nodes in the tree
	 * 
	 * @param tree
	 */	
	public void logPostOrderTraversal(Tree<PathResource> tree) {
		
    	logger.info("Post-Order Traversal (bottom-up):");
    	try {
			Trees.walkTree(tree,
					(treeNode) -> {
						PathResource res = treeNode.getData();
						logger.info(res.toString());
					},
					WalkOption.POST_ORDER_TRAVERSAL);
		} catch (TreeNodeVisitException e) {
			logger.error("Error walking tree in post-order (bottom-up) traversal", e);
		} 		
		
	}	

}
