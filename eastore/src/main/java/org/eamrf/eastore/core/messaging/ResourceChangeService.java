/**
 * 
 */
package org.eamrf.eastore.core.messaging;

import java.time.LocalDate;
import java.time.LocalTime;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.aop.profiler.MethodTimer;
import org.eamrf.eastore.core.messaging.model.EventCode;
import org.eamrf.eastore.core.messaging.model.ResourceChangeMessage;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Message service for notifying clients that resources have changed on the server.
 * 
 *  Messages are broadcasted to /topic/resource/change
 * 
 * @author slenzi
 */
@Service
public class ResourceChangeService {

	@InjectLogger
	private Logger logger;	
	
    @Autowired
    private SimpMessagingTemplate template;	
    
    private final String messageDestination = "/topic/resource/change";
	
	public ResourceChangeService() { }
	
	/**
	 * Broadcasts a message that the contents of the directory changed.
	 * 
	 * When clients receive this message, they can issue a REST call to the server to get
	 * the latest data (if they wish.)
	 * 
	 * @param dirNodeId - the id of the directory path resource
	 */
	@MethodTimer
	public void directoryContentsChanged(Long dirNodeId){
		
		EventCode event = EventCode.DIRECTORY_CONTENTS_CHANGED;
		
		ResourceChangeMessage mesg = new ResourceChangeMessage();
		mesg.setCode(event.getCodeString());
		mesg.setMessage(EventCode.getCodeMessage(event.getCodeString()));
		mesg.setNodeId(dirNodeId);
		mesg.setDate(LocalDate.now());
		mesg.setTime(LocalTime.now());
		
		template.convertAndSend(messageDestination, mesg);
		
	}

}
