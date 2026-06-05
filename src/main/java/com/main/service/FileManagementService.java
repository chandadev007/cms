package com.main.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileManagementService {

    @Value("${cms.file-management.app-code}")
    private String appCode;
    private final String uploadUrl;
    private final String downloadUrl;
    private final RestClient restClient;

    private static final Logger logger = LoggerFactory.getLogger(FileManagementService.class);

    public FileManagementService(RestClient fileManagementRestClient, // Injected bean from config
            @Value("${cms.file-management.upload.url}") String uploadUrl,
            @Value("${cms.file-management.download.url}") String downloadUrl) {
        this.restClient = fileManagementRestClient;
        this.uploadUrl = uploadUrl;
        this.downloadUrl = downloadUrl;
    }

    public String uploadFile(MultipartFile attachment, String category) {
        if (attachment == null) {
            throw new IllegalArgumentException("No files provided for upload.");
        }
        logger.info("Initiating file upload for category: {}", category);
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            MultipartFile[] files = { attachment };

            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    try {
                        ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                            @Override
                            public String getFilename() {
                                return file.getOriginalFilename();
                            }
                        };
                        // Adding to the same key appends it as an array inside the HTTP body
                        body.add("Document", fileResource);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read file: " + file.getOriginalFilename(), e);
                    }
                }
            }
            // Using the parameters passed into the method and the injected @Value fields
            body.add("ApplicationId", appCode);
            body.add("Type", category);

            final String uploadUrl2 = uploadUrl;
            if (uploadUrl2 != null) {
                final MediaType multipart_FORM_DATA2 = MediaType.MULTIPART_FORM_DATA;
                if (multipart_FORM_DATA2 != null) {
                    JsonNode responseNode = restClient.post()
                            .uri(uploadUrl2)
                            .contentType(multipart_FORM_DATA2)
                            .body(body)
                            .retrieve()
                            .body(JsonNode.class);
                    if (responseNode == null)
                        throw new RuntimeException("Empty response from gateway Ekyc verify basic infor");

                    return responseNode.toString();
                } else {
                    logger.error("MediaType.MULTIPART_FORM_DATA is null");
                    throw new RuntimeException("MediaType.MULTIPART_FORM_DATA is null");
                }
            } else {
                logger.error("Upload URL is not configured");
                throw new RuntimeException("Upload URL is not configured");
            }
        } catch (Exception e) {
            logger.error("Error occurred while calling remote upload service: {}", e);
            throw new RuntimeException("Error occurred while calling remote upload service", e);
        }
    }

    public byte[] downloadFile(String filePath, String category) {
        logger.info("Initiating file download for category: {}, filePath: {}", category, filePath);
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("ApplicationId", appCode);
            requestBody.put("Type", category);
            requestBody.put("FilePath", filePath);

            final String downloadUrl2 = downloadUrl;
            if (downloadUrl2 != null) {
                final MediaType application_JSON2 = MediaType.APPLICATION_JSON;
                if (application_JSON2 != null) {
                    // 2. Execute POST request using fluent API
                    return restClient.post()
                            .uri(downloadUrl2)
                            .contentType(application_JSON2)
                            .body(requestBody)
                            .retrieve()
                            .body(byte[].class);
                } else {
                    logger.error("MediaType.APPLICATION_JSON is null");
                    throw new RuntimeException("MediaType.APPLICATION_JSON is null");
                }
            } else {
                logger.error("Download URL is not configured");
                throw new RuntimeException("Download URL is not configured");
            }
        } catch (Exception e) {
            logger.error("Error occurred while calling remote download service: {}", e);
            throw new RuntimeException("Error occurred while calling remote download service: ", e);
        }
    }

}