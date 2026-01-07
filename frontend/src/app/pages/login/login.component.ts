import { Component } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { NgIf } from '@angular/common';

import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, NgIf],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  error: string | null = null;
  loading = false;

  readonly form;

  constructor(
    private readonly fb: FormBuilder,
    private readonly auth: AuthService,
    private readonly router: Router
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]],
    });

    if (this.auth.isLoggedIn()) {
      this.router.navigateByUrl('/');
    }
  }

  submit(): void {
    this.error = null;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const email = this.form.value.email ?? '';
    const password = this.form.value.password ?? '';

    this.loading = true;
    this.auth.login(email, password).subscribe({
      next: () => {
        this.loading = false;
        const role = this.auth.getRole();
        if (role === 'CLIENT_USER') return void this.router.navigateByUrl('/client/briefings');
        if (role === 'AGENCY_ADMIN') return void this.router.navigateByUrl('/admin/companies');
        if (role === 'AGENCY_USER')
          return void this.router.navigateByUrl('/agency/briefings/inbox');
        return void this.router.navigateByUrl('/');
      },
      error: (err: unknown) => {
        this.loading = false;
        if (err instanceof HttpErrorResponse) {
          if (err.status === 401 || err.status === 400) {
            this.error = 'Email ou senha inválidos.';
            return;
          }
        }
        this.error = 'Falha ao autenticar. Tente novamente.';
      },
    });
  }
}
