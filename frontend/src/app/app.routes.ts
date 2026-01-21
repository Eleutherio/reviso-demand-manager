import { Routes } from '@angular/router';

import { AuthGuard } from './core/auth.guard';
import { RoleGuard } from './core/role.guard';
import { ShellComponent } from './layout/shell/shell.component';
import { LoginComponent } from './pages/login/login.component';
import { AcceptInviteComponent } from './pages/accept-invite/accept-invite.component';
import { RoleLandingComponent } from './pages/role-landing/role-landing.component';
import { ClientDashboardComponent } from './pages/client/client-dashboard.component';
import { ClientBriefingsComponent } from './pages/client/client-briefings.component';
import { ClientRequestsComponent } from './pages/client/client-requests.component';
import { ClientRequestDetailComponent } from './pages/client/client-request-detail.component';
import { AgencyInboxComponent } from './pages/agency/agency-inbox.component';
import { AgencyRequestsComponent } from './pages/agency/agency-requests.component';
import { AgencyCreateRequestComponent } from './pages/agency/agency-create-request.component';
import { AgencyRequestDetailComponent } from './pages/agency/agency-request-detail.component';
import { AgencyReportsComponent } from './pages/agency/agency-reports.component';
import { AgencyWorkflowComponent } from './pages/agency/agency-workflow.component';
import { AgencyEventsComponent } from './pages/agency/agency-events.component';
import { AdminCompaniesComponent } from './pages/admin/admin-companies.component';
import { AdminUsersComponent } from './pages/admin/admin-users.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'invite', component: AcceptInviteComponent },
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
        path: 'client/requests/:id',
        component: ClientRequestDetailComponent,
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
        path: 'agency/requests/new',
        component: AgencyCreateRequestComponent,
        canActivate: [RoleGuard],
        data: { roles: ['AGENCY_USER', 'AGENCY_ADMIN'] },
      },
      {
        path: 'agency/requests/:id',
        component: AgencyRequestDetailComponent,
        canActivate: [RoleGuard],
        data: { roles: ['AGENCY_USER', 'AGENCY_ADMIN'] },
      },
      {
        path: 'agency/workflow',
        component: AgencyWorkflowComponent,
        canActivate: [RoleGuard],
        data: { roles: ['AGENCY_USER', 'AGENCY_ADMIN'] },
      },
      {
        path: 'agency/events',
        component: AgencyEventsComponent,
        canActivate: [RoleGuard],
        data: { roles: ['AGENCY_USER', 'AGENCY_ADMIN'] },
      },
      {
        path: 'agency/reports',
        component: AgencyReportsComponent,
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
