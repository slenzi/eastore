package org.eamrf.eastore.web.jaxrs;

import org.eamrf.core.util.Mailer;
import org.eamrf.core.util.StringUtil;
import org.eamrf.eastore.core.constants.AppConstants;
import org.eamrf.eastore.core.properties.ManagedProperties;
import org.eamrf.web.rs.exception.WebServiceException;
import org.eamrf.web.rs.exception.WebServiceException.WebExceptionType;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;

/**
 * All PRS RS Service Jax-RS resources should extends this handler to gain basic email error handling.
 * 
 * @author slenzi
 */
public abstract class BaseResourceHandler {

    @Autowired
    private ManagedProperties appProps;
    
    private Mailer mailer = new Mailer();
	
	public BaseResourceHandler() {
		
	}
	
	public abstract Logger getLogger();
	
    /**
     * Builds a simply OK response message in JSON.
     * 
     * @return
     */
    public String buildJsonOK(){
    	
    	Gson gson = new Gson();
    	StringBuffer buf = new StringBuffer();
    	buf.append("{");
    	buf.append(gson.toJson("reply") + " : " + gson.toJson("ok"));
    	buf.append("}");
    	
    	return buf.toString();
    	
    }	
	
	public void handleError(String message, WebExceptionType type) throws WebServiceException {
		handleError(message, type, null);
	}		
	
	public void handleError(String message, WebExceptionType type, Throwable e) throws WebServiceException {
		String nl = System.getProperty("line.separator");
		if(e != null){
			e.printStackTrace();
			getLogger().error(e.getMessage(), e);
			getLogger().error(message, e);
			sendEmail(message + nl + nl + e.getMessage());
			throw new WebServiceException(type, message + ", " + e.getMessage());
		}else{
			getLogger().error(message);
			sendEmail(message);
			throw new WebServiceException(type, message);
		}
	}
	
	private void sendEmail(String message){
		String mailServer = appProps.getProperty(AppConstants.PROP_NAME_MAIL_SERVER);
		String mailTo = appProps.getProperty(AppConstants.PROP_NAME_MAIL_TO);
		String mailFrom = appProps.getProperty(AppConstants.PROP_NAME_MAIL_FROM);
		String mailSubject = appProps.getProperty(AppConstants.PROP_NAME_MAIL_SUBJECT);
		try {
			mailer.send(mailServer, mailTo, mailFrom, mailSubject, message);
		} catch (Exception e) {
			getLogger().error("Error sending email. " + e.getMessage(),e);
		}		
	}

}
