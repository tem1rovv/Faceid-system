package com.faceid.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class RegisterResponse {
    private UUID id;
    private String name;
    private String message;
    private OffsetDateTime registeredAt;
}
