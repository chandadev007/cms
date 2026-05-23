package com.main.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileManagementService {

    @Value("${cms.file-management.app-code}")
    private String appCode;
    @Value("${cms.file-management.upload.url}")
    private String uploadUrl;
    @Value("${cms.file-management.download.url}")
    private String downloadUrl;
    @Value("${cms.file-management.user}")
    private String user;
    @Value("${cms.file-management.password}")
    private String password;

    public String uploadFile(MultipartFile attachment, String category) {

        if (attachment == null) {
            throw new IllegalArgumentException("No files provided for upload.");
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Optional: Adding Basic Auth if your remote URL requires the 'user' and
        // 'password' for authentication
        headers.setBasicAuth(user, password);

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

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        try {
            String response = restTemplate.postForObject(uploadUrl, requestEntity, String.class);
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while calling remote upload service", e);
        }
    }

    public byte[] downloadFile(String filePath, String category) {
        RestTemplate restTemplate = new RestTemplate();

        // 1. Set Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(user, password);

        // 2. Create Request Body
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("ApplicationId", appCode);
        requestBody.put("Type", category);
        requestBody.put("FilePath", filePath);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            return restTemplate.postForObject(downloadUrl, requestEntity, byte[].class);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while calling remote download service: " + e.getMessage(), e);
        }
    }

}