import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AccessProfileDto {
  id: string;
  name: string;
  description?: string | null;
  isDefault?: boolean | null;
}

@Injectable({ providedIn: 'root' })
export class AdminAccessProfilesApi {
  constructor(private readonly http: HttpClient) {}

  listAccessProfiles(): Observable<AccessProfileDto[]> {
    return this.http.get<AccessProfileDto[]>('/api/admin/access-profiles');
  }
}
