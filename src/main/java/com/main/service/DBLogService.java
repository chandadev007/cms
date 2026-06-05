package com.main.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.main.repository.DBLogRepository;

@Service
public class DBLogService {
    private static final Logger logger = LoggerFactory.getLogger(DBLogService.class);
    private final DBLogRepository dbLogRepository;
    private final ObjectMapper objectMapper;

    public DBLogService(DBLogRepository dbLogRepository, ObjectMapper objectMapper) {
        this.dbLogRepository = dbLogRepository;
        this.objectMapper = objectMapper;
    }

    public String createEkycLog(String actionName, String actionType, String tableId, String unitId, String userId,
            String userName, String appChannel) {
        logger.info("Initiating Ekyc creation for: {}", actionName);
        try {
            String outParams = dbLogRepository.createEkycLog(actionName, actionType, tableId, unitId, userId, userName, appChannel);

            JsonNode jsonNode = objectMapper.readTree(outParams);
            Map<String, Object> response = new HashMap<>();
            response.put("p_result", jsonNode.has("p_result") ? jsonNode.get("p_result").asText() : "");

            if (!"success".equalsIgnoreCase(response.get("p_result").toString())) {
                logger.warn("Failed to create Ekyc log for action: {}. Result: {}", actionName, response.get("p_result"));
            } else {
                logger.info("Ekyc log created successfully for action: {}", actionName);
            }

            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("Error while calling ekyc_create procedure", e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public String createEkybLog(String actionName, String actionType, String tableId, String unitId, String userId,
            String userName, String appChannel) {
        logger.info("Initiating Ekyb creation for: {}", actionName);
        try {
            String outParams = dbLogRepository.createEkybLog(actionName, actionType, tableId, unitId, userId, userName, appChannel);

            JsonNode jsonNode = objectMapper.readTree(outParams);
            Map<String, Object> response = new HashMap<>();
            response.put("p_result", jsonNode.has("p_result") ? jsonNode.get("p_result").asText() : "");

            if (!"success".equalsIgnoreCase(response.get("p_result").toString())) {
                logger.warn("Failed to create Ekyb log for action: {}. Result: {}", actionName, response.get("p_result"));
            } else {
                logger.info("Ekyb log created successfully for action: {}", actionName);
            }

            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("Error while calling ekyc_create procedure", e);
            throw new RuntimeException("Database error: ", e);
        }
    }
}
