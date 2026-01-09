import { ChangeDetectorRef, Component } from '@angular/core';
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
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef
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
      const emailCtrl = this.form.get('email');
      const passwordCtrl = this.form.get('password');
      if (emailCtrl?.hasError('required')) {
        this.error = 'Informe seu email.';
      } else if (emailCtrl?.hasError('email')) {
        this.error = 'Informe um email valido.';
      } else if (passwordCtrl?.hasError('required')) {
        this.error = 'Informe sua senha.';
      } else {
        this.error = 'Preencha os campos para continuar.';
      }
      this.form.markAllAsTouched();
      return;
    }

    const email = this.form.value.email ?? '';
    const password = this.form.value.password ?? '';

    this.loading = true;
    this.cdr.markForCheck();
    this.auth.login(email, password).subscribe({
      next: () => {
        this.loading = false;
        this.cdr.markForCheck();
        const role = this.auth.getRole();
        if (role === 'CLIENT_USER') return void this.router.navigateByUrl('/client/briefings');
        if (role === 'AGENCY_ADMIN') return void this.router.navigateByUrl('/admin/companies');
        if (role === 'AGENCY_USER')
          return void this.router.navigateByUrl('/agency/briefings/inbox');
        return void this.router.navigateByUrl('/');
      },
      error: (err: unknown) => {
        this.loading = false;
        this.error = this.resolveLoginError(err);
        this.cdr.markForCheck();
      },
    });
  }

  private resolveLoginError(err: unknown): string {
    if (err instanceof HttpErrorResponse) {
      const serverMessage = this.extractServerMessage(err);
      if (serverMessage) {
        const normalized = serverMessage.toLowerCase();
        if (normalized.includes('inativo')) {
          return 'Seu usuario foi desativado. Contate um administrador.';
        }
        if (normalized.includes('credenciais')) {
          return 'Email ou senha invalidos.';
        }
        if (normalized.includes('muitas tentativas')) {
          return 'Muitas tentativas. Aguarde alguns minutos e tente novamente.';
        }
      }

      if (err.status === 0) {
        return 'Nao foi possivel conectar. Verifique sua internet e tente novamente.';
      }
      if (err.status === 400 || err.status === 401) {
        return 'Email ou senha invalidos.';
      }
      if (err.status === 403) {
        return 'Seu acesso nao esta permitido. Fale com o administrador.';
      }
      if (err.status === 404) {
        return 'Servico de login indisponivel. Tente novamente em instantes.';
      }
      if (err.status === 408 || err.status === 504) {
        return 'Tempo de resposta excedido. Tente novamente.';
      }
      if (err.status === 429) {
        return 'Muitas tentativas. Aguarde alguns minutos e tente novamente.';
      }
      if (err.status >= 500) {
        return 'Servico indisponivel no momento. Tente novamente mais tarde.';
      }
    }
    return 'Falha ao autenticar. Tente novamente.';
  }

  private extractServerMessage(err: HttpErrorResponse): string | null {
    const payload = err.error as { message?: unknown } | string | null;
    if (!payload) return null;
    if (typeof payload === 'string') return payload;
    if (typeof payload.message === 'string') return payload.message;
    return null;
  }
}

