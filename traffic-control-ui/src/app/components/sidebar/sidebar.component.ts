import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html'
})
export class SidebarComponent {
  @Input() collapsed = false;
  @Output() toggle = new EventEmitter<void>();

  navItems = [
    { icon: 'fa-gauge-high', label: 'Dashboard', route: '/dashboard' },
    { icon: 'fa-triangle-exclamation', label: 'Alerts', route: '/alerts' },
    { icon: 'fa-car-burst', label: 'Incidents', route: '/incidents' },
    { icon: 'fa-chart-line', label: 'Analytics', route: '/analytics' }
  ];
}