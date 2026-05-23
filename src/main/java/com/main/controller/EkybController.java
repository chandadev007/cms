package com.main.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.main.service.DBLogService;
import com.main.service.EkybService;

public class EkybController {
        private final ObjectMapper objectMapper = new ObjectMapper();
    private final EkybService ekybService;
    private final DBLogService dbLogService;

    public EkybController(EkybService ekybService, DBLogService dbLogService) {
        this.ekybService = ekybService;
        this.dbLogService = dbLogService;
    }

    // cms_101: ekyc create
    public ResponseEntity<?> cms_201(JsonNode data, String unitCode, String userId, String userName) {
        try {

            String singleId = data.path("singleId").asText();
            String tin = data.path("tin").asText();
            String companyNameKh = data.path("companyNameKh").asText();
            String companyNameEn = data.path("companyNameEn").asText();
            String dirList = data.path("dirList").asText();
            String type = data.path("type").asText();
            String note = data.path("note").asText();

            String ekycResult = ekybService.createEkyb(singleId, tin, companyNameKh, companyNameEn, dirList, type, note);
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

    // default response error builder
    private ResponseEntity<Map<String, String>> buildErrorResponse(HttpStatus status, String detail) {
        return ResponseEntity.status(status)
                .body(Map.of("error", "1", "errorDetail", detail));
    }
}
