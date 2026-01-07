import { Component } from '@angular/core';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-client-requests',
  template: `
    <h2>Minhas Requisições (placeholder)</h2>
    <p>
      Role atual: <strong>{{ role }}</strong>
    </p>
  `,
})
export class ClientRequestsComponent {
  readonly role;
  constructor(private readonly auth: AuthService) {
    this.role = this.auth.getRole();
  }
}
