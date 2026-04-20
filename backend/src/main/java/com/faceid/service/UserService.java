package com.faceid.service;

import com.faceid.dto.RegisterRequest;
import com.faceid.dto.RegisterResponse;
import com.faceid.dto.UserDto;
import com.faceid.model.FaceUser;
import com.faceid.repository.FaceUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final FaceUserRepository faceUserRepository;
    private final PythonServiceClient pythonServiceClient;


    @Transactional
    public RegisterResponse registerUser(RegisterRequest request) {
        log.info("Registering user: {}", request.getName());

        double[] rawEmbedding = pythonServiceClient.extractEmbedding(request.getImageBase64());
        double[] embedding = EmbeddingUtils.l2Normalize(rawEmbedding);

        FaceUser user = FaceUser.builder()
                .name(request.getName().trim())
                .embedding(embedding)
                .imageB64(stripToThumbnail(request.getImageBase64()))
                .build();

        FaceUser saved = faceUserRepository.save(user);
        log.info("User registered successfully: id={}, name={}", saved.getId(), saved.getName());

        return RegisterResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .message("User registered successfully")
                .registeredAt(saved.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return faceUserRepository.findAllOrderByCreatedAtDesc()
                .stream()
                .map(u -> UserDto.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .imageB64(u.getImageB64())
                        .createdAt(u.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FaceUser> getAllUsersWithEmbeddings() {
        return faceUserRepository.findAllOrderByCreatedAtDesc();
    }

    @Transactional
    public void deleteUser(UUID id) {
        if (!faceUserRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found with id: " + id);
        }
        faceUserRepository.deleteById(id);
        log.info("Deleted user id={}", id);
    }

    private String stripToThumbnail(String imageBase64) {
        // Store as-is; the Python service already works with it.
        // In production you would resize to ~100x100 here.
        return imageBase64;
    }
}
