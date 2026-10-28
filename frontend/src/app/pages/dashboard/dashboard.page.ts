import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { getApiErrorMessage } from '../../services/api-error';
import { TranscriptionApiService } from '../../services/transcription-api.service';
import { Transcription } from '../../models/transcription.model';

@Component({
  selector: 'app-dashboard-page',
  imports: [DatePipe, RouterLink],
  templateUrl: './dashboard.page.html',
  styleUrl: './dashboard.page.scss',
})
export class DashboardPage implements OnInit {
  private readonly api = inject(TranscriptionApiService);
  readonly transcripts = signal<Transcription[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');

  readonly totalDuration = () =>
    this.transcripts().reduce((total, item) => total + item.durationSeconds, 0);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.list().subscribe({
      next: (items) => {
        this.transcripts.set(items);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(getApiErrorMessage(error));
        this.loading.set(false);
      },
    });
  }

  formatDuration(seconds: number): string {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return minutes ? `${minutes}m ${remainingSeconds}s` : `${remainingSeconds}s`;
  }
}
