package com.faceid.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecognizeRequest {

    @NotNull(message = "Image is required")
    @NotBlank(message = "Image must not be empty")
    private String imageBase64;
}
