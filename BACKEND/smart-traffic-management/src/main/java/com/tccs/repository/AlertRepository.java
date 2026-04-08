package com.tccs.repository;

import com.tccs.model.entity.Alert;
import com.tccs.model.enums.AlertSeverity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByAcknowledgedFalseOrderByCreatedAtDesc();

    List<Alert> findByJunctionCode(String junctionCode);

    List<Alert> findBySeverityAndAcknowledgedFalse(AlertSeverity severity);

    long countByAcknowledgedFalse();

    List<Alert> findTop50ByOrderByCreatedAtDesc();
}