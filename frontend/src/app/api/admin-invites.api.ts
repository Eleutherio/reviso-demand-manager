import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserRole } from './user';

export interface CreateAgencyUserInviteDto {
  fullName: string;
  email: string;
  accessProfileId: string;
}

export interface CreateClientUserInviteDto {
  fullName: string;
  email: string;
  companyId?: string | null;
  companyCode?: string | null;
}

export interface UserInviteDto {
  id: string;
  fullName: string;
  email: string;
  role: UserRole;
  status: string;
  expiresAt: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class AdminInvitesApi {
  constructor(private readonly http: HttpClient) {}

  createAgencyUserInvite(dto: CreateAgencyUserInviteDto): Observable<UserInviteDto> {
    return this.http.post<UserInviteDto>('/api/admin/invites/agency-users', dto);
  }

  createClientUserInvite(dto: CreateClientUserInviteDto): Observable<UserInviteDto> {
    return this.http.post<UserInviteDto>('/api/admin/invites/client-users', dto);
  }
}
