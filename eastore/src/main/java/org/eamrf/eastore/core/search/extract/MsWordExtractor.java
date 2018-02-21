/**
 * 
 */
package org.eamrf.eastore.core.search.extract;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

/**
 * Extract text from Microsoft Word documents.
 * 
 *	.doc      application/msword
 *	.dot      application/msword
 *	
 *	.docx     application/vnd.openxmlformats-officedocument.wordprocessingml.document
 *	.dotx     application/vnd.openxmlformats-officedocument.wordprocessingml.template
 *	.docm     application/vnd.ms-word.document.macroEnabled.12
 *	.dotm     application/vnd.ms-word.template.macroEnabled.12
 * 
 * @author slenzi
 */
public class MsWordExtractor implements FileTextExtractor {

	/**
	 * 
	 */
	public MsWordExtractor() {

	}
	
	/* (non-Javadoc)
	 * @see org.eamrf.eastore.core.service.search.extract.FileContentExtractor#canExtract(java.lang.String)
	 */
	@Override
	public boolean canExtract(String mimeType) {
		if(mimeType.equals("application/msword") || 
				mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
				mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.template") ||
				mimeType.equals("application/vnd.ms-word.document.macroEnabled.12") ||
				mimeType.equals("application/vnd.ms-word.template.macroEnabled.12")) {
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eamrf.eastore.core.service.search.extract.FileContentExtractor#extract(java.nio.file.Path)
	 */
	@Override
	public String extract(Path filePath) throws IOException {

		String text = null;
		try(BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(filePath))){
			if (FileMagic.valueOf(bis) == FileMagic.OLE2) {
				//System.out.println("MS Document type: OLE2");
				WordExtractor ex = new WordExtractor(bis);
				text = ex.getText();
				ex.close();
			} else if(FileMagic.valueOf(bis) == FileMagic.OOXML) {
				//System.out.println("MS Document type: OOXML");
				XWPFDocument doc = new XWPFDocument(bis);
				XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
				text = extractor.getText();
				extractor.close();
			}
		}
		return text;		
		
	}

}
