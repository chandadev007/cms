package com.main.queue;

import com.main.model.Ekyc;
import com.main.service.DBLogService;
import com.main.service.EkycService;
import com.main.service.FileManagementService;
import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class EkycGatewayWorker {

        private static final Logger logger = LoggerFactory.getLogger(EkycGatewayWorker.class);

        private final EkycService ekycService;
        private final FileManagementService fileManagementService;
        private final DBLogService dbLogService;
        private final RestClient restClient;
        private final String ekycVerifyInfoUrl;

        public EkycGatewayWorker(
                        EkycService ekycService,
                        FileManagementService fileManagementService,
                        DBLogService dbLogService,
                        RestClient camdxRestClient, // Injected bean from config
                        @Value("${cms.camdx.ekyc.verify-infor}") String ekycVerifyInfoUrl) {
                this.ekycService = ekycService;
                this.fileManagementService = fileManagementService;
                this.dbLogService = dbLogService;
                this.restClient = camdxRestClient;
                this.ekycVerifyInfoUrl = ekycVerifyInfoUrl;
        }

        @Async("CamdxQueueExecutor") // ekyc verify basic information
        public void sendToEkycVerifyInfo(Ekyc ekyc) {
                logger.info("Starting EkycGatewayWorker for basic info verification for Ekyc ID: {}", ekyc.getId());
                try {
                        Map<String, String> map = new HashMap<>();
                        map.put("messageId", "01");
                        map.put("applicationId", ekyc.getAppCode());
                        map.put("channel", ekyc.getAppChannel());
                        map.put("idNumber", ekyc.getIdNumber());
                        map.put("firstNameKh", ekyc.getFirstNameKh());
                        map.put("lastNameKh", ekyc.getLastNameKh());
                        map.put("firstNameEn", ekyc.getFirstNameEn());
                        map.put("lastNameEn", ekyc.getLastNameEn());
                        map.put("gender", ekyc.getGender());
                        map.put("dob", ekyc.getDob());
                        map.put("issuedDate", ekyc.getIssuedDate());
                        map.put("expiredDate", ekyc.getExpiredDate());

                        final String ekycVerifyInfoUrl2 = ekycVerifyInfoUrl;
                        if (ekycVerifyInfoUrl2 != null) {
                                final MediaType application_JSON2 = MediaType.APPLICATION_JSON;
                                if (application_JSON2 != null) {
                                        JsonNode responseNode = restClient.post()
                                                        .uri(ekycVerifyInfoUrl2)
                                                        .contentType(application_JSON2)
                                                        .body(map)
                                                        .retrieve()
                                                        .body(JsonNode.class);
                                        if (responseNode == null)
                                                throw new RuntimeException(
                                                                "Empty response from gateway Ekyc verify basic infor");

                                        String code = responseNode.path("code").asText();
                                        switch (code) {
                                                case "0", "00" -> {

                                                        String incorrectField = "";
                                                        JsonNode incorrectFieldsNode = responseNode.path("data")
                                                                        .path("incorrectFields");

                                                        incorrectField = incorrectFieldsNode.toString().replace("[", "")
                                                                        .replace("]",
                                                                                        "");
                                                        incorrectField = incorrectField.length() > 0
                                                                        ? incorrectField + " are incorrect fields"
                                                                        : incorrectField;

                                                        String score = responseNode.path("data").path("score").asText();
                                                        if (isNumericScore(score)) {
                                                                ekycService.updateFinalStatus(ekyc.getId(),
                                                                                ekyc.getIdNumber(), "3",
                                                                                score,
                                                                                incorrectField);
                                                                dbLogService.createEkycLog(
                                                                                "update status to success by worker",
                                                                                "SYSTEM", ekyc.getId(), null,
                                                                                null,
                                                                                null, ekyc.getAppChannel());
                                                        } else {
                                                                ekycService.updateFinalStatus(ekyc.getId(),
                                                                                ekyc.getIdNumber(), "3",
                                                                                "0",
                                                                                "got invalid score {\"" + score
                                                                                                + "\"} from Camdx "
                                                                                                + incorrectField);
                                                                dbLogService.createEkycLog(
                                                                                "update status to success by worker",
                                                                                "SYSTEM", ekyc.getId(), null,
                                                                                null,
                                                                                null, ekyc.getAppChannel());
                                                        }
                                                }
                                                case "01" -> {
                                                        ekycService.updateFinalStatus(ekyc.getId(), ekyc.getIdNumber(),
                                                                        "4", "0",
                                                                        "failed to validation");
                                                        dbLogService.createEkycLog("update status to failed by worker",
                                                                        "SYSTEM",
                                                                        ekyc.getId(), null,
                                                                        null, null, ekyc.getAppChannel());
                                                }
                                                case "02" -> {
                                                        ekycService.updateFinalStatus(ekyc.getId(), ekyc.getIdNumber(),
                                                                        "4", "0",
                                                                        "gateway connection has problem");
                                                        dbLogService.createEkycLog("update status to failed by worker",
                                                                        "SYSTEM",
                                                                        ekyc.getId(), null,
                                                                        null, null, ekyc.getAppChannel());
                                                }
                                                case "03" -> {
                                                        ekycService.updateFinalStatus(ekyc.getId(), ekyc.getIdNumber(),
                                                                        "2", "0",
                                                                        "customer not exist");
                                                        dbLogService.createEkycLog(
                                                                        "update status to not found by worker",
                                                                        "SYSTEM",
                                                                        ekyc.getId(), null,
                                                                        null, null, ekyc.getAppChannel());
                                                }
                                                default -> {
                                                        ekycService.updateFinalStatus(ekyc.getId(), ekyc.getIdNumber(),
                                                                        "4", "0",
                                                                        "gateway connection has problem");
                                                        dbLogService.createEkycLog("update status to failed by worker",
                                                                        "SYSTEM",
                                                                        ekyc.getId(), null,
                                                                        null, null, ekyc.getAppChannel());
                                                }
                                        }
                                } else {
                                        logger.error("MediaType.APPLICATION_JSON is null in EkycGatewayWorker for basic info verification for Ekyc ID: {}",
                                                        ekyc.getId());
                                }
                        } else {
                                logger.error("Ekyc verify basic URL is not configured in EkycGatewayWorker for basic info verification for Ekyc ID: {}",
                                                ekyc.getId());
                        }
                } catch (Exception e) {
                        logger.error("Exception in EkycGatewayWorker for basic info verification for Ekyc ID: {}: {}",
                                        ekyc.getId(), e.getMessage(), e);

                        ekycService.updateFinalStatus(ekyc.getId(), ekyc.getIdNumber(), "4", "0",
                                        "gateway timeout");
                        dbLogService.createEkycLog("update status to failed by worker", "SYSTEM",
                                        ekyc.getId(), null,
                                        null, null, ekyc.getAppChannel());
                }
        }

        @Async("CamdxQueueExecutor") // ekyc verify with face
        public void sendToEkycVerifyFace(Ekyc ekyc) {

                try {
                        byte[] fileContent = {};
                        try {
                                fileContent = fileManagementService.downloadFile(ekyc.getSelfiePath(), "selfie");
                        } catch (Exception e) {
                                ekycService.updateFinalFace(ekyc.getId(), ekyc.getIdNumber(), "4", "0", "0",
                                                "file management gateway timeout");
                                dbLogService.createEkycLog("update status to failed by worker", "SYSTEM", ekyc.getId(),
                                                null,
                                                null, null, ekyc.getAppChannel());
                                e.printStackTrace();
                        }

                        Map<String, String> map = new HashMap<>();
                        map.put("messageId", "02");
                        map.put("applicationId", ekyc.getAppCode());
                        map.put("channel", ekyc.getAppChannel());
                        map.put("idNumber", ekyc.getIdNumber());
                        map.put("firstNameKh", ekyc.getFirstNameKh());
                        map.put("lastNameKh", ekyc.getLastNameKh());
                        map.put("firstNameEn", ekyc.getFirstNameEn());
                        map.put("lastNameEn", ekyc.getLastNameEn());
                        map.put("gender", ekyc.getGender());
                        map.put("dob", ekyc.getDob());
                        map.put("issuedDate", ekyc.getIssuedDate());
                        map.put("expiredDate", ekyc.getExpiredDate());
                        map.put("faceImg", Base64.getEncoder().encodeToString(fileContent));

                        final String ekycVerifyInfoUrl2 = ekycVerifyInfoUrl;
                        if (ekycVerifyInfoUrl2 != null) {
                                final MediaType application_JSON2 = MediaType.APPLICATION_JSON;
                                if (application_JSON2 != null) {
                                        JsonNode responseNode = restClient.post()
                                                        .uri(ekycVerifyInfoUrl2)
                                                        .contentType(application_JSON2)
                                                        .body(map)
                                                        .retrieve()
                                                        .body(JsonNode.class);
                                        if (responseNode == null)
                                                throw new RuntimeException(
                                                                "Empty response from gateway Ekyc verify face");

                                        logger.info("Received response from Ekyc verify face for Ekyc ID: {}: {}",
                                                        ekyc.getId(), responseNode.toString());
                                        String code = responseNode.path("code").asText();
                                        switch (code) {
                                                case "0", "00" -> {
                                                        String incorrectField = "";
                                                        JsonNode incorrectFieldsNode = responseNode.path("data")
                                                                        .path("userInfo")
                                                                        .path("incorrectFields");

                                                        incorrectField = incorrectFieldsNode.toString().replace("[", "")
                                                                        .replace("]",
                                                                                        "");
                                                        incorrectField = incorrectField.length() > 0
                                                                        ? incorrectField + " are incorrect fields"
                                                                        : incorrectField;

                                                        String score = responseNode.path("data").path("userInfo")
                                                                        .path("score")
                                                                        .asText();
                                                        String faceScore = responseNode.path("data")
                                                                        .path("faceMoiScore").asText();

                                                        if (isNumericScore(score) && isNumericScore(faceScore)) {

                                                                ekycService.updateFinalFace(ekyc.getId(),
                                                                                ekyc.getIdNumber(), "3",
                                                                                score, faceScore,
                                                                                incorrectField);
                                                                dbLogService.createEkycLog(
                                                                                "update status to success by worker",
                                                                                "SYSTEM", ekyc.getId(), null,
                                                                                null,
                                                                                null, ekyc.getAppChannel());
                                                        } else if (isNumericScore(score)
                                                                        && !isNumericScore(faceScore)) {

                                                                ekycService.updateFinalFace(ekyc.getId(),
                                                                                ekyc.getIdNumber(), "3",
                                                                                score, "0",
                                                                                "got invalid score {\"" + faceScore
                                                                                                + "\"} from Camdx "
                                                                                                + incorrectField);
                                                                dbLogService.createEkycLog(
                                                                                "update status to success by worker",
                                                                                "SYSTEM", ekyc.getId(), null,
                                                                                null,
                                                                                null, ekyc.getAppChannel());
                                                        } else if (!isNumericScore(score)
                                                                        && isNumericScore(faceScore)) {

                                                                ekycService.updateFinalFace(ekyc.getId(),
                                                                                ekyc.getIdNumber(), "3", "0",
                                                                                faceScore,
                                                                                "got invalid score {\"" + score
                                                                                                + "\"} from Camdx "
                                                                                                + incorrectField);
                                                                dbLogService.createEkycLog(
                                                                                "update status to success by worker",
                                                                                "SYSTEM", ekyc.getId(), null,
                                                                                null,
                                                                                null, ekyc.getAppChannel());
                                                        } else {
                                                                ekycService.updateFinalFace(ekyc.getId(),
                                                                                ekyc.getIdNumber(), "3",
                                                                                "0", "0",
                                                                                "got invalid score {\"" + score
                                                                                                + "\"} from Camdx "
                                                                                                + incorrectField);
                                                                dbLogService.createEkycLog(
                                                                                "update status to success by worker",
                                                                                "SYSTEM", ekyc.getId(), null,
                                                                                null,
                                                                                null, ekyc.getAppChannel());
                                                        }
                                                }
                                                case "01" -> {
                                                        ekycService.updateFinalFace(ekyc.getId(), ekyc.getIdNumber(),
                                                                        "4", "0", "0",
                                                                        "failed to validation");
                                                        dbLogService.createEkycLog("update status to failed by worker",
                                                                        "SYSTEM",
                                                                        ekyc.getId(), null,
                                                                        null, null, ekyc.getAppChannel());
                                                }
                                                case "02" -> {
                                                        ekycService.updateFinalFace(ekyc.getId(), ekyc.getIdNumber(),
                                                                        "4", "0", "0",
                                                                        "gateway connection has problem");
                                                        dbLogService.createEkycLog("update status to failed by worker",
                                                                        "SYSTEM",
                                                                        ekyc.getId(), null,
                                                                        null, null, ekyc.getAppChannel());
                                                }
                                                case "03" -> {
                                                        ekycService.updateFinalFace(ekyc.getId(), ekyc.getIdNumber(),
                                                                        "2", "0", "0",
                                                                        "customer not exist");
                                                        dbLogService.createEkycLog(
                                                                        "update status to not found by worker",
                                                                        "SYSTEM",
                                                                        ekyc.getId(), null,
                                                                        null, null, ekyc.getAppChannel());
                                                }
                                                case "04" -> {
                                                        ekycService.updateFinalFace(ekyc.getId(), ekyc.getIdNumber(),
                                                                        "4", "0", "0",
                                                                        "face cannot detect");
                                                        dbLogService.createEkycLog("update status to failed by worker",
                                                                        "SYSTEM",
                                                                        ekyc.getId(), null,
                                                                        null, null, ekyc.getAppChannel());
                                                }
                                                default -> {
                                                        ekycService.updateFinalFace(ekyc.getId(), ekyc.getIdNumber(),
                                                                        "4", "0", "0",
                                                                        "gateway connection has problem");
                                                        dbLogService.createEkycLog("update status to failed by worker",
                                                                        "SYSTEM",
                                                                        ekyc.getId(), null,
                                                                        null, null, ekyc.getAppChannel());
                                                }
                                        }
                                } else {
                                        logger.error("MediaType.APPLICATION_JSON is null in EkycGatewayWorker for face verification for Ekyc ID: {}",
                                                        ekyc.getId());
                                }
                        } else {
                                logger.error("Ekyc verify basic URL is not configured in EkycGatewayWorker for face verification for Ekyc ID: {}",
                                                ekyc.getId());
                        }
                } catch (Exception e) {
                        logger.error("Exception in EkycGatewayWorker for face verification for Ekyc ID: {}: {}",
                                        ekyc.getId(), e.getMessage(), e);

                        ekycService.updateFinalFace(ekyc.getId(), ekyc.getIdNumber(), "4", "0", "0",
                                        "gateway timeout");
                        dbLogService.createEkycLog("update status to failed by worker", "SYSTEM", ekyc.getId(), null,
                                        null, null, ekyc.getAppChannel());
                        e.printStackTrace();
                }
        }

        private boolean isNumericScore(String scoreStr) {
                if (scoreStr == null || scoreStr.trim().isEmpty() || "NaN".equalsIgnoreCase(scoreStr.trim())) {
                        return false;
                }
                // Matches positive/negative integers and decimals (e.g., "92.5", "100", "0")
                return scoreStr.matches("-?\\d+(\\.\\d+)?");
        }
}