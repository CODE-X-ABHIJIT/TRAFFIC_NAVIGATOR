package com.tccs.model.dto;

import com.tccs.model.enums.SignalState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SignalOverrideRequest {

    @NotBlank(message = "Junction code is required")
    private String junctionCode;

    @NotBlank(message = "Direction is required")
    private String direction; // NORTH, SOUTH, EAST, WEST or "ALL"

    @NotNull(message = "Signal state is required")
    private SignalState state;

    private Integer durationSeconds; // optional custom duration
    private String reason;           // why override
}