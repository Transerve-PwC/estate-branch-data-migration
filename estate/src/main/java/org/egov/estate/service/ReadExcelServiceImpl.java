package org.egov.estate.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.compress.archivers.dump.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.Styles;
import org.apache.poi.xssf.model.StylesTable;
import org.egov.estate.entities.AuctionBidder;
import org.egov.estate.entities.CourtCase;
import org.egov.estate.entities.Demand;
import org.egov.estate.entities.Document;
import org.egov.estate.entities.EstateAccount;
import org.egov.estate.entities.Owner;
import org.egov.estate.entities.OwnerDetails;
import org.egov.estate.entities.PaymentConfig;
import org.egov.estate.entities.PaymentConfigItems;
import org.egov.estate.entities.Property;
import org.egov.estate.entities.PropertyDetails;
import org.egov.estate.entities.PropertyDueAmount;
import org.egov.estate.model.PropertyResponse;
import org.egov.estate.repository.PropertyRepository;
import org.egov.estate.service.StreamingSheetContentsHandler.StreamingRowProcessor;
import org.egov.estate.util.FileStoreUtils;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ReadExcelServiceImpl implements ReadExcelService {

	private static final String SYSTEM = "System";
	private static final String TENANTID = "ch.chandigarh";
	private static final String ES_APPROVED = "ES_APPROVED";
	private static final String APPROVE = "APPROVE";
	private static final String ES_DRAFTED = "ES_DRAFTED";
	private static final String PROPERTY_MASTER = "PROPERTY_MASTER";

	@Autowired
	private PropertyRepository propertyRepository;

	@Autowired
	private FileStoreUtils fileStoreUtils;

	@Value("${file.location}")
	private String fileLocation;

	@Override
	public PropertyResponse getDataFromExcel(File file, int sheetIndex) {
		try {
			OPCPackage opcPackage = OPCPackage.open(file);
			return this.process(opcPackage, sheetIndex);
		} catch (IOException | OpenXML4JException | SAXException e) {
			log.error("Error while parsing Excel", e);
			throw new CustomException("PARSE_ERROR", "Could not parse excel. Error is " + e.getMessage());
		}
	}

	@Override
	public PropertyResponse getDataFromExcelforOwner(File file, int sheetIndex) {
		try {
			OPCPackage opcPackage = OPCPackage.open(file);
			return this.processOwner(opcPackage, sheetIndex);
		} catch (IOException | OpenXML4JException | SAXException e) {
			log.error("Error while parsing Excel", e);
			throw new CustomException("PARSE_ERROR", "Could not parse excel. Error is " + e.getMessage());
		}
	}

	@Override
	public PropertyResponse getDataFromExcelforPreviousOwner(File file, int sheetIndex) {
		try {
			OPCPackage opcPackage = OPCPackage.open(file);
			return this.processPreviousOwner(opcPackage, sheetIndex);
		} catch (IOException | OpenXML4JException | SAXException e) {
			log.error("Error while parsing Excel", e);
			throw new CustomException("PARSE_ERROR", "Could not parse excel. Error is " + e.getMessage());
		}
	}

	@Override
	public PropertyResponse getDataFromExcelforCourtCase(File file, int sheetIndex) {
		try {
			OPCPackage opcPackage = OPCPackage.open(file);
			return this.processCourtCase(opcPackage, sheetIndex);
		} catch (IOException | OpenXML4JException | SAXException e) {
			log.error("Error while parsing Excel", e);
			throw new CustomException("PARSE_ERROR", "Could not parse excel. Error is " + e.getMessage());
		}
	}

	@Override
	public PropertyResponse getDataFromExcelforPayment(File file, int sheetIndex) {
		try {
			OPCPackage opcPackage = OPCPackage.open(file);
			return this.processPayment(opcPackage, sheetIndex);
		} catch (IOException | OpenXML4JException | SAXException e) {
			log.error("Error while parsing Excel", e);
			throw new CustomException("PARSE_ERROR", "Could not parse excel. Error is " + e.getMessage());
		}
	}

	@Override
	public PropertyResponse getDataFromExcelforBidder(File file, int sheetIndex) {
		try {
			OPCPackage opcPackage = OPCPackage.open(file);
			return this.processBidder(opcPackage, sheetIndex);
		} catch (IOException | OpenXML4JException | SAXException e) {
			log.error("Error while parsing Excel", e);
			throw new CustomException("PARSE_ERROR", "Could not parse excel. Error is " + e.getMessage());
		}
	}

	@Override
	public PropertyResponse getDataFromExcelforDoc(File file, int sheetIndex) {
		try {
			OPCPackage opcPackage = OPCPackage.open(file);
			return this.processDoc(opcPackage, sheetIndex);
		} catch (IOException | OpenXML4JException | SAXException e) {
			log.error("Error while parsing Excel", e);
			throw new CustomException("PARSE_ERROR", "Could not parse excel. Error is " + e.getMessage());
		}
	}

	private PropertyResponse process(OPCPackage xlsxPackage, int sheetNo)
			throws IOException, OpenXML4JException, SAXException, CustomException {
		ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(xlsxPackage);
		XSSFReader xssfReader = new XSSFReader(xlsxPackage);
		StylesTable styles = xssfReader.getStylesTable();
		XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
		int index = 0;
		while (iter.hasNext()) {
			try (InputStream stream = iter.next()) {

				if (index == sheetNo) {
					SheetContentsProcessor processor = new SheetContentsProcessor();
					processSheet(styles, strings, new StreamingSheetContentsHandler(processor), stream);
					if (!processor.propertyList.isEmpty()) {
						return saveProperties(processor.propertyList, processor.skippedFileNos);
					} else {
						PropertyResponse propertyResponse = PropertyResponse.builder()
								.skippedFileNos(processor.skippedFileNos)
								.build();
						return propertyResponse;
					}
				}
				index++;
			}
		}
		throw new CustomException("PARSE_ERROR", "Could not process sheet no " + sheetNo);
	}

	private PropertyResponse processOwner(OPCPackage xlsxPackage, int sheetNo)
			throws IOException, OpenXML4JException, SAXException, CustomException {
		ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(xlsxPackage);
		XSSFReader xssfReader = new XSSFReader(xlsxPackage);
		StylesTable styles = xssfReader.getStylesTable();
		XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
		int index = 0;
		while (iter.hasNext()) {
			try (InputStream stream = iter.next()) {

				if (index == sheetNo) {
					SheetContentsProcessorOwner processorOwner = new SheetContentsProcessorOwner();
					processSheet(styles, strings, new StreamingSheetContentsHandler(processorOwner), stream);
					if (!processorOwner.propertyList.isEmpty()) {
						PropertyResponse propertyResponse = PropertyResponse.builder()
								.generatedCount(processorOwner.propertyList.size())
								.skippedFileNos(processorOwner.skippedFileNos).build();
						return propertyResponse;
					} else {
						PropertyResponse propertyResponse = PropertyResponse.builder()
								.skippedFileNos(processorOwner.skippedFileNos)
								.build();
						return propertyResponse;
					}
				}
				index++;
			}
		}
		throw new CustomException("PARSE_ERROR", "Could not process sheet no " + sheetNo);
	}

	private PropertyResponse processPreviousOwner(OPCPackage xlsxPackage, int sheetNo)
			throws IOException, OpenXML4JException, SAXException, CustomException {
		ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(xlsxPackage);
		XSSFReader xssfReader = new XSSFReader(xlsxPackage);
		StylesTable styles = xssfReader.getStylesTable();
		XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
		int index = 0;
		while (iter.hasNext()) {
			try (InputStream stream = iter.next()) {

				if (index == sheetNo) {
					SheetContentsProcessorPreviousOwner processorPreviousOwner = new SheetContentsProcessorPreviousOwner();
					processSheet(styles, strings, new StreamingSheetContentsHandler(processorPreviousOwner), stream);
					if (!processorPreviousOwner.propertyList.isEmpty()) {
						PropertyResponse propertyResponse = PropertyResponse.builder()
								.generatedCount(processorPreviousOwner.propertyList.size())
								.skippedFileNos(processorPreviousOwner.skippedFileNos).build();
						return propertyResponse;
					} else {
						PropertyResponse propertyResponse = PropertyResponse.builder()
								.skippedFileNos(processorPreviousOwner.skippedFileNos)
								.build();
						return propertyResponse;
					}
				}
				index++;
			}
		}
		throw new CustomException("PARSE_ERROR", "Could not process sheet no " + sheetNo);
	}

	private PropertyResponse processCourtCase(OPCPackage xlsxPackage, int sheetNo)
			throws IOException, OpenXML4JException, SAXException, CustomException {
		ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(xlsxPackage);
		XSSFReader xssfReader = new XSSFReader(xlsxPackage);
		StylesTable styles = xssfReader.getStylesTable();
		XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
		int index = 0;
		while (iter.hasNext()) {
			try (InputStream stream = iter.next()) {

				if (index == sheetNo) {
					SheetContentsProcessorCourtCase processorCourtCase = new SheetContentsProcessorCourtCase();
					processSheet(styles, strings, new StreamingSheetContentsHandler(processorCourtCase), stream);
					if (!processorCourtCase.propertyList.isEmpty()) {
						PropertyResponse propertyResponse = PropertyResponse.builder()
								.generatedCount(processorCourtCase.propertyList.size())
								.skippedFileNos(processorCourtCase.skippedFileNos).build();
						return propertyResponse;
					} else {
						PropertyResponse propertyResponse = PropertyResponse.builder()
								.skippedFileNos(processorCourtCase.skippedFileNos)
								.build();
						return propertyResponse;
					}
				}
				index++;
			}
		}
		throw new CustomException("PARSE_ERROR", "Could not process sheet no " + sheetNo);
	}

	private PropertyResponse processPayment(OPCPackage xlsxPackage, int sheetNo)
			throws IOException, OpenXML4JException, SAXException, CustomException {
		ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(xlsxPackage);
		XSSFReader xssfReader = new XSSFReader(xlsxPackage);
		StylesTable styles = xssfReader.getStylesTable();
		XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
		int index = 0;
		while (iter.hasNext()) {
			try (InputStream stream = iter.next()) {

				if (index == sheetNo) {
					SheetContentsProcessorPayment processorPayment = new SheetContentsProcessorPayment();
					processSheet(styles, strings, new StreamingSheetContentsHandler(processorPayment), stream);
					if (!processorPayment.propertyList.isEmpty()) {
						PropertyResponse propertyResponse = PropertyResponse.builder()
								.generatedCount(processorPayment.propertyList.size())
								.skippedFileNos(processorPayment.skippedFileNos).build();
						return propertyResponse;
					} else {
						PropertyResponse propertyResponse = PropertyResponse.builder()
								.skippedFileNos(processorPayment.skippedFileNos)
								.build();
						return propertyResponse;
					}
				}
				index++;
			}
		}
		throw new CustomException("PARSE_ERROR", "Could not process sheet no " + sheetNo);
	}

	private PropertyResponse processBidder(OPCPackage xlsxPackage, int sheetNo)
			throws IOException, OpenXML4JException, SAXException, CustomException {
		ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(xlsxPackage);
		XSSFReader xssfReader = new XSSFReader(xlsxPackage);
		StylesTable styles = xssfReader.getStylesTable();
		XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
		int index = 0;
		while (iter.hasNext()) {
			try (InputStream stream = iter.next()) {

				if (index == sheetNo) {
					SheetContentsProcessorBidder processorBidder = new SheetContentsProcessorBidder();
					processSheet(styles, strings, new StreamingSheetContentsHandler(processorBidder), stream);
					if (!processorBidder.propertyList.isEmpty()) {
						PropertyResponse propertyResponse = PropertyResponse.builder()
								.generatedCount(processorBidder.propertyList.size())
								.skippedFileNos(processorBidder.skippedFileNos).build();
						return propertyResponse;
					} else {
						PropertyResponse propertyResponse = PropertyResponse.builder()
								.skippedFileNos(processorBidder.skippedFileNos)
								.build();
						return propertyResponse;
					}
				}
				index++;
			}
		}
		throw new CustomException("PARSE_ERROR", "Could not process sheet no " + sheetNo);
	}

	private PropertyResponse processDoc(OPCPackage xlsxPackage, int sheetNo)
			throws IOException, OpenXML4JException, SAXException, CustomException {
		ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(xlsxPackage);
		XSSFReader xssfReader = new XSSFReader(xlsxPackage);
		StylesTable styles = xssfReader.getStylesTable();
		XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
		int index = 0;
		while (iter.hasNext()) {
			try (InputStream stream = iter.next()) {

				if (index == sheetNo) {
					SheetContentsProcessorDoc processorDoc = new SheetContentsProcessorDoc();
					processSheet(styles, strings, new StreamingSheetContentsHandler(processorDoc), stream);
					if (!processorDoc.propertyList.isEmpty()) {
						PropertyResponse propertyResponse = PropertyResponse.builder()
								.generatedCount(processorDoc.propertyList.size())
								.skippedFileNos(processorDoc.skippedFileNos).build();
						return propertyResponse;
					} else {
						PropertyResponse propertyResponse = PropertyResponse.builder()
								.skippedFileNos(processorDoc.skippedFileNos)
								.build();
						return propertyResponse;
					}
				}
				index++;
			}
		}
		throw new CustomException("PARSE_ERROR", "Could not process sheet no " + sheetNo);
	}

	private void processSheet(Styles styles, SharedStrings strings, SheetContentsHandler sheetHandler,
			InputStream sheetInputStream) throws IOException, SAXException {
		DataFormatter formatter = new DataFormatter();
		InputSource sheetSource = new InputSource(sheetInputStream);
		try {
			SAXParserFactory saxFactory = SAXParserFactory.newInstance();
			saxFactory.setNamespaceAware(false);
			SAXParser saxParser = saxFactory.newSAXParser();
			XMLReader sheetParser = saxParser.getXMLReader();
			ContentHandler handler = new MyXSSFSheetXMLHandler(styles, null, strings, sheetHandler, formatter, false);
			sheetParser.setContentHandler(handler);
			sheetParser.parse(sheetSource);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("SAX parser appears to be broken - " + e.getMessage());
		}
	}

	protected Object getValueFromCell(Row row, int cellNo, Row.MissingCellPolicy cellPolicy) {
		Cell cell1 = row.getCell(cellNo, cellPolicy);
		Object objValue = "";
		switch (cell1.getCellType()) {
		case BLANK:
			objValue = "";
			break;
		case STRING:
			objValue = cell1.getStringCellValue();
			break;
		case NUMERIC:
			try {
				if (DateUtil.isCellDateFormatted(cell1)) {
					objValue = cell1.getDateCellValue().getTime();
				} else {
					throw new InvalidFormatException();
				}
			} catch (Exception ex1) {
				try {
					objValue = cell1.getNumericCellValue();
				} catch (Exception ex2) {
					objValue = 0.0;
				}
			}

			break;
		case FORMULA:
			objValue = cell1.getNumericCellValue();
			break;

		default:
			objValue = "";
		}
		return objValue;
	}

	protected long convertStrDatetoLong(String dateStr) {
		try {
			SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy");
			Date d = f.parse(dateStr);
			return d.getTime();
		} catch (Exception e) {
			log.error("Date parsing issue occur :" + e.getMessage());
		}
		return 0;
	}

	private class SheetContentsProcessor implements StreamingRowProcessor {

		List<Property> propertyList = new ArrayList<>();
		Set<String> skippedFileNos = new HashSet<>();

		@Override
		public void processRow(Row currentRow) {
			if (currentRow.getRowNum() >= 7) {
				if (currentRow.getCell(1) != null) {
					String firstCell = String
							.valueOf(getValueFromCell(currentRow, 1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK))
							.trim();
					if (isNumeric(firstCell)) {
						firstCell = String.valueOf(Double.valueOf(firstCell).intValue());
					}
					Property propertyDb = propertyRepository
								.getPropertyByFileNumber(firstCell);
					
					if (propertyDb == null) {

						int i = 2;
						List<String> excelValues = new ArrayList<>();
						while (i <= 27) {
							excelValues.add(String
									.valueOf(
											getValueFromCell(currentRow, i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK))
									.trim());
							i++;
						}
						if (isNumeric(excelValues.get(5))) {

							Demand demand = Demand.builder().demandDate(convertStrDatetoLong(excelValues.get(25)))
									.build();

							PropertyDueAmount propertyDueAmount = PropertyDueAmount.builder()
									.fileNumber(firstCell)
									.balanceRent(Double.valueOf(excelValues.get(21)))
									.balanceGST(Double.valueOf(excelValues.get(22)))
									.balanceRentPenalty(Double.valueOf(excelValues.get(23)))
									.balanceGSTPenalty(Double.valueOf(excelValues.get(24))).tenantId(TENANTID).build();

							PropertyDetails propertyDetails = PropertyDetails.builder().tenantId(TENANTID)
									.propertyType(excelValues.get(0))
									.branchType("ESTATE_BRANCH")
									.areaSqft(Double.valueOf(excelValues.get(5)).intValue())
									.ratePerSqft(new BigDecimal(excelValues.get(6)))
									.typeOfAllocation(excelValues.get(7)).propertyRegisteredTo(excelValues.get(8))
									.entityType(excelValues.get(9))
									.lastNocDate(convertStrDatetoLong(excelValues.get(10)))
									.serviceCategory(excelValues.get(11)).schemeName(excelValues.get(12))
									.dateOfAuction(convertStrDatetoLong(excelValues.get(13)))
									.modeOfAuction(excelValues.get(14))
									.emdDate(convertStrDatetoLong(excelValues.get(16))).companyName(excelValues.get(17))
									.companyRegistrationNumber(excelValues.get(18))
									.companyRegistrationDate(convertStrDatetoLong(excelValues.get(19)))
									.companyAddress(excelValues.get(20)).demand(demand).build();

							EstateAccount estateAccount = EstateAccount.builder()
									.build();

							if (!excelValues.get(15).isEmpty()) {
								propertyDetails.setEmdAmount(new BigDecimal(excelValues.get(15)));
								estateAccount.setRemainingAmount(propertyDetails.getEmdAmount().doubleValue());
							} else {
								estateAccount.setRemainingAmount(0D);
							}
							estateAccount.setPropertyDetails(propertyDetails);
							estateAccount.setCreatedBy(SYSTEM);
							
							estateAccount.setCreatedTime(System.currentTimeMillis());
							propertyDetails.setEstateAccount(estateAccount);

							Property property = Property.builder().tenantId(TENANTID).action("").state(ES_DRAFTED)
									.propertyMasterOrAllotmentOfSite(PROPERTY_MASTER)
									.fileNumber(firstCell)
									.category(excelValues.get(1)).subCategory(excelValues.get(2))
									.siteNumber(String.valueOf(Math.round(Float.parseFloat(excelValues.get(3)))))
									.sectorNumber(excelValues.get(4))
									.propertyDueAmount(propertyDueAmount)
									.propertyDetails(propertyDetails).build();

							demand.setPropertyDetails(propertyDetails);
							demand.setCreatedBy(SYSTEM);
							propertyDueAmount.setProperty(property);
							propertyDetails.setProperty(property);
							propertyDetails.setCreatedBy(SYSTEM);
							property.setCreatedBy(SYSTEM);
							propertyList.add(property);
						} else {
								skippedFileNos.add(firstCell);
								log.error("We are skipping uploading property for file number: " + firstCell
										+ " because of incorrect data.");

						}
					} else {
							skippedFileNos.add(firstCell);
							log.error("We are skipping uploading property for file number: " + firstCell
									+ " as it already exists.");

						
					}
				}
			}

		}
	}

	private class SheetContentsProcessorOwner implements StreamingRowProcessor {

		Set<String> skippedFileNos = new HashSet<>();
		List<Property> propertyList = new ArrayList<>();

		@Override
		public void processRow(Row currentRow) {
			if (currentRow.getRowNum() >= 7) {
				if (currentRow.getCell(1) != null) {
					String firstCell = String
							.valueOf(getValueFromCell(currentRow, 1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK))
							.trim();
					if (isNumeric(firstCell)) {
						firstCell = String.valueOf(Double.valueOf(firstCell).intValue());
					}
					Property propertyDb = propertyRepository
								.getPropertyByFileNumber(firstCell);
					
					if (propertyDb != null) {
						int i = 2;
						List<String> excelValues = new ArrayList<>();
						while (i <= 13) {
							excelValues.add(String
									.valueOf(
											getValueFromCell(currentRow, i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK))
									.trim());
							i++;
						}
						if (isNumeric(excelValues.get(5).substring(1, excelValues.get(5).length() - 1))
								&& isNumeric(excelValues.get(6))) {
							OwnerDetails ownerDetails = OwnerDetails.builder().tenantId(TENANTID)
									.ownerName(excelValues.get(0)).guardianName(excelValues.get(1))
									.guardianRelation(excelValues.get(2)).dob(convertStrDatetoLong(excelValues.get(3)))
									.address(excelValues.get(4))
									.mobileNumber(excelValues.get(5).substring(1, excelValues.get(5).length() - 1))
									.dateOfAllotment(convertStrDatetoLong(excelValues.get(8)))
									.allotmentNumber(excelValues.get(9))
									.possesionDate(convertStrDatetoLong(excelValues.get(10)))
									.isDirector(new Boolean(excelValues.get(11)))
									.isCurrentOwner(Boolean.valueOf("true")).build();
							Owner owner = Owner.builder().tenantId(TENANTID).share(Double.valueOf(excelValues.get(6)))
									.cpNumber(excelValues.get(7)).build();
							owner.setCreatedBy(SYSTEM);
							ownerDetails.setOwner(owner);
							ownerDetails.setCreatedBy(SYSTEM);
							owner.setOwnerDetails(ownerDetails);
							PropertyDetails propertyDetails = propertyDb.getPropertyDetails();
							Set<Owner> ownerList = new HashSet<>();
							ownerList.add(owner);
							propertyDetails.setOwners(ownerList);
							owner.setPropertyDetails(propertyDetails);
							propertyDb.setPropertyDetails(propertyDetails);
							propertyRepository.save(propertyDb);
							propertyList.add(propertyDb);
						} else {
								skippedFileNos.add(firstCell);
								log.error("We are skipping uploading property for file number: " + firstCell
										+ " because of incorrect data.");
							
						}
					} else {
							skippedFileNos.add(firstCell);
							log.error("We are skipping uploading owner details for property with file number: " + firstCell
									+ " as it does not exists.");

					}
				}
			}

		}

	}

	private class SheetContentsProcessorPreviousOwner implements StreamingRowProcessor {

		Set<String> skippedFileNos = new HashSet<>();
		List<Property> propertyList = new ArrayList<>();

		@Override
		public void processRow(Row currentRow) {
			if (currentRow.getRowNum() >= 7) {
				if (currentRow.getCell(1) != null) {
					String firstCell = String
							.valueOf(getValueFromCell(currentRow, 1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK))
							.trim();
					if (isNumeric(firstCell)) {
						firstCell = String.valueOf(Double.valueOf(firstCell).intValue());
					}
					Property propertyDb = propertyRepository
								.getPropertyByFileNumber(firstCell);
					
					if (propertyDb != null) {
						int i = 2;
						List<String> excelValues = new ArrayList<>();
						while (i <= 13) {
							excelValues.add(String
									.valueOf(
											getValueFromCell(currentRow, i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK))
									.trim());
							i++;
						}
						if (isNumeric(excelValues.get(6).substring(1, excelValues.get(6).length() - 1))
								&& isNumeric(excelValues.get(10))) {
							OwnerDetails ownerDetails = OwnerDetails.builder().tenantId(TENANTID)
									.isPreviousOwnerRequired(Boolean.valueOf(excelValues.get(0)))
									.isCurrentOwner(Boolean.valueOf("false"))
									.ownerName(excelValues.get(1)).guardianName(excelValues.get(2))
									.guardianRelation(excelValues.get(3)).dob(convertStrDatetoLong(excelValues.get(4)))
									.address(excelValues.get(5))
									.mobileNumber(excelValues.get(6).substring(1, excelValues.get(6).length() - 1))
									.sellerName(excelValues.get(7)).sellerGuardianName(excelValues.get(8))
									.sellerRelation(excelValues.get(9)).modeOfTransfer(excelValues.get(11)).build();
							Owner owner = Owner.builder().tenantId(TENANTID).ownerDetails(ownerDetails)
									.share(Double.valueOf(excelValues.get(10))).build();
							owner.setCreatedBy(SYSTEM);
							ownerDetails.setOwner(owner);
							ownerDetails.setCreatedBy(SYSTEM);
							PropertyDetails propertyDetails = propertyDb.getPropertyDetails();
							Set<Owner> ownerList = new HashSet<>();
							ownerList.add(owner);
							propertyDetails.setOwners(ownerList);
							owner.setPropertyDetails(propertyDetails);
							propertyDb.setPropertyDetails(propertyDetails);
							propertyRepository.save(propertyDb);
							propertyList.add(propertyDb);
						} else {
								skippedFileNos.add(firstCell);
								log.error("We are skipping uploading property for file number: " + firstCell
										+ " because of incorrect data.");
							
						}
					} else {
							skippedFileNos.add(firstCell);
							log.error("We are skipping uploading property for file number: " + firstCell
									+ " as it does not exists.");
						
					}
				}
			}
		}

	}

	private class SheetContentsProcessorCourtCase implements StreamingRowProcessor {

		Set<String> skippedFileNos = new HashSet<>();
		List<Property> propertyList = new ArrayList<>();

		@Override
		public void processRow(Row currentRow) {
			if (currentRow.getRowNum() >= 7) {
				if (currentRow.getCell(1) != null) {
					String firstCell = String
							.valueOf(getValueFromCell(currentRow, 1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK))
							.trim();
					if (isNumeric(firstCell)) {
						firstCell = String.valueOf(Double.valueOf(firstCell).intValue());
					}
					Property propertyDb = propertyRepository
								.getPropertyByFileNumber(firstCell);
					
					if (propertyDb != null) {
						int i = 2;
						List<String> excelValues = new ArrayList<>();
						while (i <= 8) {
							excelValues.add(String
									.valueOf(
											getValueFromCell(currentRow, i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK))
									.trim());
							i++;
						}
						CourtCase courtCase = CourtCase.builder().tenantId(TENANTID)
								.estateOfficerCourt(excelValues.get(0)).commissionersCourt(excelValues.get(1))
								.chiefAdministartorsCourt(excelValues.get(2)).advisorToAdminCourt(excelValues.get(3))
								.honorableDistrictCourt(excelValues.get(4)).honorableHighCourt(excelValues.get(5))
								.honorableSupremeCourt(excelValues.get(6))
								.propertyDetails(propertyDb.getPropertyDetails()).build();
						courtCase.setCreatedBy(SYSTEM);
						Set<CourtCase> courtCases = new HashSet<>();
						courtCases.add(courtCase);
						propertyDb.getPropertyDetails().setCourtCases(courtCases);
						propertyRepository.save(propertyDb);
						propertyList.add(propertyDb);
					} else {
							skippedFileNos.add(firstCell);
							log.error("We are skipping uploading property for file number: " + firstCell
									+ " as it does not exists.");
						
					}
				}
			}

		}

	}

	private class SheetContentsProcessorPayment implements StreamingRowProcessor {

		Set<String> skippedFileNos = new HashSet<>();
		List<Property> propertyList = new ArrayList<>();

		@Override
		public void processRow(Row currentRow) {
			if (currentRow.getRowNum() >= 7) {
				if (currentRow.getCell(1) != null) {
					String firstCell = String
							.valueOf(getValueFromCell(currentRow, 1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK))
							.trim();
					if (isNumeric(firstCell)) {
						firstCell = String.valueOf(Double.valueOf(firstCell).intValue());
					}
					Property propertyDb = propertyRepository
								.getPropertyByFileNumber(firstCell);
					
					if (propertyDb != null) {
						int i = 2;
						List<String> excelValues = new ArrayList<>();
						while (i <= 18) {
							excelValues.add(String
									.valueOf(
											getValueFromCell(currentRow, i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK))
									.trim());
							i++;
						}
						if (excelValues.get(0).equalsIgnoreCase("true")) {
							if (isNumeric(excelValues.get(3)) && isNumeric(excelValues.get(4))
									&& isNumeric(excelValues.get(6)) && isNumeric(excelValues.get(14))
									&& isNumeric(excelValues.get(15))) {

								PaymentConfigItems paymentConfigItems = PaymentConfigItems.builder().tenantId(TENANTID)
										.groundRentAmount(new BigDecimal(excelValues.get(4)))
										.groundRentEndMonth(Long.valueOf(
												excelValues.get(6).substring(0, excelValues.get(6).length() - 2)))
										.build();
								if (!excelValues.get(5).isEmpty()) {
									paymentConfigItems.setGroundRentStartMonth(Long
											.valueOf(excelValues.get(5).substring(0, excelValues.get(5).length() - 2)));
								}
								Set<PaymentConfigItems> paymentConfigItemsList = new HashSet<>();

								PaymentConfig paymentConfig = PaymentConfig.builder().tenantId(TENANTID)
										.isGroundRent(Boolean.valueOf(excelValues.get(0)))
										.groundRentGenerationType(excelValues.get(1))
										.groundRentBillStartDate(convertStrDatetoLong(excelValues.get(2)))
										.groundRentGenerateDemand(Long.valueOf(
												excelValues.get(3).substring(0, excelValues.get(3).length() - 2)))
										.isIntrestApplicable(Boolean.valueOf(excelValues.get(13)))
										.rateOfInterest(new BigDecimal(excelValues.get(14)))
										.noOfMonths(Long.valueOf(
												excelValues.get(15).substring(0, excelValues.get(15).length() - 2)))
										.dueDateOfPayment(convertStrDatetoLong(excelValues.get(16)))
										.propertyDetails(propertyDb.getPropertyDetails()).build();
								paymentConfig.setSecurityAmount(paymentConfigItems.getGroundRentAmount().multiply(BigDecimal.valueOf(paymentConfig.getNoOfMonths())));
								paymentConfig.setCreatedBy(SYSTEM);
								paymentConfigItems.setPaymentConfig(paymentConfig);
								paymentConfigItemsList.add(paymentConfigItems);
								paymentConfig.setPaymentConfigItems(paymentConfigItemsList);
								propertyDb.getPropertyDetails().setPaymentConfig(paymentConfig);
								propertyRepository.save(propertyDb);
								propertyList.add(propertyDb);
							} else {
									skippedFileNos.add(firstCell);
									log.error("We are skipping uploading property for file number: " + firstCell
											+ " because of incorrect data.");
								
							}
						} else if(excelValues.get(0).equalsIgnoreCase("false")) {
							if (isNumeric(excelValues.get(9)) && isNumeric(excelValues.get(10))
									&& isNumeric(excelValues.get(12)) && isNumeric(excelValues.get(14))
									&& isNumeric(excelValues.get(15))) {
								PaymentConfigItems paymentConfigItems = PaymentConfigItems.builder().tenantId(TENANTID)
										.groundRentAmount(new BigDecimal(excelValues.get(10)))
										.groundRentEndMonth(Long.valueOf(
												excelValues.get(12).substring(0, excelValues.get(12).length() - 2)))
										.build();
								if (!excelValues.get(11).isEmpty()) {
									paymentConfigItems.setGroundRentStartMonth(Long.valueOf(
											excelValues.get(11).substring(0, excelValues.get(11).length() - 2)));
								}
								Set<PaymentConfigItems> paymentConfigItemsList = new HashSet<>();
								paymentConfigItemsList.add(paymentConfigItems);
								PaymentConfig paymentConfig = PaymentConfig.builder().tenantId(TENANTID)
										.isGroundRent(Boolean.valueOf(excelValues.get(0)))
										.groundRentGenerationType(excelValues.get(7))
										.groundRentBillStartDate(convertStrDatetoLong(excelValues.get(8)))
										.groundRentGenerateDemand(Long.valueOf(
												excelValues.get(9).substring(0, excelValues.get(9).length() - 2)))
										.isIntrestApplicable(Boolean.valueOf(excelValues.get(13)))
										.rateOfInterest(new BigDecimal(excelValues.get(14)))
										.noOfMonths(Long.valueOf(
												excelValues.get(15).substring(0, excelValues.get(15).length() - 2)))
										.dueDateOfPayment(convertStrDatetoLong(excelValues.get(16)))
										.propertyDetails(propertyDb.getPropertyDetails()).build();
								paymentConfig.setSecurityAmount(paymentConfigItems.getGroundRentAmount().multiply(BigDecimal.valueOf(paymentConfig.getNoOfMonths())));
								paymentConfig.setCreatedBy(SYSTEM);
								paymentConfigItems.setPaymentConfig(paymentConfig);
								paymentConfigItemsList.add(paymentConfigItems);
								paymentConfig.setPaymentConfigItems(paymentConfigItemsList);
								propertyDb.getPropertyDetails().setPaymentConfig(paymentConfig);
								propertyRepository.save(propertyDb);
								propertyList.add(propertyDb);
							} else {
									skippedFileNos.add(firstCell);
									log.error("We are skipping uploading property for file number: " + firstCell
											+ " because of incorrect data.");
								
							}
						} else if(excelValues.get(0).equalsIgnoreCase("N/A")) {
							PaymentConfig paymentConfig = PaymentConfig.builder().tenantId(TENANTID)
									.isGroundRent(Boolean.valueOf(excelValues.get(0)))
									.build();
							System.out.println(paymentConfig.getIsGroundRent());
						}

					} else {
							skippedFileNos.add(firstCell);
							log.error("We are skipping uploading property for file number: " + firstCell
									+ " as it does not exists.");
						
					}
				}
			}
		}
	}

	private class SheetContentsProcessorBidder implements StreamingRowProcessor {

		Set<String> skippedFileNos = new HashSet<>();
		List<Property> propertyList = new ArrayList<>();

		@Override
		public void processRow(Row currentRow) {
			if (currentRow.getRowNum() >= 7) {
				if (currentRow.getCell(1) != null) {
					String firstCell = String
							.valueOf(getValueFromCell(currentRow, 1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK))
							.trim();
					if (isNumeric(firstCell)) {
						firstCell = String.valueOf(Double.valueOf(firstCell).intValue());
					}
					Property propertyDb = propertyRepository
								.getPropertyByFileNumber(firstCell);
					
					if (propertyDb != null) {
						int i = 2;
						List<String> excelValues = new ArrayList<>();
						while (i <= 6) {
							excelValues.add(String
									.valueOf(
											getValueFromCell(currentRow, i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK))
									.trim());
							i++;
						}
						if (isNumeric(excelValues.get(2))) {
							AuctionBidder auctionBidder = AuctionBidder.builder().auctionId(excelValues.get(0))
									.state(ES_APPROVED)
									.action(APPROVE)
									.bidderName(excelValues.get(1))
									.depositedEMDAmount(new BigDecimal(excelValues.get(2)))
									.depositDate(convertStrDatetoLong(excelValues.get(3)))
									.emdValidityDate(convertStrDatetoLong(excelValues.get(4))).refundStatus("").build();
							auctionBidder.setCreatedBy(SYSTEM);
							auctionBidder.setLastModifiedBy(SYSTEM);
							auctionBidder.setPropertyDetails(propertyDb.getPropertyDetails());
							Set<AuctionBidder> bidders = new HashSet<>();
							bidders.add(auctionBidder);
							propertyDb.getPropertyDetails().setBidders(bidders);
							propertyRepository.save(propertyDb);
							propertyList.add(propertyDb);
						} else {
								skippedFileNos.add(firstCell);
								log.error("We are skipping uploading property for file number: " + firstCell
										+ " because of incorrect data.");
							
						}
					} else {
							skippedFileNos.add(firstCell);
							log.error("We are skipping uploading property for file number: " + firstCell
									+ " as it does not exists.");
						
					}
				}
			}
		}
	}

	private class SheetContentsProcessorDoc implements StreamingRowProcessor {

		Set<String> skippedFileNos = new HashSet<>();
		List<Property> propertyList = new ArrayList<>();

		@Override
		public void processRow(Row currentRow) {
			File folder = new File(fileLocation);
			String[] listOfFiles = folder.list();
			List<String> filesList = Arrays.asList(listOfFiles);

			if (!filesList.isEmpty()) {
			if (currentRow.getRowNum() >= 2) {
				if (currentRow.getCell(0) != null) {
					String firstCell = String
							.valueOf(getValueFromCell(currentRow, 0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK))
							.trim();
					String documentName = String
							.valueOf(getValueFromCell(currentRow, 3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)).trim();
					if (isNumeric(firstCell)) {
						firstCell = String.valueOf(Double.valueOf(firstCell).intValue());
					}
					if (filesList.contains(documentName)) {
						
						Property propertyDb = propertyRepository
									.getPropertyByFileNumber(firstCell);
						
					if (propertyDb != null) {
						byte[] bytes = null;
						List<HashMap<String, String>> response = null;
						try {
							bytes = Files.readAllBytes(Paths.get(folder + "/" + documentName));
							ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
							outputStream.write(bytes);
							String [] tenantId = propertyDb.getTenantId().split("\\.");
							response = fileStoreUtils.uploadStreamToFileStore(outputStream, tenantId[0],
									documentName);
							outputStream.close();
						} catch (IOException e) {
							log.error("error while converting file into byte output stream");
						}
						
						String documentType = String
								.valueOf(getValueFromCell(currentRow, 1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)).trim();
						String documentFor = String
								.valueOf(getValueFromCell(currentRow, 4, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)).trim();
						String num = "";
						for (int i = 0; i < documentFor.length(); i++) {
							if (Character.isDigit(documentFor.charAt(i))) {
								num = num + documentFor.charAt(i);
							}
						}
						String docType = "";
						if (documentType.toUpperCase().contains("Certified copy of lease".toUpperCase())) {
							docType = "CERTIFIED_COPY_LEASE";
						} else if (documentType.toUpperCase().contains("Notarized copy of sale deed".toUpperCase())) {
							docType = "NOTARIZED_COPY_DEED";
						} else if (documentType.toUpperCase().contains("intending Transferee(s)/applicant(s)".toUpperCase())) {
							docType = "SELF_ATTESTED_PHOTO_IDENTITY_PROOF";
						} else if (documentType.toUpperCase().contains("Indemnity bond of transferee".toUpperCase())) {
							docType = "INDEMNITY_BOND_TRANSFEREE";
						} else if (documentType.toUpperCase().contains("witnesses of Indemnity Bond".toUpperCase())) {
							docType = "SELF_ATTESTED_PHOTO_IDENTITY_PROOF_WITNESSES_INDEMITY_BOND";
						} else if (documentType.toUpperCase().contains("Clearance of previous mortgage".toUpperCase())) {
							docType = "CLEARANCE_PREVIOUS_MORTGAGE";
						} else if (documentType.toUpperCase().contains("Sewerage connection".toUpperCase())) {
							docType = "SEWERAGE_CONNECTION";
						} else if (documentType.toUpperCase().contains("Furnish proof of construction".toUpperCase())) {
							docType = "PROOF_OF_CONSTRUCTION";
						} else if (documentType.toUpperCase().contains("Indemnity Bond duly attested by Notary Public".toUpperCase())) {
							docType = "INDEMNITY_BOND";
						} else if (documentType.toUpperCase().contains("Notarized copy of GPA/SPA".toUpperCase())) {
							docType = "NOTARIZED_COPY_GPA_SPA";
						} else if (documentType.toUpperCase().contains("Affidavit regarding validity of GPA/SPA".toUpperCase())) {
							docType = "AFFIDAVIT_VALIDITY_GPA_SPA";
						} else if (documentType.toUpperCase().contains("Affidavit to the effect".toUpperCase())) {
							docType = "AFFIDAVIT_EFFECT";
						} else if (documentType.toUpperCase().contains("Attested copy of partnership deed".toUpperCase())) {
							docType = "ATTESTED_COPY_PARTNERSHIP_DEED";
						} else if (documentType.toUpperCase().contains("Copy of Memorandum".toUpperCase())) {
							docType = "COPY_OF_MEMORANDUM";
						} else if (documentType.toUpperCase().contains("No due Certificate of property tax".toUpperCase())) {
							docType = "NO_DUE_CERTIFICATE";
						} else if(documentType.equalsIgnoreCase("document a")) {
							docType = "DOCUMENT_A";
						} else if(documentType.equalsIgnoreCase("document b")) {
							docType = "DOCUMENT_B";
						} else if(documentType.equalsIgnoreCase("document c")) {
							docType = "DOCUMENT_C";
						} else if(documentType.equalsIgnoreCase("document d")) {
							docType = "DOCUMENT_D";
						}
						else if(documentFor.equalsIgnoreCase("payment")) {
							docType = documentType;
						}
						Document document = Document.builder()
								.tenantId(propertyDb.getTenantId()).active(true).documentType(docType)
								.fileStoreId(response.get(0).get("fileStoreId")).build();
						List<Owner> ownerList = null;
						if (documentFor.equalsIgnoreCase("Owner"+num)) {
							 ownerList = propertyDb
									.getPropertyDetails().getOwners().stream().filter(owner -> owner.getOwnerDetails()
											.getIsCurrentOwner().toString().equalsIgnoreCase("true"))
									.collect(Collectors.toList());
							Comparator<Owner> compare = (o1, o2) -> o1.getOwnerDetails().getCreatedTime()
									.compareTo(o2.getOwnerDetails().getCreatedTime());
							Collections.sort(ownerList, compare);
							if(!ownerList.isEmpty()) {
							document.setReferenceId(ownerList.get(Integer.parseInt(num) - 1).getOwnerDetails().getId());
							document.setCreatedBy(SYSTEM);
							document.setProperty(propertyDb);
							Set<Document> documents = new HashSet<>();
							documents.add(document);
							propertyDb.setDocuments(documents);
							propertyRepository.save(propertyDb);
							propertyList.add(propertyDb);

							} else {
									skippedFileNos.add(firstCell);
									log.error("We are skipping uploading document for the property having file number: " + firstCell
											+ " as it does not have owner.");
								
							}
						} else if(documentFor.equalsIgnoreCase("payment")) {
							document.setReferenceId(propertyDb.getPropertyDetails().getId());
							document.setCreatedBy(SYSTEM);
							document.setProperty(propertyDb);
							Set<Document> documents = new HashSet<>();
							documents.add(document);
							propertyDb.setDocuments(documents);
							propertyRepository.save(propertyDb);
							propertyList.add(propertyDb);

						} else if (documentFor.equalsIgnoreCase("Previous Owner"+num)) {
							 ownerList = propertyDb
									.getPropertyDetails().getOwners().stream().filter(owner -> owner.getOwnerDetails()
											.getIsCurrentOwner().toString().equalsIgnoreCase("false"))
									.collect(Collectors.toList());
							Comparator<Owner> compare = (o1, o2) -> o1.getOwnerDetails().getCreatedTime()
									.compareTo(o2.getOwnerDetails().getCreatedTime());
							Collections.sort(ownerList, compare);
							if(!ownerList.isEmpty()) {
							document.setReferenceId(ownerList.get(Integer.parseInt(num) - 1).getOwnerDetails().getId());
							document.setCreatedBy(SYSTEM);
							document.setProperty(propertyDb);
							Set<Document> documents = new HashSet<>();
							documents.add(document);
							propertyDb.setDocuments(documents);
							propertyRepository.save(propertyDb);
							propertyList.add(propertyDb);

							} else {
									skippedFileNos.add(firstCell);
									log.error("We are skipping uploading document for the property having file number: " + firstCell
											+ " as it does not have previous owner.");
								
							}
							}
					} else {
							skippedFileNos.add(firstCell);
							log.error("We are skipping uploading property for file number: " + firstCell
									+ " as it does not exists.");
						
					}
					} else {
						skippedFileNos.add(firstCell);
						log.error("No Document with name "+ documentName + " is present in the document folder");
					}
				}
				}
			} else {
				throw new CustomException("NO_FILES_PRESENT", "No files present in document folder");
			}
		}

	}

	private PropertyResponse saveProperties(List<Property> properties, Set<String> skippedFileNos) {
		properties.forEach(property -> {
			propertyRepository.save(property);
		});
		PropertyResponse propertyResponse = PropertyResponse.builder().generatedCount(properties.size())
				.skippedFileNos(skippedFileNos).build();
		return propertyResponse;
	}

	private Boolean isNumeric(String value) {
		if (value != null && !value.matches("[1-9][0-9]*(\\.[0-9]*)?")) {
			return false;
		}
		return true;
	}

}
