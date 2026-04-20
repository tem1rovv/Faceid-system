package com.faceid.service;

import com.faceid.dto.RecognizeRequest;
import com.faceid.dto.RecognizeResponse;
import com.faceid.model.FaceUser;
import com.faceid.model.RecognitionLog;
import com.faceid.repository.RecognitionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecognitionService {

    private final UserService userService;
    private final PythonServiceClient pythonServiceClient;
    private final RecognitionLogRepository recognitionLogRepository;

    @Value("${recognition.similarity-threshold:0.40}")
    private double similarityThreshold;

    @Transactional
    public RecognizeResponse recognize(RecognizeRequest request) {
        log.debug("Starting recognition pipeline");

        double[] queryRaw = pythonServiceClient.extractEmbedding(request.getImageBase64());
        double[] queryEmbed = EmbeddingUtils.l2Normalize(queryRaw);

        List<FaceUser> users = userService.getAllUsersWithEmbeddings();

        if (users.isEmpty()) {
            log.warn("No enrolled users in database — cannot recognise");
            return buildNoMatch(0.0, "No users enrolled in the system", request.getImageBase64());
        }

        FaceUser bestUser = null;
        double bestScore = -1.0;

        for (FaceUser user : users) {
            double[] storedEmbed = EmbeddingUtils.l2Normalize(user.getEmbedding());
            double score = EmbeddingUtils.cosineSimilarity(queryEmbed, storedEmbed);
            log.debug("User {} [{}] — similarity={}", user.getName(), user.getId(), score);

            if (score > bestScore) {
                bestScore = score;
                bestUser = user;
            }
        }

        // Step 4 — threshold check
        boolean matched = bestScore >= similarityThreshold;
        log.info("Recognition result: matched={}, bestScore={}, user={}",
                matched, bestScore, matched && bestUser != null ? bestUser.getName() : "none");

        // Step 5 — persist log
        persistLog(matched ? bestUser : null, bestScore, matched, request.getImageBase64());

        if (matched && bestUser != null) {
            return RecognizeResponse.builder()
                    .match(true)
                    .confidence(roundTo4(bestScore))
                    .message("Face recognised successfully")
                    .user(RecognizeResponse.UserInfo.builder()
                            .id(bestUser.getId())
                            .name(bestUser.getName())
                            .build())
                    .build();
        }

        return buildNoMatch(bestScore, "Face not recognised — similarity below threshold", request.getImageBase64());
    }


    private RecognizeResponse buildNoMatch(double score, String message, String imageBase64) {
        persistLog(null, score, false, imageBase64);
        return RecognizeResponse.builder()
                .match(false)
                .confidence(roundTo4(score))
                .message(message)
                .user(null)
                .build();
    }

    private void persistLog(FaceUser user, double score, boolean matched, String imageB64) {
        try {
            RecognitionLog log = RecognitionLog.builder()
                    .matchedUser(user)
                    .confidence(score)
                    .matched(matched)
                    .imageB64(imageB64)
                    .build();
            recognitionLogRepository.save(log);
        } catch (Exception e) {
            // Non-critical — log but don't fail the response
            log.error("Failed to persist recognition log: {}", e.getMessage());
        }
    }

    private double roundTo4(double d) {
        return Math.round(d * 10000.0) / 10000.0;
    }
}
