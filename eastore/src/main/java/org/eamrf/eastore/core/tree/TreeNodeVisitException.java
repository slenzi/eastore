package org.eamrf.eastore.core.tree;

public class TreeNodeVisitException extends Exception {

	private static final long serialVersionUID = -8100042447662927566L;

	public TreeNodeVisitException() {
		
	}

	public TreeNodeVisitException(String message) {
		super(message);
	}

	public TreeNodeVisitException(Throwable cause) {
		super(cause);
	}

	public TreeNodeVisitException(String message, Throwable cause) {
		super(message, cause);
	}

	public TreeNodeVisitException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
