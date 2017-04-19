package org.eamrf.repository.jdbc;

import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

public abstract class SpringJdbcUtil {

	/**
	 * Use when you want to select a single entity from the database, but you want to ignore
	 * EmptyResultDataAccessException: Incorrect result size: expected 1, actual 0, exceptions.
	 * 
	 * If resultSet.next() returns true (we have some data), then we call the provided row mapper on the first
	 * row of the result set and return our model/entity object. If the result set has no
	 * rows of data, then we simply return null.
	 * 
	 * @param mapper - a row mapper that will be used to map the first row of data in the result set
	 * to your model/entity
	 * @return
	 */
	public static <T> ResultSetExtractor getSingletonExtractor(RowMapper<? extends T> mapper) {
	    return rs -> rs.next() ? mapper.mapRow(rs, 1) : null;
	}
	
}
