import { AlertSeverity } from './alert.model';

export type IncidentType = 'ACCIDENT' | 'VIP_MOVEMENT' | 'ROADBLOCK' |
                           'CONSTRUCTION' | 'PROTEST' | 'WEATHER_HAZARD' | 'OTHER';

export interface IncidentRequest {
  junctionCode?: string;
  incidentType: IncidentType;
  latitude: number;
  longitude: number;
  description?: string;
  severity?: AlertSeverity;
  reportedBy?: string;
}

export interface IncidentResponse {
  id: number;
  junctionCode: string;
  incidentType: IncidentType;
  latitude: number;
  longitude: number;
  description: string;
  severity: AlertSeverity;
  active: boolean;
  reportedBy: string;
  reportedAt: string;
  resolvedAt: string | null;
}
