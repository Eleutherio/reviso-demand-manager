import { Component } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, map, of, startWith, switchMap, Subject } from 'rxjs';

import { RequestsApi } from '../../api/requests.api';
import type { RequestStatus, SortDirection } from '../../api/requests.api';
import type { Page, RequestDto } from '../../api/request';

@Component({
  selector: 'app-agency-requests',
  standalone: true,
  imports: [CommonModule, DatePipe, RouterLink],
  template: `
    <h2>Requisições</h2>

    <div style="border: 1px solid #ddd; padding: 12px; border-radius: 6px; margin: 12px 0;">
      <h3 style="margin: 0 0 8px;">Filtros</h3>

      <div
        style="display: grid; gap: 10px; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));"
      >
        <label style="display: grid; gap: 4px;">
          Empresa (companyId)
          <input
            placeholder="UUID"
            [value]="filters.companyId"
            (input)="filters.companyId = getValue($event)"
          />
        </label>

        <label style="display: grid; gap: 4px;">
          Status
          <select [value]="filters.status" (change)="filters.status = getStatusValue($event)">
            <option value="">Todos</option>
            <option value="NEW">NEW</option>
            <option value="IN_PROGRESS">IN_PROGRESS</option>
            <option value="IN_REVIEW">IN_REVIEW</option>
            <option value="CHANGES_REQUESTED">CHANGES_REQUESTED</option>
            <option value="APPROVED">APPROVED</option>
            <option value="DELIVERED">DELIVERED</option>
            <option value="DONE">DONE</option>
            <option value="CANCELED">CANCELED</option>
            <option value="CLOSED">CLOSED</option>
          </select>
        </label>

        <label style="display: grid; gap: 4px;">
          Prioridade
          <select [value]="filters.priority" (change)="filters.priority = getSelectValue($event)">
            <option value="">Todas</option>
            <option value="LOW">LOW</option>
            <option value="MEDIUM">MEDIUM</option>
            <option value="HIGH">HIGH</option>
            <option value="URGENT">URGENT</option>
          </select>
        </label>

        <label style="display: grid; gap: 4px;">
          Tipo
          <select [value]="filters.type" (change)="filters.type = getSelectValue($event)">
            <option value="">Todos</option>
            <option value="POST">POST</option>
            <option value="STORY">STORY</option>
            <option value="BANNER">BANNER</option>
            <option value="LANDING_PAGE">LANDING_PAGE</option>
            <option value="TRAFFIC">TRAFFIC</option>
            <option value="EMAIL">EMAIL</option>
            <option value="VIDEO">VIDEO</option>
            <option value="OTHER">OTHER</option>
          </select>
        </label>

        <label style="display: grid; gap: 4px;">
          Criado de
          <input
            type="datetime-local"
            [value]="filters.createdFrom"
            (input)="filters.createdFrom = getValue($event)"
          />
        </label>

        <label style="display: grid; gap: 4px;">
          Criado até
          <input
            type="datetime-local"
            [value]="filters.createdTo"
            (input)="filters.createdTo = getValue($event)"
          />
        </label>

        <label style="display: grid; gap: 4px;">
          Vencimento até
          <input
            type="datetime-local"
            [value]="filters.dueBefore"
            (input)="filters.dueBefore = getValue($event)"
          />
        </label>

        <label style="display: grid; gap: 4px;">
          Página
          <input
            type="number"
            min="0"
            [value]="filters.page"
            (input)="filters.page = getNumberValue($event, 0)"
          />
        </label>

        <label style="display: grid; gap: 4px;">
          Tamanho
          <input
            type="number"
            min="1"
            max="100"
            [value]="filters.size"
            (input)="filters.size = getNumberValue($event, 20)"
          />
        </label>

        <label style="display: grid; gap: 4px;">
          Ordenar por
          <select [value]="filters.sortBy" (change)="filters.sortBy = getSelectValue($event)">
            <option value="createdAt">createdAt</option>
            <option value="dueDate">dueDate</option>
            <option value="title">title</option>
            <option value="priority">priority</option>
          </select>
        </label>

        <label style="display: grid; gap: 4px;">
          Direção
          <select
            [value]="filters.direction"
            (change)="filters.direction = getDirectionValue($event)"
          >
            <option value="desc">desc</option>
            <option value="asc">asc</option>
          </select>
        </label>
      </div>

      <div style="display: flex; gap: 8px; flex-wrap: wrap; margin-top: 10px;">
        <button type="button" (click)="search()">Buscar</button>
        <button type="button" (click)="clear()">Limpar</button>
        <button type="button" (click)="reload()">Atualizar</button>
      </div>
    </div>

    <div style="display: flex; gap: 12px; flex-wrap: wrap; align-items: center; margin: 12px 0;">
      <button type="button" (click)="prev()" [disabled]="filters.page === 0">Anterior</button>
      <button type="button" (click)="next()">Próximo</button>
      <span
        >Página: <strong>{{ filters.page + 1 }}</strong></span
      >
    </div>

    @if (vm$ | async; as vm) { @if (vm.status === 'loading') {
    <p>Carregando...</p>
    } @else if (vm.status === 'error') {
    <p style="color: inherit;">Erro ao carregar requisições: {{ vm.message }}</p>
    } @else {
    <p>
      Total: <strong>{{ vm.page.totalElements }}</strong>
    </p>

    <div style="overflow-x: auto;">
      <table style="width: 100%; border-collapse: collapse;">
        <thead>
          <tr>
            <th style="text-align: left; padding: 8px;">Título</th>
            <th style="text-align: left; padding: 8px;">Empresa</th>
            <th style="text-align: left; padding: 8px;">Status</th>
            <th style="text-align: left; padding: 8px;">Prioridade</th>
            <th style="text-align: left; padding: 8px;">Depto</th>
            <th style="text-align: left; padding: 8px;">Due date</th>
            <th style="text-align: left; padding: 8px;">Criado em</th>
          </tr>
        </thead>
        <tbody>
          @for (r of vm.page.content; track r.id) {
          <tr>
            <td style="padding: 8px;">
              <a [routerLink]="['/agency/requests', r.id]">{{ r.title }}</a>
            </td>
            <td style="padding: 8px;">{{ r.companyName }}</td>
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
export class AgencyRequestsComponent {
  filters: {
    companyId: string;
    status: '' | RequestStatus;
    priority: string;
    type: string;
    createdFrom: string;
    createdTo: string;
    dueBefore: string;
    page: number;
    size: number;
    sortBy: string;
    direction: SortDirection;
  } = {
    companyId: '',
    status: '',
    priority: '',
    type: '',
    createdFrom: '',
    createdTo: '',
    dueBefore: '',
    page: 0,
    size: 20,
    sortBy: 'createdAt',
    direction: 'desc',
  };

  private readonly reload$ = new Subject<void>();
  readonly vm$ = this.reload$.pipe(
    startWith(void 0),
    switchMap(() =>
      this.api
        .list({
          companyId: this.filters.companyId.trim() || undefined,
          status: this.filters.status || undefined,
          priority: this.filters.priority || undefined,
          type: this.filters.type || undefined,
          createdFrom: this.toIsoUtcOrUndefined(this.filters.createdFrom),
          createdTo: this.toIsoUtcOrUndefined(this.filters.createdTo),
          dueBefore: this.toIsoUtcOrUndefined(this.filters.dueBefore),
          page: this.filters.page,
          size: this.filters.size,
          sortBy: this.filters.sortBy,
          direction: this.filters.direction,
        })
        .pipe(
          map((page: Page<RequestDto>) => ({ status: 'ready' as const, page })),
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

  constructor(private readonly api: RequestsApi) {}

  getValue(ev: Event): string {
    const target = ev.target as HTMLInputElement;
    return target.value;
  }

  getSelectValue(ev: Event): string {
    const target = ev.target as HTMLSelectElement;
    return target.value;
  }

  getDirectionValue(ev: Event): SortDirection {
    const v = this.getSelectValue(ev);
    return v === 'asc' ? 'asc' : 'desc';
  }

  getStatusValue(ev: Event): '' | RequestStatus {
    const v = this.getSelectValue(ev);
    if (!v) return '';

    const allowed: readonly RequestStatus[] = [
      'NEW',
      'IN_PROGRESS',
      'IN_REVIEW',
      'CHANGES_REQUESTED',
      'APPROVED',
      'DELIVERED',
      'DONE',
      'CANCELED',
      'CLOSED',
    ];

    return (allowed as readonly string[]).includes(v) ? (v as RequestStatus) : '';
  }

  getNumberValue(ev: Event, fallback: number): number {
    const target = ev.target as HTMLInputElement;
    const n = Number(target.value);
    return Number.isFinite(n) ? n : fallback;
  }

  private toIsoUtcOrUndefined(dtLocalValue: string): string | undefined {
    const trimmed = (dtLocalValue ?? '').trim();
    if (!trimmed) return undefined;
    return new Date(trimmed).toISOString();
  }

  search(): void {
    if (this.filters.page < 0) this.filters.page = 0;
    if (this.filters.size < 1) this.filters.size = 20;
    this.reload();
  }

  clear(): void {
    this.filters = {
      companyId: '',
      status: '',
      priority: '',
      type: '',
      createdFrom: '',
      createdTo: '',
      dueBefore: '',
      page: 0,
      size: 20,
      sortBy: 'createdAt',
      direction: 'desc',
    };
    this.reload();
  }

  reload(): void {
    this.reload$.next();
  }

  prev(): void {
    if (this.filters.page === 0) return;
    this.filters.page -= 1;
    this.reload();
  }

  next(): void {
    this.filters.page += 1;
    this.reload();
  }
}


