import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router, UrlTree } from '@angular/router';
import { AuthService } from './auth.service';
import type { UserRole } from './roles';

@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate {
  constructor(private readonly auth: AuthService, private readonly router: Router) {}

  canActivate(route: ActivatedRouteSnapshot): boolean | UrlTree {
    const allowed = (route.data?.['roles'] as UserRole[] | undefined) ?? [];
    if (allowed.length === 0) return true;

    const role = this.auth.getRole();
    if (role && allowed.includes(role)) return true;

    return this.router.parseUrl('/');
  }
}
