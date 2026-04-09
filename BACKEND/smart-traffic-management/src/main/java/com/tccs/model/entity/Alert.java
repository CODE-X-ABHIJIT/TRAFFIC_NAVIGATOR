package com.tccs.model.entity;

import com.tccs.model.enums.AlertSeverity;
import com.tccs.model.enums.AlertType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "alerts")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "junction_code")
    private String junctionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AlertSeverity severity = AlertSeverity.MEDIUM;

    @Column(nullable = false, length = 300)
    private String message;

    @Column(name = "is_acknowledged")
    @Builder.Default
    private boolean acknowledged = false;

    @Column(name = "created_at")
private OffsetDateTime createdAt;

@Column(name = "acknowledged_at")
private OffsetDateTime acknowledgedAt;

@PrePersist
protected void onCreate() {
    createdAt = OffsetDateTime.now(ZoneId.of("Asia/Kolkata"));
}

}