import { Component, OnDestroy, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranscriptionRequest } from '../../models/transcription.model';
import { getApiErrorMessage } from '../../services/api-error';
import { LiveTranscriptionService } from '../../services/live-transcription.service';
import { TranscriptionApiService } from '../../services/transcription-api.service';

@Component({
  selector: 'app-live-transcription-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './live-transcription.page.html',
  styleUrl: './live-transcription.page.scss',
})
export class LiveTranscriptionPage implements OnDestroy {
  private readonly formBuilder = inject(FormBuilder);
  private readonly api = inject(TranscriptionApiService);

  readonly live = inject(LiveTranscriptionService);
  readonly saving = signal(false);
  readonly saveError = signal('');
  readonly saveSuccess = signal(false);
  readonly form = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(150)]],
    language: ['Auto-detected', [Validators.required, Validators.maxLength(30)]],
  });

  async start(): Promise<void> {
    this.saveError.set('');
    this.saveSuccess.set(false);
    await this.live.start();
  }

  async stop(): Promise<void> {
    await this.live.stop();
  }

  save(): void {
    this.saveError.set('');
    if (this.form.invalid || !this.live.finalizedText().trim() || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    const request: TranscriptionRequest = {
      ...this.form.getRawValue(),
      transcriptText: this.live.finalizedText().trim(),
      durationSeconds: this.live.elapsedSeconds(),
    };

    this.saving.set(true);
    this.api.create(request).subscribe({
      next: () => {
        this.saveSuccess.set(true);
        this.saving.set(false);
      },
      error: (error: unknown) => {
        this.saveError.set(getApiErrorMessage(error));
        this.saving.set(false);
      },
    });
  }

  async newRecording(): Promise<void> {
    if (this.saving()) return;
    if (this.live.finalizedText().trim() && !this.saveSuccess()) {
      this.live.error.set('Save or discard this transcript before starting another recording.');
      return;
    }

    await this.live.dispose();
    this.live.reset();
    this.form.reset({ title: '', language: 'Auto-detected' });
    this.form.markAsPristine();
    this.form.markAsUntouched();
    this.saveError.set('');
    this.saveSuccess.set(false);
  }

  formatElapsedTime(seconds: number): string {
    const minutes = Math.floor(seconds / 60).toString().padStart(2, '0');
    const remainder = (seconds % 60).toString().padStart(2, '0');
    return `${minutes}:${remainder}`;
  }

  ngOnDestroy(): void {
    void this.live.dispose();
  }
}
