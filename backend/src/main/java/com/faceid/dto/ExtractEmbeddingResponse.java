package com.faceid.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response from Python /extract-embedding endpoint.
 */
@Data
@NoArgsConstructor
public class ExtractEmbeddingResponse {

    private boolean success;
    private double[] embedding;

    @JsonProperty("face_detected")
    private boolean faceDetected;

    private String message;

    @JsonProperty("face_count")
    private int faceCount;
}
