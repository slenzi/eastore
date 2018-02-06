/**
 * 
 */
package org.eamrf.eastore.core.search.extract;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author slenzi
 *
 */
public interface FileTextExtractor {
	
	/**
	 * True or false where or not the extractor can extract text for the given mime type
	 * 
	 * @param mimeType
	 * @return
	 */
	public boolean canExtract(String mimeType);

	/**
	 * Extract text from the file at the path
	 * 
	 * @param filePath
	 * @return
	 * @throws IOException
	 */
	public String extract(Path filePath) throws IOException;
	
}
