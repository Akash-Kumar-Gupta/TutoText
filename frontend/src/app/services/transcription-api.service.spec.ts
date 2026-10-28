import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../environments/environment';
import { Transcription, TranscriptionRequest } from '../models/transcription.model';
import { TranscriptionApiService } from './transcription-api.service';

describe('TranscriptionApiService', () => {
  let service: TranscriptionApiService;
  let httpTesting: HttpTestingController;
  const baseUrl = environment.apiBaseUrl.replace(/\/$/, '');
  const sample: Transcription = {
    id: 12,
    title: 'Planning session',
    transcriptText: 'A useful idea.',
    language: 'English',
    durationSeconds: 90,
    createdAt: '2026-10-27T10:00:00Z',
    updatedAt: '2026-10-27T10:00:00Z',
  };
  const request: TranscriptionRequest = {
    title: sample.title,
    transcriptText: sample.transcriptText,
    language: sample.language,
    durationSeconds: sample.durationSeconds,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TranscriptionApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('lists transcripts from the configured API base URL', () => {
    service.list().subscribe((items) => expect(items).toEqual([sample]));

    const req = httpTesting.expectOne(`${baseUrl}/transcriptions`);
    expect(req.request.method).toBe('GET');
    req.flush([sample]);
  });

  it('gets a transcript by ID', () => {
    service.get(sample.id).subscribe((item) => expect(item).toEqual(sample));

    const req = httpTesting.expectOne(`${baseUrl}/transcriptions/${sample.id}`);
    expect(req.request.method).toBe('GET');
    req.flush(sample);
  });

  it('creates and updates transcripts using the backend request shape', () => {
    service.create(request).subscribe((item) => expect(item).toEqual(sample));
    const createReq = httpTesting.expectOne(`${baseUrl}/transcriptions`);
    expect(createReq.request.method).toBe('POST');
    expect(createReq.request.body).toEqual(request);
    createReq.flush(sample);

    service.update(sample.id, request).subscribe((item) => expect(item).toEqual(sample));
    const updateReq = httpTesting.expectOne(`${baseUrl}/transcriptions/${sample.id}`);
    expect(updateReq.request.method).toBe('PUT');
    expect(updateReq.request.body).toEqual(request);
    updateReq.flush(sample);
  });

  it('deletes a transcript by ID', () => {
    service.delete(sample.id).subscribe();

    const req = httpTesting.expectOne(`${baseUrl}/transcriptions/${sample.id}`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
