/**
 * 
 */
package org.eamrf.eastore.core.socket.messaging;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eamrf.concurrent.task.AbstractQueuedTask;
import org.eamrf.concurrent.task.QueuedTaskManager;
import org.eamrf.concurrent.task.TaskManagerProvider;
import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.aop.profiler.MethodTimer;
import org.eamrf.eastore.core.exception.ServiceException;
import org.eamrf.eastore.core.socket.messaging.model.EventCode;
import org.eamrf.eastore.core.socket.messaging.model.ResourceChangeMessage;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Message service for notifying clients that resources have changed on the server.
 * 
 * Messages are broadcasted to /topic/resource/change
 * 
 * @author slenzi
 */
@Service
public class ResourceChangeService {

	@InjectLogger
	private Logger logger;	
	
    @Autowired
    private SimpMessagingTemplate template;
    
    @Autowired
    private TaskManagerProvider taskManagerProvider;    
    
    private final String messageDestination = "/topic/resource/change";
    
    private ExecutorService executorService = null;
    private QueuedTaskManager taskManager = null;
    
    /**
     * A task for the queued task manager which encapsulates the logic for broadcasting a resource change message.
     * 
     * @author slenzi
     *
     */
	private class ResourceChangeTask extends AbstractQueuedTask<Void> {

		private Long nodeId = null;
		private EventCode event = null;
		
		/**
		 * Create a task for broadcasting the change event
		 * 
		 * @param event - The type of event we're broadcasting
		 * @param nodeId - The resource node ID that has changed
		 */
		public ResourceChangeTask(EventCode event, Long nodeId) {
			this.event = event;
			this.nodeId = nodeId;
		}
		
		@Override
		public Void doWork() throws ServiceException {
			
			ResourceChangeMessage mesg = new ResourceChangeMessage();
			mesg.setCode(event.getCodeString());
			mesg.setMessage(EventCode.getCodeMessage(event.getCodeString()));
			mesg.setNodeId(nodeId);
			mesg.setDate(LocalDate.now());
			mesg.setTime(LocalTime.now());
			
			// broadcast the actual message to the clients
			logger.info("Broadcasting directory change event for nodeId = " + nodeId);
			template.convertAndSend(messageDestination, mesg);
			
			return null;
				
		}
		
		/**
		 * Get Id of node that we're broadcasting a change event for
		 * 
		 * @return
		 */
		public Long getNodeId() {
			return this.nodeId;
		}

		/**
		 * Get logger for this task
		 */
		@Override
		public Logger getLogger() {
			return logger;
		}

		/**
		 * Compare this task with other tasks. We use this equality check to
		 * make sure that we don't add a duplicate ResourceChangeTask to our queue.
		 */
		@Override
		public boolean equals(Object otherObject) {
			if(otherObject instanceof ResourceChangeTask) {
				ResourceChangeTask otherTask = (ResourceChangeTask)otherObject;
				logger.info("This node = " + this.nodeId + " and other node = " + otherTask.getNodeId());
				if(this.getNodeId() != null && otherTask.getNodeId() != null && this.getNodeId().equals(otherTask.getNodeId())) {
					// a ResourceChangeTask for the same node...
					return true;
				}
			}else {
				logger.info("Other object not a ResourceChangeTask");
			}
			return false;
		}
		
	};    
	
	public ResourceChangeService() { }
    
	@PostConstruct
	public void init(){
		
		executorService = Executors.newSingleThreadExecutor();
		
		// wait 2 seconds between consuming tasks. Don't want to flood the clients with update messages
		taskManager = taskManagerProvider.createQueuedTaskManager(2000L);
		
		taskManager.startTaskManager(executorService);
		
	}
	
	@PreDestroy
	public void cleanup() {
		
		taskManager.stopTaskManager();
		
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
	public void directoryContentsChanged(Long dirNodeId){
		
		ResourceChangeTask task = createResourceChangeTask(EventCode.DIRECTORY_CONTENTS_CHANGED, dirNodeId);
		
		// Don't want to flood the clients with messages, so only add the task if the task
		// manager doesn't already have a similar task.
		if(!taskManager.contains(task)) {
			taskManager.addTask(task);
		}		
		
	}
	
	/**
	 * Create a new resource change task
	 * 
	 * @param event
	 * @param nodeId
	 * @return
	 */
	private ResourceChangeTask createResourceChangeTask(EventCode event, Long nodeId) {
		
		ResourceChangeTask task = new ResourceChangeTask(event, nodeId);
		task.setName("Broadcast event: " + event.toString() + " [nodeId=" + nodeId + "]");
		
		return task;
		
	}

}
