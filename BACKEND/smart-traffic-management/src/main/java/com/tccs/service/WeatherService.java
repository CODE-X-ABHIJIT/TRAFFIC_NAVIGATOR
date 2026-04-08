package com.tccs.service;

import com.tccs.integration.OpenWeatherClient;
import com.tccs.model.dto.WeatherData;
import com.tccs.model.entity.Alert;
import com.tccs.model.enums.AlertSeverity;
import com.tccs.model.enums.AlertType;
import com.tccs.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final OpenWeatherClient weatherClient;
    private final AlertRepository alertRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Default coordinates (city center)
    private static final double DEFAULT_LAT = 20.2961;
    private static final double DEFAULT_LON = 85.8245;

    /**
     * Gets current weather and generates alerts if hazardous.
     */
    public WeatherData getCurrentWeather() {
        WeatherData weather = weatherClient.getWeather(DEFAULT_LAT, DEFAULT_LON);

        if (weather.isHazardous()) {
            Alert alert = Alert.builder()
                    .alertType(AlertType.WEATHER_WARNING)
                    .severity(AlertSeverity.HIGH)
                    .message(String.format(
                            "Weather alert: %s — Temperature: %.1f°C, " +
                            "Visibility: %.1f km, Wind: %.1f km/h",
                            weather.getCondition(), weather.getTemperature(),
                            weather.getVisibility(), weather.getWindSpeed()))
                    .build();
            alertRepository.save(alert);
            messagingTemplate.convertAndSend("/topic/alerts", alert);
            log.warn("Hazardous weather detected: {}", weather.getCondition());
        }

        return weather;
    }
}