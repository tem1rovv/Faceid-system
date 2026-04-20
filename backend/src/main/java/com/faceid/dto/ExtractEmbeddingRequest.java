package com.faceid.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request to Python /extract-embedding endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractEmbeddingRequest {
    @JsonProperty("image_base64")
    private String imageBase64;
}

