import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse, DashboardSummary, WeatherData } from '../models/dashboard.model';
import { JunctionStatus, CongestionHeatmapPoint } from '../models/junction.model';
import { SignalOverrideRequest, ControlMode } from '../models/signal.model';
import { Alert } from '../models/alert.model';
import { IncidentRequest, IncidentResponse } from '../models/incident.model';

@Injectable({ providedIn: 'root' })
export class ApiService {

  private base = 'http://localhost:8090/api';

  constructor(private http: HttpClient) {}

  private unwrap<T>(obs: Observable<ApiResponse<T>>): Observable<T> {
    return obs.pipe(map(res => res.data));
  }

  // Dashboard
  getDashboardSummary(): Observable<DashboardSummary> {
    return this.unwrap(this.http.get<ApiResponse<DashboardSummary>>(`${this.base}/dashboard/summary`));
  }

  getHeatmapData(): Observable<CongestionHeatmapPoint[]> {
    return this.unwrap(this.http.get<ApiResponse<CongestionHeatmapPoint[]>>(`${this.base}/dashboard/heatmap`));
  }

  getWeather(): Observable<WeatherData> {
    return this.unwrap(this.http.get<ApiResponse<WeatherData>>(`${this.base}/dashboard/weather`));
  }

  // Traffic
  getLiveTraffic(): Observable<JunctionStatus[]> {
    return this.unwrap(this.http.get<ApiResponse<JunctionStatus[]>>(`${this.base}/traffic/live`));
  }

  getJunctionTraffic(code: string): Observable<JunctionStatus> {
    return this.unwrap(this.http.get<ApiResponse<JunctionStatus>>(`${this.base}/traffic/junction/${code}`));
  }

  refreshTraffic(): Observable<any> {
    return this.http.post(`${this.base}/traffic/refresh`, {});
  }

  // Signal Control
  overrideSignal(req: SignalOverrideRequest): Observable<any> {
    return this.http.post(`${this.base}/signals/override`, req);
  }

  setControlMode(code: string, mode: ControlMode): Observable<any> {
    return this.http.put(`${this.base}/signals/mode/${code}?mode=${mode}`, {});
  }

  emergencyClearance(code: string, direction: string): Observable<any> {
    return this.http.post(`${this.base}/signals/emergency/${code}?direction=${direction}`, {});
  }

  optimizeJunction(code: string): Observable<any> {
    return this.http.post(`${this.base}/signals/optimize/${code}`, {});
  }

  // Alerts
  getRecentAlerts(): Observable<Alert[]> {
    return this.unwrap(this.http.get<ApiResponse<Alert[]>>(`${this.base}/alerts`));
  }

  getUnacknowledgedAlerts(): Observable<Alert[]> {
    return this.unwrap(this.http.get<ApiResponse<Alert[]>>(`${this.base}/alerts/unacknowledged`));
  }

  getAlertCount(): Observable<{ unacknowledged: number }> {
    return this.unwrap(this.http.get<ApiResponse<{ unacknowledged: number }>>(`${this.base}/alerts/count`));
  }

  acknowledgeAlert(id: number): Observable<any> {
    return this.http.put(`${this.base}/alerts/${id}/acknowledge`, {});
  }

  acknowledgeAllAlerts(): Observable<any> {
    return this.http.put(`${this.base}/alerts/acknowledge-all`, {});
  }

  // Incidents
  getActiveIncidents(): Observable<IncidentResponse[]> {
    return this.unwrap(this.http.get<ApiResponse<IncidentResponse[]>>(`${this.base}/incidents`));
  }

  reportIncident(req: IncidentRequest): Observable<IncidentResponse> {
    return this.unwrap(this.http.post<ApiResponse<IncidentResponse>>(`${this.base}/incidents`, req));
  }

  resolveIncident(id: number): Observable<any> {
    return this.http.put(`${this.base}/incidents/${id}/resolve`, {});
  }

  // Analytics
  getCongestionTrends(junctionCode?: string, hours: number = 6): Observable<any[]> {
    let params = new HttpParams().set('hours', hours.toString());
    if (junctionCode) params = params.set('junctionCode', junctionCode);
    return this.unwrap(this.http.get<ApiResponse<any[]>>(`${this.base}/analytics/congestion-trends`, { params }));
  }

  getPeakHours(days: number = 7): Observable<{ [hour: number]: number }> {
    const params = new HttpParams().set('days', days.toString());
    return this.unwrap(this.http.get<ApiResponse<{ [hour: number]: number }>>(`${this.base}/analytics/peak-hours`, { params }));
  }
}