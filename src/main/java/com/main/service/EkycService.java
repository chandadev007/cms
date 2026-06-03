package com.main.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.main.model.Ekyc;
import com.main.model.History;
import com.main.model.ReportFull;
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

            if(!"success".equalsIgnoreCase(response.get("p_result").toString())) {
                logger.warn("Ekyc creation failed for idNumber: {}, response: {}", idNumber, response);
            } else {
                logger.info("Ekyc creation successful for idNumber: {}, out_id: {}", idNumber, response.get("out_id"));
            }

            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("Error while calling ekyc_create procedure", e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public String updateToProcessing(String id) {
        logger.info("Initiating Ekyc update to processing for: {}", id);
        try {
            String outParams = ekycRepository.updateToProcessing(id);

            JsonNode jsonNode = objectMapper.readTree(outParams);
            Map<String, Object> response = new HashMap<>();
            response.put("p_result", jsonNode.has("p_result") ? jsonNode.get("p_result").asText() : "");

            if(!"success".equalsIgnoreCase(response.get("p_result").toString())) {
                logger.warn("Ekyc update to processing failed for id: {}, response: {}", id, response);
            } else {
                logger.info("Ekyc update to processing successful for id: {}", id);
            }

            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("Error while calling ekyc_processing procedure", e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public String updateFinalStatus(String id, String idNumber, String status, String score, String errorDetail) {
        logger.info("Initiating Ekyc update for verify basic infor : {}", idNumber);
        try {
            String outParams = ekycRepository.updateFinalStatus(id, idNumber, status, score, errorDetail);

            JsonNode jsonNode = objectMapper.readTree(outParams);
            Map<String, Object> response = new HashMap<>();
            response.put("p_result", jsonNode.has("p_result") ? jsonNode.get("p_result").asText() : "");

            if(!"success".equalsIgnoreCase(response.get("p_result").toString())) {
                logger.warn("Ekyc final status update failed for idNumber: {}, response: {}", idNumber, response);
            } else {
                logger.info("Ekyc final status update successful for idNumber: {}", idNumber);
            }

            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("Error while calling ekyc_final_status procedure", e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public String updateFinalFace(String id, String idNumber, String status, String score, String faceScore,
            String errorDetail) {
        logger.info("Initiating Ekyc update for verify face : {}", idNumber);
        try {
            String outParams = ekycRepository.updateFinalFace(id, idNumber, status, score, faceScore, errorDetail);
            
            JsonNode jsonNode = objectMapper.readTree(outParams);
            Map<String, Object> response = new HashMap<>();
            response.put("p_result", jsonNode.has("p_result") ? jsonNode.get("p_result").asText() : "");

            if(!"success".equalsIgnoreCase(response.get("p_result").toString())) {
                logger.warn("Ekyc final face update failed for idNumber: {}, response: {}", idNumber, response);
            } else {
                logger.info("Ekyc final face update successful for idNumber: {}", idNumber);
            }

            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("Error while calling ekyc_final_face procedure", e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public List<Ekyc> findPendingRecords() {
        try {
            return ekycRepository.findPendingRecords();
        } catch (Exception e) {
            logger.error("Error while fetching ekyc pending records ", e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public Ekyc getEkycById(String id) {
        try {
            return ekycRepository.getEkycById(id);
        } catch (Exception e) {
            logger.error("Error while fetching ekyc detail: ", e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public List<Ekyc> getEkycPage(int size, int page, String searchValue) {
        try {
            return ekycRepository.getEkycPage(size, page, searchValue);
        } catch (Exception e) {
            logger.error("Error while fetching ekyc page: ", e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public int getEkycPageCount(String searchValue) {
        try {
            return ekycRepository.getEkycPageCount(searchValue);
        } catch (Exception e) {
            logger.error("Error while fetching ekyc page: ", e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public List<History> getHistories(int size, int page, String searchValue, String requestType, String statusDesc,
            String fromDate, String toDate, String userId) {
        try {
            return ekycRepository.getHistories(size, page, searchValue, requestType, statusDesc, fromDate, toDate,
                    userId);
        } catch (Exception e) {
            logger.error("Error while fetching history page: ", e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public int getHistoriesCount(String searchValue, String requestType, String statusDesc,
            String fromDate, String toDate, String userId) {
        try {
            return ekycRepository.getHistoriesCount(searchValue, requestType, statusDesc, fromDate, toDate, userId);
        } catch (Exception e) {
            logger.error("Error while fetching history page: ", e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public List<Ekyc> getHistoryById(String id) {
        try {
            return ekycRepository.getHistoryById(id);
        } catch (Exception e) {
            logger.error("Error while fetching ekyc history detail: ", e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public Map<String, String> getSummaryStatus(String fromDate, String toDate) {
        try {
            return ekycRepository.getSummaryStatus(fromDate, toDate);
        } catch (Exception e) {
            logger.error("Error while fetching dashboard summary status : ", e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public Map<String, String> getRequestbyInputer(String fromDate, String toDate) {
        try {
            return ekycRepository.getRequestbyInputer(fromDate, toDate);
        } catch (Exception e) {
            logger.error("Error while fetching dashboard count request : ", e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public Map<String, String> getSummaryByChannel(String fromDate, String toDate) {
        try {
            return ekycRepository.getSummaryByChannel(fromDate, toDate);
        } catch (Exception e) {
            logger.error("Error while fetching dashboard count channel : ", e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public int checkEkycExisting(String idNumber, String firstNameKh, String lastNameKh, String firstNameEn,
            String lastNameEn, String gender, String dob, String issuedDate, String expiredDate,
            String type) {
        try {
            return ekycRepository.checkEkycExisting(idNumber, firstNameKh, lastNameKh, firstNameEn, lastNameEn, gender,
                    dob, issuedDate, expiredDate, type);
        } catch (Exception e) {
            logger.error("Error while fetching ekyc check exist : ", e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public List<ReportFull> getFullReport(String fromDate, String toDate, String channel, String requestType) {
        try {
            return ekycRepository.getFullReport(fromDate, toDate, channel, requestType);
        } catch (Exception e) {
            logger.error("Error while fetching full report : ", e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }
}