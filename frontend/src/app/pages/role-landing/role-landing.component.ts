import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-role-landing',
  template: '',
})
export class RoleLandingComponent {
  constructor(private readonly auth: AuthService, private readonly router: Router) {
    const role = this.auth.getRole();
    if (role === 'CLIENT_USER') {
      this.router.navigateByUrl('/client/briefings');
      return;
    }
    if (role === 'AGENCY_ADMIN') {
      this.router.navigateByUrl('/admin/companies');
      return;
    }
    if (role === 'AGENCY_USER') {
      this.router.navigateByUrl('/agency/briefings/inbox');
      return;
    }
    this.router.navigateByUrl('/login');
  }
}
