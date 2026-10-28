export interface Transcription {
  id: number;
  title: string;
  transcriptText: string;
  language: string;
  durationSeconds: number;
  createdAt: string;
  updatedAt: string;
}

export interface TranscriptionRequest {
  title: string;
  transcriptText: string;
  language: string;
  durationSeconds: number;
}

export interface ApiErrorResponse {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
  validationErrors?: Record<string, string>;
}
