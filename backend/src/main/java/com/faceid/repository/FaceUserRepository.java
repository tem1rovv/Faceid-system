package com.faceid.repository;

import com.faceid.model.FaceUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FaceUserRepository extends JpaRepository<FaceUser, UUID> {

    Optional<FaceUser> findByName(String name);

    boolean existsByName(String name);

    /**
     * Load only id, name, embedding to avoid loading heavy image blobs.
     * We use a JPQL projection approach.
     */
    @Query("SELECT u FROM FaceUser u ORDER BY u.createdAt DESC")
    List<FaceUser> findAllOrderByCreatedAtDesc();

    @Query("SELECT u.id, u.name, u.embedding FROM FaceUser u")
    List<Object[]> findAllEmbeddings();
}
