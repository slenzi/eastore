/**
 * 
 */
package org.eamrf.repository.jdbc.oracle.ecoguser.eastore;

import java.nio.file.Path;
import java.sql.Timestamp;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.core.util.DateUtil;
import org.eamrf.eastore.core.service.tree.file.PathResourceUtil;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.FileMetaResource;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for the download log
 * 
 * @author slenzi
 */
@Repository
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Exception.class)
public class DownloadLogRepository {

    @InjectLogger
    private Logger logger;
    
	// Spring Boot takes care of initializing the JdbcTemplate and DataSource, so we can simply autowire it! Magic!
    @Autowired
    private JdbcTemplate jdbcTemplate;    
	
	/**
	 * 
	 */
	public DownloadLogRepository() {
	
	}
	
	/**
	 * Log the download in the download log table, and return the unique log id which can
	 * be used to locate the log entry.
	 * 
	 * @param file - the file that is being downloaded
	 * @param userId - id of the user that is downloading the file
	 * @return The unique log id for the log entry.
	 * @throws Exception
	 */
	public Long logDownload(FileMetaResource file, String userId) throws Exception {
		
		Long downloadId = getNextDownloadId();
		Timestamp dtNow = DateUtil.getCurrentTime();
		Path filePath = PathResourceUtil.buildPath(file.getStore(), file);
		
		jdbcTemplate.update(
				"insert into eas_download (down_id, file_path, user_id, down_date) values (?, ?, ?, ?)",
				downloadId, filePath.toString(), userId, dtNow
				);
		
		return downloadId;
		
	}
	
	/**
	 * Get next id from eas_download_id_sequence
	 * 
	 * @return
	 * @throws Exception
	 */
	private Long getNextDownloadId() throws Exception {
		
		Long id = jdbcTemplate.queryForObject(
				"select eas_download_id_sequence.nextval from dual", Long.class);
		
		return id;
		
	}	

}
