import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';
import { LiveSessionToken } from '../models/live-session.model';

@Injectable({ providedIn: 'root' })
export class LiveSessionApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl.replace(/\/$/, '');

  createToken(): Observable<LiveSessionToken> {
    return this.http.post<LiveSessionToken>(`${this.baseUrl}/live-sessions/token`, {});
  }
}
