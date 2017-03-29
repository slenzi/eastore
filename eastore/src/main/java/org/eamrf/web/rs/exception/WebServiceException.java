/**
 * 
 */
package org.eamrf.web.rs.exception;

/**
 * @author slenzi
 *
 */
public class WebServiceException extends Exception {

	/*
	public static final int CODE_UNKNOWN = 0;
	public static final int CODE_NOT_FOUND = 1;
	public static final int CODE_MISSING_REQUIRED_INPUT = 2;
	public static final int CODE_INVALID_INPUT = 3;
	public static final int CODE_DATABSE_ERROR = 4;
	*/
	
	public enum WebExceptionType {
		
		CODE_UNKNOWN(0),
		CODE_NOT_FOUND(1),
		CODE_MISSING_REQUIRED_INPUT(2),
		CODE_INVALID_INPUT(3),
		CODE_DATABSE_ERROR(4),
		CODE_IO_ERROR(5);
		
		private final int value;

	    private WebExceptionType(int value) {
	        this.value = value;
	    }

	    public int getValue() {
	        return value;
	    }		
		
	}	
	
	private WebExceptionType errorType = WebExceptionType.CODE_UNKNOWN;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1429984561375153208L;

	/**
	 * 
	 */
	public WebServiceException(WebExceptionType type, String message) {
		super(message);
		this.errorType = type; 
	}

	/**
	 * @param cause
	 */
	public WebServiceException(WebExceptionType type, Throwable cause) {
		super(cause);
		this.errorType = type;
	}

	/**
	 * @param message
	 * @param cause
	 */
	public WebServiceException(WebExceptionType type, String message, Throwable cause) {
		super(message, cause);
		this.errorType = type;
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public WebServiceException(WebExceptionType type, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		this.errorType = type;
	}

	/**
	 * @return the errorCode
	 */
	public WebExceptionType getExceptionType() {
		return errorType;
	}

	/**
	 * @param errorCode the errorCode to set
	 */
	public void setExceptionType(WebExceptionType type) {
		this.errorType = type;
	}

}
