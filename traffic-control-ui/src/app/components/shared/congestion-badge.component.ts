import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-congestion-badge',
  standalone: true,
  imports: [CommonModule],
  template: `<span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold" [ngClass]="getClass()">{{ label }}</span>`
})
export class CongestionBadgeComponent {
  @Input() label = 'LOW';

  getClass(): string {
    switch (this.label) {
      case 'CRITICAL': return 'bg-red-500/20 text-red-400 border border-red-500/40';
      case 'HIGH': return 'bg-orange-500/20 text-orange-400 border border-orange-500/40';
      case 'MEDIUM': return 'bg-amber-500/20 text-amber-400 border border-amber-500/40';
      default: return 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/40';
    }
  }
}