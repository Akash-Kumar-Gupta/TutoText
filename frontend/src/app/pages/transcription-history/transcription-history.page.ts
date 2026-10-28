import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { getApiErrorMessage } from '../../services/api-error';
import { TranscriptionApiService } from '../../services/transcription-api.service';
import { Transcription } from '../../models/transcription.model';

@Component({
  selector: 'app-transcription-history-page',
  imports: [DatePipe, FormsModule, RouterLink],
  templateUrl: './transcription-history.page.html',
  styleUrl: './transcription-history.page.scss',
})
export class TranscriptionHistoryPage implements OnInit {
  private readonly api = inject(TranscriptionApiService);
  readonly transcripts = signal<Transcription[]>([]);
  readonly query = signal('');
  readonly loading = signal(true);
  readonly error = signal('');
  readonly success = signal('');

  readonly filteredTranscripts = computed(() => {
    const query = this.query().trim().toLocaleLowerCase();
    if (!query) return this.transcripts();
    return this.transcripts().filter((item) =>
      [item.title, item.language, item.transcriptText].some((value) =>
        value.toLocaleLowerCase().includes(query),
      ),
    );
  });

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

  delete(item: Transcription): void {
    if (!window.confirm(`Delete "${item.title}"? This cannot be undone.`)) return;

    this.error.set('');
    this.success.set('');
    this.api.delete(item.id).subscribe({
      next: () => {
        this.transcripts.update((items) => items.filter((transcript) => transcript.id !== item.id));
        this.success.set(`“${item.title}” was deleted.`);
      },
      error: (error: unknown) => this.error.set(getApiErrorMessage(error)),
    });
  }

  formatDuration(seconds: number): string {
    const minutes = Math.floor(seconds / 60);
    return minutes ? `${minutes}m ${seconds % 60}s` : `${seconds}s`;
  }

  preview(text: string): string {
    return text.length > 150 ? `${text.slice(0, 150).trim()}…` : text;
  }
}
