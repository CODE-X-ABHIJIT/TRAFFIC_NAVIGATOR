package com.tccs.controller;

import com.tccs.model.dto.ApiResponse;
import com.tccs.model.dto.JunctionStatusDTO;
import com.tccs.service.TrafficDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/traffic")
@RequiredArgsConstructor
public class TrafficController {

    private final TrafficDataService trafficDataService;

    /**
     * GET /api/traffic/live
     * Live traffic data for ALL junctions.
     */
    @GetMapping("/live")
    public ResponseEntity<ApiResponse<List<JunctionStatusDTO>>> getLiveTraffic() {
        List<JunctionStatusDTO> statuses = trafficDataService.getAllJunctionStatuses();
        return ResponseEntity.ok(ApiResponse.ok(statuses));
    }

    /**
     * GET /api/traffic/junction/{code}
     * Live traffic data for a specific junction.
     */
    @GetMapping("/junction/{code}")
    public ResponseEntity<ApiResponse<JunctionStatusDTO>> getJunctionTraffic(
            @PathVariable String code) {
        JunctionStatusDTO status = trafficDataService.getJunctionStatus(code);
        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    /**
     * POST /api/traffic/refresh
     * Force a traffic data refresh.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> forceRefresh() {
        trafficDataService.refreshAllTrafficData();
        return ResponseEntity.ok(ApiResponse.ok("Traffic data refreshed"));
    }
}