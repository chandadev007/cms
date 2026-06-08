package com.main.utilities;

public class StatusClassification {
    public static String statusConvertor(double score, String status) {
        if (status == null) {
            return "Unknow";
        }

        return switch (status.trim()) {
            case "0" -> "Submitted";
            case "1" -> "Processing";
            case "2" -> "Not Found";
            case "4" -> "Failed";
            case "3" -> {
                if (score >= 90)
                    yield "Low risk";
                if (score >= 70)
                    yield "Medium risk";
                yield "High risk";
            }
            default -> "Unknow";
        };
    }
}
