import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthPage } from './auth.page';
import { AuthService } from '../../services/auth.service';

describe('AuthPage', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuthPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('renders account registration based on its route mode', async () => {
    const fixture = TestBed.createComponent(AuthPage);
    fixture.componentRef.setInput('mode', 'register');
    fixture.detectChanges();
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Create your account');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Already registered?');
  });

  it('validates credentials before sending a registration request', () => {
    const fixture = TestBed.createComponent(AuthPage);
    fixture.componentRef.setInput('mode', 'register');
    fixture.detectChanges();

    fixture.componentInstance.submit();

    expect(fixture.componentInstance.form.controls.email.touched).toBe(true);
    expect(fixture.componentInstance.form.controls.password.touched).toBe(true);
    httpTesting.expectNone('/api/auth/register');
  });

  it('submits registration credentials and stores the returned session in memory', async () => {
    const fixture = TestBed.createComponent(AuthPage);
    fixture.componentRef.setInput('mode', 'register');
    fixture.detectChanges();
    fixture.componentInstance.form.setValue({
      email: 'person@example.com',
      password: 'correct-horse-battery',
    });

    fixture.componentInstance.submit();
    const request = httpTesting.expectOne('/api/auth/register');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      email: 'person@example.com',
      password: 'correct-horse-battery',
    });
    request.flush({
      accessToken: 'registration-token',
      tokenType: 'Bearer',
      expiresAt: '2026-10-29T22:30:00Z',
      email: 'person@example.com',
    });
    await fixture.whenStable();

    expect(TestBed.inject(AuthService).isAuthenticated()).toBe(true);
  });
});
