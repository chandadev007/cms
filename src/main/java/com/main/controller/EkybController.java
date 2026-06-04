package com.main.controller;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.main.model.Ekyb;
import com.main.model.Ekyc;
import com.main.model.History;
import com.main.model.Pagination;
import com.main.model.TemplateResponse;
import com.main.model.TemplateResponseWithPagination;
import com.main.service.DBLogService;
import com.main.service.EkybService;
import com.main.service.EkycService;

public class EkybController {
    private static final Logger logger = LoggerFactory.getLogger(EkybController.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EkycService ekycService;
    private final EkybService ekybService;
    private final DBLogService dbLogService;

    public EkybController(EkybService ekybService, EkycService ekycService, DBLogService dbLogService) {
        this.ekybService = ekybService;
        this.ekycService = ekycService;
        this.dbLogService = dbLogService;
    }

    // cms_201: ekyb create
    public ResponseEntity<?> cms_201(JsonNode data, String unitCode, String userId, String userName) {
        try {

            String singleId = data.path("singleId").asText();
            String tin = data.path("tin").asText();
            String companyNameKh = data.path("companyNameKh").asText();
            String companyNameEn = data.path("companyNameEn").asText();
            JsonNode dirList = data.path("dirList");
            String type = data.path("type").asText();
            String note = data.path("note").asText();

            if (type == null || type.isEmpty())
                return buildErrorResponse(HttpStatus.OK, "Verify type reqired to fill");
            // verify basic company information
            else if ("1".equalsIgnoreCase(type)) {
                if ((tin == null || tin.isEmpty()) ||
                        (singleId == null || singleId.isEmpty()) ||
                        (companyNameKh == null || companyNameKh.isEmpty()) ||
                        (companyNameEn == null || companyNameEn.isEmpty()) ||
                        (note == null || note.isEmpty())) {
                    return buildErrorResponse(HttpStatus.OK, "All fields required to fill");
                }
            }
            // verify company TIN
            else if ("2".equalsIgnoreCase(type)) {
                if ((tin == null || tin.isEmpty()) ||
                        (companyNameKh == null || companyNameKh.isEmpty()) ||
                        (companyNameEn == null || companyNameEn.isEmpty()) ||
                        (note == null || note.isEmpty())) {
                    return buildErrorResponse(HttpStatus.OK, "All fields required to fill");
                }
            }
            // verify company director
            else if ("3".equalsIgnoreCase(type)) {
                if ((singleId == null || singleId.isEmpty()) ||
                        (note == null || note.isEmpty())) {
                    return buildErrorResponse(HttpStatus.OK, "Single ID, Purpose/note fields required to fill");
                }
            }

            // check company exist
            int isExist = ekybService.checkEkybExisting(type, singleId, tin, companyNameEn, companyNameKh);
            if (isExist > 0) {
                return buildErrorResponse(HttpStatus.OK,
                        "Customer already exist");
            }

            // start do process
            String ekycResult = ekybService.createEkyb(singleId, tin, companyNameKh, companyNameEn, dirList.toString(),
                    type, note);
            JsonNode ekycResultJson = objectMapper.readTree(ekycResult);

            String pResult = ekycResultJson.path("p_result").asText();
            String outId = ekycResultJson.path("out_id").asText();

            if (pResult == null || !"success".equalsIgnoreCase(pResult) || outId == null
                    || outId.isEmpty()) {
                return buildErrorResponse(HttpStatus.OK, "The process failed to execute: " +
                        pResult);
            }

            // log process
            dbLogService.createEkybLog("Create", "USER", outId, unitCode, userId, userName);

            // process executed successfully
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "0", "errorDetail", "Process executed successfully"));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error Internal Server");
        }
    }

    // cms_202: Ekyb detail
    public ResponseEntity<?> cms_202(JsonNode data) {
        try {
            String id = data.path("id").asText();
            if (id == null || id.isEmpty()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("error", "1", "errorDetail", "The Id not provided"));
            }

            Ekyb ekyb = ekybService.getEkybById(id);
            TemplateResponse<Ekyb> response = new TemplateResponse<>();
            response.setError("0");
            response.setData(ekyb);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error Internal Server");
        }
    }

    // cms_203: Ekyb page
    public ResponseEntity<?> cms_203(JsonNode data) {
        try {
            int size = data.has("pageSize") ? Integer.parseInt(data.path("pageSize").asText()) : 10;
            int page = data.has("pageIndex") ? Integer.parseInt(data.path("pageIndex").asText()) : 1;
            String searchValue = data.path("searchValue").asText();

            if (searchValue == null || searchValue.isEmpty()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("error", "0", "errorDetail", ""));
            }

            List<Ekyb> ekybs = ekybService.getEkybPage(size, page, searchValue);

            Pagination pagination = new Pagination();
            int totalElement = ekybService.getEkybPageCount(searchValue);
            int totalPages = totalElement / size;
            if (totalElement % size > 0)
                totalPages++;

            pagination.setTotalElements(totalElement);
            pagination.setPageIndex(page);
            pagination.setPageSize(size);
            pagination.setFirstPage(1);
            pagination.setLastPage(totalPages);
            pagination.setTotalPages(totalPages);

            TemplateResponseWithPagination<List<Ekyb>> response = new TemplateResponseWithPagination<List<Ekyb>>();
            response.setError("0");
            response.setData(ekybs);
            response.setPageable(pagination);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error Internal Server");
        }
    }

    // cms_204: OSA app list
    public ResponseEntity<?> cms_204(JsonNode data) {
        try {
            int size = data.has("pageSize") ? Integer.parseInt(data.path("pageSize").asText()) : 10;
            int page = data.has("pageIndex") ? Integer.parseInt(data.path("pageIndex").asText()) : 1;
            String searchValue = data.path("searchValue").asText();
            String requestType = data.path("requestType").asText();
            String statusDesc = data.path("statusDesc").asText();
            String fromDate = data.path("fromDate").asText();
            String toDate = data.path("toDate").asText();

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            if (fromDate != null && !fromDate.isEmpty())
                fromDate = new SimpleDateFormat("yyyyMMddHHmm").format(dateFormat.parse(fromDate + " 00:00"));

            if (toDate != null && !toDate.isBlank())
                toDate = new SimpleDateFormat("yyyyMMddHHmm").format(dateFormat.parse(toDate + " 23:59"));

            List<History> OSAs = ekybService.getListByAppChannel(size, page, searchValue, "OSA", requestType,
                    statusDesc, fromDate, toDate);

            Pagination pagination = new Pagination();
            int totalElement = ekybService.getListByAppChannelCount(searchValue, "OSA", requestType, statusDesc,
                    fromDate, toDate);
            int totalPages = totalElement / size;
            if (totalElement % size > 0)
                totalPages++;

            pagination.setTotalElements(totalElement);
            pagination.setPageIndex(page);
            pagination.setPageSize(size);
            pagination.setFirstPage(1);
            pagination.setLastPage(totalPages);
            pagination.setTotalPages(totalPages);

            // summary status
            Map<String, String> summaryByStatus = ekybService.getSummaryStatusByAppChannel(searchValue, "OSA",
                    requestType, statusDesc, fromDate, toDate);
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

            TemplateResponseWithPagination<List<History>> response = new TemplateResponseWithPagination<List<History>>();
            response.setError("0");
            response.setData(OSAs);
            response.setPageable(pagination);
            response.setSummaryStatus(summaryStatus);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error Internal Server");
        }
    }

    // cms_205: OSA view detail
    public ResponseEntity<?> cms_205(JsonNode data) {
        try {
            String id = data.path("id").asText();
            if (id == null || id.isEmpty()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("error", "1", "errorDetail", "The Id not provided"));
            }
            String requestType = data.path("requestType").asText();
            // eKYC type
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

                if ("OSA".equalsIgnoreCase(ekyc.getAppChannel())) {
                    TemplateResponse<Ekyc> response = new TemplateResponse<>();
                    response.setError("0");
                    response.setData(ekyc);

                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(response);
                } else {
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(Map.of("error", "1", "errorDetail", "Customer not exsit"));
                }
            }
            // eKYB type
            else if ("eKYB".equalsIgnoreCase(requestType)) {
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

                if ("OSA".equalsIgnoreCase(ekyb.getAppChannel())) {
                    TemplateResponse<Ekyb> response = new TemplateResponse<>();
                    response.setError("0");
                    response.setData(ekyb);

                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(response);
                } else {
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(Map.of("error", "1", "errorDetail", "Customer not exsit"));
                }
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "1", "errorDetail", "Customer not exsit"));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error Internal Server");
        }
    }

    // cms_206: Mobile app list
    public ResponseEntity<?> cms_206(JsonNode data) {
        try {
            int size = data.has("pageSize") ? Integer.parseInt(data.path("pageSize").asText()) : 10;
            int page = data.has("pageIndex") ? Integer.parseInt(data.path("pageIndex").asText()) : 1;
            String searchValue = data.path("searchValue").asText();
            String requestType = data.path("requestType").asText();
            String statusDesc = data.path("statusDesc").asText();
            String fromDate = data.path("fromDate").asText();
            String toDate = data.path("toDate").asText();

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            if (fromDate != null && !fromDate.isEmpty())
                fromDate = new SimpleDateFormat("yyyyMMddHHmm").format(dateFormat.parse(fromDate + " 00:00"));

            if (toDate != null && !toDate.isBlank())
                toDate = new SimpleDateFormat("yyyyMMddHHmm").format(dateFormat.parse(toDate + " 23:59"));

            List<History> MBs = ekybService.getListByAppChannel(size, page, searchValue, "Mobile Banking",
                    requestType,
                    statusDesc, fromDate, toDate);

            Pagination pagination = new Pagination();
            int totalElement = ekybService.getListByAppChannelCount(searchValue, "Mobile Banking", requestType,
                    statusDesc,
                    fromDate, toDate);
            int totalPages = totalElement / size;
            if (totalElement % size > 0)
                totalPages++;

            pagination.setTotalElements(totalElement);
            pagination.setPageIndex(page);
            pagination.setPageSize(size);
            pagination.setFirstPage(1);
            pagination.setLastPage(totalPages);
            pagination.setTotalPages(totalPages);

            // summary status
            Map<String, String> summaryByStatus = ekybService.getSummaryStatusByAppChannel(searchValue,
                    "Mobile Banking",
                    requestType, statusDesc, fromDate, toDate);
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

            TemplateResponseWithPagination<List<History>> response = new TemplateResponseWithPagination<List<History>>();
            response.setError("0");
            response.setData(MBs);
            response.setPageable(pagination);
            response.setSummaryStatus(summaryStatus);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error Internal Server");
        }
    }

    // cms_207: Mobile view detail
    public ResponseEntity<?> cms_207(JsonNode data) {
        try {
            String id = data.path("id").asText();
            if (id == null || id.isEmpty()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("error", "1", "errorDetail", "The Id not provided"));
            }
            String requestType = data.path("requestType").asText();
            // eKYC type
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

                if ("Mobile Banking".equalsIgnoreCase(ekyc.getAppChannel())) {
                    TemplateResponse<Ekyc> response = new TemplateResponse<>();
                    response.setError("0");
                    response.setData(ekyc);

                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(response);
                } else {
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(Map.of("error", "1", "errorDetail", "Customer not exsit"));
                }
            }
            // eKYB type
            else if ("eKYB".equalsIgnoreCase(requestType)) {
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

                if ("Mobile Banking".equalsIgnoreCase(ekyb.getAppChannel())) {
                    TemplateResponse<Ekyb> response = new TemplateResponse<>();
                    response.setError("0");
                    response.setData(ekyb);

                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(response);
                } else {
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(Map.of("error", "1", "errorDetail", "Customer not exsit"));
                }
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "1", "errorDetail", "Customer not exsit"));
        } catch (Exception e) {
            logger.error("Error Internal Server: ", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error Internal Server");
        }
    }

    // cms_208: T24 list
    public ResponseEntity<?> cms_208(JsonNode data) {
        try {
            int size = data.has("pageSize") ? Integer.parseInt(data.path("pageSize").asText()) : 10;
            int page = data.has("pageIndex") ? Integer.parseInt(data.path("pageIndex").asText()) : 1;
            String searchValue = data.path("searchValue").asText();
            String requestType = data.path("requestType").asText();
            String statusDesc = data.path("statusDesc").asText();
            String fromDate = data.path("fromDate").asText();
            String toDate = data.path("toDate").asText();

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            if (fromDate != null && !fromDate.isEmpty())
                fromDate = new SimpleDateFormat("yyyyMMddHHmm").format(dateFormat.parse(fromDate + " 00:00"));

            if (toDate != null && !toDate.isBlank())
                toDate = new SimpleDateFormat("yyyyMMddHHmm").format(dateFormat.parse(toDate + " 23:59"));

            List<History> T24s = ekybService.getListByAppChannel(size, page, searchValue, "T24",
                    requestType,
                    statusDesc, fromDate, toDate);

            Pagination pagination = new Pagination();
            int totalElement = ekybService.getListByAppChannelCount(searchValue, "T24", requestType,
                    statusDesc,
                    fromDate, toDate);
            int totalPages = totalElement / size;
            if (totalElement % size > 0)
                totalPages++;

            pagination.setTotalElements(totalElement);
            pagination.setPageIndex(page);
            pagination.setPageSize(size);
            pagination.setFirstPage(1);
            pagination.setLastPage(totalPages);
            pagination.setTotalPages(totalPages);

            // summary status
            Map<String, String> summaryByStatus = ekybService.getSummaryStatusByAppChannel(searchValue,
                    "Mobile Banking",
                    requestType, statusDesc, fromDate, toDate);
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

            TemplateResponseWithPagination<List<History>> response = new TemplateResponseWithPagination<List<History>>();
            response.setError("0");
            response.setData(T24s);
            response.setPageable(pagination);
            response.setSummaryStatus(summaryStatus);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error Internal Server");
        }
    }

    // cms_209: T24 view detail
    public ResponseEntity<?> cms_209(JsonNode data) {
        try {
            String id = data.path("id").asText();
            if (id == null || id.isEmpty()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("error", "1", "errorDetail", "The Id not provided"));
            }
            String requestType = data.path("requestType").asText();
            // eKYC type
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

                if ("T24".equalsIgnoreCase(ekyc.getAppChannel())) {
                    TemplateResponse<Ekyc> response = new TemplateResponse<>();
                    response.setError("0");
                    response.setData(ekyc);

                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(response);
                } else {
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(Map.of("error", "1", "errorDetail", "Customer not exsit"));
                }
            }
            // eKYB type
            else if ("eKYB".equalsIgnoreCase(requestType)) {
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

                if ("T24".equalsIgnoreCase(ekyb.getAppChannel())) {
                    TemplateResponse<Ekyb> response = new TemplateResponse<>();
                    response.setError("0");
                    response.setData(ekyb);

                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(response);
                } else {
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(Map.of("error", "1", "errorDetail", "Customer not exsit"));
                }
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "1", "errorDetail", "Customer not exsit"));
        } catch (Exception e) {
            logger.error("Error Internal Server: ", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error Internal Server");
        }
    }

    // default response error builder
    private ResponseEntity<Map<String, String>> buildErrorResponse(HttpStatus status, String detail) {
        return ResponseEntity.status(status)
                .body(Map.of("error", "1", "errorDetail", detail));
    }
}
