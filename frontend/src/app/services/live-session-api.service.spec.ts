import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../environments/environment';
import { LiveSessionApiService } from './live-session-api.service';

describe('LiveSessionApiService', () => {
  let service: LiveSessionApiService;
  let httpTesting: HttpTestingController;
  const baseUrl = environment.apiBaseUrl.replace(/\/$/, '');

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(LiveSessionApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('requests a short-lived live session token from the configured backend', () => {
    service.createToken().subscribe((response) =>
      expect(response).toEqual({
        token: 'authTokens/test-token',
        expiresAt: '2026-10-27T04:00:00Z',
        model: 'gemini-3.5-transcribe-live',
      }),
    );

    const request = httpTesting.expectOne(`${baseUrl}/live-sessions/token`);
    expect(request.request.method).toBe('POST');
    request.flush({
      token: 'authTokens/test-token',
      expiresAt: '2026-10-27T04:00:00Z',
      model: 'gemini-3.5-transcribe-live',
    });
  });
});
