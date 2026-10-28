import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';
import { Transcription, TranscriptionRequest } from '../models/transcription.model';

@Injectable({ providedIn: 'root' })
export class TranscriptionApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl.replace(/\/$/, '');

  list(): Observable<Transcription[]> {
    return this.http.get<Transcription[]>(this.baseUrl + '/transcriptions');
  }

  get(id: number): Observable<Transcription> {
    return this.http.get<Transcription>(`${this.baseUrl}/transcriptions/${id}`);
  }

  create(request: TranscriptionRequest): Observable<Transcription> {
    return this.http.post<Transcription>(this.baseUrl + '/transcriptions', request);
  }

  update(id: number, request: TranscriptionRequest): Observable<Transcription> {
    return this.http.put<Transcription>(`${this.baseUrl}/transcriptions/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/transcriptions/${id}`);
  }
}
