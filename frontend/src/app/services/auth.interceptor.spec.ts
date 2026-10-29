import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => httpTesting.verify());

  it('does not attach a bearer token to login and attaches it to protected API requests', () => {
    auth.login({ email: 'person@example.com', password: 'correct-horse-battery' }).subscribe();
    const login = httpTesting.expectOne('/api/auth/login');
    expect(login.request.headers.has('Authorization')).toBe(false);
    login.flush({
      accessToken: 'memory-token',
      tokenType: 'Bearer',
      expiresAt: '2026-10-29T22:30:00Z',
      email: 'person@example.com',
    });

    http.get('/api/transcriptions').subscribe();
    const protectedRequest = httpTesting.expectOne('/api/transcriptions');
    expect(protectedRequest.request.headers.get('Authorization')).toBe('Bearer memory-token');
    protectedRequest.flush([]);
    expect(auth.isAuthenticated()).toBe(true);
  });
});
