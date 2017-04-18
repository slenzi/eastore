package org.eamrf.repository.jdbc.oracle.ecoguser.ws.model.impl;

import java.util.HashSet;
import java.util.Set;

/**
 * Taken from MatchInformaticsLayer project
 * 
 * In oracle ecogtst the table is under the ecoguser schema, but in
 * oracle ecogprod it's under the webapps schema....uhg!
 * 
 * @author sal
 */
public class WSUser {

	private int id = 0;
	private String login = null;
	private String hashedPassword = null;
	private String wsName = null;
	private Set<WSUserRole> roles = new HashSet<WSUserRole>();
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	
	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	
	public String getHashedPassword() {
		return hashedPassword;
	}

	public void setHashedPassword(String hashedPassword) {
		this.hashedPassword = hashedPassword;
	}

	
	public String getWsName() {
		return wsName;
	}

	public void setWsName(String wsName) {
		this.wsName = wsName;
	}

	public Set<WSUserRole> getRoles() {
		return roles;
	}

	public void setRoles(Set<WSUserRole> roles) {
		this.roles = roles;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((login == null) ? 0 : login.hashCode());
		result = prime * result + ((wsName == null) ? 0 : wsName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WSUser other = (WSUser) obj;
		if (login == null) {
			if (other.login != null)
				return false;
		} else if (!login.equals(other.login))
			return false;
		if (wsName == null) {
			if (other.wsName != null)
				return false;
		} else if (!wsName.equals(other.wsName))
			return false;
		return true;
	}
	
	public String toString() {
		String self = "";

		self = self.concat("WS_LOGIN: " +  this.getLogin());
		self = self.concat("\nWS_PASSWORD: " + this.getHashedPassword());

		return self;
	}
}