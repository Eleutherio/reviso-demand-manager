import { Component } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, firstValueFrom, map, of, startWith, Subject, switchMap } from 'rxjs';
import { AdminUsersApi, CreateUserDto, UpdateUserDto } from '../../api/admin-users.api';
import { UserDto, UserRole } from '../../api/user';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, DatePipe],
  template: `
    <div style="padding: 12px 0;">
      <h3>{{ editingId ? 'Editar usuario' : 'Criar usuario' }}</h3>

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
            type="password"
            required
            [value]="form.password"
            [style.borderColor]="fieldErrors.password ? '#c00' : ''"
            (input)="onTextInput('password', $event)"
          />
          @if (fieldErrors.password) {
          <small style="color: #c00;">{{ fieldErrors.password }}</small>
          }
        </label>
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
          ID da empresa (obrigatório para referenciar Cliente)
          <input
            name="companyId"
            placeholder="UUID da empresa"
            [value]="form.companyIdText"
            [style.borderColor]="fieldErrors.companyId ? '#c00' : ''"
            (input)="onTextInput('companyId', $event)"
          />
          @if (fieldErrors.companyId) {
          <small style="color: #c00;">{{ fieldErrors.companyId }}</small>
          }
        </label>
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

    <div style="padding: 8px 0; max-width: 420px;">
      <label style="display: grid; gap: 4px; min-width: 260px;">
        Buscar usuarios
        <input
          name="search"
          placeholder="Nome, email, função ou cliente ID"
          [value]="searchTerm"
          (input)="searchTerm = getValue($event)"
        />
      </label>
    </div>

    <div style="overflow-x: auto;">
      <table style="width: 100%; border-collapse: collapse;">
        <thead>
          <tr>
            <th style="text-align: left; padding: 8px;">Nome</th>
            <th style="text-align: left; padding: 8px;">Email</th>
            <th style="text-align: left; padding: 8px;">Função</th>
            <th style="text-align: left; padding: 8px;">Ativo</th>
            <th style="text-align: left; padding: 8px;">Cliente ID</th>
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
            <td style="padding: 8px;">{{ u.companyId ?? '-' }}</td>
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
    } }
  `,
})
export class AdminUsersComponent {
  readonly vm$;
  readonly roleLabels: Record<UserRole, string> = {
    AGENCY_ADMIN: 'Administrador',
    AGENCY_USER: 'Usuario',
    CLIENT_USER: 'Cliente',
  };

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

  private readonly refresh$ = new Subject<void>();

  constructor(private readonly api: AdminUsersApi) {
    this.vm$ = this.refresh$.pipe(
      startWith(undefined),
      switchMap(() => this.api.listUsers()),
      map((users: UserDto[]) => ({ status: 'ready' as const, users })),
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
    const target = ev.target as HTMLInputElement;
    return target.value;
  }

  getUserRole(ev: Event): UserRole {
    const target = ev.target as HTMLSelectElement;
    return target.value as UserRole;
  }

  getBoolStr(ev: Event): 'true' | 'false' {
    const target = ev.target as HTMLSelectElement;
    return target.value === 'true' ? 'true' : 'false';
  }

  getFilteredUsers(users: UserDto[]): UserDto[] {
    const term = this.searchTerm.trim().toLowerCase();

    return users.filter((user) => {
      const companyId = user.companyId ?? '';
      const roleLabel = this.roleLabels[user.role] ?? user.role;
      const matchesSearch =
        !term ||
        user.fullName.toLowerCase().includes(term) ||
        user.email.toLowerCase().includes(term) ||
        user.role.toLowerCase().includes(term) ||
        roleLabel.toLowerCase().includes(term) ||
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
    this.form.companyIdText = u.companyId ?? '';
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

    if (!this.editingId && !this.form.password.trim()) {
      errors.password = 'Informe a senha.';
    }

    if (this.form.role === 'CLIENT_USER' && !this.form.companyIdText.trim()) {
      errors.companyId = 'Informe o ID da empresa.';
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

    const companyId = this.form.companyIdText.trim() || null;

    const base = {
      fullName: this.form.fullName.trim(),
      email: this.form.email.trim(),
      role: this.form.role,
      companyId,
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
      this.formError = err instanceof Error ? err.message : 'Falha inesperada ao salvar usuario';
    }
  }

  async onDelete(u: UserDto): Promise<void> {
    const message = `Remover usuario ${u.fullName}?`;
    if (!confirm(message)) {
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
      this.formError = err instanceof Error ? err.message : 'Falha inesperada ao remover usuario';
    }
  }
}
