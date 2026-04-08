import { JunctionStatus } from './junction.model';

export interface DashboardSummary {
  totalJunctions: number;
  activeJunctions: number;
  autoModeCount: number;
  manualModeCount: number;
  activeIncidents: number;
  unacknowledgedAlerts: number;
  averageCongestion: number;
  totalVehiclesDetected: number;
  averageCitySpeed: number;
  overallStatus: string;
  junctions: JunctionStatus[];
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface WeatherData {
  condition: string;
  temperature: number;
  humidity: number;
  windSpeed: number;
  visibility: number;
  hazardous: boolean;
}