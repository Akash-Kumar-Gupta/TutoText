import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { getApiErrorMessage } from '../../services/api-error';
import { TranscriptionApiService } from '../../services/transcription-api.service';
import { Transcription } from '../../models/transcription.model';

@Component({
  selector: 'app-transcription-detail-page',
  imports: [DatePipe, ReactiveFormsModule, RouterLink],
  templateUrl: './transcription-detail.page.html',
  styleUrl: './transcription-detail.page.scss',
})
export class TranscriptionDetailPage implements OnInit {
  private readonly api = inject(TranscriptionApiService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);

  readonly transcription = signal<Transcription | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly editing = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  private id = 0;

  readonly form = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(150)]],
    language: ['', [Validators.required, Validators.maxLength(30)]],
    transcriptText: ['', [Validators.required, Validators.maxLength(4_000_000)]],
    durationSeconds: [0, [Validators.required, Validators.min(0), Validators.max(2_147_483_647)]],
  });

  ngOnInit(): void {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    if (this.route.snapshot.queryParamMap.get('saved') === '1') {
      this.success.set('Your transcript has been saved.');
    }
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.get(this.id).subscribe({
      next: (item) => {
        this.transcription.set(item);
        this.form.reset({
          title: item.title,
          language: item.language,
          transcriptText: item.transcriptText,
          durationSeconds: item.durationSeconds,
        });
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(getApiErrorMessage(error));
        this.loading.set(false);
      },
    });
  }

  beginEdit(): void {
    this.success.set('');
    this.error.set('');
    this.editing.set(true);
  }

  cancelEdit(): void {
    const item = this.transcription();
    if (item) {
      this.form.reset({
        title: item.title,
        language: item.language,
        transcriptText: item.transcriptText,
        durationSeconds: item.durationSeconds,
      });
    }
    this.editing.set(false);
    this.error.set('');
  }

  save(): void {
    this.error.set('');
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.api.update(this.id, this.form.getRawValue()).subscribe({
      next: (item) => {
        this.transcription.set(item);
        this.editing.set(false);
        this.saving.set(false);
        this.success.set('Your changes have been saved.');
      },
      error: (error: unknown) => {
        this.error.set(getApiErrorMessage(error));
        this.saving.set(false);
      },
    });
  }

  formatDuration(seconds: number): string {
    const minutes = Math.floor(seconds / 60);
    return minutes ? `${minutes}m ${seconds % 60}s` : `${seconds}s`;
  }
}
