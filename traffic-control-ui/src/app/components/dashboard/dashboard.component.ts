import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { ApiService } from '../../services/api.service';
import { WebSocketService } from '../../services/websocket.service';
import { DashboardSummary } from '../../models/dashboard.model';
import { JunctionStatus } from '../../models/junction.model';
import { Alert } from '../../models/alert.model';
import { IncidentResponse } from '../../models/incident.model';
import { StatsCardComponent } from '../shared/stats-card.component';
import { CongestionBadgeComponent } from '../shared/congestion-badge.component';
import { TrafficMapComponent } from '../traffic-map/traffic-map.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, StatsCardComponent, CongestionBadgeComponent, TrafficMapComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit, OnDestroy {
  summary: DashboardSummary | null = null;
  junctions: JunctionStatus[] = [];
  recentAlerts: Alert[] = [];
  activeIncidents: IncidentResponse[] = [];
  loading = true;
  private subs: Subscription[] = [];

  constructor(private api: ApiService, private ws: WebSocketService, private router: Router) {}

  ngOnInit(): void {
    this.loadDashboard();
    this.loadAlerts();
    this.loadIncidents();

    // Live signal updates
    this.subs.push(this.ws.liveSignals.subscribe(data => {
      if (data.length > 0) { this.junctions = data; this.loading = false; }
    }));

    // New alerts — reload alerts and incidents
    this.subs.push(this.ws.alertStream.subscribe(alert => {
      this.recentAlerts = [alert, ...this.recentAlerts].slice(0, 10);
      // Reload incidents when emergency alert comes
      if (alert.alertType === 'INCIDENT_REPORTED' || alert.message?.includes('EMERGENCY')) {
        this.loadIncidents();
      }
    }));

    // Listen for new incidents via WebSocket
    this.subs.push(this.ws.incidentStream.subscribe(incident => {
      // Add to list if not already present
      const exists = this.activeIncidents.find(i => i.id === incident.id);
      if (!exists && incident.active) {
        this.activeIncidents = [incident, ...this.activeIncidents];
      } else if (exists && !incident.active) {
        this.activeIncidents = this.activeIncidents.filter(i => i.id !== incident.id);
      }
    }));

    // Auto-refresh incidents every 15 seconds
    this.subs.push(interval(15000).subscribe(() => {
      this.loadIncidents();
    }));
  }

  loadDashboard(): void {
    this.api.getDashboardSummary().subscribe({
      next: (d) => { this.summary = d; this.junctions = d.junctions; this.loading = false; },
      error: () => this.loading = false
    });
  }

  loadAlerts(): void {
    this.api.getUnacknowledgedAlerts().subscribe(a => this.recentAlerts = a.slice(0, 10));
  }

  loadIncidents(): void {
    this.api.getActiveIncidents().subscribe(i => {
      this.activeIncidents = i;
    });
  }

  onJunctionClick(code: string): void { this.router.navigate(['/junction', code]); }

  acknowledgeAlert(id: number): void {
    this.api.acknowledgeAlert(id).subscribe(() => {
      this.recentAlerts = this.recentAlerts.filter(a => a.id !== id);
    });
  }

  getCongestionBarColor(level: number): string {
    if (level >= 0.75) return 'bg-red-500';
    if (level >= 0.50) return 'bg-orange-500';
    if (level >= 0.25) return 'bg-amber-500';
    return 'bg-emerald-500';
  }

  getCongestionBarWidth(level: number): string { return `${Math.round(level * 100)}%`; }

  getAlertIcon(type: string): string {
    const m: any = { CONGESTION: 'fa-car text-orange-400', SPEED_DROP: 'fa-gauge-simple text-red-400', WEATHER_WARNING: 'fa-cloud-bolt text-blue-400', INCIDENT_REPORTED: 'fa-car-burst text-red-400', DENSITY_SPIKE: 'fa-users text-amber-400' };
    return m[type] || 'fa-circle-exclamation text-slate-400';
  }

  getAlertBorder(severity: string): string {
    const m: any = { CRITICAL: 'border-l-red-500', HIGH: 'border-l-orange-500', MEDIUM: 'border-l-amber-500' };
    return m[severity] || 'border-l-blue-500';
  }

  ngOnDestroy(): void { this.subs.forEach(s => s.unsubscribe()); }
}