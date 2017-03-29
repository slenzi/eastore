/**
 * 
 */
package org.eamrf.eastore.web.jaxrs.prs.rs;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.DateUtil;
import org.eamrf.core.util.StringUtil;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.properties.ManagedProperties;
import org.eamrf.eastore.core.service.StoreService;
import org.eamrf.eastore.web.jaxrs.BaseResourceHandler;
import org.eamrf.repository.oracle.ecoguser.eastore.model.ParentChildMapping;
import org.eamrf.web.rs.exception.WebServiceException;
import org.eamrf.web.rs.exception.WebServiceException.WebExceptionType;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

/**
 * @author slenzi
 */
@Path("/store")
@Service("eaStoreResource")
public class EAStoreResource extends BaseResourceHandler {

    @InjectLogger
    private Logger logger;
    
    @Autowired
    private ManagedProperties appProps;
    
    @Autowired
    private StoreService storeService;

	public EAStoreResource() {

	}
	
    /**
     * Echo message back to client. This serves as a test method for jax-rs clients.
     * 
     * @param message - a message from the client which will be echoed back.
     * @return
     * @throws WebServiceException
     */
    @GET
	@Path("/echo")
	@Produces(MediaType.APPLICATION_JSON)    
    public Response echo(@QueryParam("message") String message) throws WebServiceException {
    	
    	String reply = "";
    	String prefix = "Hello from " + EAStoreResource.class.getName() + 
    			", the time is " + DateUtil.defaultFormat(DateUtil.getCurrentTime());
    	
    	if(message == null || message.trim().equals("")){
    		reply = prefix + ". You did not send a message to be echoed back. :(";
    	}else{
    		reply = prefix + ". Your message was => " + message;
    	}
    	
    	Gson gson = new Gson();
    	
    	StringBuffer buf = new StringBuffer();
    	buf.append("{");
    	buf.append(gson.toJson("reply") + " : " + gson.toJson(reply));
    	buf.append("}");
    	
    	return Response.ok(buf.toString(), MediaType.APPLICATION_JSON).build();
    }
    
    @GET
    @Path("/mappings/{nodeId}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ParentChildMapping> getParentChildMappings(@PathParam("nodeId") Long nodeId) throws WebServiceException {
    	
    	if(nodeId == null){
    		handleError("Missing nodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	List<ParentChildMapping> mappings = null;
    	try {
			mappings = storeService.getParentChildMappings(nodeId);
		} catch (ServiceException e) {
			handleError(e.getMessage(), WebExceptionType.CODE_IO_ERROR, e);
		}
    	
    	return mappings;
    	
    }

	/* (non-Javadoc)
	 * @see org.eamrf.eastore.web.jaxrs.BaseResourceHandler#getLogger()
	 */
	@Override
	public Logger getLogger() {
		return logger;
	}

}
