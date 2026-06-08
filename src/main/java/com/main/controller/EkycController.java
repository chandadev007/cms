package com.main.controller;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.main.model.Ekyb;
import com.main.model.Ekyc;
import com.main.model.History;
import com.main.model.Pagination;
import com.main.model.ReportFull;
import com.main.model.TemplateResponse;
import com.main.model.TemplateResponseWithPagination;
import com.main.service.DBLogService;
import com.main.service.EkybService;
import com.main.service.EkycService;
import com.main.service.FileManagementService;

public class EkycController {

    private static final Logger logger = LoggerFactory.getLogger(EkycController.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EkycService ekycService;
    private final EkybService ekybService;
    private final DBLogService dbLogService;
    private final FileManagementService fileManagementService;

    public EkycController(EkycService ekycService, EkybService ekybService, DBLogService dbLogService,
            FileManagementService fileManagementService) {
        this.ekycService = ekycService;
        this.ekybService = ekybService;
        this.dbLogService = dbLogService;
        this.fileManagementService = fileManagementService;
    }

    // cms_101: ekyc create
    public ResponseEntity<?> cms_101(JsonNode data, MultipartFile attachment, String unitCode, String userId,
            String userName) {
        try {
            String idNumber = data.path("idNumber").asText();
            String firstNameKh = data.path("firstNameKh").asText();
            String lastNameKh = data.path("lastNameKh").asText();
            String firstNameEn = data.path("firstNameEn").asText();
            String lastNameEn = data.path("lastNameEn").asText();
            String gender = data.path("gender").asText();
            String dob = data.path("dob").asText();
            String issuedDate = data.path("issuedDate").asText();
            String expiredDate = data.path("expiredDate").asText();
            String note = data.path("note").asText();
            String type = data.path("type").asText();

            // validation field required
            if (idNumber == null || idNumber.isEmpty())
                return buildErrorResponse(HttpStatus.OK, "Id card number is required to fill");
            if (note == null || note.isEmpty())
                return buildErrorResponse(HttpStatus.OK, "Purpose/note is required to fill");
            if (type == null || type.isEmpty())
                return buildErrorResponse(HttpStatus.OK, "Verify Type is requird to select");
            if ((firstNameKh == null || firstNameKh.isEmpty()) &&
                    (lastNameKh == null || lastNameKh.isEmpty()) &&
                    (firstNameEn == null || firstNameEn.isEmpty()) &&
                    (lastNameEn == null || lastNameEn.isEmpty()) &&
                    (gender == null || gender.isEmpty()) &&
                    (dob == null || dob.isEmpty()) &&
                    (issuedDate == null || issuedDate.isEmpty()) &&
                    (expiredDate == null || expiredDate.isEmpty()))
                return buildErrorResponse(HttpStatus.OK, "Do not allow empty all fields (At leave fill one fill)");

            // check exist customer with basic information
            int isExist = ekycService.checkEkycExisting(idNumber, firstNameKh, lastNameKh, firstNameEn, lastNameEn,
                    gender, dob, issuedDate, expiredDate, type);
            if (isExist > 0) {
                return buildErrorResponse(HttpStatus.OK,
                        "Customer already exist");
            }

            // get selfie path
            String selfiePath = "";
            if ("2".equalsIgnoreCase(type)) {
                if (attachment == null || attachment.isEmpty())
                    return buildErrorResponse(HttpStatus.OK, "Verify User with face is required photo upload");

                try {
                    String fileResult = fileManagementService.uploadFile(attachment, "selfie");
                    JsonNode fileResulJsonNode = objectMapper.readTree(fileResult);

                    String fileErrorCode = fileResulJsonNode.path("errorCode").asText();
                    if (fileErrorCode == null || !"00".equalsIgnoreCase(fileErrorCode)
                            || "0".equalsIgnoreCase(fileErrorCode)) {
                        return buildErrorResponse(HttpStatus.OK,
                                fileResulJsonNode.path("errorDetail").asText());
                    }

                    JsonNode fileData = fileResulJsonNode.path("data");
                    ArrayNode fileDatas = (ArrayNode) fileData;

                    for (JsonNode temp : fileDatas) {
                        if (selfiePath == null || selfiePath.isEmpty()) {
                            selfiePath = temp.path("filePath").asText();
                        }
                    }
                } catch (Exception e) {
                    return buildErrorResponse(HttpStatus.OK, "file management gateway timeout");
                }
            }

            // create ekyc
            String ekycResult = ekycService.createEkyc(idNumber, firstNameKh, lastNameKh,
                    firstNameEn, lastNameEn, gender, dob, issuedDate, expiredDate, note, type, selfiePath,
                    "Web Protal");

            JsonNode ekycResultJson = objectMapper.readTree(ekycResult);
            String pResult = ekycResultJson.path("p_result").asText();
            String outId = ekycResultJson.path("out_id").asText();

            if (pResult == null || !"success".equalsIgnoreCase(pResult) || outId == null
                    || outId.isEmpty()) {
                return buildErrorResponse(HttpStatus.OK, "The process failed to execute: " +
                        pResult);
            }

            // log process
            dbLogService.createEkycLog("Create", "USER", outId, unitCode, userId,
                    userName, "Web Protal");

            // process executed successfully
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "0", "errorDetail", "Process executed successfully"));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error Internal Server");
        }
    }

    // cms_102: Ekyc page
    public ResponseEntity<?> cms_102(JsonNode data) {
        try {
            int size = data.has("pageSize") ? Integer.parseInt(data.path("pageSize").asText()) : 10;
            int page = data.has("pageIndex") ? Integer.parseInt(data.path("pageIndex").asText()) : 1;
            String searchValue = data.path("searchValue").asText();

            if (searchValue == null || searchValue.isEmpty()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("error", "0", "errorDetail", ""));
            }

            List<Ekyc> ekycs = ekycService.getEkycPage(size, page, searchValue);
            Pagination pagination = new Pagination();
            int totalElement = ekycService.getEkycPageCount(searchValue);
            int totalPages = totalElement / size;

            if (totalElement % size > 0)
                totalPages++;

            pagination.setTotalElements(totalElement);
            pagination.setPageIndex(page);
            pagination.setPageSize(size);
            pagination.setFirstPage(1);
            pagination.setLastPage(totalPages);
            pagination.setTotalPages(totalPages);

            TemplateResponseWithPagination<List<Ekyc>> response = new TemplateResponseWithPagination<List<Ekyc>>();
            response.setError("0");
            response.setData(ekycs);
            response.setPageable(pagination);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error Internal Server");
        }
    }

    // cmd_03: Ekyc detail
    public ResponseEntity<?> cms_103(JsonNode data) {
        try {
            String id = data.path("id").asText();
            if (id == null || id.isEmpty()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("error", "1", "errorDetail", "The Id not provided"));
            }

            Ekyc ekyc = ekycService.getEkycById(id);
            if (ekyc == null) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("error", "1", "errorDetail", "Customer not exsit"));
            }

            TemplateResponse<Ekyc> response = new TemplateResponse<Ekyc>();
            response.setError("0");
            response.setData(ekyc);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error Internal Server");
        }
    }

    // cms_104: My history
    public ResponseEntity<?> cms_104(JsonNode data, String userId) {
        try {

            int size = data.has("pageSize") ? Integer.parseInt(data.path("pageSize").asText()) : 10;
            int page = data.has("pageIndex") ? Integer.parseInt(data.path("pageIndex").asText()) : 1;
            String searchValue = data.path("searchValue").asText();
            String requestType = data.path("requestType").asText();
            String statusDesc = data.path("statusDesc").asText();
            String fromDate = data.path("fromDate").asText();
            String toDate = data.path("toDate").asText();

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            if (fromDate != null && !fromDate.isEmpty()) {
                fromDate = new SimpleDateFormat("yyyyMMddHHmm").format(dateFormat.parse(fromDate + " 00:00"));
            }
            if (toDate != null && !toDate.isBlank()) {
                toDate = new SimpleDateFormat("yyyyMMddHHmm").format(dateFormat.parse(toDate + " 23:59"));
            }

            List<History> histories = ekycService.getHistories(size, page, searchValue, requestType, statusDesc,
                    fromDate, toDate, userId);

            Pagination pagination = new Pagination();
            int totalElement = ekycService.getHistoriesCount(searchValue, requestType, statusDesc, fromDate, toDate,
                    userId);
            int totalPages = totalElement / size;

            if (totalElement % size > 0)
                totalPages++;

            pagination.setTotalElements(totalElement);
            pagination.setPageIndex(page);
            pagination.setPageSize(size);
            pagination.setFirstPage(1);
            pagination.setLastPage(totalPages);
            pagination.setTotalPages(totalPages);

            TemplateResponseWithPagination<List<History>> response = new TemplateResponseWithPagination<List<History>>();
            response.setError("0");
            response.setData(histories);
            response.setPageable(pagination);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error Internal Server");
        }
    }

    // cms_105: History detail
    public ResponseEntity<?> cms_105(JsonNode data) {
        try {
            String id = data.path("id").asText();
            String requestType = data.path("requestType").asText();

            if ("eKYC".equalsIgnoreCase(requestType)) {
                List<Ekyc> ekycs = ekycService.getHistoryById(id);

                if (ekycs.size() <= 0)
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(Map.of("error", "1", "errorDetail", "Customer not exsit"));

                Ekyc ekyc = new Ekyc();
                for (int i = 0; i < ekycs.size(); i++) {

                    ekyc.setId(ekycs.get(i).getId());
                    ekyc.setAppCode(ekycs.get(i).getAppCode());
                    ekyc.setAppChannel(ekycs.get(i).getAppChannel());
                    ekyc.setIdNumber(ekycs.get(i).getIdNumber());
                    ekyc.setFirstNameKh(ekycs.get(i).getFirstNameKh());
                    ekyc.setLastNameKh(ekycs.get(i).getLastNameKh());
                    ekyc.setFirstNameEn(ekycs.get(i).getFirstNameEn());
                    ekyc.setLastNameEn(ekycs.get(i).getLastNameEn());
                    ekyc.setGender(ekycs.get(i).getGender());
                    ekyc.setDob(ekycs.get(i).getDob());
                    ekyc.setIssuedDate(ekycs.get(i).getIssuedDate());
                    ekyc.setExpiredDate(ekycs.get(i).getExpiredDate());
                    ekyc.setScore(ekycs.get(i).getScore());
                    ekyc.setFaceScore(ekycs.get(i).getFaceScore());
                    ekyc.setType(ekycs.get(i).getType());
                    ekyc.setNote(ekycs.get(i).getNote());
                    ekyc.setSelfiePath(ekycs.get(i).getSelfiePath());
                    ekyc.setErrorDetail(ekycs.get(i).getErrorDetail());
                    ekyc.setStatus(ekycs.get(i).getStatus());
                    ekyc.setStatusDesc(ekycs.get(i).getStatusDesc());

                    if (i == 0)
                        ekyc.setStep1(ekycs.get(i).getStep1());
                    else if (i == 1)
                        ekyc.setStep2(ekycs.get(i).getStep2());
                }
                if (ekycs.size() >= 3) {
                    ekyc.setStep3(ekycs.get(ekycs.size() - 1).getStep3());
                }

                TemplateResponse<Ekyc> response = new TemplateResponse<>();
                response.setError("0");
                response.setData(ekyc);

                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response);

            } else if ("eKYB".equalsIgnoreCase(requestType)) {
                List<Ekyb> ekybs = ekybService.getHistoryById(id);

                if (ekybs.size() <= 0)
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(Map.of("error", "1", "errorDetail", "Customer not exsit"));

                Ekyb ekyb = new Ekyb();
                for (int i = 0; i < ekybs.size(); i++) {

                    ekyb.setId(ekybs.get(i).getId());
                    ekyb.setAppCode(ekybs.get(i).getAppCode());
                    ekyb.setAppChannel(ekybs.get(i).getAppChannel());
                    ekyb.setSingleId(ekybs.get(i).getSingleId());
                    ekyb.setTin(ekybs.get(i).getTin());
                    ekyb.setCompanyNameKh(ekybs.get(i).getCompanyNameKh());
                    ekyb.setCompanyNameEn(ekybs.get(i).getCompanyNameEn());
                    ekyb.setDirList(ekybs.get(i).getDirList());
                    ekyb.setScore(ekybs.get(i).getScore());
                    ekyb.setStatus(ekybs.get(i).getStatus());
                    ekyb.setStatusDesc(ekybs.get(i).getStatusDesc());
                    ekyb.setType(ekybs.get(i).getType());
                    ekyb.setNote(ekybs.get(i).getNote());
                    ekyb.setErrorDetail(ekybs.get(i).getErrorDetail());
                    ekyb.setResDirList(ekybs.get(i).getResDirList());

                    if (i == 0)
                        ekyb.setStep1(ekybs.get(i).getStep1());
                    else if (i == 1)
                        ekyb.setStep2(ekybs.get(i).getStep2());
                }
                if (ekybs.size() >= 3) {
                    ekyb.setStep3(ekybs.get(ekybs.size() - 1).getStep3());
                }

                TemplateResponse<Ekyb> response = new TemplateResponse<>();
                response.setError("0");
                response.setData(ekyb);

                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "0", "errorDetail", ""));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error Internal Server");
        }
    }

    // cms_106: All transaction
    public ResponseEntity<?> cms_106(JsonNode data) {
        try {

            int size = data.has("pageSize") ? Integer.parseInt(data.path("pageSize").asText()) : 10;
            int page = data.has("pageIndex") ? Integer.parseInt(data.path("pageIndex").asText()) : 1;
            String searchValue = data.path("searchValue").asText();
            String requestType = data.path("requestType").asText();
            String statusDesc = data.path("statusDesc").asText();
            String fromDate = data.path("fromDate").asText();
            String toDate = data.path("toDate").asText();

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            if (fromDate != null && !fromDate.isEmpty()) {
                fromDate = new SimpleDateFormat("yyyyMMddHHmm").format(dateFormat.parse(fromDate + " 00:00"));
            }
            if (toDate != null && !toDate.isBlank()) {
                toDate = new SimpleDateFormat("yyyyMMddHHmm").format(dateFormat.parse(toDate + " 23:59"));
            }

            List<History> allTrans = ekycService.getHistories(size, page, searchValue, requestType, statusDesc,
                    fromDate, toDate, "");

            Pagination pagination = new Pagination();
            int totalElement = ekycService.getHistoriesCount(searchValue, requestType, statusDesc, fromDate, toDate,
                    "");
            int totalPages = totalElement / size;

            if (totalElement % size > 0)
                totalPages++;

            pagination.setTotalElements(totalElement);
            pagination.setPageIndex(page);
            pagination.setPageSize(size);
            pagination.setFirstPage(1);
            pagination.setLastPage(totalPages);
            pagination.setTotalPages(totalPages);

            TemplateResponseWithPagination<List<History>> response = new TemplateResponseWithPagination<List<History>>();
            response.setError("0");
            response.setData(allTrans);
            response.setPageable(pagination);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error Internal Server");
        }
    }

    // cms_107: Dashboard summary
    public ResponseEntity<?> cms_107(JsonNode data) {
        try {
            String fromDate = data.path("fromDate").asText();
            String toDate = data.path("toDate").asText();

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            if (fromDate != null && !fromDate.isEmpty())
                fromDate = new SimpleDateFormat("yyyyMMddHHmm").format(dateFormat.parse(fromDate + " 00:00"));
            else
                fromDate = new SimpleDateFormat("yyyyMM'010000'").format(new Date());

            if (toDate != null && !toDate.isBlank())
                toDate = new SimpleDateFormat("yyyyMMddHHmm").format(dateFormat.parse(toDate + " 23:59"));
            else
                toDate = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());

            Map<String, Object> mapResponse = new HashMap<>();

            // summary status
            Map<String, String> summaryByStatus = ekycService.getSummaryStatus(fromDate, toDate);
            Map<String, String> summaryStatus = new HashMap<>();

            int verified = Integer.parseInt(summaryByStatus.get("3") != null
                    ? !summaryByStatus.get("3").isEmpty() ? summaryByStatus.get("3") : "0"
                    : "0");
            int notFound = Integer.parseInt(summaryByStatus.get("2") != null
                    ? !summaryByStatus.get("2").isEmpty() ? summaryByStatus.get("2") : "0"
                    : "0");
            int failed = Integer.parseInt(summaryByStatus.get("4") != null
                    ? !summaryByStatus.get("4").isEmpty() ? summaryByStatus.get("4") : "0"
                    : "0");
            int processing = Integer.parseInt(summaryByStatus.get("1") != null
                    ? !summaryByStatus.get("1").isEmpty() ? summaryByStatus.get("1") : "0"
                    : "0");
            int submitted = Integer.parseInt(summaryByStatus.get("0") != null
                    ? !summaryByStatus.get("0").isEmpty() ? summaryByStatus.get("0") : "0"
                    : "0");

            summaryStatus.put("total", String.valueOf(verified + notFound + failed + processing + submitted));
            summaryStatus.put("verifired", String.valueOf(verified));
            summaryStatus.put("failed", String.valueOf(notFound + failed));
            summaryStatus.put("processing", String.valueOf(processing + submitted));
            mapResponse.put("summaryStatus", summaryStatus);

            // count request
            mapResponse.put("request", ekycService.getRequestbyInputer(fromDate, toDate));

            // count by channel
            mapResponse.put("channel", ekycService.getSummaryByChannel(fromDate, toDate));

            TemplateResponse<Map<String, Object>> response = new TemplateResponse<>();
            response.setError("0");
            response.setData(mapResponse);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error Internal Server");
        }
    }

    // cms_108: Full Report
    public ResponseEntity<?> cms_108(JsonNode data) {
        try {
            String fromDate = data.path("fromDate").asText("");
            String toDate = data.path("toDate").asText("");
            String channel = data.path("channel").asText("");
            String requestType = data.path("requestType").asText("");
            String format = data.path("format").asText("");

            if (fromDate.isEmpty() || toDate.isEmpty()) {
                return buildErrorResponse(HttpStatus.OK, "Both fromDate and toDate are required.");
            }

            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate start = LocalDate.parse(fromDate, inputFormatter);
            LocalDate end = LocalDate.parse(toDate, inputFormatter);
            long monthsBetween = ChronoUnit.MONTHS.between(start, end);
            // than 6 months
            if (monthsBetween > 6 || (monthsBetween == 6 && start.plusMonths(6).isBefore(end))) {
                return buildErrorResponse(HttpStatus.OK, "Date range cannot exceed 6 months.");
            }

            String dateRange = (fromDate != null && !fromDate.isEmpty()) && (toDate != null && !toDate.isEmpty())
                    ? "From " + fromDate + " to " + toDate
                    : (fromDate != null && !fromDate.isEmpty()) ? "From " + fromDate + " to now"
                            : (toDate != null && !toDate.isEmpty()) ? "From start to " + toDate
                                    : "From start to now";

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            if (fromDate != null && !fromDate.isEmpty())
                fromDate = new SimpleDateFormat("yyyyMMddHHmm").format(dateFormat.parse(fromDate + " 00:00"));

            if (toDate != null && !toDate.isEmpty())
                toDate = new SimpleDateFormat("yyyyMMddHHmm").format(dateFormat.parse(toDate + " 23:59"));

            // 2. Generate Excel file if format is excel
            if ("excel".equalsIgnoreCase(format)) {
                byte[] excelBytes;
                List<ReportFull> fullReport = ekycService.getFullReport(fromDate, toDate, channel, requestType);

                String templateDir = System.getenv("TEMPLATE_DIR");
                if (templateDir == null || templateDir.isEmpty()) {
                    templateDir = "file/template";
                }
                String templatePath = templateDir + "/Template_Customer_Risk_Report.xlsx";

                try (InputStream fileInputStream = new FileInputStream(templatePath);
                        Workbook templateWorkbook = new XSSFWorkbook(fileInputStream);
                        SXSSFWorkbook workbook = new SXSSFWorkbook((XSSFWorkbook) templateWorkbook, 100);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

                    Sheet sheet = workbook.getSheetAt(0);
                    Font commonFont = templateWorkbook.createFont();
                    commonFont.setFontName("Times New Roman");
                    commonFont.setFontHeightInPoints((short) 11);

                    Font headerFont = templateWorkbook.createFont();
                    headerFont.setFontName("Times New Roman");
                    headerFont.setFontHeightInPoints((short) 12);
                    headerFont.setBold(true);

                    CellStyle commonStyle = templateWorkbook.createCellStyle();
                    commonStyle.setFont(commonFont);
                    commonStyle.setVerticalAlignment(VerticalAlignment.CENTER);

                    // Set report title
                    sheet.createRow(1).createCell(1).setCellValue("Date:");
                    sheet.getRow(1).getCell(1).setCellStyle(commonStyle);
                    sheet.getRow(1).createCell(2).setCellValue(dateRange);
                    sheet.getRow(1).getCell(2).setCellStyle(commonStyle);

                    sheet.createRow(2).createCell(1).setCellValue("Source:");
                    sheet.getRow(2).getCell(1).setCellStyle(commonStyle);
                    sheet.getRow(2).createCell(2)
                            .setCellValue(channel != null && !channel.isEmpty() ? channel : "All sources");
                    sheet.getRow(2).getCell(2).setCellStyle(commonStyle);

                    sheet.createRow(3).createCell(1).setCellValue("Type:");
                    sheet.getRow(3).getCell(1).setCellStyle(commonStyle);
                    sheet.getRow(3).createCell(2).setCellValue(
                            requestType != null && !requestType.isEmpty() ? requestType : "All (eKYC & eKYB)");
                    sheet.getRow(3).getCell(2).setCellStyle(commonStyle);

                    // Create header style
                    CellStyle tableHeaderStyle = templateWorkbook.createCellStyle();
                    tableHeaderStyle.setFont(headerFont);
                    tableHeaderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                    tableHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    tableHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
                    tableHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                    tableHeaderStyle.setBorderTop(BorderStyle.THIN);
                    tableHeaderStyle.setBorderBottom(BorderStyle.THIN);
                    tableHeaderStyle.setBorderLeft(BorderStyle.THIN);
                    tableHeaderStyle.setBorderRight(BorderStyle.THIN);
                    tableHeaderStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
                    tableHeaderStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
                    tableHeaderStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
                    tableHeaderStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());

                    // Create header row
                    sheet.createRow(4).createCell(0).setCellValue("No.");
                    sheet.getRow(4).createCell(1).setCellValue("Customer name EN");
                    sheet.getRow(4).createCell(2).setCellValue("Date assessment");
                    sheet.getRow(4).createCell(3).setCellValue("Current assessment");
                    sheet.getRow(4).createCell(4).setCellValue("Current score");
                    sheet.getRow(4).createCell(5).setCellValue("Status");
                    sheet.getRow(4).createCell(6).setCellValue("Gender");
                    sheet.getRow(4).createCell(7).setCellValue("Birth date");
                    sheet.getRow(4).createCell(8).setCellValue("National ID");
                    sheet.getRow(4).createCell(9).setCellValue("Customer name KH");
                    sheet.getRow(4).createCell(10).setCellValue("LEGAL DOCUMENT NAME");
                    sheet.getRow(4).createCell(11).setCellValue("Nationality");
                    sheet.getRow(4).createCell(12).setCellValue("Residence");
                    sheet.getRow(4).createCell(13).setCellValue("Sector");
                    sheet.getRow(4).createCell(14).setCellValue("Branch code");
                    sheet.getRow(4).createCell(15).setCellValue("Legal Issue Date");
                    sheet.getRow(4).createCell(16).setCellValue("Legal Expire Date");

                    for (int i = 0; i <= 16; i++) {
                        sheet.getRow(4).getCell(i).setCellStyle(tableHeaderStyle);
                    }

                    // Create table cell style
                    CellStyle tableStyle = templateWorkbook.createCellStyle();
                    tableStyle.setFont(commonFont);
                    tableStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                    tableStyle.setBorderTop(BorderStyle.THIN);
                    tableStyle.setBorderBottom(BorderStyle.THIN);
                    tableStyle.setBorderLeft(BorderStyle.THIN);
                    tableStyle.setBorderRight(BorderStyle.THIN);
                    tableStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
                    tableStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
                    tableStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
                    tableStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());

                    int rowIndex = 5; // Starting row index for data population (assuming header is in row 4)
                    int index = 1;
                    // Populate data rows
                    for (ReportFull report : fullReport) {

                        sheet.createRow(rowIndex).createCell(0).setCellValue(index++);
                        sheet.getRow(rowIndex).createCell(1).setCellValue(report.getCustomerNameEn());
                        sheet.getRow(rowIndex).createCell(2).setCellValue(report.getDateAssessment());
                        sheet.getRow(rowIndex).createCell(3).setCellValue(report.getCurrentAssessment());
                        sheet.getRow(rowIndex).createCell(4).setCellValue(Double.parseDouble(report.getCurrentScore()));
                        sheet.getRow(rowIndex).createCell(5).setCellValue(report.getStatus());
                        sheet.getRow(rowIndex).createCell(6).setCellValue(report.getGender());
                        sheet.getRow(rowIndex).createCell(7).setCellValue(report.getBirthDate());

                        Cell cell = sheet.getRow(rowIndex).createCell(8);
                        String nationalId = report.getNationalId() != null ? report.getNationalId() : "";
                        cell.setCellValue(nationalId);

                        sheet.getRow(rowIndex).createCell(9).setCellValue(report.getCustomerNameKh());
                        sheet.getRow(rowIndex).createCell(10).setCellValue(report.getLegalDocName());
                        sheet.getRow(rowIndex).createCell(11).setCellValue(report.getNationality());
                        sheet.getRow(rowIndex).createCell(12).setCellValue(report.getResidence());
                        sheet.getRow(rowIndex).createCell(13).setCellValue(report.getSector());
                        sheet.getRow(rowIndex).createCell(14).setCellValue(report.getBranchCode());
                        sheet.getRow(rowIndex).createCell(15).setCellValue(report.getIssuedDate());
                        sheet.getRow(rowIndex).createCell(16).setCellValue(report.getExpiredDate());

                        for (int i = 0; i <= 16; i++) {
                            if (i != 8) {
                                sheet.getRow(rowIndex).getCell(i).setCellStyle(tableStyle);
                            } else {
                                CellStyle textStyle = workbook.createCellStyle();
                                textStyle.setFont(commonFont);
                                textStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                                textStyle.setBorderTop(BorderStyle.THIN);
                                textStyle.setBorderBottom(BorderStyle.THIN);
                                textStyle.setBorderLeft(BorderStyle.THIN);
                                textStyle.setBorderRight(BorderStyle.THIN);
                                textStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
                                textStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
                                textStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
                                textStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
                                
                                DataFormat dataFormat = workbook.createDataFormat();
                                textStyle.setDataFormat(dataFormat.getFormat("@"));
                                cell.setCellStyle(textStyle);
                            }
                        }
                        rowIndex++;
                    }
                    workbook.write(bos);
                    excelBytes = bos.toByteArray();

                    // Temporary disk files cleaning for SXSSF
                    workbook.dispose();
                }
                String outFileName = "Report_Full_"
                        + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()) + ".xlsx";
                return ResponseEntity.ok()
                        .contentType(MediaType
                                .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .header("Content-Disposition",
                                "attachment; filename=\"" + outFileName + "\"")
                        .header("Cache-Control", "must-revalidate, post-check=0, pre-check=0")
                        .body(excelBytes);
            }
            // csv format is not implemented yet, return not implemented message
            else if ("csv".equalsIgnoreCase(format)) {
                List<ReportFull> fullReport = ekycService.getFullReport(fromDate, toDate, channel, requestType);
                byte[] csvBytes;

                String outFileName = "Report_Full_"
                        + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()) + ".csv";

                // 1. Build the CSV contents in memory
                try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        // Explicitly use UTF-8 to protect international strings like Khmer text
                        PrintWriter writer = new PrintWriter(new OutputStreamWriter(bos, StandardCharsets.UTF_8))) {

                    // Write Byte Order Mark (BOM) so Excel reads UTF-8 characters properly
                    writer.write('\ufeff');

                    writer.println("RISK MANAGEMENT | Index");
                    writer.println(",Date:," + dateRange);
                    writer.println(",Source:," + (channel != null && !channel.isEmpty() ? channel : "All sources"));
                    writer.println(",Type:,"
                            + (requestType != null && !requestType.isEmpty() ? requestType : "All (eKYC & eKYB)"));

                    // 2. Add CSV Header Row
                    writer.println(
                            "No.,Customer name EN,Date assessment,Current assessment,Current Score,Status,Gender,Birth date,National ID,Customer name KH,LEGAL DOCUMENT NAME,Nationality,Residence,Sector,Branch code,Legal Issued Date,Legal Expired Date");

                    int index = 1;
                    // 3. Loop and add data lines
                    for (ReportFull report : fullReport) {
                        // CRITICAL: Format the string as ="021003005"
                        String formattedNationalId = "=\"" + report.getNationalId() + "\"";

                        writer.println(String.join(",",
                                String.valueOf(index++),
                                escapeCsv(report.getCustomerNameEn()),
                                escapeCsv(report.getDateAssessment()),
                                escapeCsv(report.getCurrentAssessment()),
                                escapeCsv(report.getCurrentScore()),
                                escapeCsv(report.getStatus()),
                                escapeCsv(report.getGender()),
                                escapeCsv(report.getBirthDate()),
                                escapeCsv(formattedNationalId),
                                escapeCsv(report.getCustomerNameKh()),
                                escapeCsv(report.getLegalDocName()),
                                escapeCsv(report.getNationality()),
                                escapeCsv(report.getResidence()),
                                escapeCsv(report.getSector()),
                                escapeCsv(report.getBranchCode()),
                                escapeCsv(report.getIssuedDate()),
                                escapeCsv(report.getExpiredDate())));
                    }

                    writer.flush();
                    csvBytes = bos.toByteArray();
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating CSV file");
                }

                // 4. Return the file download with corrected content types and body payload
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                        .header("Content-Disposition", "attachment; filename=\"" + outFileName + "\"")
                        .header("Cache-Control", "must-revalidate, post-check=0, pre-check=0")
                        .body(csvBytes); // Fixed: Changed from excelBytes to csvBytes
            } else {
                return buildErrorResponse(HttpStatus.BAD_REQUEST,
                        "Invalid format specified. Supported formats are 'excel' and 'csv'.");
            }
        } catch (Exception e) {
            logger.error("Error Internal Server: ", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error Internal Server");
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String cleanValue = value.replace("\n", " ").replace("\r", " ");
        if (cleanValue.contains(",") || cleanValue.contains("\"") || cleanValue.contains("'")) {
            cleanValue = cleanValue.replace("\"", "\"\"");
            return "\"" + cleanValue + "\"";
        }
        return cleanValue;
    }

    // default response error builder
    private ResponseEntity<Map<String, String>> buildErrorResponse(HttpStatus status, String detail) {
        return ResponseEntity.status(status)
                .body(Map.of("error", "1", "errorDetail", detail));
    }
}
