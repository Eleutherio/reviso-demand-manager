import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, shareReplay } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface AppConfig {
  billingProvider: 'mock' | 'stripe';
  trialDays: number;
  isMock: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ConfigService {
  private config$: Observable<AppConfig> | null = null;

  constructor(private http: HttpClient) {}

  getConfig(): Observable<AppConfig> {
    if (!this.config$) {
      this.config$ = this.http.get<AppConfig>(`${environment.apiUrl}/public/config`).pipe(
        catchError(() => of({ billingProvider: 'mock', trialDays: 14, isMock: true } as AppConfig)),
        shareReplay(1)
      );
    }
    return this.config$;
  }
}
