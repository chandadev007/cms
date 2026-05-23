package com.main.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.main.repository.EkybRepository;

@Service
public class EkybService {
    private static final Logger logger = LoggerFactory.getLogger(EkycService.class);
    private final EkybRepository ekybRepository;
    private final ObjectMapper objectMapper;

    public EkybService(EkybRepository ekybRepository, ObjectMapper objectMapper) {
        this.ekybRepository = ekybRepository;
        this.objectMapper = objectMapper;
    }

    public String createEkyb(String singleId, String tin, String nameKH, String nameEn, String dirListJson,
            String type, String note) {
        logger.info("Initiating Ekyc creation for: {}", singleId + tin);
        try {
            String outParams = ekybRepository.createEkyb(singleId, tin, nameKH, nameEn, dirListJson, type, note,
                    null);

            JsonNode jsonNode = objectMapper.readTree(outParams);

            Map<String, Object> response = new HashMap<>();
            response.put("p_result", jsonNode.has("p_result") ? jsonNode.get("p_result").asText() : "");
            response.put("out_id",
                    jsonNode.has("out_id") ? jsonNode.get("out_id").asText() : "");

            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("Error while calling ekyc_create procedure", e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }
}
