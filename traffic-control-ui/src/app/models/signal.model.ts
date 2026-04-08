export type SignalState = 'RED' | 'GREEN' | 'YELLOW' | 'BLINKING';
export type ControlMode = 'AUTO' | 'MANUAL';

export interface SignalStatus {
  signalId: number;
  direction: string;
  state: SignalState;
  greenDuration: number;
  countdownSeconds: number;
  vehicleDensity: number;
  vehicleSpeed: number;
  freeFlowSpeed: number;
  phaseOrder: number;
}

export interface SignalOverrideRequest {
  junctionCode: string;
  direction: string;
  state: SignalState;
  durationSeconds?: number;
  reason?: string;
}