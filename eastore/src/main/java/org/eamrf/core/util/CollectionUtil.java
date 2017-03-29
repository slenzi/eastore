package org.eamrf.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class CollectionUtil {

	/**
	 * Check if collection is empty.
	 * 
	 * @param c
	 * @return
	 */
	public static <T> boolean isEmpty(Collection<T> c){
		return ((c == null || c.size() == 0) ? true : false);
	}
	
	/**
	 * If iterable is null return empty list, otherwise return iterable.
	 * 
	 * e.g.
	 * for (Object object : emptyIfNull(someList)) {
	 * 	 // do something
	 * }
	 * 
	 * @param iterable
	 * @return
	 */
	public static <T> Iterable<T> emptyIfNull(Iterable<T> iterable) {
	    return iterable == null ? Collections.<T>emptyList() : iterable;
	}	

	/**
	 * Split a collection into a list of smaller collections.
	 *  
	 * @param input The collection  to split
	 * @param size The max number of elements in the smaller collection.
	 * @return A list of lists.
	 */
	public static <E extends Object> List<List<E>> split(Collection<E> input, int size) {
	    List<List<E>> master = new ArrayList<List<E>>();
	    if (input != null && input.size() > 0) {
	        List<E> col = new ArrayList<E>(input);
	        boolean done = false;
	        int startIndex = 0;
	        int endIndex = col.size() > size ? size : col.size();
	        while (!done) {
	            master.add(col.subList(startIndex, endIndex));
	            if (endIndex == col.size()) {
	                done = true;
	            }else {
	                startIndex = endIndex;
	                endIndex = col.size() > (endIndex + size) ? (endIndex + size) : col.size();
	            }
	        }
	    }
	    return master;
	}	
	
}
