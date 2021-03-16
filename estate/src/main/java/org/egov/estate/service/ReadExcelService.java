package org.egov.estate.service;

import java.io.File;

import org.egov.estate.model.PropertyResponse;

public interface ReadExcelService {

	public PropertyResponse getDataFromExcel(File file, int sheetIndex);
	public PropertyResponse getDataFromExcelforOwner(File file, int sheetIndex);
	public PropertyResponse getDataFromExcelforPreviousOwner(File file, int sheetIndex);
	public PropertyResponse getDataFromExcelforCourtCase(File file, int sheetIndex);
	public PropertyResponse getDataFromExcelforPayment(File file, int sheetIndex);
	public PropertyResponse getDataFromExcelforBidder(File file, int sheetIndex);
	public PropertyResponse getDataFromExcelforDoc(File file, int sheetIndex);
}
