import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { environment } from '../../environments/environment';
import { LiveTranscriptionPage } from './live-transcription.page';

describe('LiveTranscriptionPage', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LiveTranscriptionPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('renders recording controls and live transcript preview without requesting the microphone', async () => {
    const fixture = TestBed.createComponent(LiveTranscriptionPage);
    fixture.detectChanges();
    await fixture.whenStable();

    const content = fixture.nativeElement as HTMLElement;
    expect(content.textContent).toContain('Live transcription');
    expect(content.textContent).toContain('Ready to start');
    expect(content.textContent).toContain('Your speech will appear here');
    expect(content.querySelector('button')?.textContent).toContain('Start recording');
    expect(fixture.componentInstance.live.status()).toBe('idle');
  });

  it('validates transcript metadata before attempting to save', async () => {
    const fixture = TestBed.createComponent(LiveTranscriptionPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.save();
    fixture.detectChanges();

    expect(fixture.componentInstance.form.controls.title.touched).toBe(true);
    expect(fixture.componentInstance.saving()).toBe(false);
  });

  it('saves a transcript and presents the new recording action without deleting the saved record', async () => {
    const fixture = TestBed.createComponent(LiveTranscriptionPage);
    fixture.detectChanges();
    const page = fixture.componentInstance;
    page.live.finalizedText.set('Saved words');
    page.live.elapsedSeconds.set(42);
    page.form.setValue({ title: 'Meeting notes', language: 'English' });

    page.save();
    const request = httpTesting.expectOne(`${environment.apiBaseUrl.replace(/\/$/, '')}/transcriptions`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      title: 'Meeting notes',
      language: 'English',
      transcriptText: 'Saved words',
      durationSeconds: 42,
    });
    request.flush({
      id: 21,
      title: 'Meeting notes',
      language: 'English',
      transcriptText: 'Saved words',
      durationSeconds: 42,
      createdAt: '2026-10-28T00:00:00Z',
      updatedAt: '2026-10-28T00:00:00Z',
    });
    fixture.detectChanges();

    expect(page.saveSuccess()).toBe(true);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Transcript saved to My transcripts.');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('+ New recording');
    expect(page.live.finalizedText()).toBe('Saved words');
    httpTesting.expectNone((candidate) => candidate.method === 'DELETE');
  });

  it('resets the page after a successful save while preserving the saved record', async () => {
    const fixture = TestBed.createComponent(LiveTranscriptionPage);
    fixture.detectChanges();
    const page = fixture.componentInstance;
    page.live.finalizedText.set('Saved words');
    page.live.interimText.set('Preview');
    page.live.elapsedSeconds.set(42);
    page.live.error.set('Previous session error');
    page.form.setValue({ title: 'Meeting notes', language: 'English' });
    page.save();
    httpTesting.expectOne(`${environment.apiBaseUrl.replace(/\/$/, '')}/transcriptions`).flush({
      id: 21,
      title: 'Meeting notes',
      language: 'English',
      transcriptText: 'Saved words',
      durationSeconds: 42,
      createdAt: '2026-10-28T00:00:00Z',
      updatedAt: '2026-10-28T00:00:00Z',
    });
    fixture.detectChanges();

    const dispose = vi.spyOn(page.live, 'dispose');
    await page.newRecording();
    fixture.detectChanges();

    expect(dispose).toHaveBeenCalled();
    expect(page.live.finalizedText()).toBe('');
    expect(page.live.interimText()).toBe('');
    expect(page.live.elapsedSeconds()).toBe(0);
    expect(page.live.error()).toBe('');
    expect(page.form.getRawValue()).toEqual({ title: '', language: 'Auto-detected' });
    expect(page.saveSuccess()).toBe(false);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Ready to start');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('00:00');
    httpTesting.expectNone((candidate) => candidate.method === 'DELETE');
  });

  it('does not reset finalized text when saving fails', async () => {
    const fixture = TestBed.createComponent(LiveTranscriptionPage);
    fixture.detectChanges();
    const page = fixture.componentInstance;
    page.live.finalizedText.set('Keep this text');
    page.form.setValue({ title: 'Meeting notes', language: 'English' });
    page.save();
    httpTesting.expectOne(`${environment.apiBaseUrl.replace(/\/$/, '')}/transcriptions`).flush(
      { message: 'Save failed' },
      { status: 500, statusText: 'Server Error' },
    );

    expect(page.live.finalizedText()).toBe('Keep this text');
    expect(page.saveSuccess()).toBe(false);
    expect(page.saveError()).toBeTruthy();
  });

  it('prevents a new recording reset from discarding unsaved finalized text', async () => {
    const fixture = TestBed.createComponent(LiveTranscriptionPage);
    fixture.detectChanges();
    const page = fixture.componentInstance;
    page.live.finalizedText.set('Unsaved words');
    page.live.elapsedSeconds.set(12);
    page.form.setValue({ title: 'Draft title', language: 'Hindi' });

    await page.newRecording();

    expect(page.live.finalizedText()).toBe('Unsaved words');
    expect(page.live.elapsedSeconds()).toBe(12);
    expect(page.form.getRawValue()).toEqual({ title: 'Draft title', language: 'Hindi' });
    expect(page.live.error()).toContain('Save or discard this transcript');
  });
});
