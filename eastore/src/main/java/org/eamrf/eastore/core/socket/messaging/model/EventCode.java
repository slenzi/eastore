package org.eamrf.eastore.core.socket.messaging.model;

public enum EventCode {

	DIRECTORY_CONTENTS_CHANGED("DIRECTORY_CONTENTS_CHANGED");
	
	private final String eventCode;
	
    private EventCode(final String eventCode) {
        this.eventCode = eventCode;
    }
    
    public String getCodeString(){
    	return eventCode;
    }
    
    public static EventCode getCode(final String eventCode){
    	if (DIRECTORY_CONTENTS_CHANGED.getCodeString().equals(eventCode)) {
			return DIRECTORY_CONTENTS_CHANGED;
		} else {
			return null;
		}
    }
    
    public static String getCodeMessage(final String eventCode){
    	if (DIRECTORY_CONTENTS_CHANGED.getCodeString().equals(eventCode)) {
			return "Contents of directory changed.";
		} else {
			return "Unknown event code, " + eventCode;
		}
    }

	@Override
	public String toString() {
		return getCodeString();
	}
    
    
	
}
