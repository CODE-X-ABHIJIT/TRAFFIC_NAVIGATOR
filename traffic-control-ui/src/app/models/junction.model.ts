import { ControlMode, SignalStatus } from './signal.model';

export interface JunctionStatus {
  junctionId: number;
  junctionCode: string;
  name: string;
  latitude: number;
  longitude: number;
  controlMode: ControlMode;
  active: boolean;
  totalCycleTime: number;
  currentPhaseIndex: number;
  congestionLevel: number;
  congestionLabel: string;
  totalVehicles: number;
  averageSpeed: number;
  signals: SignalStatus[];
}

export interface CongestionHeatmapPoint {
  junctionCode: string;
  junctionName: string;
  latitude: number;
  longitude: number;
  congestionLevel: number;
  intensity: number;
  vehicleDensity: number;
  avgSpeed: number;
}