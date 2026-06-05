package com.main.service;

import com.main.model.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TokenValidationService {

    @Value("${cms.app-management.url}")
    private String remoteUrl;

    private static final Logger logger = LoggerFactory.getLogger(TokenValidationService.class);
    private final RestClient restClient;
    private final ObjectMapper mapper;

    // Inject beans for better performance
    public TokenValidationService(RestClient appManagementRestClient, ObjectMapper mapper) {
        this.restClient = appManagementRestClient;
        this.mapper = mapper;
    }

    public String userValidation(String userId, String token) {
        if (token == null || token.isEmpty() || userId == null || userId.isEmpty()) {
            return buildErrorJson("Token and User ID are required");
        }
        logger.info("Initiating Token validation for: {}", userId);

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
            logger.info("Exception during token validation for userId {}: {}", userId, e);
            return buildErrorJson("Validation Error: " + e.getMessage());
        }
    }

    /**
     * FIX: Use Object.class instead of JsonNode.class to avoid abstract class
     * instantiation error.
     */
    private JsonNode callRemoteService(String msgId, String userId, String token) throws Exception {

        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("messageId", msgId);
        requestMap.put("userId", userId);
        if (token != null) {
            requestMap.put("jwtToken", token);
        }

        final String remoteUrl2 = remoteUrl;
        if (remoteUrl2 != null) {
            final MediaType application_JSON2 = MediaType.APPLICATION_JSON;
            if (application_JSON2 != null) {
                JsonNode response = restClient.post()
                        .uri(remoteUrl2)
                        .contentType(application_JSON2)
                        .body(requestMap)
                        .retrieve()
                        .body(JsonNode.class);
                return (response == null) ? JsonNodeFactory.instance.objectNode() : response;
            } else {
                return JsonNodeFactory.instance.objectNode();
            }
        } else {
            return JsonNodeFactory.instance.objectNode();
        }
    }

    private String buildErrorJson(String detail) {
        // Using a Map for error JSON is safer than String concatenation to handle
        // special characters
        try {
            Map<String, String> error = new HashMap<>();
            error.put("error", "2");
            error.put("errorDetail", detail);
            return mapper.writeValueAsString(error);
        } catch (Exception e) {
            return "{\"error\":\"2\", \"errorDetail\":\"" + detail + "\"}";
        }
    }
}