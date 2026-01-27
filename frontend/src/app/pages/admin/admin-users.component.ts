import { Component } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, firstValueFrom, forkJoin, map, of, startWith, Subject, switchMap } from 'rxjs';
import { AdminCompaniesApi } from '../../api/admin-companies.api';
import { AdminAccessProfilesApi, AccessProfileDto } from '../../api/admin-access-profiles.api';
import { AdminInvitesApi, CreateAgencyUserInviteDto, CreateClientUserInviteDto } from '../../api/admin-invites.api';
import { AdminUsersApi, CreateUserDto, UpdateUserDto } from '../../api/admin-users.api';
import { CompanyDto } from '../../api/company';
import { UserDto, UserRole } from '../../api/user';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, DatePipe],
  styleUrls: ['./admin-users.component.scss'],
  template: `
    <section class="users-page">
      <header class="users-header">
        <div class="users-title-block">
          <p class="users-eyebrow">Administração</p>
          <h2 class="users-title">
            Gestão de Usuários - {{ agencyName ?? '...' }}
          </h2>
          <p class="users-subtitle">
            Aqui você adiciona, edita e define papéis criativos dos seus colaboradores.
          </p>
        </div>
      </header>

      <div class="users-grid">
        <section class="users-card users-card--form">
          <div class="users-card__head">
            <h3 class="users-card__title">
              {{ editingId ? 'Editar usuário' : 'Adicionar usuário' }}
            </h3>
            <p class="users-muted">Gerencie usuários e convites em um único painel.</p>
          </div>

          <form (submit)="onSubmit($event)" class="users-form">
            @if (!editingId) {
            <label class="users-field">
              <span class="users-field__label">Modo de cadastro</span>
              <div class="users-mode">
                <label class="users-check">
                  <input
                    type="radio"
                    name="createMode"
                    value="invite"
                    [checked]="createMode === 'invite'"
                    (change)="setCreateMode('invite')"
                  />
                  <span>Convite por e-mail</span>
                </label>
                <label class="users-check">
                  <input
                    type="radio"
                    name="createMode"
                    value="manual"
                    [checked]="createMode === 'manual'"
                    (change)="setCreateMode('manual')"
                  />
                  <span>Cadastro manual</span>
                </label>
              </div>
            </label>
            }

            <label class="users-field">
              <span class="users-field__label">
                Perfil de acesso <span class="users-required">*</span>
              </span>
              <select
                name="role"
                required
                [value]="form.role"
                class="users-select"
                (change)="onRoleChange($event)"
              >
                <option value="AGENCY_ADMIN">{{ roleLabels.AGENCY_ADMIN }}</option>
                <option value="AGENCY_USER">{{ roleLabels.AGENCY_USER }}</option>
              </select>
            </label>

            <label class="users-field">
              <span class="users-field__label">
                Nome completo <span class="users-required">*</span>
              </span>
              <input
                name="fullName"
                required
                [value]="form.fullName"
                class="users-input"
                [class.users-input--error]="fieldErrors.fullName"
                (input)="onTextInput('fullName', $event)"
              />
              @if (fieldErrors.fullName) {
              <small class="users-error">{{ fieldErrors.fullName }}</small>
              }
            </label>

            <label class="users-field">
              <span class="users-field__label">E-mail <span class="users-required">*</span></span>
              <input
                name="email"
                type="email"
                required
                [value]="form.email"
                class="users-input"
                [class.users-input--error]="fieldErrors.email"
                (input)="onTextInput('email', $event)"
              />
              @if (fieldErrors.email) {
              <small class="users-error">{{ fieldErrors.email }}</small>
              }
            </label>

            @if (form.role !== 'CLIENT_USER') {
            <label class="users-field">
              <span class="users-field__label">
                Papel do usuário <span class="users-required">*</span>
              </span>
              <select
                name="accessProfileId"
                required
                [value]="form.accessProfileId"
                class="users-select"
                [class.users-input--error]="fieldErrors.accessProfileId"
                (change)="onAccessProfileChange($event)"
              >
                <option value="" disabled>Selecione</option>
                @for (p of selectableProfiles; track p.id) {
                <option [value]="p.id">{{ getProfileDisplayName(p.name) }}</option>
                }
              </select>
              @if (fieldErrors.accessProfileId) {
              <small class="users-error">{{ fieldErrors.accessProfileId }}</small>
              } @else if (selectableProfiles.length === 0) {
              <small class="users-muted">Nenhum papel do usuário disponível.</small>
              }
            </label>
            }
            @if (!editingId && createMode === 'manual') {
            <label class="users-field">
              <span class="users-field__label">Senha <span class="users-required">*</span></span>
              <input
                name="password"
                [type]="showPassword ? 'text' : 'password'"
                minlength="8"
                autocomplete="new-password"
                required
                [value]="form.password"
                class="users-input"
                [class.users-input--error]="fieldErrors.password"
                (input)="onTextInput('password', $event)"
              />
              @if (fieldErrors.password) {
              <small class="users-error">{{ fieldErrors.password }}</small>
              }
            </label>
            <label class="users-field">
              <span class="users-field__label">
                Repetir senha <span class="users-required">*</span>
              </span>
              <input
                name="confirmPassword"
                [type]="showPassword ? 'text' : 'password'"
                minlength="8"
                autocomplete="new-password"
                required
                [value]="form.confirmPassword"
                class="users-input"
                [class.users-input--error]="fieldErrors.confirmPassword"
                (input)="onTextInput('confirmPassword', $event)"
              />
              @if (fieldErrors.confirmPassword) {
              <small class="users-error">{{ fieldErrors.confirmPassword }}</small>
              }
            </label>
            <div class="users-form__row">
              <label class="users-check">
                <input
                  type="checkbox"
                  [checked]="showPassword"
                  (change)="showPassword = getChecked($event)"
                />
                <span>Mostrar senha</span>
              </label>
              <small class="users-muted">Mínimo 8 caracteres.</small>
            </div>
            }

            @if (editingId) {
            <label class="users-field">
              <span class="users-field__label">Ativo</span>
              <select
                name="active"
                [value]="form.activeStr"
                class="users-select"
                (change)="form.activeStr = getBoolStr($event)"
              >
                <option value="true">Sim</option>
                <option value="false">Não</option>
              </select>
            </label>
            }

            @if (form.role === 'CLIENT_USER') {
            <label class="users-field">
              <span class="users-field__label">
                Empresa (código ou UUID) <span class="users-required">*</span>
              </span>
              <input
                name="companyId"
                list="company-options"
                placeholder="CABC-CL-XXX"
                [value]="form.companyIdText"
                class="users-input"
                [class.users-input--error]="fieldErrors.companyId"
                (input)="onTextInput('companyId', $event)"
              />
              @if (fieldErrors.companyId) {
              <small class="users-error">{{ fieldErrors.companyId }}</small>
              } @else {
              <small class="users-muted">Ex.: CABC-CL-XXX ou UUID.</small>
              }
            </label>

            <datalist id="company-options">
              @for (c of clientCompanies; track c.id) {
              <option [value]="c.companyCode">{{ c.name }} ({{ c.companyCode }})</option>
              }
            </datalist>

            @if (clientCompanies.length === 0) {
            <small class="users-muted">
              Cadastre uma empresa cliente para usar o código.
            </small>
            }
            }

            <div class="users-form__actions">
              <button type="submit" class="btn btn--primary">
                {{
                  editingId
                    ? 'Salvar'
                    : createMode === 'invite'
                      ? inviteSent
                        ? 'Enviado'
                        : 'Enviar convite'
                      : 'Criar usuário'
                }}
              </button>
              @if (editingId) {
              <button type="button" class="btn btn--ghost" (click)="cancelEdit()">
                Cancelar
              </button>
              }
            </div>
            @if (formSuccess) {
            <div class="users-alert">{{ formSuccess }}</div>
            }
            @if (formError) {
            <div class="users-alert users-alert--error">{{ formError }}</div>
            }
          </form>
        </section>

        <section class="users-card users-card--list">
          <div class="users-card__head users-card__head--row">
            <div>
              <h3 class="users-card__title">Usuários cadastrados</h3>
              <p class="users-muted">Filtre e acompanhe os acessos do time.</p>
            </div>
          </div>

          @if (vm$ | async; as vm) {
          @if (vm.status === 'loading') {
          <div class="users-state">Carregando...</div>
          } @else if (vm.status === 'error') {
          <div class="users-alert users-alert--error">
            Erro ao carregar usuários: {{ vm.message }}
          </div>
          } @else {
          <div class="users-toolbar">
            <div class="users-total">
              <span>Total</span>
              <strong>{{ vm.users.length }}</strong>
            </div>
          </div>
          @if (vm.leakCount > 0) {
          <div class="users-alert users-alert--error">
            {{ vm.leakCount }} usuário(s) de outra agência foram ocultados por segurança.
          </div>
          }

          <div class="users-filters">
            <label class="users-field">
              <span class="users-field__label">Buscar usuários</span>
              <input
                name="search"
                placeholder="Nome, e-mail ou perfil"
                [value]="searchTerm"
                class="users-input"
                (input)="searchTerm = getValue($event)"
              />
            </label>

            <label class="users-field">
              <span class="users-field__label">Papel do usuário</span>
              <select
                name="filterRole"
                [value]="filterRole"
                class="users-select"
                (change)="filterRole = getFilterRole($event)"
              >
                <option value="">Todas</option>
                <option value="AGENCY_ADMIN">{{ roleLabels.AGENCY_ADMIN }}</option>
                <option value="AGENCY_USER">{{ roleLabels.AGENCY_USER }}</option>
              </select>
            </label>

            <label class="users-field">
              <span class="users-field__label">Ativo</span>
              <select
                name="filterActive"
                [value]="filterActive"
                class="users-select"
                (change)="filterActive = getFilterActive($event)"
              >
                <option value="all">Todos</option>
                <option value="active">Ativos</option>
                <option value="inactive">Inativos</option>
              </select>
            </label>
          </div>
          @if (vm.users.length === 0) {
          <div class="users-empty">Nenhum usuário cadastrado.</div>
          } @else if (getFilteredUsers(vm.users).length === 0) {
          <div class="users-empty">Nenhum usuário encontrado com os filtros atuais.</div>
          } @else {
          <div class="users-table-wrap">
            <table class="users-table">
              <thead>
                <tr>
                  <th>Nome</th>
                  <th>E-mail</th>
                  <th>Perfil de acesso</th>
                  <th>Papel do usuário</th>
                  <th>Ativo</th>
                  <th>Última vez logado</th>
                  <th>Criado em</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                @for (u of getFilteredUsers(vm.users); track u.id) {
                <tr>
                  <td>{{ u.fullName }}</td>
                  <td>{{ u.email }}</td>
                  <td>
                    <span
                      class="users-chip"
                      [class.users-chip--admin]="u.role === 'AGENCY_ADMIN'"
                      [class.users-chip--agency]="u.role === 'AGENCY_USER'"
                      [class.users-chip--client]="u.role === 'CLIENT_USER'"
                    >
                      {{ roleLabels[u.role] ?? u.role }}
                    </span>
                  </td>
                  <td>
                    <span class="users-profile">{{ getAccessProfileLabel(u) }}</span>
                  </td>
                  <td>
                    <span
                      class="users-chip"
                      [class.users-chip--active]="u.active"
                      [class.users-chip--inactive]="!u.active"
                    >
                      {{ u.active ? 'Sim' : 'Não' }}
                    </span>
                  </td>
                  <td>
                    <span
                      class="users-chip users-chip--status"
                      [class.users-chip--online]="getLastLoginTone(u) === 'online'"
                      [class.users-chip--recent]="getLastLoginTone(u) === 'recent'"
                      [class.users-chip--stale]="getLastLoginTone(u) === 'stale'"
                      [class.users-chip--unknown]="getLastLoginTone(u) === 'unknown'"
                    >
                      {{ getLastLoginLabel(u) }}
                    </span>
                  </td>
                  <td>{{ u.createdAt | date : 'dd/MM/yyyy HH:mm' }}</td>
                  <td class="users-actions">
                    <button
                      type="button"
                      class="btn btn--ghost btn--sm"
                      (click)="startEdit(u)"
                    >
                      Editar
                    </button>
                    <button
                      type="button"
                      class="btn btn--danger btn--sm"
                      (click)="openDeleteModal(u)"
                    >
                      Remover
                    </button>
                  </td>
                </tr>
                }
              </tbody>
            </table>
          </div>
          }
          }
          }
        </section>
      </div>
      @if (deleteTarget) {
      <div class="users-modal" (click)="closeDeleteModal()">
        <div class="users-modal__panel" (click)="$event.stopPropagation()">
          <div class="users-modal__head">
            <h4 class="users-modal__title">Confirmar exclusão</h4>
            <button
              type="button"
              class="users-modal__close"
              aria-label="Fechar"
              [disabled]="deletePending"
              (click)="closeDeleteModal()"
            >
              &times;
            </button>
          </div>
          <p class="users-modal__text">
            Tem certeza de que deseja remover <strong>{{ deleteTarget.fullName }}</strong>?
          </p>
          <label class="users-modal__check">
            <input
              type="checkbox"
              [checked]="deleteConfirmed"
              (change)="deleteConfirmed = getChecked($event); deleteError = null"
            />
            <span>Estou ciente de que essa ação não pode ser desfeita.</span>
          </label>
          @if (deleteError) {
          <div class="users-alert users-alert--error">{{ deleteError }}</div>
          }
          <div class="users-modal__actions">
            <button
              type="button"
              class="btn btn--ghost"
              [disabled]="deletePending"
              (click)="closeDeleteModal()"
            >
              Cancelar
            </button>
            <button
              type="button"
              class="btn btn--danger"
              [disabled]="!deleteConfirmed || deletePending"
              (click)="confirmDelete()"
            >
              Remover
            </button>
          </div>
        </div>
      </div>
      }
    </section>`,
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
  accessProfiles: AccessProfileDto[] = [];
  selectableProfiles: AccessProfileDto[] = [];
  agencyName: string | null = null;

  private companyById = new Map<string, CompanyDto>();
  private companyByCode = new Map<string, CompanyDto>();
  private profileById = new Map<string, AccessProfileDto>();
  private readonly allowedProfileNames = ['atendimento', 'criacao', 'gestor'];
  private readonly profileDisplayNames: Record<string, string> = {
    atendimento: 'Atendimento',
    criacao: 'Criação',
    gestor: 'Gestor',
  };
  private readonly lastLoginFormatter = new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });

  editingId: string | null = null;
  createMode: 'invite' | 'manual' = 'invite';

  form: {
    fullName: string;
    email: string;
    password: string;
    confirmPassword: string;
    role: UserRole;
    companyIdText: string;
    accessProfileId: string;
    activeStr: 'true' | 'false';
  } = {
    fullName: '',
    email: '',
    password: '',
    confirmPassword: '',
    role: 'AGENCY_USER',
    companyIdText: '',
    accessProfileId: '',
    activeStr: 'true',
  };

  formError: string | null = null;
  formSuccess: string | null = null;
  inviteSent = false;
  deleteTarget: UserDto | null = null;
  deleteConfirmed = false;
  deletePending = false;
  deleteError: string | null = null;
  fieldErrors: Partial<
    Record<'fullName' | 'email' | 'password' | 'confirmPassword' | 'companyId' | 'accessProfileId', string>
  > = {};
  searchTerm = '';
  filterRole: '' | UserRole = '';
  filterActive: 'all' | 'active' | 'inactive' = 'all';
  showPassword = false;

  private readonly refresh$ = new Subject<void>();

  constructor(
    private readonly api: AdminUsersApi,
    private readonly invitesApi: AdminInvitesApi,
    private readonly companiesApi: AdminCompaniesApi,
    private readonly accessProfilesApi: AdminAccessProfilesApi,
    private readonly auth: AuthService
  ) {
    this.vm$ = this.refresh$.pipe(
      startWith(undefined),
      switchMap(() =>
        forkJoin({
          users: this.api.listUsers(),
          companies: this.companiesApi.listCompanies(),
          profiles: this.accessProfilesApi.listAccessProfiles(),
        })
      ),
      map(({ users, companies, profiles }) => {
        this.setCompanies(companies);
        this.setAccessProfiles(profiles);
        const agencyUsers = this.filterUsersByAgency(users);
        const safeUsers = this.filterPanelUsers(agencyUsers);
        return {
          status: 'ready' as const,
          users: safeUsers,
          leakCount: Math.max(0, users.length - agencyUsers.length),
        };
      }),
      startWith({ status: 'loading' as const }),
      catchError((err: unknown) => {
        this.setCompanies([]);
        this.setAccessProfiles([]);
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
    const agencyCompany = companies.find((company) => company.type === 'AGENCY');
    this.agencyName = agencyCompany?.name ?? companies[0]?.name ?? null;
    this.companyById = new Map(companies.map((company) => [company.id, company]));
    this.companyByCode = new Map(
      companies
        .filter((company) => company.companyCode)
        .map((company) => [company.companyCode.toLowerCase(), company])
    );
  }

  private setAccessProfiles(profiles: AccessProfileDto[]): void {
    this.accessProfiles = profiles;
    this.profileById = new Map(profiles.map((profile) => [profile.id, profile]));
    this.selectableProfiles = profiles.filter((profile) =>
      this.allowedProfileNames.includes(this.normalizeProfileName(profile.name))
    );
    this.applyDefaultAccessProfile();
  }

  private applyDefaultAccessProfile(): void {
    if (this.editingId) return;
    if (this.form.role === 'CLIENT_USER') return;
    if (this.form.accessProfileId) return;
    this.form.accessProfileId = this.resolveDefaultAccessProfileId();
  }

  private resolveDefaultAccessProfileId(): string {
    const defaultProfile = this.selectableProfiles.find((profile) => profile.isDefault);
    if (defaultProfile) {
      return defaultProfile.id;
    }
    return this.selectableProfiles[0]?.id ?? '';
  }

  setCreateMode(mode: 'invite' | 'manual'): void {
    if (this.form.role === 'AGENCY_ADMIN' && mode === 'invite') {
      this.createMode = 'manual';
      return;
    }
    this.createMode = mode;
    this.inviteSent = false;
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

  private normalizeProfileName(name: string): string {
    return name
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim()
      .toLowerCase();
  }

  getProfileDisplayName(name: string | null | undefined): string {
    if (!name) return '';
    const normalized = this.normalizeProfileName(name);
    return this.profileDisplayNames[normalized] ?? name;
  }

  private isCurrentUser(user: UserDto): boolean {
    const currentEmail = this.auth.getEmail() ?? this.auth.decodeJwt()?.email ?? '';
    if (!currentEmail) return false;
    return user.email.toLowerCase() === currentEmail.toLowerCase();
  }

  private getActivityAt(user: UserDto): string | null | undefined {
    return user.lastSeenAt ?? user.lastLoginAt;
  }

  getAccessProfileLabel(user: UserDto): string {
    if (user.role === 'CLIENT_USER') return '-';
    if (!user.accessProfileId) return '—';
    const profile = this.profileById.get(user.accessProfileId);
    return profile ? this.getProfileDisplayName(profile.name) : '—';
  }

  getLastLoginLabel(user: UserDto): string {
    if (this.isCurrentUser(user)) return 'Online';
    const lastLoginAt = this.getActivityAt(user);
    if (!lastLoginAt) return 'Nunca';
    const parsed = new Date(lastLoginAt);
    if (Number.isNaN(parsed.getTime())) return '—';
    const diffMs = Date.now() - parsed.getTime();
    const diffMinutes = Math.floor(diffMs / 60000);
    if (diffMinutes < 5) return 'Online';
    if (diffMinutes < 60) return `Online há ${diffMinutes} min`;
    const diffHours = Math.floor(diffMinutes / 60);
    if (diffHours < 24) {
      return diffHours === 1 ? 'Online há 1 hora' : `Online há ${diffHours} horas`;
    }
    return this.lastLoginFormatter.format(parsed);
  }

  getLastLoginTone(user: UserDto): 'online' | 'recent' | 'stale' | 'unknown' {
    if (this.isCurrentUser(user)) return 'online';
    const lastLoginAt = this.getActivityAt(user);
    if (!lastLoginAt) return 'unknown';
    const parsed = new Date(lastLoginAt);
    if (Number.isNaN(parsed.getTime())) return 'unknown';
    const diffMs = Date.now() - parsed.getTime();
    const diffMinutes = Math.floor(diffMs / 60000);
    if (diffMinutes < 5) return 'online';
    if (diffMinutes < 60 * 24) return 'recent';
    return 'stale';
  }

  private resolveAgencyId(users: UserDto[]): string | null {
    const tokenAgencyId = this.auth.getAgencyId();
    if (tokenAgencyId) return tokenAgencyId;

    const agencyIds = Array.from(
      new Set(users.map((user) => user.agencyId).filter((id): id is string => !!id))
    );
    if (agencyIds.length === 1) return agencyIds[0];

    const email = this.auth.getEmail() ?? this.auth.decodeJwt()?.email ?? '';
    if (!email) return null;
    const match = users.find((user) => user.email.toLowerCase() === email.toLowerCase());
    return match?.agencyId ?? null;
  }

  private filterUsersByAgency(users: UserDto[]): UserDto[] {
    const agencyId = this.resolveAgencyId(users);
    if (!agencyId) return [];
    return users.filter((user) => user.agencyId === agencyId);
  }

  private filterPanelUsers(users: UserDto[]): UserDto[] {
    return users.filter(
      (user) => user.role === 'AGENCY_ADMIN' || user.role === 'AGENCY_USER'
    );
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

  getFilteredUsers(users: UserDto[]): UserDto[] {
    const term = this.searchTerm.trim().toLowerCase();

    return users.filter((user) => {
      if (this.filterRole && user.role !== this.filterRole) return false;
      if (this.filterActive === 'active' && !user.active) return false;
      if (this.filterActive === 'inactive' && user.active) return false;

      if (!term) return true;

      const roleLabel = this.roleLabels[user.role] ?? user.role;
      const accessProfileLabel = this.getAccessProfileLabel(user);
      const matchesSearch =
        user.fullName.toLowerCase().includes(term) ||
        user.email.toLowerCase().includes(term) ||
        user.role.toLowerCase().includes(term) ||
        roleLabel.toLowerCase().includes(term) ||
        accessProfileLabel.toLowerCase().includes(term);

      return matchesSearch;
    });
  }

  startEdit(u: UserDto): void {
    this.formError = null;
    this.formSuccess = null;
    this.fieldErrors = {};
    this.inviteSent = false;
    this.editingId = u.id;
    this.form.fullName = u.fullName;
    this.form.email = u.email;
    this.form.password = '';
    this.form.confirmPassword = '';
    this.form.role = u.role;
    this.form.companyIdText = u.companyCode ?? u.companyId ?? '';
    this.form.accessProfileId = u.accessProfileId ?? '';
    this.form.activeStr = u.active ? 'true' : 'false';
  }

  cancelEdit(): void {
    this.formError = null;
    this.formSuccess = null;
    this.fieldErrors = {};
    this.inviteSent = false;
    this.editingId = null;
    this.resetForm();
  }

  private resetForm(): void {
    this.form = {
      fullName: '',
      email: '',
      password: '',
      confirmPassword: '',
      role: 'AGENCY_USER',
      companyIdText: '',
      accessProfileId: this.resolveDefaultAccessProfileId(),
      activeStr: 'true',
    };
    this.createMode = 'invite';
    this.inviteSent = false;
    this.showPassword = false;
    this.fieldErrors = {};
  }

  onTextInput(
    field: 'fullName' | 'email' | 'password' | 'confirmPassword' | 'companyId',
    ev: Event
  ): void {
    const value = this.getValue(ev);
    if (field === 'fullName') {
      this.form.fullName = value;
    } else if (field === 'email') {
      this.form.email = value;
    } else if (field === 'password') {
      this.form.password = value;
    } else if (field === 'confirmPassword') {
      this.form.confirmPassword = value;
    } else {
      this.form.companyIdText = value;
    }

    if (this.fieldErrors[field] && value.trim()) {
      delete this.fieldErrors[field];
    }
    this.inviteSent = false;
  }

  onAccessProfileChange(ev: Event): void {
    const target = ev.target as HTMLSelectElement;
    this.form.accessProfileId = target.value;
    if (this.fieldErrors.accessProfileId && target.value) {
      delete this.fieldErrors.accessProfileId;
    }
    this.inviteSent = false;
  }

  onRoleChange(ev: Event): void {
    this.form.role = this.getUserRole(ev);
    if (this.form.role !== 'CLIENT_USER') {
      this.form.companyIdText = '';
      delete this.fieldErrors.companyId;
      if (!this.form.accessProfileId) {
        this.form.accessProfileId = this.resolveDefaultAccessProfileId();
      }
    } else {
      this.form.accessProfileId = '';
      delete this.fieldErrors.accessProfileId;
    }

    if (this.form.role === 'AGENCY_ADMIN') {
      this.createMode = 'manual';
    }
    this.inviteSent = false;
  }

  private validateForm(): boolean {
    const errors: typeof this.fieldErrors = {};

    if (!this.form.fullName.trim()) {
      errors.fullName = 'Informe o nome completo.';
    }

    if (!this.form.email.trim()) {
      errors.email = 'Informe o e-mail.';
    }

    if (!this.editingId && this.createMode === 'invite' && this.form.role === 'AGENCY_ADMIN') {
      errors.fullName = 'AGENCY_ADMIN deve ser criado via cadastro manual.';
    }

    if (!this.editingId && this.createMode === 'manual') {
      const password = this.form.password.trim();
      const confirmPassword = this.form.confirmPassword.trim();
      if (!password) {
        errors.password = 'Informe a senha.';
      } else if (password.length < 8) {
        errors.password = 'Senha deve ter ao menos 8 caracteres.';
      }
      if (!confirmPassword) {
        errors.confirmPassword = 'Repita a senha.';
      } else if (password && confirmPassword !== password) {
        errors.confirmPassword = 'As senhas não conferem.';
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
    } else {
      const accessProfileId = this.form.accessProfileId.trim();
      if (!accessProfileId) {
        errors.accessProfileId = 'Selecione o papel do usuário.';
      } else if (!this.isUuid(accessProfileId)) {
        errors.accessProfileId = 'Papel do usuário inválido.';
      }
    }

    this.fieldErrors = errors;
    return Object.keys(errors).length === 0;
  }
  async onSubmit(ev: Event): Promise<void> {
    ev.preventDefault();
    this.formError = null;
    this.formSuccess = null;
    this.fieldErrors = {};

    if (!this.validateForm()) {
      return;
    }

    const companyRef = this.form.companyIdText.trim();
    const hasCompanyRef = companyRef.length > 0;
    const isUuid = hasCompanyRef && this.isUuid(companyRef);
    const companyId = isUuid ? companyRef : null;
    const companyCode = hasCompanyRef && !isUuid ? this.normalizeCompanyCode(companyRef) : null;
    const accessProfileId = this.form.accessProfileId.trim();

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
          ...(this.form.role === 'CLIENT_USER' ? {} : { accessProfileId }),
          active: this.form.activeStr === 'true',
        };
        await firstValueFrom(this.api.updateUser(this.editingId, dto));
        this.cancelEdit();
        this.refresh$.next();
        return;
      }

      if (this.createMode === 'invite') {
        if (this.form.role === 'AGENCY_USER') {
          const inviteDto: CreateAgencyUserInviteDto = {
            fullName: base.fullName,
            email: base.email,
            accessProfileId,
          };
          await firstValueFrom(this.invitesApi.createAgencyUserInvite(inviteDto));
          this.formSuccess = 'Convite enviado com sucesso.';
          this.resetForm();
          this.inviteSent = true;
          return;
        }

        if (this.form.role === 'CLIENT_USER') {
          const inviteDto: CreateClientUserInviteDto = {
            fullName: base.fullName,
            email: base.email,
            companyId: base.companyId,
            companyCode: base.companyCode,
          };
          await firstValueFrom(this.invitesApi.createClientUserInvite(inviteDto));
          this.formSuccess = 'Convite enviado com sucesso.';
          this.resetForm();
          this.inviteSent = true;
          return;
        }
      }

      const dto: CreateUserDto = {
        ...base,
        ...(this.form.role === 'CLIENT_USER' ? {} : { accessProfileId }),
        password: this.form.password,
      };
      await firstValueFrom(this.api.createUser(dto));
      this.formSuccess = 'Usuário criado com sucesso.';
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

  openDeleteModal(u: UserDto): void {
    this.deleteTarget = u;
    this.deleteConfirmed = false;
    this.deletePending = false;
    this.deleteError = null;
  }

  closeDeleteModal(): void {
    if (this.deletePending) return;
    this.deleteTarget = null;
    this.deleteConfirmed = false;
    this.deletePending = false;
    this.deleteError = null;
  }

  async confirmDelete(): Promise<void> {
    const target = this.deleteTarget;
    if (!target || this.deletePending) return;
    if (!this.deleteConfirmed) {
      this.deleteError = 'Confirme que está ciente para continuar.';
      return;
    }

    this.deletePending = true;
    this.deleteError = null;

    try {
      await firstValueFrom(this.api.deleteUser(target.id));
      if (this.editingId === target.id) {
        this.cancelEdit();
      }
      this.refresh$.next();
      this.closeDeleteModal();
    } catch (err: unknown) {
      if (err instanceof HttpErrorResponse) {
        this.deleteError = `Erro HTTP ${err.status}: ${
          err.error?.message ?? err.statusText ?? 'Falha ao remover'
        }`;
        return;
      }
      this.deleteError = err instanceof Error ? err.message : 'Falha inesperada ao remover usuário';
    } finally {
      this.deletePending = false;
    }
  }

}
