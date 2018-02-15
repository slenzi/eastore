/**
 * 
 */
package org.eamrf.eastore.core.service.file;

/**
 * @author slenzi
 *
 */
public enum PermissionError {

	// write, rename, delete
	WRITE("Denied Write Permission"),
	
	// reading
	READ("Denied Read Permission"),
	
	// administering group permissions
	EXECUTE("Denied Execute Permission");
	
	private final String error;
	
	private PermissionError(final String error) {
		this.error = error;
	}

	@Override
	public String toString() {
		return this.error;
	}	
	
}
