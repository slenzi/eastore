/**
 * 
 */
package org.eamrf.eastore.core.service.file.task;

/**
 * Functional interface for tracking of task completion events
 * 
 * @author slenzi
 */
@FunctionalInterface
public interface TaskCompletionListener<V> {

	public void onComplete(V value);
	
}
