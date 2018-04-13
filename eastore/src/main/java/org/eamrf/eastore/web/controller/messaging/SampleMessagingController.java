package org.eamrf.eastore.web.controller.messaging;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.socket.messaging.model.ReplyMessage;
import org.slf4j.Logger;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for web socket configuration
 * 
 * @author slenzi
 */
@Controller
@RequestMapping("/messaging/sample")
public class SampleMessagingController {

    @InjectLogger
    private Logger logger;	
	
	public SampleMessagingController() {
		
	}
	
    @MessageMapping("/hi")
	@SendTo("/topic/hi")
	public ReplyMessage processMessage() throws Exception {
		
    	logger.info(SampleMessagingController.class.getName() + ".processMessage(...) called.");
    	
    	return new ReplyMessage("Reply From Server!");
		
	}	

}
