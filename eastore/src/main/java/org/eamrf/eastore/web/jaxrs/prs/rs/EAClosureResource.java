/**
 * 
 */
package org.eamrf.eastore.web.jaxrs.prs.rs;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.properties.ManagedProperties;
import org.eamrf.eastore.core.service.ClosureService;
import org.eamrf.eastore.web.jaxrs.BaseResourceHandler;
import org.eamrf.repository.oracle.ecoguser.eastore.model.ParentChildMapping;
import org.eamrf.web.rs.exception.WebServiceException;
import org.eamrf.web.rs.exception.WebServiceException.WebExceptionType;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author slenzi
 */
@Path("/closure")
@Service("eaClosureResource")
public class EAClosureResource extends BaseResourceHandler {

    @InjectLogger
    private Logger logger;
    
    @Autowired
    private ManagedProperties appProps;
    
    @Autowired
    private ClosureService closureService;
    
	public EAClosureResource() {

	}
    
    /**
     * Get parent-child mappings
     * 
     * @param nodeId
     * @return
     * @throws WebServiceException
     */
    @GET
    @Path("/mappings/{nodeId}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ParentChildMapping> getParentChildMappings(@PathParam("nodeId") Long nodeId) throws WebServiceException {
    	
    	if(nodeId == null){
    		handleError("Missing nodeId param.", WebExceptionType.CODE_IO_ERROR);
    	}
    	
    	List<ParentChildMapping> mappings = null;
    	try {
			mappings = closureService.getParentChildMappings(nodeId);
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
