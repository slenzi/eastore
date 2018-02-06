/**
 * 
 */
package org.eamrf.eastore.core.search.extract;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;

/**
 * Extract text from Adobe PDF files.
 * 
 * .pdf		application/pdf
 * 
 * @author slenzi
 */
public class PdfExtractor implements FileTextExtractor {

	/**
	 * 
	 */
	public PdfExtractor() {

	}


	
	/* (non-Javadoc)
	 * @see org.eamrf.eastore.core.service.search.extract.FileContentExtractor#canExtract(java.lang.String)
	 */
	@Override
	public boolean canExtract(String mimeType) {
		if(mimeType.equals("application/pdf")) {
			return true;
		}
		return false;
	}



	/* (non-Javadoc)
	 * @see org.eamrf.eastore.core.service.search.extract.FileContentExtractor#extract(java.nio.file.Path)
	 */
	@Override
	public String extract(Path filePath) throws IOException {

		try (PDDocument document = PDDocument.load(filePath.toFile())) {

            if (!document.isEncrypted()) {

                PDFTextStripperByArea stripper = new PDFTextStripperByArea();
                stripper.setSortByPosition(true);

                PDFTextStripper tStripper = new PDFTextStripper();

                String pdfFileInText = tStripper.getText(document);

                /*
				// split by whitespace
                String lines[] = pdfFileInText.split("\\r?\\n");
                for (String line : lines) {
                    System.out.println(line);
                }
                */
                
                return pdfFileInText;

            }			
			
		}
		
		return null;		
		
	}

}
