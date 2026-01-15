import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserDto, UserRole } from './user';

export interface CreateUserDto {
  fullName: string;
  email: string;
  password: string;
  role: UserRole;
  companyId?: string | null;
  companyCode?: string | null;
}

export interface UpdateUserDto {
  fullName: string;
  email: string;
  role: UserRole;
  companyId?: string | null;
  companyCode?: string | null;
  active?: boolean | null;
}

@Injectable({ providedIn: 'root' })
export class AdminUsersApi {
  constructor(private readonly http: HttpClient) {}

  listUsers(): Observable<UserDto[]> {
    return this.http.get<UserDto[]>('/api/admin/users');
  }

  createUser(dto: CreateUserDto): Observable<UserDto> {
    return this.http.post<UserDto>('/api/admin/users', dto);
  }

  updateUser(id: string, dto: UpdateUserDto): Observable<UserDto> {
    return this.http.patch<UserDto>(`/api/admin/users/${id}`, dto);
  }

  deleteUser(id: string): Observable<void> {
    return this.http.delete<void>(`/api/admin/users/${id}`);
  }
}
