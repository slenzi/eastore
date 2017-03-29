package org.eamrf.eastore.core.config;

import javax.sql.DataSource;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.StringUtil;
import org.eamrf.eastore.core.properties.ManagedProperties;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Configure JDBC datasources
 * 
 * @author slenzi
 * @deprecated - spring boot will automatically create a data source for us.
 */
//@Configuration
public class DataSourceConfig {

	@InjectLogger
	private Logger logger;		
	
    @Autowired
    private ManagedProperties appProps;   
    
	public DataSourceConfig() {
		
	}
	
	/**
	 * Get ecoguser data source
	 * 
	 * @return
	 */
	@Bean(name="ecoguserOracleDataSource")
	@Primary
	public DataSource getEcoguserOracleDataSource(){
		
		logger.info(this.getClass().getName() + ".getEcoguserOracleDataSource() called.");
		
		DataSource dataSource = getOracleEcoguserDatasource();
		
		return dataSource;
		
	}
	
	/**
	 * Create data source for oracle ecoguser
	 * 
	 * @return
	 */
	private DataSource getOracleEcoguserDatasource(){
		
		String dbUrl = appProps.getProperty("jdbc.oracle.ecoguser.url");
		String dbDriver = appProps.getProperty("jdbc.oracle.ecoguser.driver");
		String dbUser = appProps.getProperty("jdbc.oracle.ecoguser.user");
		String dbPassword = appProps.getProperty("jdbc.oracle.ecoguser.password");		
		
		return getDriverManagerDataSource(dbUrl, dbDriver, dbUser, dbPassword);
		
	}	

	/**
	 * Create data source using JDBC connection params
	 * 
	 * @param dbUrl - jdbc url
	 * @param dbDriver - jdbc driver name
	 * @param dbUser - database user
	 * @param dbPassword - database password
	 * @return
	 */
	private DataSource getDriverManagerDataSource(String dbUrl, String dbDriver, String dbUser, String dbPassword){
		
		logger.info("Database driver = " + dbDriver);
		logger.info("Database url = " + dbUrl);
		logger.info("Database user = " + dbUser);
		logger.info("Database password = *******");
		
		if(StringUtil.isNullEmpty(dbDriver) || StringUtil.isNullEmpty(dbUrl) || StringUtil.isNullEmpty(dbUser)){
			
			logger.error("Missing required values for data source. Check driver name, connection url, username, and/or password");
			
			return null;
		}
		
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		
		dataSource.setDriverClassName(dbDriver);
		dataSource.setUrl(dbUrl);
        dataSource.setUsername(dbUser);
        dataSource.setPassword(dbPassword);

        return dataSource;
	}	
	
}
