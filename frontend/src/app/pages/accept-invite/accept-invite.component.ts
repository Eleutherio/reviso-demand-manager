import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';

import { InvitesApi } from '../../api/invites.api';

@Component({
  selector: 'app-accept-invite',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './accept-invite.component.html',
  styleUrl: './accept-invite.component.scss',
})
export class AcceptInviteComponent {
  readonly form;
  token = '';
  kind: 'agency' | 'client' = 'agency';
  loading = false;
  success: string | null = null;
  error: string | null = null;

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly invitesApi: InvitesApi
  ) {
    this.form = this.fb.group({
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
    });

    this.route.queryParamMap.subscribe((params) => {
      this.token = (params.get('token') ?? '').trim();
      const kind = (params.get('kind') ?? '').trim().toLowerCase();
      this.kind = kind === 'client' ? 'client' : 'agency';
      this.error = null;
      this.success = null;
    });
  }

  get portalLabel(): string {
    return this.kind === 'client' ? 'portal do cliente' : 'painel da agencia';
  }

  get loginLabel(): string {
    return this.kind === 'client' ? 'Login do cliente' : 'Login da agencia';
  }

  get tokenInvalid(): boolean {
    return !this.token;
  }

  submit(): void {
    this.error = null;
    this.success = null;

    if (!this.token) {
      this.error = 'Token invalido ou expirado.';
      return;
    }

    if (this.form.invalid) {
      this.error = this.resolveValidationError();
      this.form.markAllAsTouched();
      return;
    }

    const password = this.form.value.password ?? '';
    const confirmPassword = this.form.value.confirmPassword ?? '';
    if (password !== confirmPassword) {
      this.error = 'As senhas nao conferem.';
      return;
    }

    this.loading = true;
    this.form.disable();

    this.invitesApi
      .acceptInvite({ token: this.token, password })
      .pipe(
        finalize(() => {
          this.loading = false;
          this.form.enable();
        })
      )
      .subscribe({
        next: (res) => {
          this.success = res?.message ?? 'Convite aceito com sucesso.';
        },
        error: (err: unknown) => {
          this.error = this.resolveInviteError(err);
        },
      });
  }

  goToLogin(): void {
    this.router.navigateByUrl('/login');
  }

  private resolveValidationError(): string {
    const passwordCtrl = this.form.get('password');
    const confirmCtrl = this.form.get('confirmPassword');

    if (passwordCtrl?.hasError('required')) return 'Informe a senha.';
    if (passwordCtrl?.hasError('minlength')) return 'Senha deve ter ao menos 8 caracteres.';
    if (confirmCtrl?.hasError('required')) return 'Confirme a senha.';
    return 'Preencha os campos para continuar.';
  }

  private resolveInviteError(err: unknown): string {
    if (err instanceof HttpErrorResponse) {
      const message = this.extractServerMessage(err);
      if (message && message.toLowerCase().includes('token')) {
        return 'Token invalido ou expirado.';
      }
      if (err.status === 400) {
        return 'Token invalido ou expirado.';
      }
      if (err.status === 409) {
        return 'Email ja esta em uso.';
      }
      if (err.status === 0) {
        return 'Nao foi possivel conectar. Verifique sua internet e tente novamente.';
      }
      if (err.status >= 500) {
        return 'Nao foi possivel aceitar o convite. Tente novamente mais tarde.';
      }
    }
    return 'Nao foi possivel aceitar o convite.';
  }

  private extractServerMessage(err: HttpErrorResponse): string | null {
    const payload = err.error as { message?: unknown } | string | null;
    if (!payload) return null;
    if (typeof payload === 'string') return payload;
    if (typeof payload.message === 'string') return payload.message;
    return null;
  }
}
