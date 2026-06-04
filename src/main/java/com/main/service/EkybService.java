package com.main.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.main.model.Ekyb;
import com.main.model.History;
import com.main.repository.EkybRepository;

@Service
public class EkybService {
    private static final Logger logger = LoggerFactory.getLogger(EkybService.class);
    private final EkybRepository ekybRepository;
    private final ObjectMapper objectMapper;

    public EkybService(EkybRepository ekybRepository, ObjectMapper objectMapper) {
        this.ekybRepository = ekybRepository;
        this.objectMapper = objectMapper;
    }

    public String createEkyb(String singleId, String tin, String nameKH, String nameEn, String dirListJson,
            String type, String note, String appChannel) {
        logger.info("Initiating Ekyb creation for: {}", singleId + tin);
        try {
            String outParams = ekybRepository.createEkyb(singleId, tin, nameKH, nameEn, dirListJson, type, note,
                    null, appChannel);

            JsonNode jsonNode = objectMapper.readTree(outParams);
            Map<String, Object> response = new HashMap<>();
            response.put("p_result", jsonNode.has("p_result") ? jsonNode.get("p_result").asText() : "");
            response.put("out_id",
                    jsonNode.has("out_id") ? jsonNode.get("out_id").asText() : "");

            if(!"success".equalsIgnoreCase(response.get("p_result").toString())) {
                logger.warn("Failed to create Ekyb for singleId: {}. Result: {}", singleId, response.get("p_result"));
            } else {
                logger.info("Ekyb created successfully for singleId: {}", singleId);
            }

            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("Error while calling ekyb_create procedure", e.getMessage());
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public String updateToProcessing(String id) {
        logger.info("Initiating Ekyb update for: {}", id);
        try {
            String outParams = ekybRepository.updateToProcessing(id);

            JsonNode jsonNode = objectMapper.readTree(outParams);
            Map<String, Object> response = new HashMap<>();
            response.put("p_result", jsonNode.has("p_result") ? jsonNode.get("p_result").asText() : "");

            if (!"success".equalsIgnoreCase(response.get("p_result").toString())) {
                logger.warn("Failed to update Ekyb to processing for id: {}. Result: {}", id, response.get("p_result"));
            } else {
                logger.info("Ekyb updated to processing successfully for id: {}", id);
            }

            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("Error while calling ekyb_processing procedure", e.getMessage());
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public List<Ekyb> findPendingRecords() {
        try {
            List<Ekyb> ekybs = ekybRepository.findPendingRecords();
            return ekybs;
        } catch (Exception e) {
            logger.error("Error while fetching ekyb pending records ", e.getMessage());
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public String updateFinalStatus(String id, String status, String score, String errorDetail) {
        logger.info("Initiating Ekyb update for: {}", id);
        try {
            String outParams = ekybRepository.updateFinalStatus(id, status, score, errorDetail);

            JsonNode jsonNode = objectMapper.readTree(outParams);
            Map<String, Object> response = new HashMap<>();
            response.put("p_result", jsonNode.has("p_result") ? jsonNode.get("p_result").asText() : "");

            if (!"success".equalsIgnoreCase(response.get("p_result").toString())) {
                logger.warn("Failed to update Ekyb final status for id: {}. Result: {}", id, response.get("p_result"));
            } else {
                logger.info("Ekyb final status updated successfully for id: {}", id);
            }
            
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("Error while calling ekyb_processing procedure", e.getMessage());
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public Ekyb getEkybById(String id) {
        try {
            return ekybRepository.getEkybById(id);
        } catch (Exception e) {
            logger.error("Error while fetching ekyb detail: ", e.getMessage());
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public List<Ekyb> getEkybPage(int size, int page, String searchString) {
        try {
            return ekybRepository.getEkybPage(size, page, searchString);
        } catch (Exception e) {
            logger.error("Error while fetching ekyb page: ", e.getMessage());
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public int getEkybPageCount(String searchValue) {
        try {
            return ekybRepository.getEkybPageCount(searchValue);
        } catch (Exception e) {
            logger.error("Error while fetching ekyb page: ", e.getMessage());
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public List<Ekyb> getHistoryById(String id) {
        try {
            return ekybRepository.getHistoryById(id);
        } catch (Exception e) {
            logger.error("Error while fetching ekyb history detail: ", e.getMessage());
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public List<History> getListByAppChannel(int size, int page, String searchValue, String appChannel,
            String requestType, String statusDesc, String fromDate, String toDate) {
        try {
            return ekybRepository.getListByAppChannel(size, page, searchValue, appChannel, requestType, statusDesc,
                    fromDate, toDate);
        } catch (Exception e) {
            logger.error("Error while fetching list by channel: ", appChannel + " " + e.getMessage());
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public int getListByAppChannelCount(String searchValue, String appChannel,
            String requestType, String statusDesc, String fromDate, String toDate) {
        try {
            return ekybRepository.getListByAppChannelCount(searchValue, appChannel, requestType, statusDesc, fromDate,
                    toDate);
        } catch (Exception e) {
            logger.error("Error while fetching list count by channel: ", appChannel + " " + e.getMessage());
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public Map<String, String> getSummaryStatusByAppChannel(String searchValue, String appChannel,
            String requestType, String statusDesc, String fromDate, String toDate) {
        try {
            return ekybRepository.getSummaryStatusByAppChannel(searchValue, appChannel, requestType, statusDesc,
                    fromDate, toDate);
        } catch (Exception e) {
            logger.error("Error while fetching summary status channel: ", appChannel + " " + e.getMessage());
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public int checkEkybExisting(String type, String singleId, String tin, String companyNameEn, String companyNameKh) {
        try {
            return ekybRepository.checkEkybExisting(type, singleId, tin, companyNameEn, companyNameKh);
        } catch (Exception e) {
            logger.error("Error while fetching ekyb check exist ", e.getMessage());
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

}
