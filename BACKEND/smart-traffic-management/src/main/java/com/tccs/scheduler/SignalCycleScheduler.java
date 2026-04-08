package com.tccs.scheduler;

import com.tccs.model.dto.JunctionStatusDTO;
import com.tccs.model.entity.Junction;
import com.tccs.model.entity.Signal;
import com.tccs.model.enums.ControlMode;
import com.tccs.model.enums.SignalState;
import com.tccs.repository.JunctionRepository;
import com.tccs.repository.SignalRepository;
import com.tccs.service.TrafficDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *  SIGNAL CYCLE SIMULATOR
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *  Runs every second.
 *  Manages RED → GREEN → YELLOW → RED
 *  transitions for all AUTO-mode junctions.
 *  Broadcasts countdown updates via WebSocket.
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SignalCycleScheduler {

    private final JunctionRepository junctionRepository;
    private final SignalRepository signalRepository;
    private final TrafficDataService trafficDataService;
    private final SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedRate = 1000) // every 1 second
    @Transactional
    public void cycleSignals() {
        List<Junction> activeJunctions = junctionRepository.findByActiveTrue();

        for (Junction junction : activeJunctions) {
            if (junction.getControlMode() == ControlMode.AUTO) {
                processAutoCycle(junction);
            } else {
                processManualCountdown(junction);
            }
        }

        // Broadcast all junction statuses every second
        broadcastLiveStatuses();
    }

    /**
     * AUTO mode: Automatically cycle through signal phases.
     */
    private void processAutoCycle(Junction junction) {
        List<Signal> signals = junction.getSignals();
        if (signals.isEmpty()) return;

        // Find the currently active signal (GREEN or YELLOW)
        Optional<Signal> activeOpt = signals.stream()
                .filter(s -> s.getCurrentState() == SignalState.GREEN
                        || s.getCurrentState() == SignalState.YELLOW)
                .findFirst();

        if (activeOpt.isEmpty()) {
            // No active signal — start the first phase
            startFirstPhase(junction, signals);
            return;
        }

        Signal active = activeOpt.get();
        active.setCountdownSeconds(active.getCountdownSeconds() - 1);

        if (active.getCountdownSeconds() <= 0) {
            if (active.getCurrentState() == SignalState.GREEN) {
                // GREEN → YELLOW transition
                active.setCurrentState(SignalState.YELLOW);
                active.setCountdownSeconds(active.getYellowDuration());
            } else if (active.getCurrentState() == SignalState.YELLOW) {
                // YELLOW → RED, activate next signal
                active.setCurrentState(SignalState.RED);
                active.setCountdownSeconds(0);
                activateNextPhase(junction, signals, active.getPhaseOrder());
            }
        }

        signalRepository.saveAll(signals);
    }

    /**
     * Start the first signal in phase order.
     */
    private void startFirstPhase(Junction junction, List<Signal> signals) {
        Signal first = signals.stream()
                .min(Comparator.comparingInt(Signal::getPhaseOrder))
                .orElse(null);

        if (first != null) {
            // Set all to RED first
            signals.forEach(s -> {
                s.setCurrentState(SignalState.RED);
                s.setCountdownSeconds(0);
            });

            // Activate first
            first.setCurrentState(SignalState.GREEN);
            first.setCountdownSeconds(first.getGreenDuration());
            junction.setCurrentPhaseIndex(first.getPhaseOrder());

            signalRepository.saveAll(signals);
            junctionRepository.save(junction);

            log.debug("Started cycle for junction {} — Phase 0 ({}) GREEN for {}s",
                    junction.getJunctionCode(), first.getDirection(),
                    first.getGreenDuration());
        }
    }

    /**
     * Activate the next signal in phase order.
     */
    private void activateNextPhase(Junction junction,
                                    List<Signal> signals,
                                    int currentPhaseOrder) {
        int nextPhase = (currentPhaseOrder + 1) % signals.size();

        Optional<Signal> nextOpt = signals.stream()
                .filter(s -> s.getPhaseOrder() == nextPhase)
                .findFirst();

        if (nextOpt.isPresent()) {
            Signal next = nextOpt.get();
            next.setCurrentState(SignalState.GREEN);
            next.setCountdownSeconds(next.getGreenDuration());
            junction.setCurrentPhaseIndex(nextPhase);
            junctionRepository.save(junction);

            log.debug("Junction {} → Phase {} ({}) GREEN for {}s",
                    junction.getJunctionCode(), nextPhase,
                    next.getDirection(), next.getGreenDuration());
        }
    }

    /**
     * MANUAL mode: just decrement countdown (no auto-transition).
     */
    private void processManualCountdown(Junction junction) {
        for (Signal signal : junction.getSignals()) {
            if (signal.getCountdownSeconds() > 0) {
                signal.setCountdownSeconds(signal.getCountdownSeconds() - 1);
            }
        }
        signalRepository.saveAll(junction.getSignals());
    }

    /**
     * Broadcast all junction statuses via WebSocket.
     */
    private void broadcastLiveStatuses() {
        try {
            List<JunctionStatusDTO> statuses =
                    trafficDataService.getAllJunctionStatuses();
            messagingTemplate.convertAndSend("/topic/live-signals", statuses);
        } catch (Exception e) {
            log.error("Error broadcasting live statuses: {}", e.getMessage());
        }
    }
}