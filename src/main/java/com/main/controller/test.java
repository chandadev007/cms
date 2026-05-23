package com.main.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class test {
    public static void main(String[] args) {

        String jsoString = "{\r\n" + //
                "    \"errorCode\": \"00\",\r\n" + //
                "    \"errorDetail\": \"\",\r\n" + //
                "    \"data\": [\r\n" + //
                "        {\r\n" + //
                "            \"fileName\": \"3.CSS - Reconciliation Process_v0.7 (5).pdf\",\r\n" + //
                "            \"filePath\": \"/20250901/121426500_OTHER_3.CSS - Reconciliation Process_v0.7 (5).pdf\"\r\n"
                + //
                "        }\r\n" + //
                "    ]\r\n" + //
                "}";

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode data = objectMapper.readTree(jsoString);

            String tt = data.path("data").asText();

            System.out.println(tt);

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }

    }
}
