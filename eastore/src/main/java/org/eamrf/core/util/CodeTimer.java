/**
 * 
 */
package org.eamrf.core.util;

import java.sql.Timestamp;

/**
 * @author sal
 *
 */
public class CodeTimer {

	private Timestamp startTime = null;
	private Timestamp endTime = null;
	
	public CodeTimer() {}
	
	public void start(){
		startTime = DateUtil.getCurrentTime();
	}
	
	public void stop(){
		endTime = DateUtil.getCurrentTime();
	}
	
	public void reset(){
		startTime = null;
		endTime = null;
	}
	
	public String getElapsedTime(){
		if(startTime != null && endTime != null){
			return DateUtil.formatMillisecondTime(endTime.getTime() - startTime.getTime());
		}
		return "Start time and/or end time were null. Call reset(), then start() and stop() before calling getElapsedTime()";
	}

}
