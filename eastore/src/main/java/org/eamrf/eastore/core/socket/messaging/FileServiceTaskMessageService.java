/**
 * 
 */
package org.eamrf.eastore.core.socket.messaging;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eamrf.concurrent.task.QueuedTaskManager;
import org.eamrf.concurrent.task.TaskManagerProvider;
import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.aop.profiler.MethodTimer;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.service.file.task.FileServiceTask;
import org.eamrf.eastore.core.socket.messaging.model.FileServiceTaskStatus;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Message service for notifying clients of progress of file service tasks
 * 
 * Messages are broadcasted to /topic/file/task
 * 
 * @author slenzi
 */
@Service
public class FileServiceTaskMessageService {

	@InjectLogger
	private Logger logger;	
	
    @Autowired
    private SimpMessagingTemplate template;
    
    @Autowired
    private TaskManagerProvider taskManagerProvider;    
    
    private final String messageDestination = "/topic/file/task";
    
    private ExecutorService executorService = null;
    private QueuedTaskManager taskManager = null;
    
    /**
     * A task for the queued task manager which encapsulates the logic for broadcasting file service task status messages
     * 
     * @author slenzi
     *
     */
	private class FileServiceTaskBroadcaster extends FileServiceTask<Void> {

		private FileServiceTask<?> task = null;
		
		private int jobCount = 0;

		public FileServiceTaskBroadcaster(FileServiceTask<?> task) {
			this.task = task;
			//notifyChange();
		}
		
		private void calculateJobCount() {
			
			jobCount = 1;
			
			notifyChange();
			
		}		
		
		@Override
		public Void doWork() throws ServiceException {
			
			calculateJobCount();
			
			FileServiceTaskStatus mesg = new FileServiceTaskStatus();
			mesg.setId(String.valueOf(task.getTaskId()));
			mesg.setJobCount(task.getJobCount());
			mesg.setJobCompletedCount(task.getCompletedJobCount());
			mesg.setProgress(String.valueOf(Math.round(task.getProgress())));
			mesg.setMessage(task.getStatusMessage());
			mesg.setUserId(task.getUserId());
			
			template.convertAndSend(messageDestination, mesg);
			
			setCompletedJobCount(getTaskId(), 1);
			
			return null;
				
		}
		
		public long getTaskId() {
			return task.getTaskId();
		}

		/**
		 * Get logger for this task
		 */
		@Override
		public Logger getLogger() {
			return logger;
		}

		@Override
		public int getJobCount() {
			return jobCount;
		}

		@Override
		public String getStatusMessage() {
			if(getJobCount() <= 0) {
				return "Broadcasting file service task status for task " + task.getTaskId() + " is pending...";
			}else {
				return "Broadcasting file service task status for task " + task.getTaskId();
			}
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object otherObject) {
			if(otherObject instanceof FileServiceTaskBroadcaster) {
				FileServiceTaskBroadcaster otherTask = (FileServiceTaskBroadcaster)otherObject;
				if(this.getTaskId() == otherTask.getTaskId()) {
					// same task
					return true;
				}
			}
			return false;
		}

		@Override
		public String getUserId() {
			return task.getUserId();
		}
	
	};    
	
	public FileServiceTaskMessageService() { }
    
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
	
	/**
	 * Broadcasts a message that the contents of the directory changed.
	 * 
	 * When clients receive this message, they can issue a REST call to the server to get
	 * the latest data (if they wish.)
	 * 
	 * @param dirNodeId - the id of the directory path resource
	 */
	@MethodTimer
	public void broadcast(FileServiceTask<?> task){
		
		FileServiceTaskBroadcaster broadcastTask = createTask(task);
		
		// Don't want to flood the clients with messages, so only add the task if the task
		// manager doesn't already have a similar task.
		if(!taskManager.contains(broadcastTask)) {
			taskManager.addTask(broadcastTask);
		}
		
	}
	
	/**
	 * Create a new resource change task
	 * 
	 * @param event
	 * @param nodeId
	 * @return
	 */
	private FileServiceTaskBroadcaster createTask(FileServiceTask<?> task) {
		
		FileServiceTaskBroadcaster broadcastTask = new FileServiceTaskBroadcaster(task);
		broadcastTask.setName("Broadcast event: [FileServiceTask: taskId=" + task.getTaskId() + "]");
		
		return broadcastTask;
		
	}

}
