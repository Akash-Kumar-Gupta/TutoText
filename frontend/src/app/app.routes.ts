import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/dashboard/dashboard.page').then((page) => page.DashboardPage),
    title: 'Dashboard · TutoText',
  },
  {
    path: 'transcriptions',
    loadComponent: () =>
      import('./pages/transcription-history/transcription-history.page').then(
        (page) => page.TranscriptionHistoryPage,
      ),
    title: 'Transcript history · TutoText',
  },
  {
    path: 'transcriptions/new',
    loadComponent: () =>
      import('./pages/transcription-form/transcription-form.page').then((page) => page.TranscriptionFormPage),
    title: 'New transcription · TutoText',
  },
  {
    path: 'transcriptions/live',
    loadComponent: () =>
      import('./pages/live-transcription/live-transcription.page').then((page) => page.LiveTranscriptionPage),
    title: 'Live transcription · TutoText',
  },
  {
    path: 'transcriptions/:id',
    loadComponent: () =>
      import('./pages/transcription-detail/transcription-detail.page').then(
        (page) => page.TranscriptionDetailPage,
      ),
    title: 'Transcript · TutoText',
  },
  { path: '**', redirectTo: '' },
];
