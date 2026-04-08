import { Component, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { WebSocketService } from '../../services/websocket.service';
import { ApiService } from '../../services/api.service';
import { Subscription, interval } from 'rxjs';
import { WeatherData } from '../../models/dashboard.model';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './header.component.html'
})
export class HeaderComponent implements OnInit, OnDestroy {
  @Output() toggleSidebar = new EventEmitter<void>();

  currentTime = new Date();
  wsConnected = false;
  alertCount = 0;
  weather: WeatherData | null = null;
  private subs: Subscription[] = [];

  constructor(private wsService: WebSocketService, private apiService: ApiService) {}

  ngOnInit(): void {
    this.subs.push(interval(1000).subscribe(() => this.currentTime = new Date()));
    this.subs.push(this.wsService.isConnected.subscribe(c => this.wsConnected = c));
    this.loadAlertCount();
    this.subs.push(this.wsService.alertStream.subscribe(() => this.loadAlertCount()));
    this.apiService.getWeather().subscribe(w => this.weather = w);
  }

  loadAlertCount(): void {
    this.apiService.getAlertCount().subscribe(res => this.alertCount = res.unacknowledged);
  }

  getWeatherIcon(): string {
    if (!this.weather) return 'fa-cloud';
    switch (this.weather.condition?.toLowerCase()) {
      case 'clear': return 'fa-sun';
      case 'rain': return 'fa-cloud-rain';
      case 'fog': case 'mist': return 'fa-smog';
      default: return 'fa-cloud-sun';
    }
  }

  ngOnDestroy(): void { this.subs.forEach(s => s.unsubscribe()); }
}