import { Component, Input, Output, EventEmitter, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import * as L from 'leaflet';
import { JunctionStatus } from '../../models/junction.model';
import { IncidentResponse } from '../../models/incident.model';

@Component({
  selector: 'app-traffic-map',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './traffic-map.component.html',
  styleUrl: './traffic-map.component.css'
})
export class TrafficMapComponent implements AfterViewInit, OnDestroy {
  @Input() set junctions(val: JunctionStatus[]) {
    this._junctions = val || [];
    if (this.map) this.updateMarkers();
  }
  @Input() set incidents(val: IncidentResponse[]) {
    this._incidents = val || [];
    if (this.map) this.updateIncidents();
  }
  @Output() junctionClicked = new EventEmitter<string>();

  private map!: L.Map;
  private junctionMarkers: L.Layer[] = [];
  private incidentMarkers: L.Layer[] = [];
  private _junctions: JunctionStatus[] = [];
  private _incidents: IncidentResponse[] = [];
  private mapReady = false;

  ngAfterViewInit(): void {
    this.map = L.map('trafficMap', {
      center: [20.2960, 85.8300],
      zoom: 13,
      zoomControl: false,
      attributionControl: false
    });

    L.control.zoom({ position: 'bottomright' }).addTo(this.map);

    // Satellite View
    const satellite = L.tileLayer(
      'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
      { maxZoom: 19 }
    );

    const labels = L.tileLayer(
      'https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Transportation/MapServer/tile/{z}/{y}/{x}',
      { maxZoom: 19, opacity: 0.7 }
    );

    const placeLabels = L.tileLayer(
      'https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}',
      { maxZoom: 19, opacity: 0.8 }
    );

    // Dark View
    const dark = L.tileLayer(
      'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',
      { maxZoom: 19, subdomains: 'abcd' }
    );

    // Street View
    const street = L.tileLayer(
      'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
      { maxZoom: 19 }
    );

    // Default: Satellite
    const satelliteGroup = L.layerGroup([satellite, labels, placeLabels]);
    satelliteGroup.addTo(this.map);

    L.control.layers(
      { '🛰️ Satellite': satelliteGroup, '🌙 Dark': dark, '🗺️ Street': street },
      {},
      { position: 'topright', collapsed: true }
    ).addTo(this.map);

    setTimeout(() => {
      this.map.invalidateSize();
      this.mapReady = true;
      // Render any data that arrived before map was ready
      this.updateMarkers();
      this.updateIncidents();
    }, 300);
  }

  private updateMarkers(): void {
    if (!this.mapReady || !this.map) return;

    // Clear old markers
    this.junctionMarkers.forEach(m => {
      try { this.map.removeLayer(m); } catch (e) {}
    });
    this.junctionMarkers = [];

    for (const j of this._junctions) {
      const color = this.getColor(j.congestionLevel);
      const r = 10 + j.congestionLevel * 10;

      // Glow ring
      const glow = L.circleMarker([j.latitude, j.longitude], {
        radius: r + 8, color, fillColor: color, fillOpacity: 0.2, weight: 0
      }).addTo(this.map);

      // Main marker
      const marker = L.circleMarker([j.latitude, j.longitude], {
        radius: r, color: '#ffffff', fillColor: color, fillOpacity: 0.9, weight: 2
      }).addTo(this.map);

      // Name label
      const label = L.marker([j.latitude, j.longitude], {
        icon: L.divIcon({
          html: `<div style="background:rgba(15,23,42,0.85);color:#e2e8f0;font-size:10px;font-weight:600;font-family:Inter,sans-serif;padding:2px 8px;border-radius:4px;text-align:center;white-space:nowrap;border:1px solid rgba(51,65,85,0.5)">${j.name}</div>`,
          className: '',
          iconSize: [120, 20],
          iconAnchor: [60, -15]
        })
      }).addTo(this.map);

      const active = j.signals?.find(s => s.state === 'GREEN');
      marker.bindPopup(`
        <div style="min-width:200px;font-family:Inter,sans-serif">
          <div style="font-weight:700;font-size:15px;margin-bottom:4px;color:#fff">${j.name}</div>
          <div style="font-size:11px;color:#94a3b8;margin-bottom:10px">${j.junctionCode} · ${j.controlMode} Mode</div>
          <table style="width:100%;font-size:12px;border-collapse:collapse">
            <tr><td style="color:#94a3b8;padding:3px 0">Congestion</td><td style="color:${color};font-weight:700;text-align:right">${j.congestionLabel}</td></tr>
            <tr><td style="color:#94a3b8;padding:3px 0">Vehicles</td><td style="font-weight:600;text-align:right;color:#fff">${j.totalVehicles}</td></tr>
            <tr><td style="color:#94a3b8;padding:3px 0">Avg Speed</td><td style="font-weight:600;text-align:right;color:#fff">${j.averageSpeed} km/h</td></tr>
            <tr><td style="color:#94a3b8;padding:3px 0">Active Signal</td><td style="color:#22c55e;font-weight:700;text-align:right">${active?.direction || '-'} 🟢 ${active?.countdownSeconds || 0}s</td></tr>
          </table>
          <div style="margin-top:10px;text-align:center;padding-top:8px;border-top:1px solid #334155">
            <span style="color:#3b82f6;font-size:11px;font-weight:600">Click to open Control Panel →</span>
          </div>
        </div>
      `, { className: 'custom-popup' });

      marker.on('click', () => this.junctionClicked.emit(j.junctionCode));
      this.junctionMarkers.push(marker, glow, label);
    }
  }

  private updateIncidents(): void {
    if (!this.mapReady || !this.map) return;

    // Clear old incident markers
    this.incidentMarkers.forEach(m => {
      try { this.map.removeLayer(m); } catch (e) {}
    });
    this.incidentMarkers = [];

    console.log('🗺️ Updating incident markers. Count:', this._incidents.length);

    for (const inc of this._incidents) {
      // Show ALL incidents (both active and recent)
      if (!inc.active) continue;

      console.log('📍 Placing incident marker:', inc.incidentType, inc.latitude, inc.longitude);

      // Determine icon based on type
      const iconHtml = this.getIncidentIconHtml(inc.incidentType);
      const severityColor = this.getSeverityColor(inc.severity);

      const icon = L.divIcon({
        html: `
          <div style="
            width: 40px; height: 40px;
            background: ${severityColor};
            border-radius: 50%;
            display: flex; align-items: center; justify-content: center;
            color: white; font-size: 16px;
            box-shadow: 0 0 20px ${severityColor}, 0 0 40px ${severityColor}66;
            animation: pulse-incident 1.5s ease-in-out infinite;
            border: 3px solid white;
            cursor: pointer;
          ">
            <i class="fas ${iconHtml}"></i>
          </div>
          <style>
            @keyframes pulse-incident {
              0%, 100% { transform: scale(1); box-shadow: 0 0 20px ${severityColor}; }
              50% { transform: scale(1.2); box-shadow: 0 0 30px ${severityColor}, 0 0 60px ${severityColor}66; }
            }
          </style>
        `,
        className: '',
        iconSize: [40, 40],
        iconAnchor: [20, 20]
      });

      const marker = L.marker([inc.latitude, inc.longitude], { icon }).addTo(this.map);

      marker.bindPopup(`
        <div style="min-width:220px;font-family:Inter,sans-serif">
          <div style="display:flex;align-items:center;gap:8px;margin-bottom:8px">
            <div style="width:32px;height:32px;background:${severityColor};border-radius:50%;display:flex;align-items:center;justify-content:center;color:white;font-size:14px">
              <i class="fas ${iconHtml}"></i>
            </div>
            <div>
              <div style="color:${severityColor};font-weight:700;font-size:14px">
                ${inc.incidentType.replace('_', ' ')}
              </div>
              <div style="color:#94a3b8;font-size:10px">
                Incident #${inc.id} · ${inc.severity}
              </div>
            </div>
          </div>
          <p style="font-size:12px;color:#e2e8f0;margin:8px 0;line-height:1.4">
            ${inc.description || 'No description provided'}
          </p>
          <div style="font-size:11px;color:#94a3b8;border-top:1px solid #334155;padding-top:8px;margin-top:8px">
            <div style="display:flex;justify-content:space-between;margin-bottom:4px">
              <span>📍 Location</span>
              <span style="color:#e2e8f0">${inc.latitude.toFixed(4)}, ${inc.longitude.toFixed(4)}</span>
            </div>
            <div style="display:flex;justify-content:space-between;margin-bottom:4px">
              <span>👤 Reported by</span>
              <span style="color:#e2e8f0">${inc.reportedBy || 'System'}</span>
            </div>
            ${inc.junctionCode ? `
            <div style="display:flex;justify-content:space-between;margin-bottom:4px">
              <span>🚦 Junction</span>
              <span style="color:#3b82f6;font-weight:600">${inc.junctionCode}</span>
            </div>` : ''}
            <div style="display:flex;justify-content:space-between">
              <span>⏰ Time</span>
              <span style="color:#e2e8f0">${new Date(inc.reportedAt).toLocaleTimeString()}</span>
            </div>
          </div>
          <div style="margin-top:10px;text-align:center;padding:6px;background:${severityColor}22;border-radius:6px">
            <span style="color:${severityColor};font-size:11px;font-weight:700">
              🚨 AUTO EMERGENCY ACTIVE
            </span>
          </div>
        </div>
      `, { className: 'custom-popup', maxWidth: 280 });

      // Also add a danger zone circle around incident
      const dangerZone = L.circle([inc.latitude, inc.longitude], {
        radius: 300, // 300 meters
        color: severityColor,
        fillColor: severityColor,
        fillOpacity: 0.08,
        weight: 1,
        dashArray: '5, 10'
      }).addTo(this.map);

      this.incidentMarkers.push(marker, dangerZone);
    }

    console.log('✅ Incident markers placed:', this.incidentMarkers.length / 2);
  }

  private getIncidentIconHtml(type: string): string {
    switch (type) {
      case 'ACCIDENT': return 'fa-car-burst';
      case 'VIP_MOVEMENT': return 'fa-star';
      case 'ROADBLOCK': return 'fa-road-barrier';
      case 'CONSTRUCTION': return 'fa-helmet-safety';
      case 'PROTEST': return 'fa-users';
      case 'WEATHER_HAZARD': return 'fa-cloud-bolt';
      default: return 'fa-triangle-exclamation';
    }
  }

  private getSeverityColor(severity: string): string {
    switch (severity) {
      case 'CRITICAL': return '#ef4444';
      case 'HIGH': return '#f97316';
      case 'MEDIUM': return '#eab308';
      default: return '#3b82f6';
    }
  }

  private getColor(level: number): string {
    if (level >= 0.75) return '#ef4444';
    if (level >= 0.50) return '#f97316';
    if (level >= 0.25) return '#eab308';
    return '#22c55e';
  }

  ngOnDestroy(): void {
    if (this.map) this.map.remove();
  }
}