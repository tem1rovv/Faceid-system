package com.faceid.controller;

import com.faceid.dto.RecognizeRequest;
import com.faceid.dto.RecognizeResponse;
import com.faceid.service.RecognitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class RecognitionController {

    private final RecognitionService recognitionService;

    @PostMapping("/recognize")
    public ResponseEntity<RecognizeResponse> recognize(@Valid @RequestBody RecognizeRequest request) {
        log.debug("POST /api/recognize");
        RecognizeResponse response = recognitionService.recognize(request);
        return ResponseEntity.ok(response);
    }
}
