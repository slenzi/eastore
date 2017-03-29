/**
 * 
 */
package org.eamrf.core.util;

import java.sql.Timestamp;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.joda.time.Interval;
import org.joda.time.Period;

/**
 * @author sal
 *
 * Date util functions for dashboard
 */
public abstract class DateUtil {

	/**
	 * If t is null then return t, otherwise format date using provided SimpleDateFormat object
	 * 
	 * @param t
	 * @param dt
	 * @return
	 */
	public static String format(Timestamp t, SimpleDateFormat dt){
		return t == null ? null : dt.format(t);
	}	
	
	/**
	 * Formats the date time to the default yyyy.MM.dd HH:mm:ss.SSS format
	 * e.g., 24-mar-2014 - happy birthday to me!
	 * 
	 * @param time
	 * @return The date in the format yyyy.MM.dd HH:mm:ss.SSS, or empty string if timestamp is null.
	 */
	public static String defaultFormat(Timestamp time){
		if(time == null){
			return "";
		}
		return formatDate(time, "yyyy.MM.dd HH:mm:ss.SSS").toLowerCase();
	}
	
	/**
	 * Formats the date to the default format yyyy.MM.dd HH:mm:ss.SSS
	 * 
	 * @param time
	 * @return The date in the format yyyy.MM.dd HH:mm:ss.SSS, or empty string if date is null.
	 */
	public static String defaultFormat(Date date){
		if(date == null){
			return "";
		}
		return formatDate(date, "yyyy.MM.dd HH:mm:ss.SSS").toLowerCase();
	}
	
	public static String defaultTimeFormat(Date date){
		if(date == null){
			return "";
		}
		return formatDate(date, "HH:mm:ss.SSS").toLowerCase();
	}	
	
	/**
	 * Formats the date to the default format yyyy.MM.dd HH:mm:ss.SSS
	 * 
	 * @param time
	 * @return The date in the format yyyy.MM.dd HH:mm:ss.SSS, or empty string if date is null.
	 */
	public static String defaultDateFormat(Date date){
		if(date == null){
			return "";
		}
		return formatDate(date, "yyyy.MM.dd").toLowerCase();
	}	
	
	   /**
     * <p>Checks if two dates are on the same day ignoring time.</p>
     * @param date1  the first date, not altered, not null
     * @param date2  the second date, not altered, not null
     * @return true if they represent the same day
     * @throws IllegalArgumentException if either date is <code>null</code>
     */
    public static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return isSameDay(cal1, cal2);
    }
    
    /**
     * <p>Checks if two calendars represent the same day ignoring time.</p>
     * @param cal1  the first calendar, not altered, not null
     * @param cal2  the second calendar, not altered, not null
     * @return true if they represent the same day
     * @throws IllegalArgumentException if either calendar is <code>null</code>
     */
    public static boolean isSameDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }
    
    /**
     * <p>Checks if a date is today.</p>
     * @param date the date, not altered, not null.
     * @return true if the date is today.
     * @throws IllegalArgumentException if the date is <code>null</code>
     */
    public static boolean isToday(Date date) {
        return isSameDay(date, Calendar.getInstance().getTime());
    }
    
    /**
     * <p>Checks if a calendar date is today.</p>
     * @param cal  the calendar, not altered, not null
     * @return true if cal date is today
     * @throws IllegalArgumentException if the calendar is <code>null</code>
     */
    public static boolean isToday(Calendar cal) {
        return isSameDay(cal, Calendar.getInstance());
    }
    
    /**
     * <p>Checks if the first date is before the second date ignoring time.</p>
     * @param date1 the first date, not altered, not null
     * @param date2 the second date, not altered, not null
     * @return true if the first date day is before the second date day.
     * @throws IllegalArgumentException if the date is <code>null</code>
     */
    public static boolean isBeforeDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return isBeforeDay(cal1, cal2);
    }
    
    /**
     * <p>Checks if the first calendar date is before the second calendar date ignoring time.</p>
     * @param cal1 the first calendar, not altered, not null.
     * @param cal2 the second calendar, not altered, not null.
     * @return true if cal1 date is before cal2 date ignoring time.
     * @throws IllegalArgumentException if either of the calendars are <code>null</code>
     */
    public static boolean isBeforeDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        if (cal1.get(Calendar.ERA) < cal2.get(Calendar.ERA)) return true;
        if (cal1.get(Calendar.ERA) > cal2.get(Calendar.ERA)) return false;
        if (cal1.get(Calendar.YEAR) < cal2.get(Calendar.YEAR)) return true;
        if (cal1.get(Calendar.YEAR) > cal2.get(Calendar.YEAR)) return false;
        return cal1.get(Calendar.DAY_OF_YEAR) < cal2.get(Calendar.DAY_OF_YEAR);
    }
    
    /**
     * <p>Checks if the first date is after the second date ignoring time.</p>
     * @param date1 the first date, not altered, not null
     * @param date2 the second date, not altered, not null
     * @return true if the first date day is after the second date day.
     * @throws IllegalArgumentException if the date is <code>null</code>
     */
    public static boolean isAfterDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return isAfterDay(cal1, cal2);
    }
    
    /**
     * <p>Checks if the first calendar date is after the second calendar date ignoring time.</p>
     * @param cal1 the first calendar, not altered, not null.
     * @param cal2 the second calendar, not altered, not null.
     * @return true if cal1 date is after cal2 date ignoring time.
     * @throws IllegalArgumentException if either of the calendars are <code>null</code>
     */
    public static boolean isAfterDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        if (cal1.get(Calendar.ERA) < cal2.get(Calendar.ERA)) return false;
        if (cal1.get(Calendar.ERA) > cal2.get(Calendar.ERA)) return true;
        if (cal1.get(Calendar.YEAR) < cal2.get(Calendar.YEAR)) return false;
        if (cal1.get(Calendar.YEAR) > cal2.get(Calendar.YEAR)) return true;
        return cal1.get(Calendar.DAY_OF_YEAR) > cal2.get(Calendar.DAY_OF_YEAR);
    }
    
    /**
     * <p>Checks if a date is after today and within a number of days in the future.</p>
     * @param date the date to check, not altered, not null.
     * @param days the number of days.
     * @return true if the date day is after today and within days in the future .
     * @throws IllegalArgumentException if the date is <code>null</code>
     */
    public static boolean isWithinDaysFuture(Date date, int days) {
        if (date == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return isWithinDaysFuture(cal, days);
    }
    
    /**
     * <p>Checks if a calendar date is after today and within a number of days in the future.</p>
     * @param cal the calendar, not altered, not null
     * @param days the number of days.
     * @return true if the calendar date day is after today and within days in the future .
     * @throws IllegalArgumentException if the calendar is <code>null</code>
     */
    public static boolean isWithinDaysFuture(Calendar cal, int days) {
        if (cal == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        Calendar today = Calendar.getInstance();
        Calendar future = Calendar.getInstance();
        future.add(Calendar.DAY_OF_YEAR, days);
        return (isAfterDay(cal, today) && ! isAfterDay(cal, future));
    }
    
    /** Returns the given date with the time set to the start of the day. */
    public static Date getStart(Date date) {
        return clearTime(date);
    }
    
    /** Returns the given date with the time values cleared. */
    public static Date clearTime(Date date) {
        if (date == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }    

    /** Determines whether or not a date has any time values (hour, minute, 
     * seconds or millisecondsReturns the given date with the time values cleared. */

    /**
     * Determines whether or not a date has any time values.
     * @param date The date.
     * @return true iff the date is not null and any of the date's hour, minute,
     * seconds or millisecond values are greater than zero.
     */
    public static boolean hasTime(Date date) {
        if (date == null) {
            return false;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        if (c.get(Calendar.HOUR_OF_DAY) > 0) {
            return true;
        }
        if (c.get(Calendar.MINUTE) > 0) {
            return true;
        }
        if (c.get(Calendar.SECOND) > 0) {
            return true;
        }
        if (c.get(Calendar.MILLISECOND) > 0) {
            return true;
        }
        return false;
    }

    /** Returns the given date with time set to the end of the day */
    public static Date getEnd(Date date) {
        if (date == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTime();
    }

    /** 
     * Returns the maximum of two dates. A null date is treated as being less
     * than any non-null date. 
     */
    public static Date max(Date d1, Date d2) {
        if (d1 == null && d2 == null) return null;
        if (d1 == null) return d2;
        if (d2 == null) return d1;
        return (d1.after(d2)) ? d1 : d2;
    }
    
    /** 
     * Returns the minimum of two dates. A null date is treated as being greater
     * than any non-null date. 
     */
    public static Date min(Date d1, Date d2) {
        if (d1 == null && d2 == null) return null;
        if (d1 == null) return d2;
        if (d2 == null) return d1;
        return (d1.before(d2)) ? d1 : d2;
    }

    /** The maximum date possible. */
    public static Date MAX_DATE = new Date(Long.MAX_VALUE);	
    
    /**
     * Returns the number of days different between the two dates.
     * 
     * If d1 and d2 are the same day then 0 is returned.
     * If d1 is one or more days after d2 then a positive day count is returned.
     * If d1 is one or more days before d2 then a negative day count is returned.
     * 
     * @param d1
     * @param d2
     * @return
     */
    public static int daysDifferent(Date d1, Date d2){
    	if(isSameDay(d1,d2)){
    		return 0;
    	}else if(isAfterDay(d1, d2)){
    		// d1 comes after d2 by one or more days
    		int diffInDays = (int)( (d1.getTime() - d2.getTime()) / (1000 * 60 * 60 * 24) );
    		return diffInDays;
    	}else{
    		// d1 comes before d2 by one or more days
    		int diffInDays = (int)( (d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24) ) * -1;
    		return diffInDays;    		
    	}
    }
    
    /**
     * Computes difference using Joda Time library.
     * 
     * @param startDate
     * @param endDate
     * @return
     */
    public static String getElapsedString(Date startDate, Date endDate){
    	Interval interval =  new Interval(startDate.getTime(), endDate.getTime());
    	Period period = interval.toPeriod();
    	int years = period.getYears();
    	int months = period.getMonths();
    	int weeks = period.getWeeks();
    	int days = period.getDays();
    	if(years > 0){
    		return years + " " + ((years==1) ? "year" : "years") + ", " +
    				months + " " + ((months==1) ? "month" : "months") + ", " + 
    				weeks + " " + ((weeks==1) ? "week" : "weeks") + ", " + 
    				days + " " + ((days==1) ? "day" : "days");    	
    	}else if(months > 0){
    		return months + " " + ((months==1) ? "month" : "months") + ", " + 
    				weeks + " " + ((weeks==1) ? "week" : "weeks") + ", " + 
    				days + " " + ((days==1) ? "day" : "days");
    	}else if (weeks > 0){
    		return weeks + " " + ((weeks==1) ? "week" : "weeks") + ", " + 
    				days + " " + ((days==1) ? "day" : "days");
    	}else if(days > 0){
    		return days + " " + ((days==1) ? "day" : "days");
    	}else{
    		return "today";
    	}
    }
    
	public static final Timestamp getCurrentTime() {
		Calendar cal = Calendar.getInstance();
		return new Timestamp(cal.getTime().getTime());
	} 
	
	public static String formatDate(Date d, String format){
		if (d != null) {
			SimpleDateFormat sdf = new SimpleDateFormat(format);
			FieldPosition pos = new FieldPosition(0);
			StringBuffer buf = new StringBuffer();
			buf = sdf.format(d,buf,pos);
			return buf.toString();
		}
		return "";
	}
	
	public static String formatTime(long time){

		long days = 0;
		long hours = 0;
		long minutes = 0;
		long seconds = 0;
		long miliseconds = 0;

		long SECONDS_IN_DAY	= 86400;
		long SECONDS_IN_HOUR = 3600;
		long SECONDS_IN_MINUTE = 60;

		long remainder = 0;

		if(time >= SECONDS_IN_DAY){
			days = time / SECONDS_IN_DAY;
			remainder = time % SECONDS_IN_DAY;
			hours = remainder / SECONDS_IN_HOUR;
			remainder = remainder % SECONDS_IN_HOUR;
			minutes = remainder / SECONDS_IN_MINUTE;
			remainder = remainder % SECONDS_IN_MINUTE;
			seconds = remainder;			
		}else if(time >= SECONDS_IN_HOUR){
			hours = time / SECONDS_IN_HOUR;
			remainder = time % SECONDS_IN_HOUR;
			minutes = remainder / SECONDS_IN_MINUTE;
			remainder = remainder % SECONDS_IN_MINUTE;
			seconds = remainder;
		}else if(time >= SECONDS_IN_MINUTE){
			minutes = time / SECONDS_IN_MINUTE;
			remainder = time % SECONDS_IN_MINUTE;
			seconds = remainder;
		}else{
			seconds = time;
		}
		
		String formattedTime = seconds + "s";
		
		if(minutes > 0){
			formattedTime = minutes + "m : " + formattedTime;
		}
		if(hours > 0){
			formattedTime = hours + "h : " + formattedTime;
		}
		if(days > 0){
			formattedTime = days + "d : " + formattedTime;
		}

		return formattedTime;

	}	
	
	public static String formatMillisecondTime(Long time){
		
		long MILLISECONDS_IN_SECOND = 1000;
			
		if(time > MILLISECONDS_IN_SECOND){
			long seconds   = time / MILLISECONDS_IN_SECOND;
			long remainder = time % MILLISECONDS_IN_SECOND;
			return formatTime(seconds) + " " + formatMillisecondTime(remainder);
		}else{
			return time + " ms";
		}
		
	}	

}
