package org.eamrf.repository.jdbc.oracle.ecoguser.ws;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.StringUtil;
import org.eamrf.eastore.core.properties.ManagedProperties;
import org.eamrf.repository.jdbc.oracle.ecoguser.ws.model.impl.WSService;
import org.eamrf.repository.jdbc.oracle.ecoguser.ws.model.impl.WSUser;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC-based repository for working with WsUser data
 * 
 * @author slenzi
 */
@Repository
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Exception.class)
public class WsUserRepository {

    @InjectLogger
    private Logger logger;
    
	// Spring Boot takes care of initializing the JdbcTemplate and DataSource, so we can simply autowire it! Magic!
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
	@Value( "${database.schema.ws.users}" )
	private String WS_USER_SCHEMA;
	
	/**
	 * Fetch user by login, for a specific service
	 * 
	 * @param login
	 * @param service
	 * @return
	 * @throws Exception
	 */
	public WSUser getUser(String login, WSService service) throws Exception {
		
		String sql =
				"select ws_user_id, ws_login, ws_password, ws_wsname from " + 
						WS_USER_SCHEMA + ".ws_users where ws_login = ? and ws_wsname = ?";
		
		return (WSUser)jdbcTemplate.queryForObject(
				sql, (rs, rowNum) -> {
					WSUser user = new WSUser();
					user.setId(rs.getInt("ws_user_id"));
					user.setLogin(rs.getString("ws_login"));
					user.setWsName(rs.getString("ws_wsname"));
					user.setHashedPassword(rs.getString("ws_password"));
					return user;
				},
				new Object[] { login, service.getServiceName() });
		
	}

}
