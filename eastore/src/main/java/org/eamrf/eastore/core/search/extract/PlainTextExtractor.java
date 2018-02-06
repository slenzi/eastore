/**
 * 
 */
package org.eamrf.eastore.core.search.extract;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Extract text from plain text files.
 * 
 * mime types that start with text/, or
 * application/plain
 * application/xml
 * application/****script
 * 
 * @author slenzi
 */
public class PlainTextExtractor implements FileTextExtractor {

	/**
	 * 
	 */
	public PlainTextExtractor() {
		
	}
	
	/* (non-Javadoc)
	 * @see org.eamrf.eastore.core.service.search.extract.FileContentExtractor#canExtract(java.lang.String)
	 */
	@Override
	public boolean canExtract(String mimeType) {

		if(mimeType.startsWith("text/") ||
				mimeType.equals("application/plain") ||
				mimeType.equals("application/xml") ||
				(mimeType.startsWith("application/") && mimeType.endsWith("script")) ) {
			return true;
		}
		return false;
		
	}



	/* (non-Javadoc)
	 * @see org.eamrf.eastore.core.service.search.extract.FileContentExtractor#extract(java.nio.file.Path)
	 */
	@Override
	public String extract(Path filePath) throws IOException {
		try(BufferedReader br = Files.newBufferedReader(filePath)){
			String str = null;
			StringBuffer buffer = new StringBuffer();
			while ((str = br.readLine()) != null) {
				buffer.append(str);
			}
			return buffer.toString();
		}
	}

}
