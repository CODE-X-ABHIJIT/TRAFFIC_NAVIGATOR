import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { ApiService } from '../../services/api.service';
import { WebSocketService } from '../../services/websocket.service';
import { NotificationService } from '../../services/notification.service';
import { JunctionStatus } from '../../models/junction.model';
import { SignalState, ControlMode } from '../../models/signal.model';
import { SignalLightComponent } from '../signal-light/signal-light.component';
import { CongestionBadgeComponent } from '../shared/congestion-badge.component';

@Component({
  selector: 'app-junction-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, SignalLightComponent, CongestionBadgeComponent],
  templateUrl: './junction-detail.component.html',
  styleUrl: './junction-detail.component.css'
})
export class JunctionDetailComponent implements OnInit, OnDestroy {
  junctionCode = '';
  junction: JunctionStatus | null = null;
  loading = true;

  overrideDirection = '';
  overrideState: SignalState = 'GREEN';
  overrideDuration = 30;
  overrideReason = '';
  emergencyDirection = '';

  private subs: Subscription[] = [];

  constructor(
    private route: ActivatedRoute, private router: Router,
    private api: ApiService, private ws: WebSocketService,
    private notif: NotificationService
  ) {}

  ngOnInit(): void {
    this.junctionCode = this.route.snapshot.paramMap.get('code') || '';
    this.loadJunction();
    this.subs.push(this.ws.liveSignals.subscribe(data => {
      const u = data.find(j => j.junctionCode === this.junctionCode);
      if (u) { this.junction = u; this.loading = false; }
    }));
  }

  loadJunction(): void {
    this.api.getJunctionTraffic(this.junctionCode).subscribe({
      next: (d) => { this.junction = d; this.loading = false; },
      error: () => { this.notif.error('Junction not found'); this.router.navigate(['/dashboard']); }
    });
  }

  toggleMode(): void {
    if (!this.junction) return;
    const newMode: ControlMode = this.junction.controlMode === 'AUTO' ? 'MANUAL' : 'AUTO';
    this.api.setControlMode(this.junctionCode, newMode).subscribe({
      next: () => { this.notif.success('Mode Changed', `Now ${newMode}`); this.loadJunction(); },
      error: () => this.notif.error('Failed to change mode')
    });
  }

  submitOverride(): void {
    if (!this.overrideDirection) { this.notif.warning('Select a direction'); return; }
    this.api.overrideSignal({
      junctionCode: this.junctionCode, direction: this.overrideDirection,
      state: this.overrideState, durationSeconds: this.overrideDuration, reason: this.overrideReason
    }).subscribe({
      next: () => { this.notif.success('Override Applied', `${this.overrideDirection} → ${this.overrideState}`); this.overrideReason = ''; },
      error: () => this.notif.error('Override failed')
    });
  }

  submitEmergency(): void {
    if (!this.emergencyDirection) { this.notif.warning('Select direction'); return; }
    if (confirm(`⚠️ EMERGENCY: All RED except ${this.emergencyDirection}. Proceed?`)) {
      this.api.emergencyClearance(this.junctionCode, this.emergencyDirection).subscribe({
        next: () => this.notif.success('Emergency Active', `${this.emergencyDirection} cleared`),
        error: () => this.notif.error('Emergency failed')
      });
    }
  }

  triggerOptimize(): void {
    this.api.optimizeJunction(this.junctionCode).subscribe({
      next: () => this.notif.success('Optimization Triggered'),
      error: () => this.notif.error('Optimization failed')
    });
  }

  ngOnDestroy(): void { this.subs.forEach(s => s.unsubscribe()); }
}