package com.faceid.repository;

import com.faceid.model.RecognitionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecognitionLogRepository extends JpaRepository<RecognitionLog, UUID> {

    @Query("SELECT r FROM RecognitionLog r LEFT JOIN FETCH r.matchedUser ORDER BY r.recognizedAt DESC")
    List<RecognitionLog> findRecentLogs();

    @Query("SELECT r FROM RecognitionLog r LEFT JOIN FETCH r.matchedUser " +
           "WHERE r.matchedUser.id = :userId ORDER BY r.recognizedAt DESC")
    List<RecognitionLog> findByUserId(@Param("userId") UUID userId);

    long countByMatchedTrue();
    long countByMatchedFalse();
}
