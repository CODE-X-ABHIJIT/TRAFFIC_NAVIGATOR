package com.tccs.service;

import com.tccs.engine.CongestionDetector;
import com.tccs.exception.ResourceNotFoundException;
import com.tccs.model.entity.Alert;
import com.tccs.model.entity.Junction;
import com.tccs.repository.AlertRepository;
import com.tccs.repository.JunctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final JunctionRepository junctionRepository;
    private final CongestionDetector congestionDetector;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Scans all junctions and generates alerts for congestion issues.
     * Called periodically by scheduler.
     */
    @Transactional
    public void scanAndGenerateAlerts() {
        List<Junction> junctions = junctionRepository.findByActiveTrue();

        for (Junction junction : junctions) {
            List<Alert> newAlerts = congestionDetector.analyzeJunction(
                    junction.getJunctionCode(), junction.getSignals());

            for (Alert alert : newAlerts) {
                alertRepository.save(alert);
                messagingTemplate.convertAndSend("/topic/alerts", alert);
            }

            if (!newAlerts.isEmpty()) {
                log.info("Generated {} alerts for junction {}",
                        newAlerts.size(), junction.getJunctionCode());
            }
        }
    }

    /**
     * Get all unacknowledged alerts.
     */
    public List<Alert> getUnacknowledgedAlerts() {
        return alertRepository.findByAcknowledgedFalseOrderByCreatedAtDesc();
    }

    /**
     * Get recent alerts (last 50).
     */
    public List<Alert> getRecentAlerts() {
        return alertRepository.findTop50ByOrderByCreatedAtDesc();
    }

    /**
     * Acknowledge an alert.
     */
    @Transactional
    public Alert acknowledgeAlert(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Alert not found: " + alertId));

        alert.setAcknowledged(true);
        alert.setAcknowledgedAt(LocalDateTime.now());
        return alertRepository.save(alert);
    }

    /**
     * Acknowledge all alerts.
     */
    @Transactional
    public void acknowledgeAll() {
        List<Alert> unacked = alertRepository
                .findByAcknowledgedFalseOrderByCreatedAtDesc();
        for (Alert a : unacked) {
            a.setAcknowledged(true);
            a.setAcknowledgedAt(LocalDateTime.now());
        }
        alertRepository.saveAll(unacked);
        log.info("Acknowledged {} alerts", unacked.size());
    }

    /**
     * Count of unacknowledged alerts.
     */
    public long getUnacknowledgedCount() {
        return alertRepository.countByAcknowledgedFalse();
    }
}