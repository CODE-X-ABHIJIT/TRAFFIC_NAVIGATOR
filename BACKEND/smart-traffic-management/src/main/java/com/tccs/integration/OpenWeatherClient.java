package com.tccs.integration;

import com.tccs.model.dto.WeatherData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *  WEATHER CLIENT — Uses Open-Meteo (FREE)
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *  Open-Meteo requires NO API key.
 *  Falls back to OpenWeatherMap if key provided.
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 */
@Component
@Slf4j
public class OpenWeatherClient {

    private final RestTemplate restTemplate;

    private static final String OPEN_METEO_URL =
            "https://api.open-meteo.com/v1/forecast";

    @Value("${traffic.openweather.api-key:NONE}")
    private String openWeatherKey;

    @Value("${traffic.openweather.base-url:https://api.openweathermap.org/data/2.5/weather}")
    private String openWeatherUrl;

    public OpenWeatherClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Gets weather data. Tries Open-Meteo first (free),
     * then OpenWeatherMap if key available.
     */
    public WeatherData getWeather(double lat, double lon) {

        // ─── Try Open-Meteo (FREE, no key) ───
        try {
            return fetchFromOpenMeteo(lat, lon);
        } catch (Exception e) {
            log.warn("Open-Meteo failed: {}", e.getMessage());
        }

        // ─── Try OpenWeatherMap (if key configured) ───
        if (!"NONE".equals(openWeatherKey) && !"YOUR_OPENWEATHER_API_KEY".equals(openWeatherKey)) {
            try {
                return fetchFromOpenWeather(lat, lon);
            } catch (Exception e) {
                log.warn("OpenWeatherMap failed: {}", e.getMessage());
            }
        }

        // ─── Fallback ───
        return defaultWeather();
    }

    /**
     * Open-Meteo — 100% FREE, no API key needed.
     * https://open-meteo.com/en/docs
     */
    @SuppressWarnings("unchecked")
    private WeatherData fetchFromOpenMeteo(double lat, double lon) {
        String url = String.format(
                "%s?latitude=%f&longitude=%f" +
                "&current=temperature_2m,relative_humidity_2m,wind_speed_10m," +
                "weather_code,visibility" +
                "&timezone=auto",
                OPEN_METEO_URL, lat, lon
        );

        log.debug("Fetching weather from Open-Meteo: lat={}, lon={}", lat, lon);

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        if (response != null && response.containsKey("current")) {
            Map<String, Object> current = (Map<String, Object>) response.get("current");

            double temperature = toDouble(current.get("temperature_2m"));
            double humidity = toDouble(current.get("relative_humidity_2m"));
            double windSpeed = toDouble(current.get("wind_speed_10m"));
            int weatherCode = toInt(current.get("weather_code"));
            double visibility = toDouble(current.get("visibility"));

            // Convert visibility from meters to km
            double visibilityKm = visibility / 1000.0;
            if (visibilityKm <= 0) visibilityKm = 10.0;

            // Convert WMO weather code to readable condition
            String condition = wmoCodeToCondition(weatherCode);

            // Determine if hazardous
            boolean hazardous = weatherCode >= 65 || // heavy rain
                    weatherCode >= 75 ||             // heavy snow
                    weatherCode >= 95 ||             // thunderstorm
                    visibilityKm < 1.0 ||
                    windSpeed > 50;

            WeatherData weather = WeatherData.builder()
                    .condition(condition)
                    .temperature(temperature)
                    .humidity(humidity)
                    .windSpeed(windSpeed)
                    .visibility(Math.round(visibilityKm * 10.0) / 10.0)
                    .hazardous(hazardous)
                    .build();

            log.info("✅ Weather fetched: {} | {}°C | Humidity: {}% | Wind: {} km/h | Visibility: {} km",
                    condition, temperature, humidity, windSpeed, visibilityKm);

            return weather;
        }

        throw new RuntimeException("Empty Open-Meteo response");
    }

    /**
     * OpenWeatherMap fallback (needs API key).
     */
    @SuppressWarnings("unchecked")
    private WeatherData fetchFromOpenWeather(double lat, double lon) {
        String url = String.format("%s?lat=%f&lon=%f&appid=%s&units=metric",
                openWeatherUrl, lat, lon, openWeatherKey);

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        if (response != null) {
            java.util.List<Map<String, Object>> weatherList =
                    (java.util.List<Map<String, Object>>) response.get("weather");
            Map<String, Object> main = (Map<String, Object>) response.get("main");
            Map<String, Object> wind = (Map<String, Object>) response.get("wind");

            String condition = weatherList != null && !weatherList.isEmpty()
                    ? (String) weatherList.get(0).get("main") : "Clear";
            double temp = main != null ? toDouble(main.get("temp")) : 25.0;
            double humidity = main != null ? toDouble(main.get("humidity")) : 50.0;
            double windSpeed = wind != null ? toDouble(wind.get("speed")) : 5.0;
            double visibility = response.containsKey("visibility")
                    ? toDouble(response.get("visibility")) / 1000.0 : 10.0;

            boolean hazardous = "Rain".equalsIgnoreCase(condition)
                    || "Thunderstorm".equalsIgnoreCase(condition)
                    || "Fog".equalsIgnoreCase(condition)
                    || visibility < 1.0;

            return WeatherData.builder()
                    .condition(condition)
                    .temperature(temp)
                    .humidity(humidity)
                    .windSpeed(windSpeed)
                    .visibility(visibility)
                    .hazardous(hazardous)
                    .build();
        }

        throw new RuntimeException("Empty OpenWeatherMap response");
    }

    /**
     * Convert WMO Weather Code to human-readable condition.
     * https://open-meteo.com/en/docs#weathervariables
     */
    private String wmoCodeToCondition(int code) {
        if (code == 0) return "Clear";
        if (code <= 3) return "Clouds";
        if (code <= 49) return "Fog";
        if (code <= 59) return "Drizzle";
        if (code <= 69) return "Rain";
        if (code <= 79) return "Snow";
        if (code <= 84) return "Rain Showers";
        if (code <= 86) return "Snow Showers";
        if (code <= 99) return "Thunderstorm";
        return "Clear";
    }

    private WeatherData defaultWeather() {
        return WeatherData.builder()
                .condition("Clear")
                .temperature(30.0)
                .humidity(70.0)
                .windSpeed(10.0)
                .visibility(10.0)
                .hazardous(false)
                .build();
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