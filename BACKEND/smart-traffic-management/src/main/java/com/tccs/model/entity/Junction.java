package com.tccs.model.entity;

import com.tccs.model.enums.ControlMode;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "junctions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Junction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "junction_code", unique = true, nullable = false, length = 10)
    private String junctionCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "control_mode", nullable = false)
    @Builder.Default
    private ControlMode controlMode = ControlMode.AUTO;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "total_cycle_time")
    @Builder.Default
    private int totalCycleTime = 120; // seconds

    @Column(name = "current_phase_index")
    @Builder.Default
    private int currentPhaseIndex = 0;

    @OneToMany(mappedBy = "junction", cascade = CascadeType.ALL,
               fetch = FetchType.EAGER, orphanRemoval = true)
    @Builder.Default
    private List<Signal> signals = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper to add signal with bidirectional relationship
    public void addSignal(Signal signal) {
        signals.add(signal);
        signal.setJunction(this);
    }
}