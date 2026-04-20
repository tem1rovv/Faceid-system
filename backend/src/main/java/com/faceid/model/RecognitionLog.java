package com.faceid.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "recognition_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecognitionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_user_id")
    private FaceUser matchedUser;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "matched", nullable = false)
    private boolean matched;

    @Column(name = "image_b64", columnDefinition = "TEXT")
    private String imageB64;

    @CreationTimestamp
    @Column(name = "recognized_at", updatable = false)
    private OffsetDateTime recognizedAt;
}
