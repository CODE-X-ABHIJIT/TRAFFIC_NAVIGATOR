package com.tccs.controller;

import com.tccs.model.dto.ApiResponse;
import com.tccs.model.dto.JunctionStatusDTO;
import com.tccs.model.dto.SignalOverrideRequest;
import com.tccs.model.enums.ControlMode;
import com.tccs.service.SignalControlService;
import com.tccs.service.TrafficDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/signals")
@RequiredArgsConstructor
public class SignalControlController {

    private final SignalControlService signalControlService;
    private final TrafficDataService trafficDataService;

    /**
     * GET /api/signals/junction/{code}
     * Get current signal status for a junction.
     */
    @GetMapping("/junction/{code}")
    public ResponseEntity<ApiResponse<JunctionStatusDTO>> getJunctionSignals(
            @PathVariable String code) {
        JunctionStatusDTO status = trafficDataService.getJunctionStatus(code);
        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    /**
     * POST /api/signals/override
     * Manual signal override by officer.
     */
    @PostMapping("/override")
    public ResponseEntity<ApiResponse<String>> overrideSignal(
            @Valid @RequestBody SignalOverrideRequest request) {
        signalControlService.overrideSignal(request);
        return ResponseEntity.ok(ApiResponse.ok(
                "Signal override applied",
                "Override successful at " + request.getJunctionCode()));
    }

    /**
     * PUT /api/signals/mode/{code}
     * Switch junction between AUTO and MANUAL mode.
     */
    @PutMapping("/mode/{code}")
    public ResponseEntity<ApiResponse<String>> setMode(
            @PathVariable String code,
            @RequestParam ControlMode mode) {
        signalControlService.setControlMode(code, mode);
        return ResponseEntity.ok(ApiResponse.ok(
                "Mode changed",
                code + " is now in " + mode + " mode"));
    }

    /**
     * POST /api/signals/emergency/{code}
     * Emergency clearance — clear one direction, red all others.
     */
    @PostMapping("/emergency/{code}")
    public ResponseEntity<ApiResponse<String>> emergencyClearance(
            @PathVariable String code,
            @RequestParam String direction) {
        signalControlService.emergencyClearance(code, direction);
        return ResponseEntity.ok(ApiResponse.ok(
                "Emergency clearance activated",
                "Direction " + direction + " cleared at " + code));
    }

    /**
     * POST /api/signals/optimize/{code}
     * Manually trigger optimization for a junction.
     */
    @PostMapping("/optimize/{code}")
    public ResponseEntity<ApiResponse<String>> optimizeJunction(
            @PathVariable String code) {
        signalControlService.optimizeJunction(code);
        return ResponseEntity.ok(ApiResponse.ok(
                "Optimization triggered for " + code));
    }
}