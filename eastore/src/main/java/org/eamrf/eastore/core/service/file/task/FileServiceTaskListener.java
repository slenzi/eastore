/**
 * 
 */
package org.eamrf.eastore.core.service.file.task;

/**
 * @author slenzi
 *
 */
@FunctionalInterface
public interface FileServiceTaskListener {

	public void onProgressChange(FileServiceTask task);
	
}
