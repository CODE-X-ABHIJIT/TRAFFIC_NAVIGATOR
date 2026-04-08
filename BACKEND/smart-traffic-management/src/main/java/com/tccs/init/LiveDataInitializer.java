package com.tccs.init;

import com.tccs.integration.OverpassApiClient;
import com.tccs.integration.OverpassApiClient.OsmJunction;
import com.tccs.model.entity.Junction;
import com.tccs.model.entity.Signal;
import com.tccs.model.enums.ControlMode;
import com.tccs.model.enums.SignalState;
import com.tccs.repository.JunctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LiveDataInitializer implements CommandLineRunner {

    private final JunctionRepository junctionRepository;
    private final OverpassApiClient overpassClient;

    @Value("${traffic.city.name:Bhubaneswar}")
    private String cityName;

    @Value("${traffic.city.junction-count:10}")
    private int junctionCount;

    @Override
    @Transactional
    public void run(String... args) {
        if (junctionRepository.count() > 0) {
            log.info("Database has {} junctions. Skipping.", junctionRepository.count());
            return;
        }

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("  🚦 Initializing {} for Traffic Control", cityName);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Fetch junctions (API first → fallback if fails)
        List<OsmJunction> junctions =
                overpassClient.fetchBhubaneswarJunctions(junctionCount);

        for (int i = 0; i < junctions.size(); i++) {
            OsmJunction osm = junctions.get(i);
            String code = String.format("J%03d", i + 1);

            createJunction(code, osm.getName(),
                    osm.getLatitude(), osm.getLongitude(),
                    calculateCycleTime(i));

            log.info("  ✅ {} — {} ({}, {}) [Source: {}]",
                    code, osm.getName(),
                    osm.getLatitude(), osm.getLongitude(),
                    osm.getSource());
        }

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("  ✅ {} junctions ready for {}", junctionRepository.count(), cityName);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void createJunction(String code, String name,
                                 double lat, double lon, int cycleTime) {
        if (junctionRepository.existsByJunctionCode(code)) return;

        Junction junction = Junction.builder()
                .junctionCode(code)
                .name(name)
                .latitude(lat)
                .longitude(lon)
                .controlMode(ControlMode.AUTO)
                .active(true)
                .totalCycleTime(cycleTime)
                .currentPhaseIndex(0)
                .build();

        String[] directions = {"NORTH", "SOUTH", "EAST", "WEST"};
        int baseGreen = cycleTime / 4 - 3;

        for (int i = 0; i < directions.length; i++) {
            int greenTime = Math.max(15, baseGreen + (i % 2 == 0 ? 2 : -2));

            Signal signal = Signal.builder()
                    .direction(directions[i])
                    .currentState(i == 0 ? SignalState.GREEN : SignalState.RED)
                    .greenDuration(greenTime)
                    .yellowDuration(3)
                    .redDuration(cycleTime - greenTime - 3)
                    .countdownSeconds(i == 0 ? greenTime : 0)
                    .vehicleDensity(0)
                    .vehicleSpeed(0)
                    .freeFlowSpeed(45.0)
                    .phaseOrder(i)
                    .build();

            junction.addSignal(signal);
        }

        junctionRepository.save(junction);
    }

    private int calculateCycleTime(int index) {
        int[] cycleTimes = {120, 130, 110, 140, 120, 150, 110, 130, 120, 140};
        return cycleTimes[index % cycleTimes.length];
    }
}