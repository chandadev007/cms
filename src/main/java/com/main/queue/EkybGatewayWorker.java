package com.main.queue;

import com.main.model.Ekyb;
import com.main.service.DBLogService;
import com.main.service.EkybService;
import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.HashMap;
import java.util.Map;

@Service
public class EkybGatewayWorker {
    private static final Logger logger = LoggerFactory.getLogger(EkybGatewayWorker.class);

    private final EkybService ekybService;
    private final DBLogService dbLogService;
    private final RestClient restClient;
    private final String ekybVerifyBasicUrl;

    public EkybGatewayWorker(
            EkybService ekybService,
            DBLogService dbLogService,
            RestClient camdxRestClient, // Injected bean from config
            @Value("${cms.camdx.ekyb.verify-basic-infor}") String ekybVerifyBasicUrl) {
        this.ekybService = ekybService;
        this.dbLogService = dbLogService;
        this.restClient = camdxRestClient;
        this.ekybVerifyBasicUrl = ekybVerifyBasicUrl;
    }

    @Async("CamdxQueueExecutor") // If Ekyb batch sizes grow, swap this to a custom "EkybQueueExecutor"
    public void sendToEkybVerifyBasicInfo(Ekyb ekyb) {
        logger.info("Starting EkybGatewayWorker for Ekyb ID: {}", ekyb.getId());
        try {
            Map<String, String> map = new HashMap<>();
            map.put("messageId", "01");
            map.put("applicationId", ekyb.getAppCode());
            map.put("channel", ekyb.getAppChannel());
            map.put("single_id", ekyb.getSingleId());
            map.put("tin", ekyb.getTin());
            map.put("company_name_en", ekyb.getCompanyNameEn());
            map.put("company_name_kh", ekyb.getCompanyNameKh());

            final String ekybVerifyBasicUrl2 = ekybVerifyBasicUrl;
            if (ekybVerifyBasicUrl2 != null) {
                final MediaType application_JSON2 = MediaType.APPLICATION_JSON;
                if (application_JSON2 != null) {
                    JsonNode responseNode = restClient.post()
                            .uri(ekybVerifyBasicUrl2)
                            .contentType(application_JSON2)
                            .body(map)
                            .retrieve()
                            .body(JsonNode.class);
                    if (responseNode == null)
                        throw new RuntimeException("Empty response from gateway Ekyb verify basic infor");

                    logger.info("Received response from Ekyb verify basic info for Ekyb ID: {}: {}", ekyb.getId(),
                            responseNode.toString());
                    String code = responseNode.path("code").asText();
                    switch (code) {
                        case "0", "00" -> {

                            String incorrectField = "";
                            JsonNode incorrectFieldsNode = responseNode.path("data").path("incorrectFields");

                            incorrectField = incorrectFieldsNode.toString().replace("[", "").replace("]", "");
                            incorrectField = incorrectField.length() > 0 ? incorrectField + " are incorrect fields"
                                    : incorrectField;

                            String score = responseNode.path("data").path("score").asText();
                            if (isNumericScore(score)) {

                                ekybService.updateFinalStatus(ekyb.getId(), "3", score, incorrectField);
                                dbLogService.createEkybLog("update status to success by worker", "SYSTEM", ekyb.getId(),
                                        null,
                                        null,
                                        null, ekyb.getAppChannel());
                            } else {
                                ekybService.updateFinalStatus(ekyb.getId(), "3", "0",
                                        "got invalid score {\"" + score + "\"} from Camdx");
                                dbLogService.createEkybLog("update status to success by worker", "SYSTEM", ekyb.getId(),
                                        null,
                                        null,
                                        null, ekyb.getAppChannel());
                            }
                        }
                        case "01" -> {
                            ekybService.updateFinalStatus(ekyb.getId(), "4", "0",
                                    "failed to validation");
                            dbLogService.createEkybLog("update status to failed by worker", "SYSTEM", ekyb.getId(),
                                    null,
                                    null,
                                    null, ekyb.getAppChannel());
                        }
                        case "02" -> {
                            ekybService.updateFinalStatus(ekyb.getId(), "4", "0",
                                    "gateway connection has problem");
                            dbLogService.createEkybLog("update status to failed by worker", "SYSTEM", ekyb.getId(),
                                    null,
                                    null,
                                    null, ekyb.getAppChannel());
                        }
                        case "03" -> {
                            ekybService.updateFinalStatus(ekyb.getId(), "2", "0", "customer not exist");
                            dbLogService.createEkybLog("update status to not found by worker", "SYSTEM", ekyb.getId(),
                                    null,
                                    null, null, ekyb.getAppChannel());
                        }
                        default -> {
                            ekybService.updateFinalStatus(ekyb.getId(), "4", "0",
                                    "gateway connection has problem");
                            dbLogService.createEkybLog("update status to failed by worker", "SYSTEM", ekyb.getId(),
                                    null,
                                    null,
                                    null, ekyb.getAppChannel());
                        }
                    }
                } else {
                    logger.error("MediaType.APPLICATION_JSON is null in EkybGatewayWorker for Ekyb ID: {}",
                            ekyb.getId());
                }
            } else {
                logger.error("Ekyb verify basic URL is not configured in EkybGatewayWorker for Ekyb ID: {}",
                        ekyb.getId());
            }
        } catch (Exception e) {
            logger.error("Exception in EkybGatewayWorker for Ekyb ID: {}: {}", ekyb.getId(), e);

            ekybService.updateFinalStatus(ekyb.getId(), "4", "0",
                    "gateway timeout");
            dbLogService.createEkybLog("update status to failed by worker", "SYSTEM", ekyb.getId(), null,
                    null,
                    null, ekyb.getAppChannel());
        }
    }

    // verify with TIN
    @Async("CamdxQueueExecutor") // If Ekyb batch sizes grow, swap this to a custom "EkybQueueExecutor"
    public void sendToEkybVerifyWithTIN(Ekyb ekyb) {
        logger.info("Starting EkybGatewayWorker for TIN verification for Ekyb ID: {}", ekyb.getId());
        try {
            Map<String, String> map = new HashMap<>();
            map.put("messageId", "02");
            map.put("applicationId", ekyb.getAppCode());
            map.put("channel", ekyb.getAppChannel());
            map.put("tin", ekyb.getTin());
            map.put("company_name_en", ekyb.getCompanyNameEn());
            map.put("company_name_kh", ekyb.getCompanyNameKh());

            final String ekybVerifyBasicUrl2 = ekybVerifyBasicUrl;
            if (ekybVerifyBasicUrl2 != null) {
                final MediaType application_JSON2 = MediaType.APPLICATION_JSON;
                if (application_JSON2 != null) {
                    JsonNode responseNode = restClient.post()
                            .uri(ekybVerifyBasicUrl2)
                            .contentType(application_JSON2)
                            .body(map)
                            .retrieve()
                            .body(JsonNode.class);
                    if (responseNode == null)
                        throw new RuntimeException("Empty response from gateway Ekyb verify basic infor");

                    logger.info("Received response from Ekyb verify with TIN for Ekyb ID: {}: {}", ekyb.getId(),
                            responseNode.toString());
                    String code = responseNode.path("code").asText();
                    switch (code) {
                        case "0", "00" -> {

                            String incorrectField = "";
                            JsonNode incorrectFieldsNode = responseNode.path("data").path("incorrectFields");

                            incorrectField = incorrectFieldsNode.toString().replace("[", "").replace("]", "");
                            incorrectField = incorrectField.length() > 0 ? incorrectField + " are incorrect fields"
                                    : incorrectField;

                            String score = responseNode.path("data").path("score").asText();
                            if (isNumericScore(score)) {
                                ekybService.updateFinalStatus(ekyb.getId(), "3", score, incorrectField);
                                dbLogService.createEkybLog("update status to success by worker", "SYSTEM", ekyb.getId(),
                                        null,
                                        null,
                                        null, ekyb.getAppChannel());
                            } else {
                                ekybService.updateFinalStatus(ekyb.getId(), "3", "0",
                                        "got invalid score {\"" + score + "\"} from Camdx");
                                dbLogService.createEkybLog("update status to success by worker", "SYSTEM", ekyb.getId(),
                                        null,
                                        null,
                                        null, ekyb.getAppChannel());
                            }
                        }
                        case "01" -> {
                            ekybService.updateFinalStatus(ekyb.getId(), "4", "0",
                                    "failed to validation");
                            dbLogService.createEkybLog("update status to failed by worker", "SYSTEM", ekyb.getId(),
                                    null,
                                    null,
                                    null, ekyb.getAppChannel());
                        }
                        case "02" -> {
                            ekybService.updateFinalStatus(ekyb.getId(), "4", "0",
                                    "gateway connection has problem");
                            dbLogService.createEkybLog("update status to failed by worker", "SYSTEM", ekyb.getId(),
                                    null,
                                    null,
                                    null, ekyb.getAppChannel());
                        }
                        case "03" -> {
                            ekybService.updateFinalStatus(ekyb.getId(), "2", "0", "customer not exist");
                            dbLogService.createEkybLog("update status to not found by worker", "SYSTEM", ekyb.getId(),
                                    null,
                                    null, null, ekyb.getAppChannel());
                        }
                        default -> {
                            ekybService.updateFinalStatus(ekyb.getId(), "4", "0",
                                    "gateway connection has problem");
                            dbLogService.createEkybLog("update status to failed by worker", "SYSTEM", ekyb.getId(),
                                    null,
                                    null,
                                    null, ekyb.getAppChannel());
                        }
                    }
                } else {
                    logger.error(
                            "MediaType.APPLICATION_JSON is null in EkybGatewayWorker for TIN verification for Ekyb ID: {}",
                            ekyb.getId());
                }
            } else {
                logger.error(
                        "Ekyb verify basic URL is not configured in EkybGatewayWorker for TIN verification for Ekyb ID: {}",
                        ekyb.getId());
            }
        } catch (Exception e) {
            logger.error("Exception in EkybGatewayWorker for TIN verification for Ekyb ID: {}: {}", ekyb.getId(), e);

            ekybService.updateFinalStatus(ekyb.getId(), "4", "0",
                    "gateway timeout");
            dbLogService.createEkybLog("update status to failed by worker", "SYSTEM", ekyb.getId(), null,
                    null,
                    null, ekyb.getAppChannel());
        }
    }

    // verify with Directors
    @Async("CamdxQueueExecutor")
    public void sendToEkybVerifyWithDirectors(Ekyb ekyb) {
        logger.info("Starting EkybGatewayWorker for Directors verification for Ekyb ID: {}", ekyb.getId());
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("messageId", "03");
            map.put("applicationId", ekyb.getAppCode());
            map.put("channel", ekyb.getAppChannel());
            map.put("single_id", ekyb.getSingleId());
            map.put("company_name_en", ekyb.getCompanyNameEn());
            map.put("company_name_kh", ekyb.getCompanyNameKh());
            map.put("directors", ekyb.getDirList());

            final String ekybVerifyBasicUrl2 = ekybVerifyBasicUrl;
            if (ekybVerifyBasicUrl2 != null) {
                final MediaType application_JSON2 = MediaType.APPLICATION_JSON;
                if (application_JSON2 != null) {
                    JsonNode responseNode = restClient.post()
                            .uri(ekybVerifyBasicUrl2)
                            .contentType(application_JSON2)
                            .body(map)
                            .retrieve()
                            .body(JsonNode.class);
                    if (responseNode == null)
                        throw new RuntimeException("Empty response from gateway Ekyb verify basic infor");

                    logger.info("Received response from Ekyb verify with TIN for Ekyb ID: {}: {}", ekyb.getId(),
                            responseNode.toString());
                    String code = responseNode.path("code").asText();

                    switch (code) {
                        case "0", "00" -> {

                            String incorrectField = "";
                            JsonNode incorrectFieldsNode = responseNode.path("data").path("company")
                                    .path("incorrectFields");

                            incorrectField = incorrectFieldsNode.toString().replace("[", "").replace("]", "");
                            incorrectField = incorrectField.length() > 0 ? incorrectField + " are incorrect fields"
                                    : incorrectField;

                            JsonNode resDirList = responseNode.path("data").path("directors");
                            String score = responseNode.path("data").path("company").path("score").asText();
                            if (isNumericScore(score)) {

                                ekybService.updateFinalDirector(ekyb.getId(), "3", score, incorrectField,
                                        resDirList.toString());
                                dbLogService.createEkybLog("update status to success by worker", "SYSTEM", ekyb.getId(),
                                        null,
                                        null,
                                        null, ekyb.getAppChannel());
                            } else {
                                ekybService.updateFinalStatus(ekyb.getId(), "3", "0",
                                        "got invalid score {\"" + score + "\"} from Camdx");
                                dbLogService.createEkybLog("update status to success by worker", "SYSTEM", ekyb.getId(),
                                        null,
                                        null,
                                        null, ekyb.getAppChannel());
                            }
                        }
                        case "01" -> {
                            ekybService.updateFinalStatus(ekyb.getId(), "4", "0",
                                    "failed to validation");
                            dbLogService.createEkybLog("update status to failed by worker", "SYSTEM", ekyb.getId(),
                                    null,
                                    null,
                                    null, ekyb.getAppChannel());
                        }
                        case "02" -> {
                            ekybService.updateFinalStatus(ekyb.getId(), "4", "0",
                                    "gateway connection has problem");
                            dbLogService.createEkybLog("update status to failed by worker", "SYSTEM", ekyb.getId(),
                                    null,
                                    null,
                                    null, ekyb.getAppChannel());
                        }
                        case "03" -> {
                            ekybService.updateFinalStatus(ekyb.getId(), "2", "0", "customer not exist");
                            dbLogService.createEkybLog("update status to not found by worker", "SYSTEM", ekyb.getId(),
                                    null,
                                    null, null, ekyb.getAppChannel());
                        }
                        default -> {
                            ekybService.updateFinalStatus(ekyb.getId(), "4", "0",
                                    "gateway connection has problem");
                            dbLogService.createEkybLog("update status to failed by worker", "SYSTEM", ekyb.getId(),
                                    null,
                                    null,
                                    null, ekyb.getAppChannel());
                        }
                    }
                } else {
                    logger.error(
                            "MediaType.APPLICATION_JSON is null in EkybGatewayWorker for TIN verification for Ekyb ID: {}",
                            ekyb.getId());
                }
            } else {
                logger.error(
                        "Ekyb verify basic URL is not configured in EkybGatewayWorker for TIN verification for Ekyb ID: {}",
                        ekyb.getId());
            }

        } catch (Exception e) {
            logger.error("Exception in EkybGatewayWorker for Directors verification for Ekyb ID: {}: {}", ekyb.getId(), e);

            ekybService.updateFinalStatus(ekyb.getId(), "4", "0",
                    "gateway timeout");
            dbLogService.createEkybLog("update status to failed by worker", "SYSTEM", ekyb.getId(), null,
                    null,
                    null, ekyb.getAppChannel());
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