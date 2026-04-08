import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SignalStatus, SignalState } from '../../models/signal.model';

@Component({
  selector: 'app-signal-light',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './signal-light.component.html',
  styleUrl: './signal-light.component.css'
})
export class SignalLightComponent {
  @Input() signal!: SignalStatus;
  @Input() compact = false;

  isActive(color: SignalState): boolean {
    return this.signal?.state === color;
  }

  getDirectionIcon(): string {
    switch (this.signal?.direction) {
      case 'NORTH': return 'fa-arrow-up';
      case 'SOUTH': return 'fa-arrow-down';
      case 'EAST': return 'fa-arrow-right';
      case 'WEST': return 'fa-arrow-left';
      default: return 'fa-circle';
    }
  }
}