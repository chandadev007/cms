package com.main.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.main.model.UserInchargeApp;
import com.main.model.UserInformation;
import com.main.model.UserRole;
import com.main.service.DBLogService;
import com.main.service.EkybService;
import com.main.service.EkycService;
import com.main.service.FileManagementService;
import com.main.service.TokenValidationService;

import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/cms")
public class BaseApi {

    private EkycController ekycController;
    private EkybController ekybController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TokenValidationService tokenValidationService;

    public BaseApi(TokenValidationService tokenValidationService, EkycService ekycService, EkybService ekybService,
            DBLogService dbLogService, FileManagementService fileManagementService) {
        this.tokenValidationService = tokenValidationService;

        this.ekycController = new EkycController(ekycService, dbLogService, fileManagementService);
        this.ekybController = new EkybController(ekybService, dbLogService);
    }

    @PostMapping(value = "/doProcess")
    public ResponseEntity<?> doProcessMethod(
            @RequestBody(required = false) String entity,
            @RequestPart(value = "payload", required = false) String payload,
            @RequestPart(value = "Document", required = false) MultipartFile attachment,
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 1. Safe Token Splitting (Protects against Null/Empty headers)
        String token = tokenSplitting(authHeader);
        if (token.isEmpty()) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Missing or invalid authorization token");
        }

        try {
            // 2. Parse payload source cleanly
            String rawJson = (entity == null || entity.isEmpty()) ? payload : entity;
            if (rawJson == null || rawJson.isEmpty()) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "Payload body or part is missing");
            }

            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode header = root.path("header");
            JsonNode data = root.path("data");

            if (header.isMissingNode() || data.isMissingNode() || !data.has("userInfo")) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid Information Structure");
            }

            String userId = data.at("/userInfo/employeeId").asText(null);
            String msgId = header.path("msgId").asText(null);

            if (userId == null || msgId == null) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "Missing employeeId or msgId");
            }

            // 3. Token Validation Call
            String userInfo = tokenValidationService.userValidation(userId, token);
            JsonNode tokenNode = objectMapper.readTree(userInfo);

            // 4. CHECK ERROR RESPONSE FIRST before trying to parse user detail fields
            if (!"0".equalsIgnoreCase(tokenNode.path("error").asText(""))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(userInfo);
            }

            // 5. Parse Data Safely now that token status is valid
            UserInformation userInformation = objectMapper.treeToValue(tokenNode, UserInformation.class);
            List<UserRole> userRoles = Optional.ofNullable(userInformation)
                    .map(UserInformation::getData)
                    .map(userData -> userData.getApplications())
                    .stream()
                    .flatMap(List::stream)
                    .filter(app -> "MTR".equalsIgnoreCase(app.getAppCode()))
                    .findFirst()
                    .map(UserInchargeApp::getRoles)
                    .orElse(Collections.emptyList());

            JsonNode tokenDataNode = tokenNode.path("data");
            String userName = "";
            String branchId = "";
            String unitCode = "";

            if (tokenDataNode.has("userInfo")) {
                JsonNode userNode = tokenDataNode.path("userInfo");
                String lastName = userNode.path("lastName").asText("").trim();
                String firstName = userNode.path("firstName").asText("").trim();
                branchId = userNode.path("branchId").asText("");
                unitCode = userNode.path("unit").asText("");
                userName = (lastName + " " + firstName).trim();
            }

            return switch (msgId) {
                case "cms_101" -> {
                    yield ekycController.cms_101(data, attachment, unitCode, userId, userName);
                }

                case "cms_201" -> {
                    yield ekybController.cms_201(data, unitCode, userId, userName);
                }

                default -> buildErrorResponse(HttpStatus.BAD_REQUEST, "The process are not matching");
            };

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error Internal Server");
        }
    }

    // Fixed NullPointer bug and messy loops using standard string tokens
    private String tokenSplitting(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return "";
        }
        // Splits away 'Bearer ' prefix and isolates the raw token string
        return authorizationHeader.substring(7).split("\r\n")[0].trim();
    }

    private ResponseEntity<Map<String, String>> buildErrorResponse(HttpStatus status, String detail) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("error", "1", "errorDetail", detail));
    }
}