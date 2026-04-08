import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

export interface ToastMessage {
  id: number;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private toasts$ = new Subject<ToastMessage>();
  private counter = 0;
  toasts = this.toasts$.asObservable();

  success(title: string, message = ''): void { this.emit('success', title, message); }
  error(title: string, message = ''): void { this.emit('error', title, message); }
  warning(title: string, message = ''): void { this.emit('warning', title, message); }
  info(title: string, message = ''): void { this.emit('info', title, message); }

  private emit(type: ToastMessage['type'], title: string, message: string): void {
    this.toasts$.next({ id: ++this.counter, type, title, message });
  }
}