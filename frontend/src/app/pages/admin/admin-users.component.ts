import { Component } from '@angular/core';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-admin-users',
  template: `
    <h2>Usuários (placeholder)</h2>
    <p>
      Role atual: <strong>{{ role }}</strong>
    </p>
  `,
})
export class AdminUsersComponent {
  readonly role;
  constructor(private readonly auth: AuthService) {
    this.role = this.auth.getRole();
  }
}
