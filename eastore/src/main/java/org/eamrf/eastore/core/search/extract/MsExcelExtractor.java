/**
 * 
 */
package org.eamrf.eastore.core.search.extract;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.hssf.extractor.ExcelExtractor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.xssf.extractor.XSSFExcelExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Extract text from Microsoft Excel files.
 * 
 *	.xls      application/vnd.ms-excel
 *	.xlt      application/vnd.ms-excel
 *	.xla      application/vnd.ms-excel
 *
 *	.xlsx     application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
 *	.xltx     application/vnd.openxmlformats-officedocument.spreadsheetml.template
 *	.xlsm     application/vnd.ms-excel.sheet.macroEnabled.12
 *	.xltm     application/vnd.ms-excel.template.macroEnabled.12
 *	.xlam     application/vnd.ms-excel.addin.macroEnabled.12
 *	.xlsb     application/vnd.ms-excel.sheet.binary.macroEnabled.12
 * 
 * @author slenzi
 */
public class MsExcelExtractor implements FileTextExtractor {

	/**
	 * 
	 */
	public MsExcelExtractor() {

	}

	/* (non-Javadoc)
	 * @see org.eamrf.eastore.core.service.search.extract.FileContentExtractor#canExtract(java.lang.String)
	 */
	@Override
	public boolean canExtract(String mimeType) {
		if(mimeType.equals("application/vnd.ms-excel") ||
				mimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
				mimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.template") ||
				mimeType.equals("application/vnd.ms-excel.sheet.macroEnabled.12") ||
				mimeType.equals("application/vnd.ms-excel.template.macroEnabled.12") ||
				mimeType.equals("application/vnd.ms-excel.addin.macroEnabled.12") ||
				mimeType.equals("application/vnd.ms-excel.sheet.binary.macroEnabled.12")) {	
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
				//System.out.println("MS Excel type: OLE2");
				HSSFWorkbook book = new HSSFWorkbook(bis);
				ExcelExtractor ex = new ExcelExtractor(book);
				text = ex.getText();
				ex.close();
			} else if(FileMagic.valueOf(bis) == FileMagic.OOXML) {
				//System.out.println("MS Excel type: OOXML");
				XSSFWorkbook book = new XSSFWorkbook(bis);
				XSSFExcelExtractor extractor = new XSSFExcelExtractor(book);
				text = extractor.getText();
				extractor.close();				
			}
		}
		return text;		
		
	}

}
