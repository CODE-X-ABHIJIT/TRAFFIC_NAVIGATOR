import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { SidebarComponent } from './components/sidebar/sidebar.component';
import { HeaderComponent } from './components/header/header.component';
import { NotificationService, ToastMessage } from './services/notification.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, SidebarComponent, HeaderComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  sidebarCollapsed = false;
  toasts: ToastMessage[] = [];

  constructor(private notifService: NotificationService) {
    this.notifService.toasts.subscribe(toast => {
      this.toasts.push(toast);
      setTimeout(() => this.removeToast(toast.id), 4000);
    });
  }

  toggleSidebar(): void { this.sidebarCollapsed = !this.sidebarCollapsed; }

  removeToast(id: number): void { this.toasts = this.toasts.filter(t => t.id !== id); }

  getToastIcon(type: string): string {
    const m: any = { success: 'fa-circle-check', error: 'fa-circle-xmark', warning: 'fa-triangle-exclamation', info: 'fa-circle-info' };
    return m[type] || 'fa-circle-info';
  }

  getToastColor(type: string): string {
    const m: any = { success: 'border-emerald-500 bg-emerald-500/10', error: 'border-red-500 bg-red-500/10', warning: 'border-amber-500 bg-amber-500/10', info: 'border-blue-500 bg-blue-500/10' };
    return m[type] || 'border-slate-500';
  }
}