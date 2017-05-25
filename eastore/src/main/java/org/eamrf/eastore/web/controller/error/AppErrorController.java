/**
 * 
 */
package org.eamrf.eastore.web.controller.error;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;

/**
 * Error handling controller
 * 
 * @author slenzi
 */
@Controller
public class AppErrorController implements ErrorController {

    @InjectLogger
    private Logger logger;
    
    @Autowired
    private ErrorAttributes errorAttributes;    
	
	private final static String ERROR_PATH = "/error";
	
	/**
	 * 
	 */
	public AppErrorController() {

	}
	
    @RequestMapping(value = ERROR_PATH)
    public ModelAndView errorHtml(HttpServletRequest request, HttpServletResponse response) {
    	
    	logger.info(AppErrorController.class.getSimpleName() + ".errorHtml(...) called");
    	
        //return new ModelAndView("/errors/error", getErrorAttributes(request, false));
    	
    	Map<String, Object> errorAtts = getErrorAttributes(request, true);
    	
    	StringBuffer buf = new StringBuffer();
    	buf.append("status = " + response.getStatus() + "\n");
    	buf.append("error = " + (String)errorAtts.get("error") + "\n");
    	buf.append("message = " + (String)errorAtts.get("message") + "\n");
    	buf.append("timeStamp  = " + errorAtts.get("timestamp").toString() + "\n");
    	buf.append("trace  = " + (String) errorAtts.get("trace") + "\n");
    	
    	logger.error(buf.toString());
    	
    	ModelAndView model = new ModelAndView("error/error_page");
    	
    	return model;
        
    }
    
    private Map<String, Object> getErrorAttributes(HttpServletRequest request, boolean includeStackTrace) {
    	
    	logger.info(AppErrorController.class.getSimpleName() + ".getErrorAttributes(...) called");
    	
        RequestAttributes requestAttributes = new ServletRequestAttributes(request);
        return errorAttributes.getErrorAttributes(requestAttributes, includeStackTrace);
        
    }    

	/* (non-Javadoc)
	 * @see org.springframework.boot.autoconfigure.web.ErrorController#getErrorPath()
	 */
	@Override
	public String getErrorPath() {
		return ERROR_PATH;
	}

}
