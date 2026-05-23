package com.main.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.main.repository.EkycRepository;

@Service
public class EkycService {
    private static final Logger logger = LoggerFactory.getLogger(EkycService.class);
    private final EkycRepository ekycRepository;
    private final ObjectMapper objectMapper;

    public EkycService(EkycRepository ekycRepository, ObjectMapper objectMapper) {
        this.ekycRepository = ekycRepository;
        this.objectMapper = objectMapper;
    }

    public String createEkyc(String idNumber, String firstNameKh, String lastNameKh, String firstNameEn,
            String lastNameEn, String gender, String dob, String issuedDae, String expiredDate, String note,
            String type, String selfiePath) {
        logger.info("Initiating Ekyc creation for: {}", idNumber);
        try {
            String outParams = ekycRepository.createEkyc(idNumber, firstNameKh, lastNameKh, firstNameEn, lastNameEn,
                    gender, dob, issuedDae, expiredDate, note, type, selfiePath, null);

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
