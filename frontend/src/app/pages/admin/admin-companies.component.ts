import { Component } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, firstValueFrom, map, of, startWith, Subject, switchMap } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import {
  AdminCompaniesApi,
  CreateCompanyDto,
  UpdateCompanyDto,
} from '../../api/admin-companies.api';
import { CompanyDto, CompanyType } from '../../api/company';

@Component({
  selector: 'app-admin-companies',
  standalone: true,
  imports: [CommonModule, DatePipe],
  template: `
    <h2>Empresas</h2>

    <p>
      Role atual: <strong>{{ role }}</strong>
    </p>

    <div style="padding: 12px 0;">
      <h3>{{ editingId ? 'Editar empresa' : 'Criar empresa' }}</h3>

      <form (submit)="onSubmit($event)" style="display: grid; gap: 8px; max-width: 720px;">
        <label style="display: grid; gap: 4px;">
          Nome
          <input name="name" required [value]="form.name" (input)="form.name = getValue($event)" />
        </label>

        <label style="display: grid; gap: 4px;">
          Tipo
          <select
            name="type"
            required
            [value]="form.type"
            (change)="form.type = getCompanyType($event)"
          >
            <option value="AGENCY">AGENCY</option>
            <option value="CLIENT">CLIENT</option>
          </select>
        </label>

        @if (editingId) {
        <label style="display: grid; gap: 4px;">
          Ativa
          <select
            name="active"
            [value]="form.activeStr"
            (change)="form.activeStr = getBoolStr($event)"
          >
            <option value="true">Sim</option>
            <option value="false">Não</option>
          </select>
        </label>
        }

        <label style="display: grid; gap: 4px;">
          Segmento
          <input
            name="segment"
            required
            [value]="form.segment"
            (input)="form.segment = getValue($event)"
          />
        </label>

        <label style="display: grid; gap: 4px;">
          Email de contato
          <input
            name="contactEmail"
            type="email"
            required
            [value]="form.contactEmail"
            (input)="form.contactEmail = getValue($event)"
          />
        </label>

        <label style="display: grid; gap: 4px;">
          Site
          <input name="site" [value]="form.site" (input)="form.site = getValue($event)" />
        </label>

        <label style="display: grid; gap: 4px;">
          Links úteis (1 por linha)
          <textarea
            name="usefulLinks"
            rows="4"
            [value]="form.usefulLinksText"
            (input)="form.usefulLinksText = getValue($event)"
          ></textarea>
        </label>

        <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap;">
          <button type="submit">{{ editingId ? 'Salvar' : 'Criar' }}</button>
          @if (editingId) {
          <button type="button" (click)="cancelEdit()">Cancelar</button>
          } @if (formError) {
          <span style="color: inherit;">{{ formError }}</span>
          }
        </div>
      </form>
    </div>

    @if (vm$ | async; as vm) { @if (vm.status === 'loading') {
    <p>Carregando...</p>
    } @else if (vm.status === 'error') {
    <p style="color: inherit;">Erro ao carregar empresas: {{ vm.message }}</p>
    } @else {
    <p>
      Total: <strong>{{ vm.companies.length }}</strong>
    </p>

    <div style="overflow-x: auto;">
      <table style="width: 100%; border-collapse: collapse;">
        <thead>
          <tr>
            <th style="text-align: left; padding: 8px;">Nome</th>
            <th style="text-align: left; padding: 8px;">Tipo</th>
            <th style="text-align: left; padding: 8px;">Ativa</th>
            <th style="text-align: left; padding: 8px;">Segmento</th>
            <th style="text-align: left; padding: 8px;">Email</th>
            <th style="text-align: left; padding: 8px;">Codigo</th>
            <th style="text-align: left; padding: 8px;">Criada em</th>
            <th style="text-align: left; padding: 8px;">ID Empresa</th>
            <th style="text-align: left; padding: 8px;"></th>
          </tr>
        </thead>
        <tbody>
          @for (c of vm.companies; track c.id) {
          <tr>
            <td style="padding: 8px;">{{ c.name }}</td>
            <td style="padding: 8px;">{{ c.type }}</td>
            <td style="padding: 8px;">{{ c.active ? 'Sim' : 'Não' }}</td>
            <td style="padding: 8px;">{{ c.segment ?? '-' }}</td>
            <td style="padding: 8px;">{{ c.contactEmail ?? '-' }}</td>
            <td style="padding: 8px;">{{ c.companyCode ?? '-' }}</td>
            <td style="padding: 8px;">{{ c.createdAt | date : 'dd/MM/yyyy HH:mm' }}</td>
            <td style="padding: 8px;">{{ c.id }}</td>
            <td style="padding: 8px;">
              <button type="button" (click)="startEdit(c)">Editar</button>
            </td>
          </tr>
          }
        </tbody>
      </table>
    </div>
    } }
  `,
})
export class AdminCompaniesComponent {
  readonly role;

  readonly vm$;

  editingId: string | null = null;

  form: {
    name: string;
    type: CompanyType;
    activeStr: 'true' | 'false';
    segment: string;
    contactEmail: string;
    site: string;
    usefulLinksText: string;
  } = {
    name: '',
    type: 'CLIENT',
    activeStr: 'true',
    segment: '',
    contactEmail: '',
    site: '',
    usefulLinksText: '',
  };

  formError: string | null = null;

  private readonly refresh$ = new Subject<void>();

  constructor(private readonly auth: AuthService, private readonly api: AdminCompaniesApi) {
    this.role = this.auth.getRole();

    this.vm$ = this.refresh$.pipe(
      startWith(undefined),
      switchMap(() => this.api.listCompanies()),
      map((companies: CompanyDto[]) => ({ status: 'ready' as const, companies })),
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
              message: 'Sem permissão (403). Este endpoint exige AGENCY_ADMIN.',
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

  getValue(ev: Event): string {
    const target = ev.target as HTMLInputElement | HTMLTextAreaElement;
    return target.value;
  }

  getCompanyType(ev: Event): CompanyType {
    const target = ev.target as HTMLSelectElement;
    return target.value as CompanyType;
  }

  getBoolStr(ev: Event): 'true' | 'false' {
    const target = ev.target as HTMLSelectElement;
    return target.value === 'true' ? 'true' : 'false';
  }

  startEdit(c: CompanyDto): void {
    this.formError = null;
    this.editingId = c.id;
    this.form.name = c.name;
    this.form.type = c.type;
    this.form.activeStr = c.active ? 'true' : 'false';
    this.form.segment = c.segment ?? '';
    this.form.contactEmail = c.contactEmail ?? '';
    this.form.site = c.site ?? '';
    this.form.usefulLinksText = (c.usefulLinks ?? []).join('\n');
  }

  cancelEdit(): void {
    this.formError = null;
    this.editingId = null;
    this.form = {
      name: '',
      type: 'CLIENT',
      activeStr: 'true',
      segment: '',
      contactEmail: '',
      site: '',
      usefulLinksText: '',
    };
  }

  private normalizeUsefulLinks(text: string): string[] | null {
    const items = text
      .split('\n')
      .map((s) => s.trim())
      .filter((s) => s.length > 0);
    return items.length > 0 ? items : null;
  }

  async onSubmit(ev: Event): Promise<void> {
    ev.preventDefault();
    this.formError = null;

    const base = {
      name: this.form.name.trim(),
      type: this.form.type,
      segment: this.form.segment.trim(),
      contactEmail: this.form.contactEmail.trim(),
      site: this.form.site.trim() || null,
      usefulLinks: this.normalizeUsefulLinks(this.form.usefulLinksText),
    };

    try {
      if (this.editingId) {
        const dto: UpdateCompanyDto = {
          ...base,
          active: this.form.activeStr === 'true',
        };
        await firstValueFrom(this.api.updateCompany(this.editingId, dto));
        this.cancelEdit();
        this.refresh$.next();
        return;
      }

      const dto: CreateCompanyDto = base;
      await firstValueFrom(this.api.createCompany(dto));
      this.cancelEdit();
      this.refresh$.next();
    } catch (err: unknown) {
      if (err instanceof HttpErrorResponse) {
        this.formError = `Erro HTTP ${err.status}: ${
          err.error?.message ?? err.statusText ?? 'Falha ao salvar'
        }`;
        return;
      }
      this.formError = err instanceof Error ? err.message : 'Falha inesperada ao salvar';
    }
  }
}
