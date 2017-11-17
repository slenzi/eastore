/**
 * 
 */
package org.eamrf.eastore.core.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author sal
 *
 */
public class TreeNode<N> {
	
	private N data = null;
	private TreeNode<N> parent = null;
	private List<TreeNode<N>> childList = null;
	
	public TreeNode() {
		super();
	}
	
	public TreeNode(N data) {
		super();
		this.data = data;
	}

	public TreeNode(N data, TreeNode<N> parent) {
		super();
		this.data = data;
		this.parent = parent;
	}

	public void setData(N data) {
		this.data = data;
	}

	public N getData() {
		return data;
	}

	public void setParent(TreeNode<N> parent) {
		this.parent = parent;
	}

	public TreeNode<N> getParent() {
		return parent;
	}
	
	public boolean hasParent(){
		return parent != null ? true : false;
	}

	public void addChildNode(TreeNode<N> child) {
		if(child == null){
			return;
		}
		if(childList == null){
			childList = new ArrayList<TreeNode<N>>();
		}
		child.setParent(this);
		childList.add(child);
	}

	public void addChildren(List<TreeNode<N>> children) {
		if(children == null || children.size() == 0){
			return;
		}
		if(childList == null){
			childList = new ArrayList<TreeNode<N>>();
		}
		for(TreeNode<N> n : children){
			n.setParent(this);
		}
		childList.addAll(children);
	}

	public void setChildren(List<TreeNode<N>> children) {
		childList = children;
		if(childList != null){
			for(TreeNode<N> n : childList){
				n.setParent(this);
			}			
		}
	}
	
	/**
	 * Sort the children
	 * 
	 * @param comparator - comparator which defines how to store the children
	 */
	public void sortChildren(Comparator<TreeNode<N>> comparator){
		if(childList != null && childList.size() > 0){
			Collections.sort(childList, comparator);
		}
	}	

	public List<TreeNode<N>> getChildren() {
		return childList;
	}

	public boolean hasChildren() {
		return ((childList != null && childList.size() > 0) ? true : false);
	}

	public int getChildCount() {
		return ((childList != null) ? childList.size() : 0);
	}
	
	public TreeNode<N> getFirstChild(){
		if(hasChildren()) {
			return childList.get(0);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TreeNode<N> other = (TreeNode<N>) obj;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		return true;
	}
}