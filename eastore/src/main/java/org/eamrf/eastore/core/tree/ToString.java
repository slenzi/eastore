package org.eamrf.eastore.core.tree;

/**
 * Functional interface for converting some object to a string
 * 
 * @author sal
 *
 * @param <T>
 */
@FunctionalInterface
public interface ToString<T> {

	public String toString(T t);
	
}
