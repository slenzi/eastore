package org.eamrf.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

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
	 * If c is null return empty list, otherwise return list.
	 * 
	 * @param c
	 * @return
	 */
	public static <T> List<T> emptyListIfNull(List<T> c) {
		
		return c == null ? Collections.<T>emptyList() : c;
		
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
	
	/**
	 * Convert a collection of T to a map of collections of T, grouping T by some attribute.
	 * 
	 * Example usage
	 * 
	 * List<Thing> myList = new ArrayList<Thing>();
	 * 
	 * Map<Integer, Collection<Thing>> myMap = CollectionUtil.convert(myList, (t) -> { 
	 * 	 return t.getSomeAttribute();
	 * }, LinkedList<Thing>::new);
	 * 		
	 * CollectionUtil.printMap(myMap);
	 * 
	 * @param c - the collection to be converted
	 * @param function - a function that acts on elements T to fetch an attribute R to be used as a key in the resulting map.
	 * @param supplier - supplier for supplying a new empty collection of T
	 * @return
	 */
	public static <R, T> Map<R, Collection<T>> convert(Collection<T> c, Function<T, R> function, Supplier<Collection<T>> supplier) {
		if(c == null || c.size() == 0) {
			return null;
		}
		Map<R, Collection<T>> map = new HashMap<R, Collection<T>>();
		for(T t : c) {
			R attr = function.apply(t);
			if(map.containsKey(attr)) {
				map.get(attr).add(t);
			}else {
				Collection<T> newc = supplier.get();
				newc.add(t);
				map.put(attr, newc);
			}
		}
		return map;
	}	
	
	/**
	 * Generic function for printing a map of collections
	 * 
	 * @param map
	 */
	public static <K, V> void printMap(Map<K, Collection<V>> map) {
		for(K k : map.keySet()) {
			System.out.println(k);
			for(V v : map.get(k)) {
				System.out.println(v);
			}
		}
	}	
	
}
