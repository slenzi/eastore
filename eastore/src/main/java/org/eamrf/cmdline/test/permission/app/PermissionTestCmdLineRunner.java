/**
 * 
 */
package org.eamrf.cmdline.test.permission.app;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.service.FileSystemService;
import org.eamrf.eastore.core.service.PathResourceTreeService;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.PathResource;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Command line runner for testing eastore/gatekeeper permissions
 * 
 * @author slenzi
 */
@Order(1)
@Component
public class PermissionTestCmdLineRunner implements CommandLineRunner {

    @InjectLogger
    private Logger logger;
    
    @Autowired
    private FileSystemService fileSystemService;
    
    @Autowired
    private PathResourceTreeService pathResourceTreeService;
	
	public PermissionTestCmdLineRunner() {

	}

	/* (non-Javadoc)
	 * @see org.springframework.boot.CommandLineRunner#run(java.lang.String[])
	 */
	@Override
	public void run(String... arg0) throws Exception {
		
		logger.info(PermissionTestCmdLineRunner.class.getName() + " running...");
		
		System.out.println("Here");
		
		PathResource resource = fileSystemService.getPathResource(100L);
		
		logger.info(resource.toString());
		
		logger.info("Done with tests.");
		
		System.exit(1);

	}

}
