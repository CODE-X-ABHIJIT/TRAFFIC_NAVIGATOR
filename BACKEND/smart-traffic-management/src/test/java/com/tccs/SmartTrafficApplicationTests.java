package com.tccs;

import com.tccs.engine.CongestionDetector;
import com.tccs.engine.GreedySignalOptimizer;
import com.tccs.model.entity.Junction;
import com.tccs.model.entity.Signal;
import com.tccs.model.enums.ControlMode;
import com.tccs.model.enums.SignalState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class SmartTrafficApplicationTests {

    @Autowired
    private GreedySignalOptimizer optimizer;

    @Autowired
    private CongestionDetector detector;

    @Test
    void contextLoads() {
        assertNotNull(optimizer);
        assertNotNull(detector);
    }

    @Test
    void testGreedyOptimizer_HighDensityGetsMoreGreenTime() {
        Junction junction = createTestJunction();
        // Set varying densities
        junction.getSignals().get(0).setVehicleDensity(50); // NORTH - heavy
        junction.getSignals().get(1).setVehicleDensity(10); // SOUTH - light
        junction.getSignals().get(2).setVehicleDensity(30); // EAST - moderate
        junction.getSignals().get(3).setVehicleDensity(20); // WEST - moderate

        List<Signal> optimized = optimizer.optimize(junction);

        // The signal with highest density should have most green time
        Signal busiest = optimized.stream()
                .max((a, b) -> Integer.compare(
                        a.getVehicleDensity(), b.getVehicleDensity()))
                .orElseThrow();

        Signal lightest = optimized.stream()
                .min((a, b) -> Integer.compare(
                        a.getVehicleDensity(), b.getVehicleDensity()))
                .orElseThrow();

        assertTrue(busiest.getGreenDuration() >= lightest.getGreenDuration(),
                "Busiest lane should get >= green time than lightest");

        // Busiest should be first in phase order
        assertEquals(0, busiest.getPhaseOrder(),
                "Busiest lane should be phase 0 (greedy choice)");
    }

    @Test
    void testGreedyOptimizer_ZeroDensity_EqualDistribution() {
        Junction junction = createTestJunction();
        // All densities = 0
        junction.getSignals().forEach(s -> s.setVehicleDensity(0));

        List<Signal> optimized = optimizer.optimize(junction);

        int firstGreen = optimized.get(0).getGreenDuration();
        for (Signal s : optimized) {
            assertEquals(firstGreen, s.getGreenDuration(),
                    "Zero density → equal green time distribution");
        }
    }

    @Test
    void testCongestionDetector_Levels() {
        assertEquals("LOW", detector.getCongestionLabel(
                detector.calculateCongestion(50, 60)));
        assertEquals("MEDIUM", detector.getCongestionLabel(
                detector.calculateCongestion(35, 60)));
        assertEquals("HIGH", detector.getCongestionLabel(
                detector.calculateCongestion(20, 60)));
        assertEquals("CRITICAL", detector.getCongestionLabel(
                detector.calculateCongestion(5, 60)));
    }

    @Test
    void testCongestionDetector_FullSpeed_NoCongestion() {
        double congestion = detector.calculateCongestion(60, 60);
        assertEquals(0.0, congestion, 0.01);
    }

    @Test
    void testCongestionDetector_ZeroSpeed_FullCongestion() {
        double congestion = detector.calculateCongestion(0, 60);
        assertEquals(1.0, congestion, 0.01);
    }

    private Junction createTestJunction() {
        Junction junction = Junction.builder()
                .id(1L)
                .junctionCode("TEST001")
                .name("Test Junction")
                .latitude(12.97)
                .longitude(77.59)
                .controlMode(ControlMode.AUTO)
                .active(true)
                .totalCycleTime(120)
                .signals(new ArrayList<>())
                .build();

        String[] dirs = {"NORTH", "SOUTH", "EAST", "WEST"};
        for (int i = 0; i < 4; i++) {
            Signal s = Signal.builder()
                    .id((long) (i + 1))
                    .direction(dirs[i])
                    .currentState(SignalState.RED)
                    .greenDuration(25)
                    .yellowDuration(3)
                    .vehicleDensity(0)
                    .phaseOrder(i)
                    .build();
            s.setJunction(junction);
            junction.getSignals().add(s);
        }

        return junction;
    }
}