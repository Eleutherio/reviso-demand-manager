import { Component } from '@angular/core';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-admin-companies',
  template: `
    <h2>Empresas (placeholder)</h2>
    <p>
      Role atual: <strong>{{ role }}</strong>
    </p>
  `,
})
export class AdminCompaniesComponent {
  readonly role;
  constructor(private readonly auth: AuthService) {
    this.role = this.auth.getRole();
  }
}
