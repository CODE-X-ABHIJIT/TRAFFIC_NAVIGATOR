import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { ApiService } from '../../services/api.service';
import { WebSocketService } from '../../services/websocket.service';
import { NotificationService } from '../../services/notification.service';
import { IncidentRequest, IncidentResponse, IncidentType } from '../../models/incident.model';
import { AlertSeverity } from '../../models/alert.model';

@Component({
  selector: 'app-incidents-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './incidents-page.component.html'
})
export class IncidentsPageComponent implements OnInit, OnDestroy {
  incidents: IncidentResponse[] = [];
  loading = true;
  showForm = false;
  submitting = false;

  form: IncidentRequest = {
    junctionCode: '', incidentType: 'ACCIDENT', latitude: 20.2961, longitude: 85.8245,
    description: '', severity: 'MEDIUM', reportedBy: ''
  };

  incidentTypes: IncidentType[] = ['ACCIDENT', 'VIP_MOVEMENT', 'ROADBLOCK', 'CONSTRUCTION', 'PROTEST', 'WEATHER_HAZARD', 'OTHER'];
  severities: AlertSeverity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  private subs: Subscription[] = [];

  constructor(
    private api: ApiService,
    private ws: WebSocketService,
    private notif: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadIncidents();

    // Auto-refresh when new incident comes via WebSocket
    this.subs.push(this.ws.incidentStream.subscribe(incident => {
      const exists = this.incidents.find(i => i.id === incident.id);
      if (!exists && incident.active) {
        this.incidents = [incident, ...this.incidents];
      }
    }));

    // Also refresh on emergency alerts
    this.subs.push(this.ws.alertStream.subscribe(alert => {
      if (alert.message?.includes('EMERGENCY') || alert.alertType === 'INCIDENT_REPORTED') {
        this.loadIncidents();
      }
    }));
  }

  loadIncidents(): void {
    this.loading = true;
    this.api.getActiveIncidents().subscribe({
      next: (d) => { this.incidents = d; this.loading = false; },
      error: () => this.loading = false
    });
  }

  submitIncident(): void {
    this.submitting = true;
    this.api.reportIncident(this.form).subscribe({
      next: (res) => {
        this.notif.success(
          '🚨 Incident Reported & Emergency Activated',
          `Incident #${res.id} — Auto clearance triggered for nearby junctions`
        );
        this.showForm = false;
        this.submitting = false;
        this.loadIncidents();
        this.resetForm();
      },
      error: () => {
        this.notif.error('Failed to report incident');
        this.submitting = false;
      }
    });
  }

  resolveIncident(id: number): void {
    if (confirm('Resolve this incident? Signals will revert to AUTO mode.')) {
      this.api.resolveIncident(id).subscribe({
        next: () => {
          this.notif.success('✅ Incident Resolved', 'Signals reverting to AUTO mode');
          this.loadIncidents();
        },
        error: () => this.notif.error('Failed to resolve')
      });
    }
  }

  resetForm(): void {
    this.form = {
      junctionCode: '', incidentType: 'ACCIDENT', latitude: 20.2961, longitude: 85.8245,
      description: '', severity: 'MEDIUM', reportedBy: ''
    };
  }

  getTypeIcon(t: string): string {
    const m: any = {
      ACCIDENT: 'fa-car-burst text-red-400',
      VIP_MOVEMENT: 'fa-star text-amber-400',
      ROADBLOCK: 'fa-road-barrier text-orange-400',
      CONSTRUCTION: 'fa-helmet-safety text-yellow-400',
      PROTEST: 'fa-users text-purple-400',
      WEATHER_HAZARD: 'fa-cloud-bolt text-blue-400'
    };
    return m[t] || 'fa-circle-exclamation text-slate-400';
  }

  getSeverityColor(s: string): string {
    const m: any = { CRITICAL: 'text-red-400', HIGH: 'text-orange-400', MEDIUM: 'text-amber-400' };
    return m[s] || 'text-blue-400';
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }
}