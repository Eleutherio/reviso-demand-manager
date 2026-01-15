import { Component } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, map, of, startWith, switchMap, Subject } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';

import { ClientApi } from '../../api/client.api';
import type { BriefingDto } from '../../api/briefing';

@Component({
  selector: 'app-client-briefings',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DatePipe],
  template: `
    <h2>Meus Briefings</h2>
    <form
      (ngSubmit)="submit()"
      [formGroup]="form"
      style="display: grid; gap: 8px; max-width: 560px;"
    >
      <label style="display: grid; gap: 4px;">
        Título
        <input formControlName="title" type="text" />
      </label>

      <label style="display: grid; gap: 4px;">
        Descrição
        <textarea formControlName="description" rows="4"></textarea>
      </label>

      <button type="submit" [disabled]="loading || form.invalid">
        {{ loading ? 'Enviando...' : 'Criar briefing' }}
      </button>

      @if (error) {
      <p style="color: inherit;">{{ error }}</p>
      }
    </form>

    <hr style="margin: 16px 0;" />

    @if (vm$ | async; as vm) { @if (vm.status === 'loading') {
    <p>Carregando...</p>
    } @else if (vm.status === 'error') {
    <p style="color: inherit;">Erro ao carregar briefings: {{ vm.message }}</p>
    } @else {
    <p>
      Total: <strong>{{ vm.briefings.length }}</strong>
    </p>

    <div style="overflow-x: auto;">
      <table style="width: 100%; border-collapse: collapse;">
        <thead>
          <tr>
            <th style="text-align: left; padding: 8px;">Título</th>
            <th style="text-align: left; padding: 8px;">Status</th>
            <th style="text-align: left; padding: 8px;">Criado em</th>
          </tr>
        </thead>
        <tbody>
          @for (b of vm.briefings; track b.id) {
          <tr>
            <td style="padding: 8px;">{{ b.title }}</td>
            <td style="padding: 8px;">{{ b.status }}</td>
            <td style="padding: 8px;">{{ b.createdAt | date : 'dd/MM/yyyy HH:mm' }}</td>
          </tr>
          }
        </tbody>
      </table>
    </div>
    } }
  `,
})
export class ClientBriefingsComponent {
  loading = false;
  error: string | null = null;

  readonly form;

  private readonly reload$ = new Subject<void>();
  readonly vm$ = this.reload$.pipe(
    startWith(void 0),
    switchMap(() =>
      this.api.listMyBriefings().pipe(
        map((briefings: BriefingDto[]) => ({ status: 'ready' as const, briefings })),
        startWith({ status: 'loading' as const }),
        catchError((err: unknown) => {
          if (err instanceof HttpErrorResponse) {
            if (err.status === 401) {
              return of({
                status: 'error' as const,
                message: 'Não autenticado (401). Faça login novamente.',
              });
            }
            if (err.status === 403) {
              return of({
                status: 'error' as const,
                message: 'Sem permissão (403). Necessário CLIENT_USER.',
              });
            }
            return of({
              status: 'error' as const,
              message: `Erro HTTP ${err.status}: ${err.statusText || 'Falha ao carregar'}`,
            });
          }
          const message = err instanceof Error ? err.message : 'Falha inesperada';
          return of({ status: 'error' as const, message });
        })
      )
    )
  );

  constructor(private readonly api: ClientApi, fb: FormBuilder) {
    this.form = fb.group({
      title: ['', [Validators.required]],
      description: [''],
    });
  }

  private reload(): void {
    this.reload$.next();
  }

  submit(): void {
    this.error = null;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const title = this.form.value.title ?? '';
    const description = this.form.value.description ?? '';

    this.loading = true;
    this.api.createBriefing({ title, description }).subscribe({
      next: () => {
        this.loading = false;
        this.form.reset({ title: '', description: '' });
        this.reload();
      },
      error: (err: unknown) => {
        this.loading = false;
        if (err instanceof HttpErrorResponse) {
          if (err.status === 401) {
            this.error = 'Não autenticado (401). Faça login novamente.';
            return;
          }
          if (err.status === 403) {
            this.error = 'Sem permissão (403). Necessário CLIENT_USER.';
            return;
          }
        }
        this.error = 'Falha ao criar briefing.';
      },
    });
  }
}
