import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('registers and keeps the returned bearer token in memory only', () => {
    const request = { email: 'person@example.com', password: 'correct-horse-battery' };
    service.register(request).subscribe((response) =>
      expect(response.email).toBe(request.email),
    );

    const httpRequest = httpTesting.expectOne('/api/auth/register');
    expect(httpRequest.request.method).toBe('POST');
    expect(httpRequest.request.body).toEqual(request);
    httpRequest.flush({
      accessToken: 'test-token',
      tokenType: 'Bearer',
      expiresAt: '2026-10-29T22:30:00Z',
      email: request.email,
    });

    expect(service.isAuthenticated()).toBe(true);
    expect(service.token()).toBe('test-token');
    expect(service.email()).toBe(request.email);
    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(sessionStorage.getItem('accessToken')).toBeNull();
  });

  it('clears the in-memory session on sign out', () => {
    service.clearSession();

    expect(service.isAuthenticated()).toBe(false);
    expect(service.token()).toBeNull();
    expect(service.email()).toBe('');
  });
});
