import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { provideRouter } from '@angular/router';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideRouter([])],
    });
  });

  it('redirects guests to login while preserving the requested route', () => {
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url: '/transcriptions/live' } as RouterStateSnapshot),
    );
    const router = TestBed.inject(Router);

    expect(result).toEqual(
      router.createUrlTree(['/login'], { queryParams: { returnUrl: '/transcriptions/live' } }),
    );
  });
});
