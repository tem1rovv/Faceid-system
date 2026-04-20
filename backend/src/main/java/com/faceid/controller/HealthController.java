package com.faceid.controller;

import com.faceid.repository.FaceUserRepository;
import com.faceid.repository.RecognitionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final FaceUserRepository faceUserRepository;
    private final RecognitionLogRepository recognitionLogRepository;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", OffsetDateTime.now(),
                "service", "FaceID Backend"
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        long totalUsers    = faceUserRepository.count();
        long totalMatched  = recognitionLogRepository.countByMatchedTrue();
        long totalRejected = recognitionLogRepository.countByMatchedFalse();

        return ResponseEntity.ok(Map.of(
                "enrolledUsers", totalUsers,
                "successfulRecognitions", totalMatched,
                "failedRecognitions", totalRejected,
                "timestamp", OffsetDateTime.now()
        ));
    }
}
