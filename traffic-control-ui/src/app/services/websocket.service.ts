import { Injectable, OnDestroy } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { BehaviorSubject, Subject } from 'rxjs';
import { JunctionStatus } from '../models/junction.model';
import { Alert } from '../models/alert.model';
import { IncidentResponse } from '../models/incident.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class WebSocketService implements OnDestroy {

  private client!: Client;

  private liveSignals$ = new BehaviorSubject<JunctionStatus[]>([]);
  private alertStream$ = new Subject<Alert>();
  private incidentStream$ = new Subject<IncidentResponse>();
  private overrideStream$ = new Subject<string>();
  private connected$ = new BehaviorSubject<boolean>(false);

  liveSignals = this.liveSignals$.asObservable();
  alertStream = this.alertStream$.asObservable();
  incidentStream = this.incidentStream$.asObservable();
  overrideNotifications = this.overrideStream$.asObservable();
  isConnected = this.connected$.asObservable();

  constructor() {
    this.connect();
  }

  connect(): void {
    this.client = new Client({
      brokerURL: environment.wsBaseUrl,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      onConnect: () => {
        console.log('✅ WebSocket Connected');
        this.connected$.next(true);
        this.subscribeAll();
      },

      onDisconnect: () => {
        console.log('❌ WebSocket Disconnected');
        this.connected$.next(false);
      },

      onStompError: (frame) => {
        console.error('STOMP Error:', frame.headers['message']);
        this.connected$.next(false);
      },

      onWebSocketError: () => {
        this.connected$.next(false);
      }
    });

    this.client.activate();
  }

  private subscribeAll(): void {
    // Live signal countdowns
    this.client.subscribe('/topic/live-signals', (msg: IMessage) => {
      this.liveSignals$.next(JSON.parse(msg.body));
    });

    // Traffic data updates
    this.client.subscribe('/topic/junction-updates', (msg: IMessage) => {
      this.liveSignals$.next(JSON.parse(msg.body));
    });

    // Real-time alerts
    this.client.subscribe('/topic/alerts', (msg: IMessage) => {
      try {
        const parsed = JSON.parse(msg.body);
        if (parsed.id !== undefined) {
          this.alertStream$.next(parsed);
        } else {
          this.alertStream$.next({
            id: 0, junctionCode: '', alertType: 'INCIDENT_REPORTED',
            severity: 'HIGH', message: typeof parsed === 'string' ? parsed : msg.body,
            acknowledged: false, createdAt: new Date().toISOString(),
            acknowledgedAt: null
          });
        }
      } catch {
        this.alertStream$.next({
          id: 0, junctionCode: '', alertType: 'INCIDENT_REPORTED',
          severity: 'MEDIUM', message: msg.body,
          acknowledged: false, createdAt: new Date().toISOString(),
          acknowledgedAt: null
        });
      }
    });

    // Incident stream
    this.client.subscribe('/topic/incidents', (msg: IMessage) => {
      try {
        const incident: IncidentResponse = JSON.parse(msg.body);
        this.incidentStream$.next(incident);
        console.log('🚨 Incident received via WebSocket:', incident);
      } catch (e) {
        console.error('Failed to parse incident:', e);
      }
    });

    // Signal override notifications
    this.client.subscribe('/topic/signal-override', (msg: IMessage) => {
      this.overrideStream$.next(msg.body);
      // Treat override as alert too
      this.alertStream$.next({
        id: 0, junctionCode: '', alertType: 'INCIDENT_REPORTED',
        severity: 'HIGH', message: '🚦 ' + msg.body,
        acknowledged: false, createdAt: new Date().toISOString(),
        acknowledgedAt: null
      });
    });
  }

  ngOnDestroy(): void {
    if (this.client?.active) this.client.deactivate();
  }
}