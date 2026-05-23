package com.main.service;

import com.main.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TokenValidationService {

    @Value("${cms.app-management.url}")
    private String remoteUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    // Inject beans for better performance
    public TokenValidationService(ObjectMapper mapper) {
        this.restTemplate = new RestTemplate();
        this.mapper = mapper;
    }

    public String userValidation(String userId, String token) {
        if (token == null || token.isEmpty() || userId == null || userId.isEmpty()) {
            return buildErrorJson("Token and User ID are required");
        }

        try {
            // 1. Validate Token
            JsonNode tokenResponse = callRemoteService("02", userId, token);
            if (!"0".equalsIgnoreCase(tokenResponse.path("errorCode").asText(""))) {
                return buildErrorJson(tokenResponse.path("errorDetail").asText("Invalid user credential"));
            }

            // 2. Get User Info
            JsonNode userInfoNode = callRemoteService("04", userId, null);
            if (!"0".equalsIgnoreCase(userInfoNode.path("errorCode").asText(""))) {
                return buildErrorJson(userInfoNode.path("errorDetail").asText("User information retrieval failed"));
            }

            // 3. Map to Object Smoothly
            UserInformation responseObj = new UserInformation();
            responseObj.setError("0");
            responseObj.setTokenStatus("0");
            responseObj.setErrorDetail("User information retrieved successfully");

            UserData userData = new UserData();
            
            // Use path() and treeToValue safely
            if (userInfoNode.has("userInfo")) {
                userData.setUserInfo(mapper.treeToValue(userInfoNode.path("userInfo"), UserInfo.class));
            }
            
            // Optimized Collection Mapping
            userData.setListUnitInCharge(mapper.convertValue(userInfoNode.path("listUnitInCharge"),
                    mapper.getTypeFactory().constructCollectionType(List.class, UserInChargeUnit.class)));
            
            userData.setApplications(mapper.convertValue(userInfoNode.path("applications"),
                    mapper.getTypeFactory().constructCollectionType(List.class, UserInchargeApp.class)));

            responseObj.setData(userData);
            return mapper.writeValueAsString(responseObj);

        } catch (Exception e) {
            e.printStackTrace();
            return buildErrorJson("Validation Error: " + e.getMessage());
        }
    }

    /**
     * FIX: Use Object.class instead of JsonNode.class to avoid abstract class instantiation error.
     */
    private JsonNode callRemoteService(String msgId, String userId, String token) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("messageId", msgId);
        requestMap.put("userId", userId);
        if (token != null) {
            requestMap.put("jwtToken", token);
        }

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestMap, headers);

        // Fetch as a generic Object (usually maps to a LinkedHashMap internally)
        Object response = restTemplate.postForObject(remoteUrl, entity, Object.class);

        // Convert the generic Object into a JsonNode safely
        return (response == null) ? mapper.createObjectNode() : mapper.valueToTree(response);
    }

    private String buildErrorJson(String detail) {
        // Using a Map for error JSON is safer than String concatenation to handle special characters
        try {
            Map<String, String> error = new HashMap<>();
            error.put("error", "1");
            error.put("errorDetail", detail);
            return mapper.writeValueAsString(error);
        } catch (Exception e) {
            return "{\"error\":\"1\", \"errorDetail\":\"" + detail + "\"}";
        }
    }
}