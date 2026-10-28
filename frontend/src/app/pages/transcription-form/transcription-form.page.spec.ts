import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { TranscriptionFormPage } from './transcription-form.page';

describe('TranscriptionFormPage', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TranscriptionFormPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
  });

  it('clearly identifies manual text entry and does not submit an invalid form', async () => {
    const fixture = TestBed.createComponent(TranscriptionFormPage);
    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Manual text entry');
    expect(compiled.textContent).toContain('Open live transcription');

    fixture.componentInstance.save();
    fixture.detectChanges();

    expect(fixture.componentInstance.form.controls.title.touched).toBe(true);
    expect(fixture.componentInstance.form.controls.transcriptText.touched).toBe(true);
    expect(fixture.componentInstance.saving()).toBe(false);
    expect(compiled.querySelector('.field-error')?.textContent).toContain('title');
  });
});
