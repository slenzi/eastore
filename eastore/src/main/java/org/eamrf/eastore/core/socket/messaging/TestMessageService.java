package org.eamrf.eastore.core.socket.messaging;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.socket.messaging.model.ReplyMessage;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Sample messaging service which writes a test message every 10 seconds to /topic/test
 * 
 * Currently enabled. (comment out @PostConstruct and @PreDestroy to disable))
 * 
 * @author sal
 */
@Service
public class TestMessageService {

	@InjectLogger
	private Logger logger;	
	
    @Autowired
    private SimpMessagingTemplate template;
    
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    
    private final String messageDestination = "/topic/test";
	
	public TestMessageService() {

	}

	@PostConstruct
	private void init(){
		
		executor.submit(() -> {
			
			while(true){
				
				//logger.info("Sending message...");
				
				LocalDate nowDate = LocalDate.now();
				LocalTime nowTime = LocalTime.now();
				
				template.convertAndSend(messageDestination, 
						new ReplyMessage("This is a message! " + nowDate + " " +nowTime));
				
				TimeUnit.SECONDS.sleep(10);
				
			}
			
		});		
		
	}
	
	@PreDestroy
	private void cleanup(){
		
		executor.shutdownNow();
		
	}
	
}
