import { Component } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { catchError, map, of, startWith, switchMap, Subject } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';

import { AgencyBriefingsApi } from '../../api/agency-briefings.api';
import type { BriefingDto } from '../../api/briefing';
import type { AgencyDepartment } from '../../api/request';

@Component({
  selector: 'app-agency-inbox',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  template: `
    <h2>Inbox Briefings</h2>
    <div style="display: flex; gap: 12px; flex-wrap: wrap; align-items: center; margin: 12px 0;">
      <label style="display: inline-flex; gap: 8px; align-items: center;">
        Status
        <select [(ngModel)]="status" (ngModelChange)="reload()">
          @for (s of statuses; track s) {
          <option [value]="s">{{ s }}</option>
          }
        </select>
      </label>

      <button type="button" (click)="reload()">Atualizar</button>
    </div>

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
            <th style="text-align: left; padding: 8px;">Empresa</th>
            <th style="text-align: left; padding: 8px;">Criado em</th>
            <th style="text-align: left; padding: 8px;">Departamento</th>
            <th style="text-align: left; padding: 8px;">Ações</th>
          </tr>
        </thead>
        <tbody>
          @for (b of vm.briefings; track b.id) {
          <tr>
            <td style="padding: 8px;">{{ b.title }}</td>
            <td style="padding: 8px;">{{ b.companyName }}</td>
            <td style="padding: 8px;">{{ b.createdAt | date : 'dd/MM/yyyy HH:mm' }}</td>
            <td style="padding: 8px;">
              <select [(ngModel)]="deptByBriefing[b.id]">
                <option [ngValue]="null">Selecione</option>
                @for (d of departments; track d) {
                <option [ngValue]="d">{{ d }}</option>
                }
              </select>
            </td>
            <td style="padding: 8px; display: flex; gap: 8px; flex-wrap: wrap;">
              <button type="button" (click)="convert(b)">Converter</button>
              <button type="button" (click)="reject(b)">Rejeitar</button>
            </td>
          </tr>
          }
        </tbody>
      </table>
    </div>
    } }
  `,
})
export class AgencyInboxComponent {
  readonly statuses = ['PENDING', 'CONVERTED', 'REJECTED'];
  status: string = 'PENDING';

  readonly departments: AgencyDepartment[] = [
    'DESIGN',
    'COPY',
    'DEV',
    'PLANNING',
    'PRODUCTION',
    'MEDIA',
  ];

  readonly deptByBriefing: Record<string, AgencyDepartment | null> = {};

  private readonly reload$ = new Subject<void>();
  readonly vm$ = this.reload$.pipe(
    startWith(void 0),
    switchMap(() =>
      this.api.listBriefings(this.status).pipe(
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
                message: 'Sem permissão (403). Necessário AGENCY_USER/ADMIN.',
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

  constructor(private readonly api: AgencyBriefingsApi) {}

  reload(): void {
    this.reload$.next();
  }

  convert(briefing: BriefingDto): void {
    const dept = this.deptByBriefing[briefing.id];
    if (!dept) return;

    this.api.convertBriefing(briefing.id, { department: dept }).subscribe({
      next: () => this.reload(),
      error: () => this.reload(),
    });
  }

  reject(briefing: BriefingDto): void {
    this.api.rejectBriefing(briefing.id).subscribe({
      next: () => this.reload(),
      error: () => this.reload(),
    });
  }
}
