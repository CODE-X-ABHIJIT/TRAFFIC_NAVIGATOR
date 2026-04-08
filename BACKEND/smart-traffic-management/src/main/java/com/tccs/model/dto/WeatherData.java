package com.tccs.model.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class WeatherData {

    private String condition;       // Clear, Rain, Fog etc.
    private double temperature;     // Celsius
    private double humidity;
    private double windSpeed;
    private double visibility;      // km
    private boolean hazardous;      // reduced visibility / heavy rain
}
