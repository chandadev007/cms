package com.main.controller;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
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
    private static final Logger logger = LoggerFactory.getLogger(BaseApi.class);

    @Value("${cms.file-management.app-code}")
    private String AppCode;
    @Value("${cms.app.role.csd_inputter_01}")
    private String CSD_Inputter_01;
    @Value("${cms.app.role.csd_inputter_02}")
    private String CSD_Inputter_02;
    @Value("${cms.app.role.lld_inputter_01}")
    private String LLD_Inputter_01;
    @Value("${cms.app.role.lld_inputter_02}")
    private String LLD_Inputter_02;

    private final FileManagementService fileManagementService;
    private EkycController ekycController;
    private EkybController ekybController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TokenValidationService tokenValidationService;

    public BaseApi(TokenValidationService tokenValidationService, EkycService ekycService, EkybService ekybService,
            DBLogService dbLogService, FileManagementService fileManagementService) {
        this.tokenValidationService = tokenValidationService;

        this.ekycController = new EkycController(ekycService, ekybService, dbLogService, fileManagementService);
        this.ekybController = new EkybController(ekybService, ekycService, dbLogService);
        this.fileManagementService = fileManagementService;
    }

    @PostMapping(value = "/doProcess")
    public ResponseEntity<?> doProcessMethod(
            @RequestBody(required = false) String entity,
            @RequestPart(value = "payload", required = false) String payload,
            @RequestPart(value = "attachment", required = false) MultipartFile attachment,
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 1. Safe Token Splitting (Protects against Null/Empty headers)
        String token = tokenSplitting(authHeader);
        if (token.isEmpty()) {
            return buildErrorResponse("Missing or invalid authorization token");
        }

        try {
            // 2. Parse payload source cleanly
            String rawJson = (entity == null || entity.isEmpty()) ? payload : entity;
            if (rawJson == null || rawJson.isEmpty()) {
                return buildErrorResponse("Payload body or part is missing");
            }

            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode header = root.path("header");
            JsonNode data = root.path("data");

            if (header.isMissingNode() || data.isMissingNode() || !data.has("userInfo")) {
                return buildErrorResponse("Invalid Information Structure");
            }

            String userId = data.at("/userInfo/employeeId").asText(null);
            String msgId = header.path("msgId").asText(null);

            if (userId == null || msgId == null) {
                return buildErrorResponse("Missing employeeId or msgId");
            }

            // 3. Token Validation Call
            String userInfo = tokenValidationService.userValidation(userId, token);
            JsonNode tokenNode = objectMapper.readTree(userInfo);

            // 4. CHECK ERROR RESPONSE FIRST before trying to parse user detail fields
            if (!"0".equalsIgnoreCase(tokenNode.path("error").asText(""))) {
                return ResponseEntity.status(HttpStatus.OK)
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
                    .filter(app -> AppCode.equalsIgnoreCase(app.getAppCode()))
                    .findFirst()
                    .map(UserInchargeApp::getRoles)
                    .orElse(Collections.emptyList());

            JsonNode tokenDataNode = tokenNode.path("data");
            String userName = "";
            String unitCode = "";

            if (tokenDataNode.has("userInfo")) {
                JsonNode userNode = tokenDataNode.path("userInfo");
                String lastName = userNode.path("lastName").asText("").trim();
                String firstName = userNode.path("firstName").asText("").trim();
                // branchId = userNode.path("branchId").asText("");
                unitCode = userNode.path("unit").asText("");
                userName = (lastName + " " + firstName).trim();
            }

            return switch (msgId) {
                // cms_101: Ekyc create
                case "cms_101" -> {

                    String type = data.path("type").asText();

                    if ("1".equalsIgnoreCase(type)) {
                        long count = Optional.ofNullable(userRoles)
                                .orElse(Collections.emptyList())
                                .stream()
                                .filter(role -> (CSD_Inputter_01.equalsIgnoreCase(role.getRoleCode())
                                        || CSD_Inputter_02.equalsIgnoreCase(role.getRoleCode())
                                        || LLD_Inputter_01.equalsIgnoreCase(role.getRoleCode())
                                        || LLD_Inputter_02.equalsIgnoreCase(role.getRoleCode())))
                                .count();

                        if (count > 0) {
                            yield ekycController.cms_101(data, attachment, unitCode, userId, userName);
                        } else {
                            yield buildErrorResponse("User has no permission to access");
                        }
                    } else if ("2".equalsIgnoreCase("2")) {
                        long count = Optional.ofNullable(userRoles)
                                .orElse(Collections.emptyList())
                                .stream()
                                .filter(role -> (CSD_Inputter_02.equalsIgnoreCase(role.getRoleCode())
                                        || LLD_Inputter_02.equalsIgnoreCase(role.getRoleCode())))
                                .count();
                        if (count > 0) {
                            yield ekycController.cms_101(data, attachment, unitCode, userId, userName);
                        } else {
                            yield buildErrorResponse("User has no permission to access");
                        }
                    } else {
                        yield buildErrorResponse("Verify Type is requird to select");
                    }
                }

                // cms_102: Ekyc page
                case "cms_102" -> {
                    yield ekycController.cms_102(data);
                }

                // cms_103: Ekyc detail
                case "cms_103" -> {
                    yield ekycController.cms_103(data);
                }

                // cms_104: History
                case "cms_104" -> {
                    yield ekycController.cms_104(data, userId);
                }

                // cms_105: History detail
                case "cms_105" -> {
                    yield ekycController.cms_105(data);
                }

                // cms_106: All transaction
                case "cms_106" -> {
                    yield ekycController.cms_106(data);
                }

                // cms_107: Dashboad
                case "cms_107" -> {
                    yield ekycController.cms_107(data);
                }

                // cms_108: Report
                case "cms_108" -> {
                    yield ekycController.cms_108(data);
                }

                // cms_201: Ekyb create
                case "cms_201" -> {

                    long count = Optional.ofNullable(userRoles)
                            .orElse(Collections.emptyList())
                            .stream()
                            .filter(role -> (CSD_Inputter_01.equalsIgnoreCase(role.getRoleCode())
                                    || CSD_Inputter_02.equalsIgnoreCase(role.getRoleCode())
                                    || LLD_Inputter_01.equalsIgnoreCase(role.getRoleCode())
                                    || LLD_Inputter_02.equalsIgnoreCase(role.getRoleCode())))
                            .count();

                    if (count > 0) {
                        yield ekybController.cms_201(data, unitCode, userId, userName);
                    } else {
                        yield buildErrorResponse("User has no permission to access");
                    }
                }

                // cms_202: Ekyb detail
                case "cms_202" -> {
                    yield ekybController.cms_202(data);
                }

                // cms_203: Ekyb page
                case "cms_203" -> {
                    yield ekybController.cms_203(data);
                }

                // cms_204: OSA list
                case "cms_204" -> {
                    yield ekybController.cms_204(data);
                }

                // cms_205: OSA view detail
                case "cms_205" -> {
                    yield ekybController.cms_205(data);
                }

                // cms_206: Mobile app data
                case "cms_206" -> {
                    yield ekybController.cms_206(data);
                }

                // cms_207: Mobile view detail
                case "cms_207" -> {
                    yield ekybController.cms_207(data);
                }

                // cms_208: T24 list
                case "cms_208" -> {
                    yield ekybController.cms_208(data);
                }

                // cms_209: T24 view detail
                case "cms_209" -> {
                    yield ekybController.cms_209(data);
                }

                // cms_download
                case "cms_download" -> {
                    try {
                        String filePath = data.path("filePath").asText();
                        if (filePath == null || filePath.isEmpty()) {
                            yield buildErrorResponse("File Path is required for download");
                        }
                        byte[] binaryFile = fileManagementService.downloadFile(filePath, "selfie");
                        ByteArrayResource resource = new ByteArrayResource(binaryFile);
                        String contentType = "application/octet-stream";

                        yield ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(contentType))
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                        "attachment; filename=\"" + resource.getFilename() + "\"")
                                .body(Base64.getEncoder().encodeToString(binaryFile));

                    } catch (Exception e) {
                        yield buildErrorResponse("file management gateway timeout");
                    }
                }
                default -> buildErrorResponse("The process are not matching");
            };

        } catch (Exception e) {
            logger.error("Error Internal Server: ", e);
            return buildErrorResponse("Error Internal Server");
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

    private ResponseEntity<Map<String, String>> buildErrorResponse(String detail) {
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("error", "1", "errorDetail", detail));
    }
}