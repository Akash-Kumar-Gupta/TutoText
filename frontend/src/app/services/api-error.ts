import { HttpErrorResponse } from '@angular/common/http';
import { ApiErrorResponse } from '../models/transcription.model';

export function getApiErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Could not reach the server. Check that the TutoText backend is running and try again.';
    }

    const body = error.error as ApiErrorResponse | null;
    if (body?.validationErrors) {
      return Object.values(body.validationErrors).join(' ');
    }
    if (body?.message) {
      return body.message;
    }

    return `The server could not complete your request (HTTP ${error.status}).`;
  }

  return 'Something unexpected happened. Please try again.';
}
