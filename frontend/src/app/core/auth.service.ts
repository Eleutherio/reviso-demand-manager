import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import type { UserRole } from './roles';

type LoginResponse = { token: string };

type JwtPayload = {
  role?: UserRole | string;
  cid?: string;
  exp?: number;
};

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenKey = 'reviso_token';

  constructor(private readonly http: HttpClient) {}

  login(email: string, password: string): Observable<void> {
    return this.http.post<LoginResponse>('/api/auth/login', { email, password }).pipe(
      map((res) => res?.token),
      tap((token) => {
        if (!token) throw new Error('Token ausente no login');
        localStorage.setItem(this.tokenKey, token);
      }),
      map(() => void 0)
    );
  }

  loginClient(companyCode: string, email: string, password: string): Observable<void> {
    return this.http
      .post<LoginResponse>('/api/auth/login-client', { companyCode, email, password })
      .pipe(
        map((res) => res?.token),
        tap((token) => {
          if (!token) throw new Error('Token ausente no login');
          localStorage.setItem(this.tokenKey, token);
        }),
        map(() => void 0)
      );
  }

  recoverCompanyCode(email: string): Observable<{ message?: string }> {
    return this.http.post<{ message?: string }>('/api/auth/recover-company-code', { email });
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isLoggedIn(): boolean {
    const token = this.getToken();
    if (!token) return false;
    const payload = this.decodeJwt(token);
    const exp = payload?.exp;
    if (typeof exp !== 'number') return true;
    const nowSec = Math.floor(Date.now() / 1000);
    if (exp <= nowSec) {
      this.logout();
      return false;
    }
    return true;
  }

  decodeJwt(token?: string | null): JwtPayload | null {
    const raw = token ?? this.getToken();
    if (!raw) return null;

    const parts = raw.split('.');
    if (parts.length < 2) return null;

    try {
      const payloadBase64Url = parts[1];
      const payloadBase64 = payloadBase64Url.replace(/-/g, '+').replace(/_/g, '/');
      const padded = payloadBase64.padEnd(
        payloadBase64.length + ((4 - (payloadBase64.length % 4)) % 4),
        '='
      );
      const json = atob(padded);
      return JSON.parse(json) as JwtPayload;
    } catch {
      return null;
    }
  }

  getRole(): UserRole | null {
    const role = this.decodeJwt()?.role;
    if (role === 'AGENCY_ADMIN' || role === 'AGENCY_USER' || role === 'CLIENT_USER') return role;
    return null;
  }

  getCompanyId(): string | null {
    const cid = this.decodeJwt()?.cid;
    return typeof cid === 'string' && cid.length > 0 ? cid : null;
  }
}
