import { Routes } from '@angular/router';

import { AuthGuard } from './core/auth.guard';
import { RoleGuard } from './core/role.guard';
import { ShellComponent } from './layout/shell/shell.component';
import { LoginComponent } from './pages/login/login.component';
import { RoleLandingComponent } from './pages/role-landing/role-landing.component';
import { ClientDashboardComponent } from './pages/client/client-dashboard.component';
import { ClientBriefingsComponent } from './pages/client/client-briefings.component';
import { ClientRequestsComponent } from './pages/client/client-requests.component';
import { AgencyInboxComponent } from './pages/agency/agency-inbox.component';
import { AgencyRequestsComponent } from './pages/agency/agency-requests.component';
import { AdminCompaniesComponent } from './pages/admin/admin-companies.component';
import { AdminUsersComponent } from './pages/admin/admin-users.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: ShellComponent,
    canActivate: [AuthGuard],
    children: [
      { path: '', component: RoleLandingComponent, pathMatch: 'full' },

      {
        path: 'client',
        component: ClientDashboardComponent,
        canActivate: [RoleGuard],
        data: { roles: ['CLIENT_USER'] },
      },
      {
        path: 'client/briefings',
        component: ClientBriefingsComponent,
        canActivate: [RoleGuard],
        data: { roles: ['CLIENT_USER'] },
      },
      {
        path: 'client/requests',
        component: ClientRequestsComponent,
        canActivate: [RoleGuard],
        data: { roles: ['CLIENT_USER'] },
      },

      {
        path: 'agency/briefings/inbox',
        component: AgencyInboxComponent,
        canActivate: [RoleGuard],
        data: { roles: ['AGENCY_USER', 'AGENCY_ADMIN'] },
      },
      {
        path: 'agency/requests',
        component: AgencyRequestsComponent,
        canActivate: [RoleGuard],
        data: { roles: ['AGENCY_USER', 'AGENCY_ADMIN'] },
      },

      {
        path: 'admin/companies',
        component: AdminCompaniesComponent,
        canActivate: [RoleGuard],
        data: { roles: ['AGENCY_ADMIN'] },
      },
      {
        path: 'admin/users',
        component: AdminUsersComponent,
        canActivate: [RoleGuard],
        data: { roles: ['AGENCY_ADMIN'] },
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
