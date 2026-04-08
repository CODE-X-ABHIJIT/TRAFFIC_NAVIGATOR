package com.tccs.controller;

import com.tccs.model.dto.ApiResponse;
import com.tccs.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * GET /api/analytics/congestion-trends?junctionCode=J001&hours=6
     */
    @GetMapping("/congestion-trends")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCongestionTrends(
            @RequestParam(required = false) String junctionCode,
            @RequestParam(defaultValue = "6") int hours) {
        return ResponseEntity.ok(ApiResponse.ok(
                analyticsService.getCongestionTrends(junctionCode, hours)));
    }

    /**
     * GET /api/analytics/peak-hours?days=7
     */
    @GetMapping("/peak-hours")
    public ResponseEntity<ApiResponse<Map<Integer, Double>>> getPeakHours(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(ApiResponse.ok(
                analyticsService.getPeakHourAnalysis(days)));
    }
}