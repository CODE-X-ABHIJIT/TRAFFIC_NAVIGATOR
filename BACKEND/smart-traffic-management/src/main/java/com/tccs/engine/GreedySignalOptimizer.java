package com.tccs.engine;

import com.tccs.model.entity.Junction;
import com.tccs.model.entity.Signal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *  WEIGHTED FAIR QUEUING + STARVATION PREVENTION ALGORITHM
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *
 *  Problem with Pure Greedy:
 *    Low density lanes get minimum green forever → STARVATION
 *
 *  This algorithm guarantees:
 *    1. Every lane gets GREEN at least once per cycle
 *    2. No lane waits more than MAX_WAIT_CYCLES without boost
 *    3. High density lanes still get more time (proportional)
 *    4. Starving lanes get priority boost automatically
 *    5. Cycle time adapts based on total congestion
 *
 *  Formula:
 *    greenTime = BASE_SHARE + PROPORTIONAL_BONUS + STARVATION_BOOST
 *
 *    BASE_SHARE        = totalGreen / numSignals (equal floor)
 *    PROPORTIONAL_BONUS = (density / totalDensity) * remainingTime
 *    STARVATION_BOOST   = extra time if lane waited too long
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 */
@Component
@Slf4j
public class GreedySignalOptimizer {

    @Value("${traffic.signal.min-green-time:15}")
    private int minGreenTime;

    @Value("${traffic.signal.max-green-time:60}")
    private int maxGreenTime;

    @Value("${traffic.signal.yellow-time:3}")
    private int yellowTime;

    // Track consecutive cycles each direction got minimum green
    // Key: "junctionCode_direction" → number of consecutive min-green cycles
    private final Map<String, Integer> starvationCounter = new ConcurrentHashMap<>();

    // Track last time each direction got a "good" green (above minimum)
    private final Map<String, LocalDateTime> lastGoodGreen = new ConcurrentHashMap<>();

    // How many consecutive min-green cycles before starvation boost kicks in
    private static final int STARVATION_THRESHOLD = 3;

    // Boost amount (seconds) when starving
    private static final int STARVATION_BOOST_SECONDS = 10;

    // Minimum percentage of cycle each lane is guaranteed
    // 4 lanes → each gets at least 15% of available green time
    private static final double MIN_SHARE_PERCENT = 0.15;

    // Maximum percentage any single lane can take
    private static final double MAX_SHARE_PERCENT = 0.45;

    /**
     * Main optimization method.
     */
    public List<Signal> optimize(Junction junction) {
        List<Signal> signals = junction.getSignals();

        if (signals == null || signals.isEmpty()) {
            log.warn("No signals for junction {}", junction.getJunctionCode());
            return signals;
        }

        int totalCycleTime = junction.getTotalCycleTime();
        int numSignals = signals.size();
        int totalYellowTime = numSignals * yellowTime;
        int availableGreenTime = totalCycleTime - totalYellowTime;

        if (availableGreenTime <= 0) {
            log.error("Invalid cycle time for {}", junction.getJunctionCode());
            return signals;
        }

        // ─── STEP 1: Calculate total density ───
        int totalDensity = signals.stream()
                .mapToInt(Signal::getVehicleDensity)
                .sum();

        // ─── STEP 2: Detect starvation ───
        Map<String, Integer> starvationMap = detectStarvation(
                junction.getJunctionCode(), signals);

        // ─── STEP 3: Allocate green time using Weighted Fair Queuing ───
        allocateGreenTime(junction.getJunctionCode(), signals,
                availableGreenTime, totalDensity, starvationMap);

        // ─── STEP 4: Determine phase order ───
        // Starving lanes go FIRST, then by density (descending)
        determinePhaseOrder(signals, starvationMap);

        // ─── STEP 5: Calculate red duration ───
        calculateRedDurations(signals);

        // ─── STEP 6: Update starvation tracking ───
        updateStarvationTracking(junction.getJunctionCode(), signals);

        // ─── STEP 7: Log results ───
        logOptimization(junction.getJunctionCode(), signals, starvationMap);

        return signals;
    }

    /**
     * ━━━ WEIGHTED FAIR QUEUING ALLOCATION ━━━
     *
     * Each lane gets:
     *   baseFairShare + proportionalBonus + starvationBoost
     *
     * baseFairShare:     Equal floor for all lanes
     * proportionalBonus: Extra time based on density ratio
     * starvationBoost:   Extra time if lane was starved
     */
    private void allocateGreenTime(String junctionCode,
                                    List<Signal> signals,
                                    int availableGreen,
                                    int totalDensity,
                                    Map<String, Integer> starvationMap) {

        int numSignals = signals.size();

        // ─── Base fair share (guaranteed floor) ───
        int baseFairShare = (int) (availableGreen * MIN_SHARE_PERCENT);
        baseFairShare = Math.max(minGreenTime, baseFairShare);

        // ─── Remaining time for proportional + starvation distribution ───
        int guaranteedTotal = baseFairShare * numSignals;
        int remainingForDistribution = availableGreen - guaranteedTotal;

        if (remainingForDistribution < 0) {
            // Not enough time — give equal minimum to all
            for (Signal signal : signals) {
                signal.setGreenDuration(Math.max(minGreenTime,
                        availableGreen / numSignals));
                signal.setYellowDuration(yellowTime);
            }
            return;
        }

        // ─── Calculate starvation boost requirements ───
        int totalStarvationBoost = 0;
        Map<String, Integer> boostMap = new HashMap<>();

        for (Signal signal : signals) {
            String key = junctionCode + "_" + signal.getDirection();
            int starvationCount = starvationMap.getOrDefault(key, 0);

            if (starvationCount >= STARVATION_THRESHOLD) {
                // This lane is starving — give it a boost
                int boost = STARVATION_BOOST_SECONDS;
                // More starvation = bigger boost (up to 20s)
                boost = Math.min(20, boost + (starvationCount - STARVATION_THRESHOLD) * 3);
                boostMap.put(key, boost);
                totalStarvationBoost += boost;
            }
        }

        // ─── Remaining after starvation boosts ───
        int remainingForProportional = Math.max(0,
                remainingForDistribution - totalStarvationBoost);

        // ─── Allocate to each signal ───
        int allocated = 0;

        for (int i = 0; i < signals.size(); i++) {
            Signal signal = signals.get(i);
            String key = junctionCode + "_" + signal.getDirection();

            // Start with base fair share
            int greenTime = baseFairShare;

            // Add proportional bonus based on density
            if (totalDensity > 0 && remainingForProportional > 0) {
                double densityRatio = (double) signal.getVehicleDensity() / totalDensity;
                int proportionalBonus = (int) Math.round(
                        densityRatio * remainingForProportional);
                greenTime += proportionalBonus;
            } else if (totalDensity == 0) {
                // No density data — equal distribution
                greenTime += remainingForProportional / numSignals;
            }

            // Add starvation boost
            int boost = boostMap.getOrDefault(key, 0);
            greenTime += boost;

            // Cap at maximum share
            int maxAllowed = (int) (availableGreen * MAX_SHARE_PERCENT);
            maxAllowed = Math.min(maxGreenTime, maxAllowed);
            greenTime = Math.min(maxAllowed, greenTime);

            // Ensure minimum
            greenTime = Math.max(minGreenTime, greenTime);

            // Last signal gets remaining time
            if (i == signals.size() - 1) {
                int remaining = availableGreen - allocated;
                greenTime = Math.max(minGreenTime, Math.min(maxAllowed, remaining));
            }

            signal.setGreenDuration(greenTime);
            signal.setYellowDuration(yellowTime);
            allocated += greenTime;
        }

        // ─── Final adjustment if over/under allocated ───
        int diff = allocated - availableGreen;
        if (diff != 0) {
            adjustAllocation(signals, diff, availableGreen);
        }
    }

    /**
     * Adjust if total allocated doesn't match available time.
     */
    private void adjustAllocation(List<Signal> signals, int excess,
                                   int availableGreen) {
        if (excess > 0) {
            // Over-allocated: reduce from lanes with most green time
            signals.sort(Comparator.comparingInt(Signal::getGreenDuration).reversed());
            for (Signal s : signals) {
                if (excess <= 0) break;
                int reduce = Math.min(excess,
                        s.getGreenDuration() - minGreenTime);
                if (reduce > 0) {
                    s.setGreenDuration(s.getGreenDuration() - reduce);
                    excess -= reduce;
                }
            }
        } else if (excess < 0) {
            // Under-allocated: add to lanes with least green time
            int deficit = -excess;
            signals.sort(Comparator.comparingInt(Signal::getGreenDuration));
            for (Signal s : signals) {
                if (deficit <= 0) break;
                int add = Math.min(deficit, maxGreenTime - s.getGreenDuration());
                if (add > 0) {
                    s.setGreenDuration(s.getGreenDuration() + add);
                    deficit -= add;
                }
            }
        }
    }

    /**
     * ━━━ STARVATION DETECTION ━━━
     * Check how many consecutive cycles each lane got minimum green.
     */
    private Map<String, Integer> detectStarvation(String junctionCode,
                                                    List<Signal> signals) {
        Map<String, Integer> result = new HashMap<>();

        for (Signal signal : signals) {
            String key = junctionCode + "_" + signal.getDirection();
            int count = starvationCounter.getOrDefault(key, 0);
            result.put(key, count);
        }

        return result;
    }

    /**
     * ━━━ PHASE ORDER ━━━
     * Starving lanes go first, then by density descending.
     * This ensures starved lanes don't have to wait through
     * other phases before getting their turn.
     */
    private void determinePhaseOrder(List<Signal> signals,
                                      Map<String, Integer> starvationMap) {
        signals.sort((a, b) -> {
            String keyA = starvationMap.keySet().stream()
                    .filter(k -> k.endsWith("_" + a.getDirection()))
                    .findFirst().orElse("");
            String keyB = starvationMap.keySet().stream()
                    .filter(k -> k.endsWith("_" + b.getDirection()))
                    .findFirst().orElse("");

            int starvA = starvationMap.getOrDefault(keyA, 0);
            int starvB = starvationMap.getOrDefault(keyB, 0);

            boolean aStarving = starvA >= STARVATION_THRESHOLD;
            boolean bStarving = starvB >= STARVATION_THRESHOLD;

            // Starving lanes go first
            if (aStarving && !bStarving) return -1;
            if (!aStarving && bStarving) return 1;

            // Among starving: most starved goes first
            if (aStarving && bStarving) {
                return Integer.compare(starvB, starvA);
            }

            // Among non-starving: highest density goes first
            return Integer.compare(b.getVehicleDensity(), a.getVehicleDensity());
        });

        for (int i = 0; i < signals.size(); i++) {
            signals.get(i).setPhaseOrder(i);
        }
    }

    /**
     * Calculate red duration for each signal.
     */
    private void calculateRedDurations(List<Signal> signals) {
        for (Signal signal : signals) {
            int otherTime = signals.stream()
                    .filter(s -> !s.getId().equals(signal.getId()))
                    .mapToInt(s -> s.getGreenDuration() + s.getYellowDuration())
                    .sum();
            signal.setRedDuration(otherTime);
        }
    }

    /**
     * ━━━ UPDATE STARVATION TRACKING ━━━
     * After optimization, track which lanes got minimum green.
     * If a lane got more than minimum, reset its starvation counter.
     */
    private void updateStarvationTracking(String junctionCode,
                                           List<Signal> signals) {
        for (Signal signal : signals) {
            String key = junctionCode + "_" + signal.getDirection();

            if (signal.getGreenDuration() <= minGreenTime + 2) {
                // Got minimum or near-minimum → increment starvation counter
                int current = starvationCounter.getOrDefault(key, 0);
                starvationCounter.put(key, current + 1);

                if (current + 1 >= STARVATION_THRESHOLD) {
                    log.warn("⚠️ STARVATION: {} {} has had minimum green for " +
                                    "{} consecutive cycles. Boost will apply next cycle.",
                            junctionCode, signal.getDirection(), current + 1);
                }
            } else {
                // Got good green → reset starvation counter
                starvationCounter.put(key, 0);
                lastGoodGreen.put(key, LocalDateTime.now());
            }
        }
    }

    /**
     * Log the optimization results.
     */
    private void logOptimization(String junctionCode, List<Signal> signals,
                                  Map<String, Integer> starvationMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("Optimized ").append(junctionCode).append(" → ");

        for (Signal s : signals) {
            String key = junctionCode + "_" + s.getDirection();
            int starvCount = starvationMap.getOrDefault(key, 0);

            sb.append(s.getDirection())
                    .append("(d=").append(s.getVehicleDensity())
                    .append(",g=").append(s.getGreenDuration()).append("s");

            if (starvCount >= STARVATION_THRESHOLD) {
                sb.append(",🚨BOOSTED");
            } else if (starvCount > 0) {
                sb.append(",wait=").append(starvCount);
            }

            sb.append(",p=").append(s.getPhaseOrder()).append(") ");
        }

        log.info(sb.toString().trim());
    }

    /**
     * Get current starvation status for monitoring.
     */
    public Map<String, Integer> getStarvationStatus() {
        return Collections.unmodifiableMap(starvationCounter);
    }

    /**
     * Reset starvation counters (e.g., after mode change).
     */
    public void resetStarvation(String junctionCode) {
        starvationCounter.entrySet()
                .removeIf(e -> e.getKey().startsWith(junctionCode + "_"));
        log.info("Starvation counters reset for {}", junctionCode);
    }
}