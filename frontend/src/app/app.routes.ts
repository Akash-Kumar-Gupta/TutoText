import { Routes } from '@angular/router';
import { authGuard } from './services/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/auth/auth.page').then((page) => page.AuthPage),
    data: { mode: 'login' },
    title: 'Sign in · TutoText',
  },
  {
    path: 'register',
    loadComponent: () => import('./pages/auth/auth.page').then((page) => page.AuthPage),
    data: { mode: 'register' },
    title: 'Create account · TutoText',
  },
  {
    path: '',
    loadComponent: () => import('./pages/dashboard/dashboard.page').then((page) => page.DashboardPage),
    canActivate: [authGuard],
    title: 'Dashboard · TutoText',
  },
  {
    path: 'transcriptions',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/transcription-history/transcription-history.page').then(
        (page) => page.TranscriptionHistoryPage,
      ),
    title: 'Transcript history · TutoText',
  },
  {
    path: 'transcriptions/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/transcription-form/transcription-form.page').then((page) => page.TranscriptionFormPage),
    title: 'New transcription · TutoText',
  },
  {
    path: 'transcriptions/live',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/live-transcription/live-transcription.page').then((page) => page.LiveTranscriptionPage),
    title: 'Live transcription · TutoText',
  },
  {
    path: 'transcriptions/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/transcription-detail/transcription-detail.page').then(
        (page) => page.TranscriptionDetailPage,
      ),
    title: 'Transcript · TutoText',
  },
  { path: '**', redirectTo: '' },
];
