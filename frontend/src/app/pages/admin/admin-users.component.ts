import { Component } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, firstValueFrom, forkJoin, map, of, startWith, Subject, switchMap } from 'rxjs';
import { AdminCompaniesApi } from '../../api/admin-companies.api';
import { AdminUsersApi, CreateUserDto, UpdateUserDto } from '../../api/admin-users.api';
import { CompanyDto } from '../../api/company';
import { UserDto, UserRole } from '../../api/user';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, DatePipe],
  template: `
    <div style="padding: 12px 0;">
      <h3>{{ editingId ? 'Editar usuário' : 'Criar usuário' }}</h3>

      <form (submit)="onSubmit($event)" style="display: grid; gap: 8px; max-width: 720px;">
        <label style="display: grid; gap: 4px;">
          Nome completo
          <input
            name="fullName"
            required
            [value]="form.fullName"
            [style.borderColor]="fieldErrors.fullName ? '#c00' : ''"
            (input)="onTextInput('fullName', $event)"
          />
          @if (fieldErrors.fullName) {
          <small style="color: #c00;">{{ fieldErrors.fullName }}</small>
          }
        </label>

        <label style="display: grid; gap: 4px;">
          Email
          <input
            name="email"
            type="email"
            required
            [value]="form.email"
            [style.borderColor]="fieldErrors.email ? '#c00' : ''"
            (input)="onTextInput('email', $event)"
          />
          @if (fieldErrors.email) {
          <small style="color: #c00;">{{ fieldErrors.email }}</small>
          }
        </label>

        @if (!editingId) {
        <label style="display: grid; gap: 4px;">
          Senha
          <input
            name="password"
            [type]="showPassword ? 'text' : 'password'"
            minlength="8"
            autocomplete="new-password"
            required
            [value]="form.password"
            [style.borderColor]="fieldErrors.password ? '#c00' : ''"
            (input)="onTextInput('password', $event)"
          />
          @if (fieldErrors.password) {
          <small style="color: #c00;">{{ fieldErrors.password }}</small>
          }
        </label>
        <div style="display: flex; gap: 12px; align-items: center;">
          <label style="display: inline-flex; gap: 6px; align-items: center;">
            <input
              type="checkbox"
              [checked]="showPassword"
              (change)="showPassword = getChecked($event)"
            />
            Mostrar senha
          </label>
          <small style="color: inherit;">Mínimo 8 caracteres.</small>
        </div>
        }

        <label style="display: grid; gap: 4px;">
          Função
          <select
            name="role"
            required
            [value]="form.role"
            (change)="onRoleChange($event)"
          >
            <option value="AGENCY_ADMIN">{{ roleLabels.AGENCY_ADMIN }}</option>
            <option value="AGENCY_USER">{{ roleLabels.AGENCY_USER }}</option>
            <option value="CLIENT_USER">{{ roleLabels.CLIENT_USER }}</option>
          </select>
        </label>

        @if (editingId) {
        <label style="display: grid; gap: 4px;">
          Ativo
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

        @if (form.role === 'CLIENT_USER') {
        <label style="display: grid; gap: 4px;">
          Empresa (código ou UUID)
          <input
            name="companyId"
            list="company-options"
            placeholder="CABC-CL-XXX"
            [value]="form.companyIdText"
            [style.borderColor]="fieldErrors.companyId ? '#c00' : ''"
            (input)="onTextInput('companyId', $event)"
          />
          @if (fieldErrors.companyId) {
          <small style="color: #c00;">{{ fieldErrors.companyId }}</small>
          } @else {
          <small style="color: inherit;">Ex.: CABC-CL-XXX ou UUID.</small>
          }
        </label>

        <datalist id="company-options">
          @for (c of clientCompanies; track c.id) {
          <option [value]="c.companyCode">{{ c.name }} ({{ c.companyCode }})</option>
          }
        </datalist>

        @if (clientCompanies.length === 0) {
        <small style="color: inherit;">
          Cadastre uma empresa cliente para usar o código.
        </small>
        }
        }

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
    <p style="color: inherit;">Erro ao carregar usuários: {{ vm.message }}</p>
    } @else {
    <p>
      Total: <strong>{{ vm.users.length }}</strong>
    </p>

    <div
      style="padding: 8px 0; display: grid; gap: 12px; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));"
    >
      <label style="display: grid; gap: 4px;">
        Buscar usuários
        <input
          name="search"
          placeholder="Nome, email, empresa ou código"
          [value]="searchTerm"
          (input)="searchTerm = getValue($event)"
        />
      </label>

      <label style="display: grid; gap: 4px;">
        Função
        <select
          name="filterRole"
          [value]="filterRole"
          (change)="filterRole = getFilterRole($event)"
        >
          <option value="">Todas</option>
          <option value="AGENCY_ADMIN">{{ roleLabels.AGENCY_ADMIN }}</option>
          <option value="AGENCY_USER">{{ roleLabels.AGENCY_USER }}</option>
          <option value="CLIENT_USER">{{ roleLabels.CLIENT_USER }}</option>
        </select>
      </label>

      <label style="display: grid; gap: 4px;">
        Ativo
        <select
          name="filterActive"
          [value]="filterActive"
          (change)="filterActive = getFilterActive($event)"
        >
          <option value="all">Todos</option>
          <option value="active">Ativos</option>
          <option value="inactive">Inativos</option>
        </select>
      </label>
    </div>

    @if (vm.users.length === 0) {
    <p>Nenhum usuário cadastrado.</p>
    } @else if (getFilteredUsers(vm.users).length === 0) {
    <p>Nenhum usuário encontrado com os filtros atuais.</p>
    } @else {
    <div style="overflow-x: auto;">
      <table style="width: 100%; border-collapse: collapse;">
        <thead>
          <tr>
            <th style="text-align: left; padding: 8px;">Nome</th>
            <th style="text-align: left; padding: 8px;">Email</th>
            <th style="text-align: left; padding: 8px;">Função</th>
            <th style="text-align: left; padding: 8px;">Ativo</th>
            <th style="text-align: left; padding: 8px;">Empresa</th>
            <th style="text-align: left; padding: 8px;">Código</th>
            <th style="text-align: left; padding: 8px;">Criado em</th>
            <th style="text-align: left; padding: 8px;"></th>
          </tr>
        </thead>
        <tbody>
          @for (u of getFilteredUsers(vm.users); track u.id) {
          <tr>
            <td style="padding: 8px;">{{ u.fullName }}</td>
            <td style="padding: 8px;">{{ u.email }}</td>
            <td style="padding: 8px;">{{ roleLabels[u.role] ?? u.role }}</td>
            <td style="padding: 8px;">{{ u.active ? 'Sim' : 'Não' }}</td>
            <td style="padding: 8px;">{{ getCompanyName(u) ?? '-' }}</td>
            <td style="padding: 8px;">
              <span>{{ u.companyCode ?? '-' }}</span>
              @if (u.companyCode) {
              <button
                type="button"
                style="margin-left: 6px;"
                (click)="copyCompanyCode(u.companyCode)"
              >
                Copiar
              </button>
              }
            </td>
            <td style="padding: 8px;">{{ u.createdAt | date : 'dd/MM/yyyy HH:mm' }}</td>
            <td style="padding: 8px; display: flex; gap: 8px; flex-wrap: wrap;">
              <button type="button" (click)="startEdit(u)">Editar</button>
              <button type="button" (click)="onDelete(u)">Remover</button>
            </td>
          </tr>
          }
        </tbody>
      </table>
    </div>
    }
    } }
  `,
})
export class AdminUsersComponent {
  readonly vm$;
  readonly roleLabels: Record<UserRole, string> = {
    AGENCY_ADMIN: 'Administrador',
    AGENCY_USER: 'Usuário',
    CLIENT_USER: 'Cliente',
  };

  companies: CompanyDto[] = [];
  clientCompanies: CompanyDto[] = [];

  private companyById = new Map<string, CompanyDto>();
  private companyByCode = new Map<string, CompanyDto>();

  editingId: string | null = null;

  form: {
    fullName: string;
    email: string;
    password: string;
    role: UserRole;
    companyIdText: string;
    activeStr: 'true' | 'false';
  } = {
    fullName: '',
    email: '',
    password: '',
    role: 'AGENCY_USER',
    companyIdText: '',
    activeStr: 'true',
  };

  formError: string | null = null;
  fieldErrors: Partial<Record<'fullName' | 'email' | 'password' | 'companyId', string>> = {};
  searchTerm = '';
  filterRole: '' | UserRole = '';
  filterActive: 'all' | 'active' | 'inactive' = 'all';
  showPassword = false;

  private readonly refresh$ = new Subject<void>();

  constructor(
    private readonly api: AdminUsersApi,
    private readonly companiesApi: AdminCompaniesApi
  ) {
    this.vm$ = this.refresh$.pipe(
      startWith(undefined),
      switchMap(() =>
        forkJoin({
          users: this.api.listUsers(),
          companies: this.companiesApi.listCompanies(),
        })
      ),
      map(({ users, companies }) => {
        this.setCompanies(companies);
        return { status: 'ready' as const, users };
      }),
      startWith({ status: 'loading' as const }),
      catchError((err: unknown) => {
        this.setCompanies([]);
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

  private setCompanies(companies: CompanyDto[]): void {
    this.companies = companies;
    this.clientCompanies = companies.filter((company) =>
      company.type === 'CLIENT' && company.active
    );
    this.companyById = new Map(companies.map((company) => [company.id, company]));
    this.companyByCode = new Map(
      companies
        .filter((company) => company.companyCode)
        .map((company) => [company.companyCode.toLowerCase(), company])
    );
  }

  getValue(ev: Event): string {
    const target = ev.target as HTMLInputElement;
    return target.value;
  }

  getChecked(ev: Event): boolean {
    const target = ev.target as HTMLInputElement;
    return target.checked;
  }

  getUserRole(ev: Event): UserRole {
    const target = ev.target as HTMLSelectElement;
    return target.value as UserRole;
  }

  getFilterRole(ev: Event): '' | UserRole {
    const target = ev.target as HTMLSelectElement;
    return target.value as '' | UserRole;
  }

  getFilterActive(ev: Event): 'all' | 'active' | 'inactive' {
    const target = ev.target as HTMLSelectElement;
    return target.value as 'all' | 'active' | 'inactive';
  }

  getBoolStr(ev: Event): 'true' | 'false' {
    const target = ev.target as HTMLSelectElement;
    return target.value === 'true' ? 'true' : 'false';
  }

  private isUuid(value: string): boolean {
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(
      value
    );
  }

  private isCompanyCode(value: string): boolean {
    return /^[A-Z0-9]{4}-(CL|AG)-[A-Z0-9]{3}(?:-\d{2})?$/i.test(value);
  }

  private normalizeCompanyCode(value: string): string {
    return value.trim().toUpperCase();
  }

  getCompanyName(user: UserDto): string | null {
    if (user.companyId) {
      const company = this.companyById.get(user.companyId);
      if (company?.name) return company.name;
    }

    if (user.companyCode) {
      const company = this.companyByCode.get(user.companyCode.toLowerCase());
      if (company?.name) return company.name;
    }

    return null;
  }

  getFilteredUsers(users: UserDto[]): UserDto[] {
    const term = this.searchTerm.trim().toLowerCase();

    return users.filter((user) => {
      if (this.filterRole && user.role !== this.filterRole) return false;
      if (this.filterActive === 'active' && !user.active) return false;
      if (this.filterActive === 'inactive' && user.active) return false;

      if (!term) return true;

      const companyId = user.companyId ?? '';
      const companyCode = user.companyCode ?? '';
      const companyName = this.getCompanyName(user) ?? '';
      const roleLabel = this.roleLabels[user.role] ?? user.role;
      const matchesSearch =
        user.fullName.toLowerCase().includes(term) ||
        user.email.toLowerCase().includes(term) ||
        user.role.toLowerCase().includes(term) ||
        roleLabel.toLowerCase().includes(term) ||
        companyName.toLowerCase().includes(term) ||
        companyCode.toLowerCase().includes(term) ||
        companyId.toLowerCase().includes(term);

      return matchesSearch;
    });
  }

  startEdit(u: UserDto): void {
    this.formError = null;
    this.fieldErrors = {};
    this.editingId = u.id;
    this.form.fullName = u.fullName;
    this.form.email = u.email;
    this.form.password = '';
    this.form.role = u.role;
    this.form.companyIdText = u.companyCode ?? u.companyId ?? '';
    this.form.activeStr = u.active ? 'true' : 'false';
  }

  cancelEdit(): void {
    this.formError = null;
    this.fieldErrors = {};
    this.editingId = null;
    this.resetForm();
  }

  private resetForm(): void {
    this.form = {
      fullName: '',
      email: '',
      password: '',
      role: 'AGENCY_USER',
      companyIdText: '',
      activeStr: 'true',
    };
    this.showPassword = false;
    this.fieldErrors = {};
  }

  onTextInput(field: 'fullName' | 'email' | 'password' | 'companyId', ev: Event): void {
    const value = this.getValue(ev);
    if (field === 'fullName') {
      this.form.fullName = value;
    } else if (field === 'email') {
      this.form.email = value;
    } else if (field === 'password') {
      this.form.password = value;
    } else {
      this.form.companyIdText = value;
    }

    if (this.fieldErrors[field] && value.trim()) {
      delete this.fieldErrors[field];
    }
  }

  onRoleChange(ev: Event): void {
    this.form.role = this.getUserRole(ev);
    if (this.form.role !== 'CLIENT_USER') {
      this.form.companyIdText = '';
      delete this.fieldErrors.companyId;
    }
  }

  private validateForm(): boolean {
    const errors: typeof this.fieldErrors = {};

    if (!this.form.fullName.trim()) {
      errors.fullName = 'Informe o nome completo.';
    }

    if (!this.form.email.trim()) {
      errors.email = 'Informe o email.';
    }

    if (!this.editingId) {
      const password = this.form.password.trim();
      if (!password) {
        errors.password = 'Informe a senha.';
      } else if (password.length < 8) {
        errors.password = 'Senha deve ter ao menos 8 caracteres.';
      }
    }

    if (this.form.role === 'CLIENT_USER') {
      const companyRef = this.form.companyIdText.trim();
      if (!companyRef) {
        errors.companyId = 'Informe o código da empresa.';
      } else {
        const isUuid = this.isUuid(companyRef);
        const normalizedCode = this.normalizeCompanyCode(companyRef);
        const isCode = this.isCompanyCode(normalizedCode);

        if (!isUuid && !isCode) {
          errors.companyId = 'Informe um UUID válido ou código no formato AAAA-CL-XXX.';
        } else if (
          isUuid &&
          this.companyById.size > 0 &&
          !this.companyById.has(companyRef)
        ) {
          errors.companyId = 'Empresa não encontrada para este UUID.';
        } else if (
          !isUuid &&
          this.companyByCode.size > 0 &&
          !this.companyByCode.has(normalizedCode.toLowerCase())
        ) {
          errors.companyId = 'Código de empresa não encontrado.';
        }
      }
    }

    this.fieldErrors = errors;
    return Object.keys(errors).length === 0;
  }

  async onSubmit(ev: Event): Promise<void> {
    ev.preventDefault();
    this.formError = null;
    this.fieldErrors = {};

    if (!this.validateForm()) {
      return;
    }

    const companyRef = this.form.companyIdText.trim();
    const hasCompanyRef = companyRef.length > 0;
    const isUuid = hasCompanyRef && this.isUuid(companyRef);
    const companyId = isUuid ? companyRef : null;
    const companyCode = hasCompanyRef && !isUuid ? this.normalizeCompanyCode(companyRef) : null;

    const base = {
      fullName: this.form.fullName.trim(),
      email: this.form.email.trim(),
      role: this.form.role,
      companyId,
      companyCode,
    };

    try {
      if (this.editingId) {
        const dto: UpdateUserDto = {
          ...base,
          active: this.form.activeStr === 'true',
        };
        await firstValueFrom(this.api.updateUser(this.editingId, dto));
        this.cancelEdit();
        this.refresh$.next();
        return;
      }

      const dto: CreateUserDto = {
        ...base,
        password: this.form.password,
      };
      await firstValueFrom(this.api.createUser(dto));
      this.resetForm();
      this.refresh$.next();
    } catch (err: unknown) {
      if (err instanceof HttpErrorResponse) {
        this.formError = `Erro HTTP ${err.status}: ${
          err.error?.message ?? err.statusText ?? 'Falha ao salvar'
        }`;
        return;
      }
      this.formError = err instanceof Error ? err.message : 'Falha inesperada ao salvar usuário';
    }
  }

  async onDelete(u: UserDto): Promise<void> {
    const confirmation = prompt(`Para remover ${u.fullName}, digite o email: ${u.email}`);
    if (!confirmation || confirmation.trim().toLowerCase() !== u.email.toLowerCase()) {
      return;
    }

    this.formError = null;

    try {
      await firstValueFrom(this.api.deleteUser(u.id));
      if (this.editingId === u.id) {
        this.cancelEdit();
      }
      this.refresh$.next();
    } catch (err: unknown) {
      if (err instanceof HttpErrorResponse) {
        this.formError = `Erro HTTP ${err.status}: ${
          err.error?.message ?? err.statusText ?? 'Falha ao remover'
        }`;
        return;
      }
      this.formError = err instanceof Error ? err.message : 'Falha inesperada ao remover usuário';
    }
  }

  copyCompanyCode(companyCode: string | null): void {
    if (!companyCode) return;

    if (navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(companyCode).catch(() => this.fallbackCopy(companyCode));
      return;
    }

    this.fallbackCopy(companyCode);
  }

  private fallbackCopy(text: string): void {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.focus();
    textarea.select();
    document.execCommand('copy');
    document.body.removeChild(textarea);
  }
}
