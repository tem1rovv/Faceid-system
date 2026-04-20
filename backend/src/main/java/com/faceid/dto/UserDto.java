package com.faceid.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class UserDto {
    private UUID id;
    private String name;
    private String imageB64;
    private OffsetDateTime createdAt;
}
