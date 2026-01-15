import { Component } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, map, of, startWith } from 'rxjs';

import { ClientApi } from '../../api/client.api';
import type { RequestDto } from '../../api/request';

@Component({
  selector: 'app-client-requests',
  standalone: true,
  imports: [CommonModule, DatePipe, RouterLink],
  template: `
    <h2>Minhas Requisições</h2>

    @if (vm$ | async; as vm) { @if (vm.status === 'loading') {
    <p>Carregando...</p>
    } @else if (vm.status === 'error') {
    <p style="color: inherit;">Erro ao carregar requisições: {{ vm.message }}</p>
    } @else {
    <p>
      Total: <strong>{{ vm.requests.length }}</strong>
    </p>

    <div style="overflow-x: auto;">
      <table style="width: 100%; border-collapse: collapse;">
        <thead>
          <tr>
            <th style="text-align: left; padding: 8px;">Título</th>
            <th style="text-align: left; padding: 8px;">Status</th>
            <th style="text-align: left; padding: 8px;">Prioridade</th>
            <th style="text-align: left; padding: 8px;">Depto</th>
            <th style="text-align: left; padding: 8px;">Due date</th>
            <th style="text-align: left; padding: 8px;">Criado em</th>
          </tr>
        </thead>
        <tbody>
          @for (r of vm.requests; track r.id) {
          <tr>
            <td style="padding: 8px;">
              <a [routerLink]="['/client/requests', r.id]">{{ r.title }}</a>
            </td>
            <td style="padding: 8px;">{{ r.status }}</td>
            <td style="padding: 8px;">{{ r.priority }}</td>
            <td style="padding: 8px;">{{ r.department }}</td>
            <td style="padding: 8px;">
              {{ r.dueDate ? (r.dueDate | date : 'dd/MM/yyyy HH:mm') : '-' }}
            </td>
            <td style="padding: 8px;">{{ r.createdAt | date : 'dd/MM/yyyy HH:mm' }}</td>
          </tr>
          }
        </tbody>
      </table>
    </div>
    } }
  `,
})
export class ClientRequestsComponent {
  readonly vm$;

  constructor(private readonly api: ClientApi) {
    this.vm$ = this.api.listMyRequests().pipe(
      map((requests: RequestDto[]) => ({ status: 'ready' as const, requests })),
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
    );
  }
}


