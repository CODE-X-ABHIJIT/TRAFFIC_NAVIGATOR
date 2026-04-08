package com.tccs.scheduler;

import com.tccs.service.AlertService;
import com.tccs.service.SignalControlService;
import com.tccs.service.TrafficDataService;
import com.tccs.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrafficDataScheduler {

    private final TrafficDataService trafficDataService;
    private final SignalControlService signalControlService;
    private final AlertService alertService;
    private final WeatherService weatherService;

    /**
     * Refresh traffic data every 30 seconds.
     */
    @Scheduled(fixedDelayString = "${traffic.data.refresh-interval-ms:30000}")
    public void refreshTrafficData() {
        try {
            trafficDataService.refreshAllTrafficData();
            log.debug("Scheduled traffic data refresh complete");
        } catch (Exception e) {
            log.error("Error refreshing traffic data: {}", e.getMessage());
        }
    }

    /**
     * Re-optimize signals every 60 seconds.
     */
    @Scheduled(fixedDelay = 60000)
    public void optimizeSignals() {
        try {
            signalControlService.optimizeAllJunctions();
        } catch (Exception e) {
            log.error("Error optimizing signals: {}", e.getMessage());
        }
    }

    /**
     * Scan for alerts every 45 seconds.
     */
    @Scheduled(fixedDelay = 45000)
    public void scanAlerts() {
        try {
            alertService.scanAndGenerateAlerts();
        } catch (Exception e) {
            log.error("Error scanning alerts: {}", e.getMessage());
        }
    }

    /**
     * Check weather every 5 minutes.
     */
    @Scheduled(fixedDelay = 300000)
    public void checkWeather() {
        try {
            weatherService.getCurrentWeather();
        } catch (Exception e) {
            log.error("Error checking weather: {}", e.getMessage());
        }
    }
}