import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  Chart, ChartConfiguration,
  LineController, BarController,
  LineElement, BarElement, PointElement,
  CategoryScale, LinearScale,
  Filler, Legend, Tooltip
} from 'chart.js';
import { ApiService } from '../../services/api.service';
import { CongestionHeatmapPoint } from '../../models/junction.model';

Chart.register(LineController, BarController, LineElement, BarElement, PointElement, CategoryScale, LinearScale, Filler, Legend, Tooltip);

@Component({
  selector: 'app-analytics-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './analytics-page.component.html',
  styleUrl: './analytics-page.component.css'
})
export class AnalyticsPageComponent implements AfterViewInit, OnDestroy {
  @ViewChild('trendCanvas') trendCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('peakCanvas') peakCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('compCanvas') compCanvas!: ElementRef<HTMLCanvasElement>;

  private trendChart: Chart | null = null;
  private peakChart: Chart | null = null;
  private compChart: Chart | null = null;

  heatmapData: CongestionHeatmapPoint[] = [];
  selectedHours = 6;
  loading = true;

  constructor(private api: ApiService) {}

  ngAfterViewInit(): void { setTimeout(() => this.loadAll(), 300); }

  loadAll(): void {
    this.loading = true;
    this.loadTrends();
    this.loadPeakHours();
    this.loadHeatmap();
  }

  loadTrends(): void {
    this.api.getCongestionTrends(undefined, this.selectedHours).subscribe(data => {
      const grouped = new Map<string, any[]>();
      data.forEach((d: any) => { const l = grouped.get(d.junctionCode) || []; l.push(d); grouped.set(d.junctionCode, l); });

      const colors = ['#3b82f6','#ef4444','#22c55e','#eab308','#a855f7','#f97316'];
      const datasets: any[] = []; let idx = 0;
      grouped.forEach((pts, code) => {
        datasets.push({ label: code, data: pts.map((p:any) => p.congestionLevel), borderColor: colors[idx%colors.length], backgroundColor: colors[idx%colors.length]+'33', fill: true, tension: 0.4, pointRadius: 0, borderWidth: 2 });
        idx++;
      });

      const first = grouped.values().next().value || [];
      const labels = first.map((p:any) => new Date(p.timestamp).toLocaleTimeString([], { hour:'2-digit', minute:'2-digit' }));

      if (this.trendChart) this.trendChart.destroy();
      this.trendChart = new Chart(this.trendCanvas.nativeElement, {
        type: 'line', data: { labels, datasets },
        options: { responsive: true, maintainAspectRatio: false,
          plugins: { legend: { labels: { color: '#94a3b8', font: { size: 11 } } }, tooltip: { callbacks: { label: (c) => `${c.dataset.label}: ${(c.parsed.y*100).toFixed(0)}%` } } },
          scales: { x: { ticks: { color: '#64748b', font: { size: 10 }, maxTicksLimit: 10 }, grid: { color: '#1e293b' } }, y: { min: 0, max: 1, ticks: { color: '#64748b', font: { size: 10 }, callback: (v) => `${(+v*100).toFixed(0)}%` }, grid: { color: '#1e293b' } } }
        }
      });
      this.loading = false;
    });
  }

  loadPeakHours(): void {
    this.api.getPeakHours(7).subscribe(data => {
      const hours = Object.keys(data).map(h => `${h}:00`);
      const values = Object.values(data) as number[];
      const bg = values.map(v => v>=0.7?'#ef444499':v>=0.5?'#f9731699':v>=0.3?'#eab30899':'#22c55e99');

      if (this.peakChart) this.peakChart.destroy();
      this.peakChart = new Chart(this.peakCanvas.nativeElement, {
        type: 'bar', data: { labels: hours, datasets: [{ data: values, backgroundColor: bg, borderRadius: 4, borderSkipped: false }] },
        options: { responsive: true, maintainAspectRatio: false,
          plugins: { legend: { display: false }, tooltip: { callbacks: { label: (c) => `Congestion: ${(c.parsed.y*100).toFixed(0)}%` } } },
          scales: { x: { ticks: { color: '#64748b', font: { size: 10 } }, grid: { color: '#1e293b' } }, y: { min: 0, max: 1, ticks: { color: '#64748b', font: { size: 10 }, callback: (v) => `${(+v*100).toFixed(0)}%` }, grid: { color: '#1e293b' } } }
        }
      });
    });
  }

  loadHeatmap(): void {
    this.api.getHeatmapData().subscribe(data => {
      this.heatmapData = data.sort((a, b) => b.congestionLevel - a.congestionLevel);
      const bg = data.map(d => d.congestionLevel>=0.7?'#ef444499':d.congestionLevel>=0.5?'#f9731699':d.congestionLevel>=0.3?'#eab30899':'#22c55e99');

      if (this.compChart) this.compChart.destroy();
      this.compChart = new Chart(this.compCanvas.nativeElement, {
        type: 'bar', data: { labels: data.map(d => d.junctionName), datasets: [{ data: data.map(d => d.congestionLevel), backgroundColor: bg, borderRadius: 4 }] },
        options: { responsive: true, maintainAspectRatio: false, indexAxis: 'y',
          plugins: { legend: { display: false }, tooltip: { callbacks: { label: (c) => `${(c.parsed.x*100).toFixed(0)}%` } } },
          scales: { x: { min: 0, max: 1, ticks: { color: '#64748b', callback: (v) => `${(+v*100).toFixed(0)}%` }, grid: { color: '#1e293b' } }, y: { ticks: { color: '#94a3b8', font: { size: 11 } }, grid: { display: false } } }
        }
      });
    });
  }

  onHoursChange(): void { this.loadTrends(); }

  getCongestionColor(l: number): string { return l>=0.75?'text-red-400':l>=0.5?'text-orange-400':l>=0.25?'text-amber-400':'text-emerald-400'; }
  getCongestionBg(l: number): string { return l>=0.75?'bg-red-500':l>=0.5?'bg-orange-500':l>=0.25?'bg-amber-500':'bg-emerald-500'; }

  ngOnDestroy(): void {
    if (this.trendChart) this.trendChart.destroy();
    if (this.peakChart) this.peakChart.destroy();
    if (this.compChart) this.compChart.destroy();
  }
}