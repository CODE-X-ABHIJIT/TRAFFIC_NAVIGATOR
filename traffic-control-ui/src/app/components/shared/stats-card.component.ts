import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-stats-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="card hover:border-slate-600 transition-all duration-300 group cursor-default">
      <div class="flex items-start justify-between">
        <div>
          <p class="text-xs font-medium text-slate-400 uppercase tracking-wider">{{ label }}</p>
          <p class="text-2xl font-bold mt-1 font-mono" [ngClass]="valueColor">{{ value }}{{ suffix }}</p>
          @if (subtext) { <p class="text-xs text-slate-500 mt-1">{{ subtext }}</p> }
        </div>
        <div class="w-10 h-10 rounded-lg flex items-center justify-center group-hover:scale-110 transition-transform"
             [ngClass]="iconBg">
          <i class="fas {{ icon }} text-sm" [ngClass]="iconColor"></i>
        </div>
      </div>
    </div>
  `
})
export class StatsCardComponent {
  @Input() label = '';
  @Input() value: string | number = '';
  @Input() suffix = '';
  @Input() subtext = '';
  @Input() icon = 'fa-chart-bar';
  @Input() iconBg = 'bg-blue-500/20';
  @Input() iconColor = 'text-blue-400';
  @Input() valueColor = 'text-white';
}