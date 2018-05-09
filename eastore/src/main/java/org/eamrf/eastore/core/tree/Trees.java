package org.eamrf.eastore.core.tree;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eamrf.eastore.core.tree.Trees.WalkOption;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.FileMetaResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.PathResource;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.ResourceType;

/**
 * Contains a bunch of static methods for working on Trees.
 * 
 * @author sal
 */
public final class Trees {

	// http://en.wikipedia.org/wiki/Tree_traversal
	public enum WalkOption {

		// bottom-up
		POST_ORDER_TRAVERSAL,

		// top-down
		PRE_ORDER_TRAVERSAL

	}

	public enum PrintOption {

		// uses <br> tag to separate lines
		HTML,
		
		// uses system line.separator to separate lines
		TERMINAL,

	}

	/**
	 * Walk a tree
	 * 
	 * @param tree
	 *            - will start at the tree's root node
	 * @param visitor
	 * @param walkOption
	 */
	public static <N> void walkTree(Tree<N> tree, TreeNodeVisitor<N> visitor, WalkOption walkOption)
			throws TreeNodeVisitException {
		walkTree(tree.getRootNode(), visitor, walkOption);
	}

	/**
	 * Walk a tree
	 * 
	 * @param start
	 *            - node to start at
	 * @param visitor
	 * @param option
	 */
	public static <N> void walkTree(TreeNode<N> start, TreeNodeVisitor<N> visitor, WalkOption walkOption)
			throws TreeNodeVisitException {

		switch (walkOption) {

		case POST_ORDER_TRAVERSAL:
			postOrderTraversal(start, visitor);
			break;

		case PRE_ORDER_TRAVERSAL:
			preOrderTraversal(start, visitor);
			break;

		default:
			preOrderTraversal(start, visitor);
			break;

		}

	}

	/**
	 * Walk tree in post-order traversal
	 * 
	 * @param node
	 * @param visitor
	 */
	private static <N> void postOrderTraversal(TreeNode<N> node, TreeNodeVisitor<N> visitor)
			throws TreeNodeVisitException {
		if (node.hasChildren()) {
			for (TreeNode<N> childNode : node.getChildren()) {
				postOrderTraversal(childNode, visitor);
			}
			visitor.visitNode(node);
		} else {
			visitor.visitNode(node);
		}
	}

	/**
	 * Walk tree in pre-order traversal
	 * 
	 * @param node
	 * @param visitor
	 */
	private static <N> void preOrderTraversal(TreeNode<N> node, TreeNodeVisitor<N> visitor)
			throws TreeNodeVisitException {

		visitor.visitNode(node);

		if (node.hasChildren()) {
			for (TreeNode<N> childNode : node.getChildren()) {
				preOrderTraversal(childNode, visitor);
			}
		}

	}

	/**
	 * Print a tree.
	 * 
	 * Traverses the tree and calls TreeNode.getData().toString() for each node.
	 * 
	 * @param start
	 *            - the node to start at
	 * @param option
	 *            - options on how to print the tree
	 * @return
	 * 
	 * @deprecated - use printTree(TreeNode<N> start, PrintOption option, ToString<N> toString)
	 */
	public static <N> String printTree(TreeNode<N> start, PrintOption option) {

		StringBuffer buffer = new StringBuffer();

		switch (option) {

		case HTML:
			printHtml(start, "", true, buffer, null);
			break;		

		case TERMINAL:
			printTerminal(start, "", true, buffer, null);
			break;

		default:
			break;

		}

		return buffer.toString();

	}

	/**
	 * Print a tree.
	 * 
	 * Traverses the tree and calls TreeNode.getData().toString() for each node.
	 * 
	 * @param start
	 *            - the node to start at
	 * @param option
	 *            - options on how to print the tree
	 * @param toString
	 *            - specify how to convert the data object stored in each tree
	 *            node into a string
	 * @return
	 */
	public static <N> String printTree(TreeNode<N> start, PrintOption option, ToString<N> toString) {

		StringBuffer buffer = new StringBuffer();

		switch (option) {

		case HTML:
			printHtml(start, "", true, buffer, toString);
			break;

		case TERMINAL:
			printTerminal(start, "", true, buffer, toString);
			break;

		default:
			break;

		}

		return buffer.toString();

	}	

	/**
	 * Print for html page
	 * 
	 * @param node
	 * @param linePrefix
	 * @param isTail
	 * @param buffer
	 */
	private static <N> void printHtml(
			TreeNode<N> node, String linePrefix, boolean isTail, StringBuffer buffer, ToString<N> toString) {

		buffer.append(linePrefix + (isTail ? "|__" : "|__")
				+ ((toString != null) ? toString.toString(node.getData()) : node.getData().toString()) + "<br>");

		if (node.hasChildren()) {

			List<TreeNode<N>> children = node.getChildren();

			for (int i = 0; i < children.size() - 1; i++) {
				printHtml(children.get(i),
						linePrefix + (isTail ? "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" : "|&nbsp;&nbsp;&nbsp;&nbsp;"), false,
						buffer, toString);
			}
			if (node.getChildren().size() >= 1) {
				printHtml(children.get(children.size() - 1),
						linePrefix + (isTail ? "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" : "|&nbsp;&nbsp;&nbsp;&nbsp;"), true,
						buffer, toString);
			}
		}

	}

	/**
	 * Print for terminal
	 * 
	 * @param node
	 * @param linePrefix
	 * @param isTail
	 * @param buffer
	 */
	private static <N> void printTerminal(
			TreeNode<N> node, String linePrefix, boolean isTail, StringBuffer buffer, ToString<N> toString) {

		buffer.append(linePrefix + (isTail ? "|__" : "|__")
				+ ((toString != null) ? toString.toString(node.getData()) : node.getData().toString())
				+ System.getProperty("line.separator"));

		if (node.hasChildren()) {

			List<TreeNode<N>> children = node.getChildren();

			for (int i = 0; i < children.size() - 1; i++) {
				printTerminal(children.get(i), linePrefix + (isTail ? "    " : "|   "), false, buffer, toString);
			}
			if (node.getChildren().size() >= 1) {
				printTerminal(children.get(children.size() - 1), linePrefix + (isTail ? "    " : "|   "), true, buffer,
						toString);
			}
		}

	}
	
	/**
	 * Recursively walks the node, and applies the comparator to the child node lists
	 * to order the children.
	 * 
	 * @param node
	 * @param comparator
	 */
	public static <N> void sortChildren(TreeNode<N> node, Comparator<TreeNode<N>> comparator) {
		
		if(node.hasChildren()){
			Collections.sort(node.getChildren(), comparator);
			for(TreeNode<N> child : node.getChildren()){
				Trees.sortChildren(child, comparator);
			}
		}
		
	}
	
	/**
	 * Counts the number of nodes in the tree.
	 * 
	 * @param tree - the tree to traverse
	 * @return
	 * @throws TreeNodeVisitException 
	 */
	public static <N> Integer nodeCount(Tree<N> tree) throws TreeNodeVisitException {
		return Trees.nodeCount(tree.getRootNode());
	}
	
	/**
	 * Counts the number of nodes in the tree of the specified type
	 * 
	 * @param tree - the tree to traverse
	 * @param clazz - the type of nodes to count
	 * @return
	 * @throws TreeNodeVisitException
	 */
	public static <N> Integer nodeCount(Tree<N> tree, Class<?> clazz) throws TreeNodeVisitException {
		return Trees.nodeCount(tree.getRootNode(), clazz);
	}	
	
	/**
	 * Counts the number of nodes in the tree
	 * 
	 * @param node - the root node of the tree
	 * @return
	 * @throws TreeNodeVisitException
	 */
	public static <N> Integer nodeCount(TreeNode<N> node) throws TreeNodeVisitException {
		AtomicInteger nodeCount = new AtomicInteger();
		Trees.walkTree(node, (treeNode) -> {
			//N n = treeNode.getData();
			nodeCount.addAndGet(1);
		}, WalkOption.PRE_ORDER_TRAVERSAL);
		return nodeCount.get();
	}
	
	/**
	 * Counts the number of nodes in the tree of the specified type
	 * 
	 * @param node - the root node of the tree
	 * @param clazz - the type of nodes to count
	 * @return
	 * @throws TreeNodeVisitException
	 */
	public static <N> Integer nodeCount(TreeNode<N> node, Class<?> clazz) throws TreeNodeVisitException {
		AtomicInteger nodeCount = new AtomicInteger();
		Trees.walkTree(node, (treeNode) -> {
			if(clazz.isInstance(treeNode.getData())) {
				nodeCount.addAndGet(1);
			}
		}, WalkOption.PRE_ORDER_TRAVERSAL);
		return nodeCount.get();
	}	

}
