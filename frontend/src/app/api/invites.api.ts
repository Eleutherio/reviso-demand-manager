import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AcceptInviteDto {
  token: string;
  password: string;
}

export interface AcceptInviteResponse {
  status: number;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class InvitesApi {
  constructor(private readonly http: HttpClient) {}

  acceptInvite(dto: AcceptInviteDto): Observable<AcceptInviteResponse> {
    return this.http.post<AcceptInviteResponse>('/api/invites/accept', dto);
  }
}
