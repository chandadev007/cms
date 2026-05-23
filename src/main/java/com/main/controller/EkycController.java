package com.main.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.main.service.DBLogService;
import com.main.service.EkycService;
import com.main.service.FileManagementService;

public class EkycController {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EkycService ekycService;
    private final DBLogService dbLogService;
    private final FileManagementService fileManagementService;

    public EkycController(EkycService ekycService, DBLogService dbLogService,
            FileManagementService fileManagementService) {
        this.ekycService = ekycService;
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

            // get selfie path
            String selfiePath = "";
            if (attachment != null) {
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
            }

            String ekycResult = ekycService.createEkyc(idNumber, firstNameKh, lastNameKh,
                    firstNameEn, lastNameEn,
                    gender, dob, issuedDate, expiredDate, note, type, selfiePath);
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

    // default response error builder
    private ResponseEntity<Map<String, String>> buildErrorResponse(HttpStatus status, String detail) {
        return ResponseEntity.status(status)
                .body(Map.of("error", "1", "errorDetail", detail));
    }
}
