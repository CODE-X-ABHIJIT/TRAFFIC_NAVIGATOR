import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { ApiService } from '../../services/api.service';
import { WebSocketService } from '../../services/websocket.service';
import { NotificationService } from '../../services/notification.service';
import { Alert } from '../../models/alert.model';

@Component({
  selector: 'app-alerts-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './alerts-page.component.html'
})
export class AlertsPageComponent implements OnInit, OnDestroy {
  alerts: Alert[] = [];
  loading = true;
  filter: 'all' | 'unacknowledged' = 'unacknowledged';
  private subs: Subscription[] = [];

  constructor(private api: ApiService, private ws: WebSocketService, private notif: NotificationService) {}

  ngOnInit(): void {
    this.loadAlerts();
    this.subs.push(this.ws.alertStream.subscribe(a => this.alerts = [a, ...this.alerts]));
  }

  loadAlerts(): void {
    this.loading = true;
    const src = this.filter === 'unacknowledged' ? this.api.getUnacknowledgedAlerts() : this.api.getRecentAlerts();
    src.subscribe({ next: (d) => { this.alerts = d; this.loading = false; }, error: () => this.loading = false });
  }

  setFilter(f: 'all' | 'unacknowledged'): void { this.filter = f; this.loadAlerts(); }

  acknowledge(id: number): void {
    this.api.acknowledgeAlert(id).subscribe(() => {
      this.alerts = this.alerts.map(a => a.id === id ? { ...a, acknowledged: true } : a);
      this.notif.success('Alert acknowledged');
    });
  }

  acknowledgeAll(): void {
    this.api.acknowledgeAllAlerts().subscribe(() => {
      this.alerts = this.alerts.map(a => ({ ...a, acknowledged: true }));
      this.notif.success('All acknowledged');
    });
  }

  getSeverityClass(s: string): string {
    const m: any = { CRITICAL: 'border-l-red-500 bg-red-500/5', HIGH: 'border-l-orange-500 bg-orange-500/5', MEDIUM: 'border-l-amber-500 bg-amber-500/5' };
    return m[s] || 'border-l-blue-500 bg-blue-500/5';
  }

  getSeverityBadge(s: string): string {
    const m: any = { CRITICAL: 'bg-red-500/20 text-red-400', HIGH: 'bg-orange-500/20 text-orange-400', MEDIUM: 'bg-amber-500/20 text-amber-400' };
    return m[s] || 'bg-blue-500/20 text-blue-400';
  }

  getTypeIcon(t: string): string {
    const m: any = { CONGESTION: 'fa-car text-orange-400', SPEED_DROP: 'fa-gauge-simple text-red-400', WEATHER_WARNING: 'fa-cloud-bolt text-blue-400', INCIDENT_REPORTED: 'fa-car-burst text-red-400', DENSITY_SPIKE: 'fa-users text-amber-400' };
    return m[t] || 'fa-circle-exclamation text-slate-400';
  }

  ngOnDestroy(): void { this.subs.forEach(s => s.unsubscribe()); }
}