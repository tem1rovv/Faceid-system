package com.faceid.service;

import com.faceid.config.NoFaceDetectedException;
import com.faceid.config.PythonServiceException;
import com.faceid.dto.ExtractEmbeddingRequest;
import com.faceid.dto.ExtractEmbeddingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class PythonServiceClient {

    @Qualifier("pythonRestTemplate")
    private final RestTemplate pythonRestTemplate;

    @Value("${python.service.url}")
    private String pythonServiceUrl;

    public double[] extractEmbedding(String imageBase64) {
        String url = pythonServiceUrl + "/extract-embedding";
        log.debug("Calling Python extract-embedding at {}", url);

        ExtractEmbeddingRequest request = ExtractEmbeddingRequest.builder()
                .imageBase64(stripDataUriPrefix(imageBase64))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ExtractEmbeddingRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<ExtractEmbeddingResponse> response =
                    pythonRestTemplate.exchange(url, HttpMethod.POST, entity, ExtractEmbeddingResponse.class);

            ExtractEmbeddingResponse body = response.getBody();
            if (body == null) {
                throw new PythonServiceException("Empty response from Python service");
            }

            if (!body.isFaceDetected()) {
                throw new NoFaceDetectedException(
                        body.getMessage() != null ? body.getMessage() : "No face detected in the image");
            }

            if (!body.isSuccess() || body.getEmbedding() == null) {
                throw new PythonServiceException(
                        body.getMessage() != null ? body.getMessage() : "Embedding extraction failed");
            }

            log.debug("Embedding extracted successfully, dimension={}", body.getEmbedding().length);
            return body.getEmbedding();

        } catch (NoFaceDetectedException | PythonServiceException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            log.error("Python service HTTP error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new PythonServiceException("Python service returned error: " + e.getStatusCode());
        } catch (ResourceAccessException e) {
            log.error("Cannot reach Python service at {}: {}", url, e.getMessage());
            throw new PythonServiceException("Cannot reach face recognition service. Is it running?", e);
        } catch (Exception e) {
            log.error("Unexpected error calling Python service: {}", e.getMessage(), e);
            throw new PythonServiceException("Unexpected error communicating with face recognition service", e);
        }
    }

    /**
     * Strips data URI prefix (e.g. "data:image/jpeg;base64,") from base64 string.
     */
    private String stripDataUriPrefix(String imageBase64) {
        if (imageBase64 == null) return null;
        int commaIdx = imageBase64.indexOf(',');
        if (commaIdx >= 0) {
            return imageBase64.substring(commaIdx + 1);
        }
        return imageBase64;
    }
}
