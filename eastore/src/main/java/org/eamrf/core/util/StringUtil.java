package org.eamrf.core.util;

public abstract class StringUtil {

	/**
	 * If s is null then return s, otherwise return trimmed version of s.
	 * 
	 * @param s
	 * @return
	 */
	public static String trim(String s){
		return s == null ? null : s.trim();
	}
	
	/**
	 * If s is null then return empty string"", otherwise return s
	 * 
	 * @param s
	 * @return
	 */
	public static String changeNull(String s){
		return s == null ? "" : s;
	}
	
	/**
	 * Is string null, or empty string, or all white space.
	 * 
	 * @param s
	 * @return
	 */
	public static boolean isNullEmpty(String s){
		if(s == null){
			return true;
		}
		if(s.trim().equals("")){
			return true;
		}
		return false;
	}

}
