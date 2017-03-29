package org.eamrf.eastore.core.tree;

import java.util.ArrayList;
import java.util.List;

import org.eamrf.eastore.core.tree.Trees.PrintOption;
import org.eamrf.eastore.core.tree.Trees.WalkOption;

public class Tree<N> {

	private TreeNode<N> rootNode = null;
	
	public Tree() {
		super();
	}

	public Tree(TreeNode<N> rootNode) {
		super();
		this.rootNode = rootNode;
	}

	/**
	 * @return the rootNode
	 */
	public TreeNode<N> getRootNode() {
		return rootNode;
	}

	/**
	 * @param rootNode the rootNode to set
	 */
	public void setRootNode(TreeNode<N> rootNode) {
		this.rootNode = rootNode;
	}
	
    public String printTree(){
    	
    	return Trees.printTree(rootNode, PrintOption.TERMINAL);
    	
    }
    
    /**
     * Prints the tree, calling toString on each object stored in the tree.
     * 
     * @return
     */
    public String printHtmlTree(){
    	
    	return Trees.printTree(rootNode, PrintOption.HTML);
    	
    }
    
    /**
     * Print the tree, using the toString function interface to specify how to print each object in the tree.
     * 
     * @param toString
     * @return
     */
    public String printHtmlTree(ToString<N> toString){
    	
    	return Trees.printTree(rootNode, PrintOption.HTML, toString);
    	
    }  
    
    public List<TreeNode<N>> toList(WalkOption option){
    	
    	if(rootNode == null){
    		return null;
    	}
    	
    	List<TreeNode<N>> nodeList = new ArrayList<TreeNode<N>>();
    	
    	try {
			Trees.walkTree(rootNode,
					(treeNode) -> {
						nodeList.add(treeNode);
					}
					, option);
		} catch (TreeNodeVisitException e) {
			// eat it
		}
    	
    	return nodeList;
    	
    } 
    
}
