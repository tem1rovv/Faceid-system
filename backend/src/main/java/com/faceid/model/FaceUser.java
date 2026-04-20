package com.faceid.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "face_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaceUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;


    @Column(name = "embedding", nullable = false, columnDefinition = "DOUBLE PRECISION[]")
    private double[] embedding;

    /**
     * Optional: base64 encoded face image for display in UI.
     */
    @Column(name = "image_b64", columnDefinition = "TEXT")
    private String imageB64;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
