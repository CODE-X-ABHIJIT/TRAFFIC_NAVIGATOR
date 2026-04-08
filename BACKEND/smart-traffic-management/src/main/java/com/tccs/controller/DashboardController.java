package com.tccs.controller;

import com.tccs.model.dto.*;
import com.tccs.service.AnalyticsService;
import com.tccs.service.TrafficDataService;
import com.tccs.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AnalyticsService analyticsService;
    private final TrafficDataService trafficDataService;
    private final WeatherService weatherService;

    /**
     * GET /api/dashboard/summary
     * Returns complete dashboard KPIs + junction statuses.
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryDTO>> getSummary() {
        DashboardSummaryDTO summary = analyticsService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    /**
     * GET /api/dashboard/heatmap
     * Returns congestion heatmap data for Mapbox rendering.
     */
    @GetMapping("/heatmap")
    public ResponseEntity<ApiResponse<List<CongestionHeatmapPoint>>> getHeatmap() {
        List<CongestionHeatmapPoint> data = trafficDataService.getHeatmapData();
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /**
     * GET /api/dashboard/weather
     * Returns current weather conditions.
     */
    @GetMapping("/weather")
    public ResponseEntity<ApiResponse<WeatherData>> getWeather() {
        WeatherData weather = weatherService.getCurrentWeather();
        return ResponseEntity.ok(ApiResponse.ok(weather));
    }
}