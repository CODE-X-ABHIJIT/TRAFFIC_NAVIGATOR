package com.tccs.engine;

import com.tccs.model.entity.Alert;
import com.tccs.model.entity.Junction;
import com.tccs.model.entity.Signal;
import com.tccs.model.enums.*;
import com.tccs.repository.AlertRepository;
import com.tccs.repository.JunctionRepository;
import com.tccs.repository.SignalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *  EMERGENCY HANDLER ENGINE
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *  Automatically handles incidents by:
 *   1. Finding nearest junction(s)
 *   2. Determining best clearance direction
 *   3. Activating emergency clearance
 *   4. Auto-reverting after timeout
 *   5. Clearing corridor for emergency vehicles
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmergencyHandler {

    private final JunctionRepository junctionRepository;
    private final SignalRepository signalRepository;
    private final AlertRepository alertRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final TaskScheduler taskScheduler;

    // Track which junctions are in emergency mode
    private final Map<String, Long> activeEmergencies = new HashMap<>();

    @Value("${traffic.emergency.auto-clearance-enabled:true}")
    private boolean autoClearanceEnabled;

    @Value("${traffic.emergency.clearance-duration-seconds:120}")
    private int clearanceDurationSeconds;

    @Value("${traffic.emergency.nearby-radius-km:1.0}")
    private double nearbyRadiusKm;

    @Value("${traffic.emergency.vip-corridor-junctions:3}")
    private int vipCorridorJunctions;

    /**
     * Main entry point — called when incident is reported.
     * Returns list of junction codes that were affected.
     */
    public List<String> handleIncident(IncidentType incidentType,
                                        double latitude, double longitude,
                                        String junctionCode,
                                        AlertSeverity severity,
                                        Long incidentId) {

        if (!autoClearanceEnabled) {
            log.info("Auto clearance disabled. Skipping emergency handling.");
            return Collections.emptyList();
        }

        List<String> affectedJunctions = new ArrayList<>();

        switch (incidentType) {
            case ACCIDENT:
                affectedJunctions = handleAccident(latitude, longitude,
                        junctionCode, severity, incidentId);
                break;

            case VIP_MOVEMENT:
                affectedJunctions = handleVipMovement(latitude, longitude,
                        junctionCode, incidentId);
                break;

            case ROADBLOCK:
            case CONSTRUCTION:
            case PROTEST:
                affectedJunctions = handleRoadBlock(latitude, longitude,
                        junctionCode, severity, incidentId);
                break;

            case WEATHER_HAZARD:
                affectedJunctions = handleWeatherHazard(latitude, longitude,
                        incidentId);
                break;

            default:
                affectedJunctions = handleGenericIncident(latitude, longitude,
                        junctionCode, severity, incidentId);
                break;
        }

        return affectedJunctions;
    }

    /**
     * ━━━ ACCIDENT HANDLING ━━━
     * - Find nearest junction
     * - Clear the direction AWAY from accident
     * - Extend green for clearance route
     */
    private List<String> handleAccident(double lat, double lon,
                                         String junctionCode,
                                         AlertSeverity severity,
                                         Long incidentId) {

        List<String> affected = new ArrayList<>();

        // Find nearest junctions
        List<Junction> nearbyJunctions = findNearbyJunctions(lat, lon);

        if (nearbyJunctions.isEmpty() && junctionCode != null) {
            junctionRepository.findByJunctionCode(junctionCode)
                    .ifPresent(nearbyJunctions::add);
        }

        for (Junction junction : nearbyJunctions) {
            // Determine which direction to clear (direction AWAY from accident)
            String clearDirection = determineClearanceDirection(
                    junction, lat, lon);

            // Activate emergency clearance
            activateEmergencyClearance(junction, clearDirection, incidentId,
                    "ACCIDENT detected nearby. Clearing " + clearDirection);

            affected.add(junction.getJunctionCode());

            // For HIGH/CRITICAL — also affect adjacent junctions
            if (severity == AlertSeverity.HIGH || severity == AlertSeverity.CRITICAL) {
                // Extend clearance duration for severe accidents
                int extendedDuration = clearanceDurationSeconds * 2;
                scheduleAutoRevert(junction.getJunctionCode(), extendedDuration,
                        incidentId);
            } else {
                scheduleAutoRevert(junction.getJunctionCode(),
                        clearanceDurationSeconds, incidentId);
            }
        }

        // Generate emergency alert
        generateEmergencyAlert(affected, "ACCIDENT",
                "Auto emergency clearance activated for accident at (" +
                        lat + ", " + lon + ")");

        log.info("🚨 ACCIDENT RESPONSE: Cleared {} junctions. Directions: {}",
                affected.size(), affected);

        return affected;
    }

    /**
     * ━━━ VIP MOVEMENT ━━━
     * - Clear multiple junctions ahead (corridor)
     * - All signals GREEN in VIP direction
     */
    private List<String> handleVipMovement(double lat, double lon,
                                            String junctionCode,
                                            Long incidentId) {

        List<String> affected = new ArrayList<>();
        List<Junction> corridor = findNearbyJunctions(lat, lon);

        // Sort by distance — nearest first
        corridor.sort((a, b) -> {
            double distA = haversineDistance(lat, lon,
                    a.getLatitude(), a.getLongitude());
            double distB = haversineDistance(lat, lon,
                    b.getLatitude(), b.getLongitude());
            return Double.compare(distA, distB);
        });

        // Clear up to N junctions for VIP corridor
        int count = 0;
        for (Junction junction : corridor) {
            if (count >= vipCorridorJunctions) break;

            String clearDirection = determineClearanceDirection(
                    junction, lat, lon);

            activateEmergencyClearance(junction, clearDirection, incidentId,
                    "VIP CORRIDOR — Priority clearance");

            scheduleAutoRevert(junction.getJunctionCode(),
                    clearanceDurationSeconds, incidentId);

            affected.add(junction.getJunctionCode());
            count++;
        }

        generateEmergencyAlert(affected, "VIP_MOVEMENT",
                "VIP corridor activated. " + affected.size() +
                        " junctions cleared.");

        log.info("⭐ VIP CORRIDOR: {} junctions cleared", affected.size());

        return affected;
    }

    /**
     * ━━━ ROADBLOCK / CONSTRUCTION / PROTEST ━━━
     * - Reduce green time for blocked direction
     * - Increase green for alternate routes
     */
    private List<String> handleRoadBlock(double lat, double lon,
                                          String junctionCode,
                                          AlertSeverity severity,
                                          Long incidentId) {

        List<String> affected = new ArrayList<>();
        List<Junction> nearby = findNearbyJunctions(lat, lon);

        for (Junction junction : nearby) {
            String blockedDirection = determineClearanceDirection(
                    junction, lat, lon);

            // Instead of full clearance — reduce blocked direction green time
            // and increase others
            adjustSignalTimingsForBlock(junction, blockedDirection);

            affected.add(junction.getJunctionCode());

            // Longer revert time for construction/protest
            int revertTime = severity == AlertSeverity.CRITICAL
                    ? clearanceDurationSeconds * 3
                    : clearanceDurationSeconds * 2;

            scheduleAutoRevert(junction.getJunctionCode(), revertTime,
                    incidentId);
        }

        generateEmergencyAlert(affected, "ROADBLOCK",
                "Signal timings adjusted for road blockage near (" +
                        lat + ", " + lon + ")");

        return affected;
    }

    /**
     * ━━━ WEATHER HAZARD ━━━
     * - Extend all green times (slower speeds)
     * - Reduce cycle frequency
     */
    private List<String> handleWeatherHazard(double lat, double lon,
                                              Long incidentId) {
        List<String> affected = new ArrayList<>();

        // Affect ALL junctions during weather hazard
        List<Junction> allJunctions = junctionRepository.findByActiveTrue();

        for (Junction junction : allJunctions) {
            // Extend green times by 50%
            for (Signal signal : junction.getSignals()) {
                int newGreen = (int) (signal.getGreenDuration() * 1.5);
                newGreen = Math.min(60, newGreen); // cap at 60s
                signal.setGreenDuration(newGreen);
            }
            signalRepository.saveAll(junction.getSignals());
            affected.add(junction.getJunctionCode());
        }

        generateEmergencyAlert(affected, "WEATHER_HAZARD",
                "All signal timings extended for weather safety. " +
                        affected.size() + " junctions affected.");

        // Weather hazards revert after longer time
        for (String code : affected) {
            scheduleAutoRevert(code, clearanceDurationSeconds * 5, incidentId);
        }

        return affected;
    }

    /**
     * ━━━ GENERIC INCIDENT ━━━
     */
    private List<String> handleGenericIncident(double lat, double lon,
                                                String junctionCode,
                                                AlertSeverity severity,
                                                Long incidentId) {
        List<String> affected = new ArrayList<>();
        List<Junction> nearby = findNearbyJunctions(lat, lon);

        if (nearby.isEmpty() && junctionCode != null) {
            junctionRepository.findByJunctionCode(junctionCode)
                    .ifPresent(nearby::add);
        }

        for (Junction junction : nearby) {
            if (severity == AlertSeverity.HIGH || severity == AlertSeverity.CRITICAL) {
                String clearDir = determineClearanceDirection(junction, lat, lon);
                activateEmergencyClearance(junction, clearDir, incidentId,
                        "Incident response — auto clearance");
            }
            affected.add(junction.getJunctionCode());
            scheduleAutoRevert(junction.getJunctionCode(),
                    clearanceDurationSeconds, incidentId);
        }

        return affected;
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  CORE HELPER METHODS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Find junctions within radius of incident.
     */
    private List<Junction> findNearbyJunctions(double lat, double lon) {
        List<Junction> allActive = junctionRepository.findByActiveTrue();
        List<Junction> nearby = new ArrayList<>();

        for (Junction j : allActive) {
            double dist = haversineDistance(lat, lon,
                    j.getLatitude(), j.getLongitude());

            if (dist <= nearbyRadiusKm) {
                nearby.add(j);
            }
        }

        // Sort by distance
        nearby.sort((a, b) -> {
            double dA = haversineDistance(lat, lon,
                    a.getLatitude(), a.getLongitude());
            double dB = haversineDistance(lat, lon,
                    b.getLatitude(), b.getLongitude());
            return Double.compare(dA, dB);
        });

        log.debug("Found {} junctions within {}km of ({}, {})",
                nearby.size(), nearbyRadiusKm, lat, lon);

        return nearby;
    }

    /**
     * Determine which direction to clear based on incident position
     * relative to junction.
     *
     * Logic: Clear the direction that leads AWAY from the incident
     * so traffic can flow away from the scene.
     */
    private String determineClearanceDirection(Junction junction,
                                                double incidentLat,
                                                double incidentLon) {
        double latDiff = incidentLat - junction.getLatitude();
        double lonDiff = incidentLon - junction.getLongitude();

        // Incident is to the NORTH of junction → clear SOUTH (away from incident)
        // Incident is to the EAST → clear WEST (away from incident)

        if (Math.abs(latDiff) > Math.abs(lonDiff)) {
            // Incident is primarily North or South
            return latDiff > 0 ? "SOUTH" : "NORTH";
        } else {
            // Incident is primarily East or West
            return lonDiff > 0 ? "WEST" : "EAST";
        }
    }

    /**
     * Activate emergency clearance at a junction.
     */
    private void activateEmergencyClearance(Junction junction,
                                             String clearDirection,
                                             Long incidentId,
                                             String reason) {

        // Switch to MANUAL mode
        junction.setControlMode(ControlMode.MANUAL);
        junctionRepository.save(junction);

        // Set all signals RED except clearance direction
        for (Signal signal : junction.getSignals()) {
            if (signal.getDirection().equalsIgnoreCase(clearDirection)) {
                signal.setCurrentState(SignalState.GREEN);
                signal.setCountdownSeconds(clearanceDurationSeconds);
            } else {
                signal.setCurrentState(SignalState.RED);
                signal.setCountdownSeconds(clearanceDurationSeconds);
            }
        }
        signalRepository.saveAll(junction.getSignals());

        // Track this emergency
        activeEmergencies.put(junction.getJunctionCode(), incidentId);

        log.info("🚨 EMERGENCY CLEARANCE: {} → {} direction GREEN for {}s. Reason: {}",
                junction.getJunctionCode(), clearDirection,
                clearanceDurationSeconds, reason);

        // Broadcast to control room
        messagingTemplate.convertAndSend("/topic/alerts",
                "🚨 AUTO EMERGENCY: " + junction.getName() +
                        " — " + clearDirection + " cleared. Reason: " + reason);

        messagingTemplate.convertAndSend("/topic/signal-override",
                "Emergency clearance at " + junction.getJunctionCode() +
                        " [AUTO — Incident #" + incidentId + "]");
    }

    /**
     * Adjust signals for road block (not full clearance).
     */
    private void adjustSignalTimingsForBlock(Junction junction,
                                              String blockedDirection) {
        junction.setControlMode(ControlMode.MANUAL);
        junctionRepository.save(junction);

        for (Signal signal : junction.getSignals()) {
            if (signal.getDirection().equalsIgnoreCase(blockedDirection)) {
                // Minimize green time for blocked direction
                signal.setGreenDuration(10); // minimum
            } else {
                // Increase green time for alternate routes
                int increased = (int) (signal.getGreenDuration() * 1.5);
                signal.setGreenDuration(Math.min(60, increased));
            }
        }
        signalRepository.saveAll(junction.getSignals());

        log.info("🚧 BLOCK ADJUSTMENT: {} → {} direction reduced, others increased",
                junction.getJunctionCode(), blockedDirection);
    }

    /**
     * Schedule auto-revert to AUTO mode after timeout.
     */
    private void scheduleAutoRevert(String junctionCode, int delaySeconds,
                                     Long incidentId) {

        log.info("⏱️ Auto-revert scheduled: {} will return to AUTO mode in {}s",
                junctionCode, delaySeconds);

        taskScheduler.schedule(() -> {
            try {
                // Only revert if still same emergency
                Long activeIncident = activeEmergencies.get(junctionCode);
                if (activeIncident != null && activeIncident.equals(incidentId)) {

                    Junction junction = junctionRepository
                            .findByJunctionCode(junctionCode).orElse(null);

                    if (junction != null) {
                        junction.setControlMode(ControlMode.AUTO);
                        junctionRepository.save(junction);
                        activeEmergencies.remove(junctionCode);

                        log.info("✅ AUTO-REVERTED: {} back to AUTO mode " +
                                "(incident #{})", junctionCode, incidentId);

                        messagingTemplate.convertAndSend("/topic/alerts",
                                "✅ " + junction.getName() +
                                        " returned to AUTO mode after emergency");

                        messagingTemplate.convertAndSend("/topic/signal-override",
                                "Auto-revert: " + junctionCode + " → AUTO");
                    }
                }
            } catch (Exception e) {
                log.error("Error reverting {}: {}", junctionCode, e.getMessage());
            }
        }, Instant.now().plusSeconds(delaySeconds));
    }

    /**
     * Generate alert for the emergency action.
     */
    private void generateEmergencyAlert(List<String> junctions, String type,
                                         String message) {
        for (String code : junctions) {
            Alert alert = Alert.builder()
                    .junctionCode(code)
                    .alertType(AlertType.INCIDENT_REPORTED)
                    .severity(AlertSeverity.CRITICAL)
                    .message("🚨 AUTO RESPONSE [" + type + "]: " + message)
                    .build();
            alertRepository.save(alert);
            messagingTemplate.convertAndSend("/topic/alerts", alert);
        }
    }

    /**
     * Called when incident is resolved — revert immediately.
     */
    public void handleIncidentResolved(Long incidentId) {
        List<String> toRevert = new ArrayList<>();

        activeEmergencies.forEach((code, id) -> {
            if (id.equals(incidentId)) toRevert.add(code);
        });

        for (String code : toRevert) {
            Junction junction = junctionRepository.findByJunctionCode(code)
                    .orElse(null);
            if (junction != null) {
                junction.setControlMode(ControlMode.AUTO);
                junctionRepository.save(junction);
                activeEmergencies.remove(code);

                log.info("✅ INCIDENT RESOLVED: {} reverted to AUTO", code);

                messagingTemplate.convertAndSend("/topic/alerts",
                        "✅ " + junction.getName() +
                                " returned to AUTO — incident resolved");
            }
        }
    }

    /**
     * Check if a junction is currently in emergency mode.
     */
    public boolean isInEmergency(String junctionCode) {
        return activeEmergencies.containsKey(junctionCode);
    }

    /**
     * Get all active emergencies.
     */
    public Map<String, Long> getActiveEmergencies() {
        return Collections.unmodifiableMap(activeEmergencies);
    }

    /**
     * Haversine distance in km.
     */
    private double haversineDistance(double lat1, double lon1,
                                     double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}