import { Component } from '@angular/core';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-agency-requests',
  template: `
    <h2>Requisições (placeholder)</h2>
    <p>
      Role atual: <strong>{{ role }}</strong>
    </p>
  `,
})
export class AgencyRequestsComponent {
  readonly role;
  constructor(private readonly auth: AuthService) {
    this.role = this.auth.getRole();
  }
}
