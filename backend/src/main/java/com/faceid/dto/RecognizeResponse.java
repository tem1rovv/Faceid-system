package com.faceid.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class RecognizeResponse {
    private boolean match;
    private UserInfo user;
    private double confidence;
    private String message;

    @Data
    @Builder
    public static class UserInfo {
        private UUID id;
        private String name;
    }
}
