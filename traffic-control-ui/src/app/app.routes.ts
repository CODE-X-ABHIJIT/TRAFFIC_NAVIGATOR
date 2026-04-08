import { Routes } from '@angular/router';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { JunctionDetailComponent } from './components/junction-detail/junction-detail.component';
import { AlertsPageComponent } from './components/alerts-page/alerts-page.component';
import { IncidentsPageComponent } from './components/incidents-page/incidents-page.component';
import { AnalyticsPageComponent } from './components/analytics-page/analytics-page.component';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'junction/:code', component: JunctionDetailComponent },
  { path: 'alerts', component: AlertsPageComponent },
  { path: 'incidents', component: IncidentsPageComponent },
  { path: 'analytics', component: AnalyticsPageComponent },
  { path: '**', redirectTo: 'dashboard' }
];