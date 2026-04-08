package com.tccs.controller;

import com.tccs.model.dto.ApiResponse;
import com.tccs.model.entity.Alert;
import com.tccs.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    /**
     * GET /api/alerts
     * Get recent alerts (last 50).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Alert>>> getRecentAlerts() {
        return ResponseEntity.ok(
                ApiResponse.ok(alertService.getRecentAlerts()));
    }

    /**
     * GET /api/alerts/unacknowledged
     * Get all unacknowledged alerts.
     */
    @GetMapping("/unacknowledged")
    public ResponseEntity<ApiResponse<List<Alert>>> getUnacknowledged() {
        return ResponseEntity.ok(
                ApiResponse.ok(alertService.getUnacknowledgedAlerts()));
    }

    /**
     * GET /api/alerts/count
     * Unacknowledged alert count.
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getAlertCount() {
        long count = alertService.getUnacknowledgedCount();
        return ResponseEntity.ok(
                ApiResponse.ok(Map.of("unacknowledged", count)));
    }

    /**
     * PUT /api/alerts/{id}/acknowledge
     * Acknowledge a single alert.
     */
    @PutMapping("/{id}/acknowledge")
    public ResponseEntity<ApiResponse<Alert>> acknowledgeAlert(@PathVariable Long id) {
        Alert alert = alertService.acknowledgeAlert(id);
        return ResponseEntity.ok(ApiResponse.ok("Alert acknowledged", alert));
    }

    /**
     * PUT /api/alerts/acknowledge-all
     * Acknowledge all alerts.
     */
    @PutMapping("/acknowledge-all")
    public ResponseEntity<ApiResponse<String>> acknowledgeAll() {
        alertService.acknowledgeAll();
        return ResponseEntity.ok(ApiResponse.ok("All alerts acknowledged"));
    }
}