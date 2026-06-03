package com.main.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.main.model.TemplateResponse;
import com.main.model.TemplateResponseWithPagination;
import com.main.service.DBLogService;
import com.main.service.EkybService;
import com.main.service.EkycService;
import com.main.service.FileManagementService;

public class EkycController {

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
                    firstNameEn, lastNameEn, gender, dob, issuedDate, expiredDate, note, type, selfiePath);

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
                    userName);

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

    // cms_107
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

    // default response error builder
    private ResponseEntity<Map<String, String>> buildErrorResponse(HttpStatus status, String detail) {
        return ResponseEntity.status(status)
                .body(Map.of("error", "1", "errorDetail", detail));
    }
}
