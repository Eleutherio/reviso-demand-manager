import { Component } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { NgFor, NgIf } from '@angular/common';
import { AuthService } from '../../core/auth.service';
import type { UserRole } from '../../core/roles';

type NavItem = {
  label: string;
  path: string;
  roles: UserRole[];
};

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NgIf, NgFor],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  readonly role: UserRole | null;
  readonly companyId: string | null;

  private readonly items: NavItem[] = [
    { label: 'Meus Briefings', path: '/client/briefings', roles: ['CLIENT_USER'] },
    { label: 'Minhas Requisições', path: '/client/requests', roles: ['CLIENT_USER'] },

    {
      label: 'Inbox Briefings',
      path: '/agency/briefings/inbox',
      roles: ['AGENCY_USER', 'AGENCY_ADMIN'],
    },
    { label: 'Requisições', path: '/agency/requests', roles: ['AGENCY_USER', 'AGENCY_ADMIN'] },

    { label: 'Empresas', path: '/admin/companies', roles: ['AGENCY_ADMIN'] },
    { label: 'Usuários', path: '/admin/users', roles: ['AGENCY_ADMIN'] },
  ];

  constructor(private readonly auth: AuthService, private readonly router: Router) {
    this.role = this.auth.getRole();
    this.companyId = this.auth.getCompanyId();
  }

  get navItems(): NavItem[] {
    const role = this.role;
    if (!role) return [];
    return this.items.filter((it) => it.roles.includes(role));
  }

  logout(): void {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}
