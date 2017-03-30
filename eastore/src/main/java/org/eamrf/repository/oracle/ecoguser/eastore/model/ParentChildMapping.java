/**
 * 
 */
package org.eamrf.repository.oracle.ecoguser.eastore.model;

import java.io.Serializable;

/**
 * @author slenzi
 */
public class ParentChildMapping implements Serializable {

	private static final long serialVersionUID = 1671623745174252908L;

	private Long parentId = 0L;
	private Long childId = 0L;
	private String name = null;
	
	public ParentChildMapping(){
		super();
	}
	
	public ParentChildMapping(Long parentId, Long childId, String name) {
		super();
		this.parentId = parentId;
		this.childId = childId;
		this.name = name;
	}

	public Long getParentId() {
		return parentId;
	}

	public Long getChildId() {
		return childId;
	}

	public String getName() {
		return name;
	}

	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}

	public void setChildId(Long childId) {
		this.childId = childId;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "ParentChildMapping [parentId=" + parentId + ", childId=" + childId + ", name=" + name + "]";
	}

}
