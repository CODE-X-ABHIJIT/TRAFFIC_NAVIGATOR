export type AlertType = 'CONGESTION' | 'SPEED_DROP' | 'SIGNAL_MALFUNCTION' |
                        'WEATHER_WARNING' | 'INCIDENT_REPORTED' | 'DENSITY_SPIKE';
export type AlertSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface Alert {
  id: number;
  junctionCode: string;
  alertType: AlertType;
  severity: AlertSeverity;
  message: string;
  acknowledged: boolean;
  createdAt:Date;
  acknowledgedAt: string | null;
}