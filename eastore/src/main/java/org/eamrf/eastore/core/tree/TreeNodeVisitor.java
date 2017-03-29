package org.eamrf.eastore.core.tree;

/**
 * Used in conjunction with org.lenzi.fstore.tree.Trees to walk a tree and visit each node.
 * 
 * @author sal
 *
 * @param <N>
 */
@FunctionalInterface
public interface TreeNodeVisitor<N> {

	public void visitNode(TreeNode<N> node) throws TreeNodeVisitException;
	
}
