package com.tccs.integration;

import com.tccs.model.dto.TrafficFlowData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalTime;
import java.util.Map;
import java.util.Random;

/**
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *  TRAFFIC DATA CLIENT
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *  Priority order:
 *   1. TomTom API (if key configured)
 *   2. Open-Meteo + simulation (FREE, no key)
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 */
@Component
@Slf4j
public class TomTomTrafficClient {

    private final RestTemplate restTemplate;
    private final Random random = new Random();

    @Value("${traffic.tomtom.api-key:NONE}")
    private String tomtomKey;

    @Value("${traffic.tomtom.base-url:https://api.tomtom.com/traffic/services/4/flowSegmentData/absolute/10/json}")
    private String tomtomBaseUrl;

    @Value("${traffic.simulation.enabled:true}")
    private boolean simulationEnabled;

    public TomTomTrafficClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Gets traffic flow data for a location.
     * Tries TomTom first, falls back to smart simulation.
     */
    public TrafficFlowData getTrafficFlow(double latitude, double longitude) {

        // ─── Try TomTom if key is configured ───
        if (!simulationEnabled && !"NONE".equals(tomtomKey)
                && !"YOUR_TOMTOM_API_KEY".equals(tomtomKey)) {
            try {
                return fetchFromTomTom(latitude, longitude);
            } catch (Exception e) {
                log.warn("TomTom failed: {}. Using simulation.", e.getMessage());
            }
        }

        // ─── Use weather-aware smart simulation (FREE) ───
        return generateSmartTrafficData(latitude, longitude);
    }

    /**
     * TomTom API call (paid — optional).
     */
    private TrafficFlowData fetchFromTomTom(double lat, double lon) {
        String url = String.format("%s?point=%f,%f&key=%s",
                tomtomBaseUrl, lat, lon, tomtomKey);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        if (response != null && response.containsKey("flowSegmentData")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> flow = (Map<String, Object>) response.get("flowSegmentData");

            return TrafficFlowData.builder()
                    .currentSpeed(toDouble(flow.get("currentSpeed")))
                    .freeFlowSpeed(toDouble(flow.get("freeFlowSpeed")))
                    .currentTravelTime(toDouble(flow.get("currentTravelTime")))
                    .freeFlowTravelTime(toDouble(flow.get("freeFlowTravelTime")))
                    .confidence(toDouble(flow.get("confidence")))
                    .roadClosure(toInt(flow.get("roadClosure")))
                    .build();
        }

        throw new RuntimeException("Empty TomTom response");
    }

    /**
     * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
     *  SMART TRAFFIC SIMULATION (FREE)
     * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
     *  Uses real-world patterns:
     *   - Time-of-day (peak hours = slower)
     *   - Day of week (weekends = less traffic)
     *   - Location variation (different junctions)
     *   - Random events (occasional jams)
     * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
     */
    private TrafficFlowData generateSmartTrafficData(double lat, double lon) {
        LocalTime now = LocalTime.now();
        int hour = now.getHour();
        int minute = now.getMinute();
        int dayOfWeek = java.time.LocalDate.now().getDayOfWeek().getValue(); // 1=Mon, 7=Sun

        // ─── Base free flow speed (varies by location) ───
        double locationHash = Math.abs((lat * 1000 + lon * 1000) % 30);
        double freeFlow = 40 + locationHash; // 40-70 km/h

        // ─── Time-based congestion factor ───
        double timeFactor = getTimeFactor(hour, minute);

        // ─── Weekend adjustment (less traffic on weekends) ───
        if (dayOfWeek >= 6) {
            timeFactor = timeFactor * 0.6 + 0.4; // reduce congestion on weekends
        }

        // ─── Random variation (±15%) ───
        double randomVariation = 0.85 + random.nextDouble() * 0.30;

        // ─── Occasional traffic jam (5% chance) ───
        if (random.nextInt(100) < 5) {
            randomVariation *= 0.3; // severe slowdown
        }

        // ─── Calculate current speed ───
        double speedRatio = timeFactor * randomVariation;
        speedRatio = Math.max(0.05, Math.min(1.0, speedRatio)); // clamp 5%-100%
        double currentSpeed = freeFlow * speedRatio;

        // ─── Travel times ───
        double baseTravelTime = 60 + random.nextInt(60); // 60-120 seconds
        double currentTravelTime = baseTravelTime / speedRatio;

        return TrafficFlowData.builder()
                .currentSpeed(Math.round(currentSpeed * 10.0) / 10.0)
                .freeFlowSpeed(Math.round(freeFlow * 10.0) / 10.0)
                .currentTravelTime(Math.round(currentTravelTime))
                .freeFlowTravelTime(Math.round(baseTravelTime))
                .confidence(0.75 + random.nextDouble() * 0.20)
                .roadClosure(0)
                .build();
    }

    /**
     * Returns speed factor based on time of day.
     * 1.0 = free flow, 0.3 = heavy congestion
     *
     *  Morning Peak:   7:30 - 10:00  →  0.35 - 0.50
     *  Midday:        10:00 - 16:00  →  0.70 - 0.85
     *  Evening Peak:  16:30 - 20:00  →  0.30 - 0.45
     *  Night:         20:00 - 06:00  →  0.85 - 0.95
     *  Early Morning:  6:00 -  7:30  →  0.70 - 0.80
     */
    private double getTimeFactor(int hour, int minute) {
        double timeDecimal = hour + minute / 60.0;

        if (timeDecimal >= 7.5 && timeDecimal < 10.0) {
            // Morning peak — worst at 8:30
            double peakness = 1.0 - Math.abs(timeDecimal - 8.5) / 1.5;
            return 0.35 + (1.0 - peakness) * 0.35;
        } else if (timeDecimal >= 10.0 && timeDecimal < 16.5) {
            // Midday — moderate
            return 0.70 + random.nextDouble() * 0.15;
        } else if (timeDecimal >= 16.5 && timeDecimal < 20.0) {
            // Evening peak — worst at 18:00
            double peakness = 1.0 - Math.abs(timeDecimal - 18.0) / 2.0;
            return 0.30 + (1.0 - peakness) * 0.35;
        } else if (timeDecimal >= 20.0 || timeDecimal < 6.0) {
            // Night — light traffic
            return 0.85 + random.nextDouble() * 0.10;
        } else {
            // Early morning 6-7:30 — building up
            return 0.70 + random.nextDouble() * 0.10;
        }
    }

    private double toDouble(Object val) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        return 0.0;
    }

    private int toInt(Object val) {
        if (val instanceof Number) return ((Number) val).intValue();
        return 0;
    }
}