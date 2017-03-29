package org.eamrf.web.rs.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.web.rs.exception.WebServiceException.WebExceptionType;
import org.slf4j.Logger;

/**
 * Map service exception to HTTP error code.
 * 
 * @author sal
 */
@Provider
public class WebServiceExceptionMapper implements ExceptionMapper<WebServiceException> {

    @InjectLogger
    private Logger logger;	
	
	@Override
	public Response toResponse(WebServiceException exception) {
		
		Response.Status status = null;
		
		if(exception.getExceptionType() == WebExceptionType.CODE_NOT_FOUND){
			status = Response.Status.NOT_FOUND;
		}else if(exception.getExceptionType() == WebExceptionType.CODE_MISSING_REQUIRED_INPUT){
			status = Response.Status.BAD_REQUEST;
		}else if(exception.getExceptionType() == WebExceptionType.CODE_INVALID_INPUT){
			status = Response.Status.BAD_REQUEST;
		}else if(exception.getExceptionType() == WebExceptionType.CODE_DATABSE_ERROR){
			status = Response.Status.INTERNAL_SERVER_ERROR;
		}else if(exception.getExceptionType() == WebExceptionType.CODE_IO_ERROR){
			status = Response.Status.INTERNAL_SERVER_ERROR;			
		}else if(exception.getExceptionType() == WebExceptionType.CODE_UNKNOWN){
			status = Response.Status.BAD_REQUEST;
		}else{
			status = Response.Status.BAD_REQUEST;
		}
		
		/*
		status = Response.Status.FORBIDDEN;
		status = Response.Status.INTERNAL_SERVER_ERROR;
		status = Response.Status.NOT_IMPLEMENTED;
		status = Response.Status.UNAUTHORIZED;
		status = Response.Status.NOT_ACCEPTABLE;
		*/
		
		String jsonString = "{\"errorMessage\":\"" + exception.getMessage() + "\"}";
		
		logger.error(WebServiceExceptionMapper.class.getName() + ".toResponse(...) => " + jsonString);
		
        //Response response = Response.status(status).entity(jsonString).build();
        
		// set access-control-allow attributes for CORS access
        Response response = Response.status(status).entity(jsonString)
	        .header("Access-Control-Allow-Origin", "*")
	        .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization, auth-token")
	        .header("Access-Control-Allow-Credentials", "true")
	        .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
	        .header("Access-Control-Max-Age", "86400")
	        .build();
        
        logger.error(response.toString());
        
        return response;
        
	}

}
