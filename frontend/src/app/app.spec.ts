import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), provideHttpClient()],
    }).compileComponents();
  });

  it('renders guest account actions and the router outlet', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.brand')?.textContent).toContain('TutoText');
    expect(compiled.querySelector('a[routerLink="/login"]')?.textContent).toContain('Sign in');
    expect(compiled.querySelector('a[routerLink="/register"]')?.textContent).toContain('Create account');
    expect(compiled.querySelector('router-outlet')).toBeTruthy();
  });
});
