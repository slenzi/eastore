package org.eamrf.repository.jdbc.oracle.ecoguser.ws.model.impl;

/**
 * Taken from MatchInformaticsLayer project
 * 
 * @author sal
 */
public class WSUserRole {

	private int id = 0;
	private WSUser user;	
	private Role role;
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public WSUser getUser() {
		return user;
	}

	public void setUser(WSUser user) {
		this.user = user;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
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
		WSUserRole other = (WSUserRole) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
}