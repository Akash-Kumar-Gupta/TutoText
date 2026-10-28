import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { getApiErrorMessage } from '../../services/api-error';
import { TranscriptionApiService } from '../../services/transcription-api.service';

@Component({
  selector: 'app-transcription-form-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './transcription-form.page.html',
  styleUrl: './transcription-form.page.scss',
})
export class TranscriptionFormPage {
  private readonly formBuilder = inject(FormBuilder);
  private readonly api = inject(TranscriptionApiService);
  private readonly router = inject(Router);

  readonly saving = signal(false);
  readonly error = signal('');
  readonly form = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(150)]],
    language: ['English', [Validators.required, Validators.maxLength(30)]],
    transcriptText: ['', [Validators.required, Validators.maxLength(4_000_000)]],
    durationSeconds: [0, [Validators.required, Validators.min(0), Validators.max(2_147_483_647)]],
  });

  save(): void {
    this.error.set('');
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.api.create(this.form.getRawValue()).subscribe({
      next: (created) =>
        void this.router.navigate(['/transcriptions', created.id], { queryParams: { saved: '1' } }),
      error: (error: unknown) => {
        this.error.set(getApiErrorMessage(error));
        this.saving.set(false);
      },
    });
  }
}
