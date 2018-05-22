package org.eamrf.eastore.core.socket.messaging;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eamrf.concurrent.task.QueuedTaskManager;
import org.eamrf.concurrent.task.TaskManagerProvider;
import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.service.file.task.FileServiceTask;
import org.eamrf.eastore.core.socket.messaging.model.UserActionStatusMessage;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Message service for notifying users the status of various actions they
 * triggered on the server. (e.g., zip-download action)
 * 
 * @author slenzi
 *
 */
@Service
public class UserActionMessageService {

	@InjectLogger
	private Logger logger;	
	
    @Autowired
    private SimpMessagingTemplate template;
    
    @Autowired
    private TaskManagerProvider taskManagerProvider;    
    
    private final String messageDestination = "/topic/action";
    
    private ExecutorService executorService = null;
    private QueuedTaskManager taskManager = null;
    
    private class UserActionBroadcaster extends FileServiceTask<Void> {

    	private int jobCount = 0;
    	private UserActionStatusMessage message = null;
    	
    	public UserActionBroadcaster(UserActionStatusMessage message) {
    		this.message = message;
    	}
    	
		@Override
		public int getJobCount() {
			return jobCount;
		}

		@Override
		public String getStatusMessage() {
			if(getJobCount() <= 0) {
				return "Broadcasting user action status for user " + message.getUserId() + " is pending...";
			}else {
				return "Broadcasting user action status for user " + message.getUserId();
			}
		}

		@Override
		public String getUserId() {
			return message.getUserId();
		}

		@Override
		public Void doWork() throws ServiceException {
			
			template.convertAndSend(messageDestination, message);
			setCompletedJobCount(this, 1);
			return null;			
			
		}

		@Override
		public Logger getLogger() {
			return logger;
		}
    	
    }
	
	public UserActionMessageService() {
		
	}
	
	@PostConstruct
	public void init(){
		
		executorService = Executors.newSingleThreadExecutor();
		
		// wait 2 seconds between consuming tasks. Don't want to flood the clients with update messages
		taskManager = taskManagerProvider.createQueuedTaskManager(1000L);
		
		taskManager.startTaskManager(executorService);
		
		// custom converter which supports java8 LocalDate and LocalTime formats
		MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
		converter.setObjectMapper(objectMapper());
		template.setMessageConverter(converter);
		
	}
	
	@PreDestroy
	public void cleanup() {
		
		taskManager.stopTaskManager();
		
	}
	
	private ObjectMapper objectMapper() {

		ObjectMapper objectMapper = new ObjectMapper()
				.registerModule(new Jdk8Module())
				.registerModule(new JavaTimeModule());
		
		//objectMapper.findAndRegisterModules();

		return objectMapper;

	}
	
	public void broadcast(UserActionStatusMessage message) {
		
		UserActionBroadcaster broadcastTask = createTask(message);
		taskManager.addTask(broadcastTask);
		
	}
	
	private UserActionBroadcaster createTask(UserActionStatusMessage message) {
		
		UserActionBroadcaster broadcastTask = new UserActionBroadcaster(message);
		broadcastTask.setName("Broadcast event: [UserActionStatusMessage: userId=" + message.getUserId() + 
				", type=" + message.getTaskType().toString() + "]");
		return broadcastTask;
	}

}
