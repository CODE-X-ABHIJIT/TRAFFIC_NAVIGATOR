package com.tccs.repository;

import com.tccs.model.entity.TrafficLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrafficLogRepository extends JpaRepository<TrafficLog, Long> {

    List<TrafficLog> findByJunctionCodeOrderByRecordedAtDesc(String junctionCode);

    @Query("SELECT t FROM TrafficLog t WHERE t.junctionCode = :code " +
           "AND t.recordedAt BETWEEN :from AND :to ORDER BY t.recordedAt ASC")
    List<TrafficLog> findByJunctionCodeAndTimeRange(
            @Param("code") String junctionCode,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT t FROM TrafficLog t WHERE t.recordedAt BETWEEN :from AND :to " +
           "ORDER BY t.recordedAt ASC")
    List<TrafficLog> findAllByTimeRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT AVG(t.congestionLevel) FROM TrafficLog t " +
           "WHERE t.junctionCode = :code AND t.recordedAt >= :since")
    Double findAvgCongestionSince(
            @Param("code") String junctionCode,
            @Param("since") LocalDateTime since);
}