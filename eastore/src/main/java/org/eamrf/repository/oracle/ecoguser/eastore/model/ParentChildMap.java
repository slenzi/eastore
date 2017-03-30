/**
 * 
 */
package org.eamrf.repository.oracle.ecoguser.eastore.model;

import java.io.Serializable;

/**
 * @author slenzi
 */
public class ParentChildMap implements Serializable {

	private static final long serialVersionUID = 1671623745174252908L;

	private Long parentId = 0L;
	private Long childId = 0L;
	private String name = null;
	private String type = null;
	
	public ParentChildMap(){
		super();
	}
	
	public ParentChildMap(Long parentId, Long childId, String name, String type) {
		super();
		this.parentId = parentId;
		this.childId = childId;
		this.name = name;
		this.type = type;
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "ParentChildMap [parentId=" + parentId + ", childId=" + childId + ", name=" + name + ", type=" + type
				+ "]";
	}


}
