/**
 * 
 */
package org.eamrf.eastore.core.search.extract;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.hslf.extractor.PowerPointExtractor;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.xslf.extractor.XSLFPowerPointExtractor;
import org.apache.poi.xslf.usermodel.XMLSlideShow;

/**
 * Extract text from Microsoft Powerpoint files
 * 
 * .ppt      application/vnd.ms-powerpoint
 * .pot      application/vnd.ms-powerpoint
 * .pps      application/vnd.ms-powerpoint
 * .ppa      application/vnd.ms-powerpoint
 * 
 * .pptx     application/vnd.openxmlformats-officedocument.presentationml.presentation
 * .potx     application/vnd.openxmlformats-officedocument.presentationml.template
 * .ppsx     application/vnd.openxmlformats-officedocument.presentationml.slideshow
 * .ppam     application/vnd.ms-powerpoint.addin.macroEnabled.12
 * .pptm     application/vnd.ms-powerpoint.presentation.macroEnabled.12
 * .potm     application/vnd.ms-powerpoint.template.macroEnabled.12
 * .ppsm     application/vnd.ms-powerpoint.slideshow.macroEnabled.12
 * 
 * @author slenzi
 */
public class MsPowerpointExtractor implements FileTextExtractor {

	/**
	 * 
	 */
	public MsPowerpointExtractor() {

	}
	
	/* (non-Javadoc)
	 * @see org.eamrf.eastore.core.service.search.extract.FileContentExtractor#canExtract(java.lang.String)
	 */
	@Override
	public boolean canExtract(String mimeType) {
		if(mimeType.equals("application/vnd.ms-powerpoint") || 
				mimeType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation") ||
				mimeType.equals("application/vnd.openxmlformats-officedocument.presentationml.template") ||
				mimeType.equals("application/vnd.openxmlformats-officedocument.presentationml.slideshow") ||
				mimeType.equals("application/vnd.ms-powerpoint.addin.macroEnabled.12") ||
				mimeType.equals("application/vnd.ms-powerpoint.presentation.macroEnabled.12") ||
				mimeType.equals("application/vnd.ms-powerpoint.template.macroEnabled.12") ||
				mimeType.equals("application/vnd.ms-powerpoint.slideshow.macroEnabled.12")) {
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
				//System.out.println("MS Powerpoint type: OLE2");
				PowerPointExtractor ex = new PowerPointExtractor(bis);
				text = ex.getText();
				ex.close();
			} else if(FileMagic.valueOf(bis) == FileMagic.OOXML) {
				//System.out.println("MS Powerpoint type: OOXML");
				XMLSlideShow show = new XMLSlideShow(bis);
				XSLFPowerPointExtractor extractor = new XSLFPowerPointExtractor(show);
				text = extractor.getText();
				extractor.close();				
			}
		}
		return text;		
		
	}

}
