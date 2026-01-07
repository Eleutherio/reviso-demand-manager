import { Component } from '@angular/core';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-client-briefings',
  template: `
    <h2>Meus Briefings (placeholder)</h2>
    <p>
      Role atual: <strong>{{ role }}</strong>
    </p>
  `,
})
export class ClientBriefingsComponent {
  readonly role;
  constructor(private readonly auth: AuthService) {
    this.role = this.auth.getRole();
  }
}
