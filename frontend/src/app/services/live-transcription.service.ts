import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { getApiErrorMessage } from './api-error';
import { LiveConnectionStatus } from '../models/live-session.model';
import { LiveSessionApiService } from './live-session-api.service';
import { isLiveSetupCompleteMessage, normalizeLiveSocketMessage } from './live-websocket-message';
import { pcm16BufferToBase64 } from './pcm-audio';

const LIVE_MODEL = 'gemini-3.5-transcribe-live';
const LIVE_SOCKET_URL =
  'wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContentConstrained';
const MAX_SESSION_SECONDS = 9 * 60 + 50;

@Injectable({ providedIn: 'root' })
export class LiveTranscriptionService {
  private readonly tokenApi = inject(LiveSessionApiService);

  readonly status = signal<LiveConnectionStatus>('idle');
  readonly interimText = signal('');
  readonly finalizedText = signal('');
  readonly elapsedSeconds = signal(0);
  readonly error = signal('');
  readonly sessionLimitReached = signal(false);

  private socket: WebSocket | null = null;
  private stream: MediaStream | null = null;
  private audioContext: AudioContext | null = null;
  private source: MediaStreamAudioSourceNode | null = null;
  private processor: AudioWorkletNode | null = null;
  private muteGain: GainNode | null = null;
  private elapsedTimer: ReturnType<typeof setInterval> | null = null;
  private setupComplete = false;
  private expectedClose = false;
  private stopping = false;
  private disposed = false;
  private turnCompleteHandler: (() => void) | null = null;

  async start(): Promise<void> {
    if (!['idle', 'error'].includes(this.status())) return;
    if (this.finalizedText().trim()) {
      this.error.set('Save or discard this transcript before starting another recording.');
      return;
    }

    this.reset();
    this.disposed = false;
    this.status.set('requesting-microphone');

    try {
      if (!navigator.mediaDevices?.getUserMedia) {
        throw new Error('Microphone capture is not supported by this browser.');
      }

      this.stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          channelCount: { ideal: 1 },
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
        },
      });
      if (this.disposed) {
        await this.releaseMedia();
        return;
      }

      this.status.set('connecting');
      const tokenResponse = await firstValueFrom(this.tokenApi.createToken());
      if (this.disposed) return;
      if (tokenResponse.model !== LIVE_MODEL) {
        throw new Error('The server returned an unexpected Live API model.');
      }

      await this.openLiveSocket(tokenResponse.token);
      if (this.disposed) {
        await this.releaseMedia();
        this.closeSocket();
        return;
      }
      await this.startAudioCapture();
      if (this.disposed) {
        await this.releaseMedia();
        this.closeSocket();
        return;
      }
      this.status.set('recording');
      this.startElapsedTimer();
    } catch (error) {
      await this.releaseMedia();
      this.closeSocket();
      if (this.disposed) return;
      this.status.set('error');
      this.error.set(this.describeStartError(error));
    }
  }

  pause(): void {
    if (this.status() !== 'recording') return;
    this.stream?.getAudioTracks().forEach((track) => (track.enabled = false));
    this.stopElapsedTimer();
    this.status.set('paused');
  }

  resume(): void {
    if (this.status() !== 'paused') return;
    this.stream?.getAudioTracks().forEach((track) => (track.enabled = true));
    this.status.set('recording');
    this.startElapsedTimer();
  }

  async stop(): Promise<void> {
    if (!['recording', 'paused'].includes(this.status()) || this.stopping) return;
    this.stopping = true;
    this.status.set('stopping');
    this.stopElapsedTimer();
    await this.releaseMedia();

    if (this.socket?.readyState === WebSocket.OPEN) {
      const turnComplete = new Promise<void>((resolve) => {
        this.turnCompleteHandler = resolve;
        this.socket?.send(JSON.stringify({ realtimeInput: { audioStreamEnd: true } }));
      });
      await Promise.race([turnComplete, this.delay(2500)]);
      this.turnCompleteHandler = null;
    }

    this.expectedClose = true;
    this.closeSocket();
    this.status.set('idle');
    this.stopping = false;
  }

  async dispose(): Promise<void> {
    this.disposed = true;
    this.stopElapsedTimer();
    this.expectedClose = true;
    await this.releaseMedia();
    this.closeSocket();
    this.status.set('idle');
  }

  reset(): void {
    this.interimText.set('');
    this.finalizedText.set('');
    this.elapsedSeconds.set(0);
    this.error.set('');
    this.sessionLimitReached.set(false);
    this.setupComplete = false;
    this.expectedClose = false;
    this.stopping = false;
  }

  private async openLiveSocket(token: string): Promise<void> {
    const url = new URL(LIVE_SOCKET_URL);
    url.searchParams.set('access_token', token);

    await new Promise<void>((resolve, reject) => {
      const timeout = setTimeout(() => reject(new Error('Timed out while connecting to Gemini Live API.')), 15_000);
      let incomingMessages = Promise.resolve();
      let incomingMessageFailed = false;
      this.socket = new WebSocket(url.toString());

      this.socket.onopen = () => {
        this.socket?.send(
          JSON.stringify({
            setup: {
              model: `models/${LIVE_MODEL}`,
              generationConfig: { responseModalities: ['TEXT'] },
              inputAudioTranscription: { languageCodes: [] },
            },
          }),
        );
      };

      this.socket.onmessage = (event: MessageEvent) => {
        incomingMessages = incomingMessages
          .then(async () => {
            if (incomingMessageFailed) return;
            const message = await normalizeLiveSocketMessage(event.data);
            if (message === null) {
              if (!this.setupComplete) {
                throw new Error('Gemini Live API sent an unsupported setup response type.');
              }
              return;
            }

            this.handleMessage(message);
            if (!this.setupComplete && this.socketReadyMessage(message)) {
              this.setupComplete = true;
              clearTimeout(timeout);
              resolve();
            }
          })
          .catch((error: unknown) => {
            incomingMessageFailed = true;
            clearTimeout(timeout);
            reject(error);
          });
      };

      this.socket.onerror = () => {
        if (!this.setupComplete) {
          clearTimeout(timeout);
          reject(new Error('Could not connect to Gemini Live API. Check network access and model availability.'));
        } else if (!this.expectedClose) {
          this.error.set('The connection to Gemini Live API was interrupted.');
          this.expectedClose = true;
          this.closeSocket();
          void this.handleUnexpectedClose();
        }
      };

      this.socket.onclose = () => {
        clearTimeout(timeout);
        if (!this.setupComplete) {
          reject(new Error('Gemini Live API closed the connection before setup completed.'));
          return;
        }
        if (!this.expectedClose && !this.stopping) {
          void this.handleUnexpectedClose();
        }
      };
    });
  }

  private socketReadyMessage(message: string): boolean {
    return isLiveSetupCompleteMessage(message);
  }

  private async startAudioCapture(): Promise<void> {
    const stream = this.stream;
    if (!stream) throw new Error('Microphone stream is unavailable.');

    const audioContext = new AudioContext({ sampleRate: 16_000 });
    if (audioContext.sampleRate !== 16_000) {
      await audioContext.close();
      throw new Error('This browser could not provide the required 16 kHz audio format.');
    }

    await audioContext.audioWorklet.addModule('/pcm16-capture.js');
    const source = audioContext.createMediaStreamSource(stream);
    const processor = new AudioWorkletNode(audioContext, 'pcm16-capture', {
      numberOfInputs: 1,
      numberOfOutputs: 1,
      channelCount: 1,
      channelCountMode: 'explicit',
      outputChannelCount: [1],
    });
    const muteGain = audioContext.createGain();
    muteGain.gain.value = 0;
    processor.port.onmessage = (event: MessageEvent<ArrayBuffer>) => {
      if (this.socket?.readyState !== WebSocket.OPEN || this.status() !== 'recording') return;
      this.socket.send(
        JSON.stringify({
          realtimeInput: {
            audio: {
              data: pcm16BufferToBase64(event.data),
              mimeType: 'audio/pcm;rate=16000',
            },
          },
        }),
      );
    };

    source.connect(processor);
    processor.connect(muteGain);
    muteGain.connect(audioContext.destination);
    this.audioContext = audioContext;
    this.source = source;
    this.processor = processor;
    this.muteGain = muteGain;
  }

  private handleMessage(message: string): void {
    let response: {
      serverContent?: {
        interimInputTranscription?: { text?: string };
        inputTranscription?: { text?: string };
        turnComplete?: boolean;
      };
      error?: { code?: number };
    };

    try {
      response = JSON.parse(message) as typeof response;
    } catch {
      this.error.set('Gemini Live API returned an unreadable response.');
      return;
    }

    if (response.error) {
      if (this.setupComplete) {
        this.error.set(
          `Gemini Live API reported an error${response.error.code ? ` (${response.error.code})` : ''}. Check model access and usage limits.`,
        );
        this.expectedClose = true;
        this.closeSocket();
        void this.handleUnexpectedClose();
        return;
      }
      this.error.set(
        `Gemini Live API reported an error${response.error.code ? ` (${response.error.code})` : ''}. Check model access and usage limits.`,
      );
      return;
    }

    const content = response.serverContent;
    if (!content) return;

    if (content.interimInputTranscription?.text !== undefined) {
      this.interimText.set(content.interimInputTranscription.text);
    }

    const finalized = content.inputTranscription?.text?.trim();
    if (finalized) {
      this.finalizedText.update((current) => (current ? `${current}\n${finalized}` : finalized));
      this.interimText.set('');
    }

    if (content.turnComplete) {
      this.turnCompleteHandler?.();
      this.turnCompleteHandler = null;
    }
  }

  private startElapsedTimer(): void {
    this.stopElapsedTimer();
    this.elapsedTimer = setInterval(() => {
      const next = this.elapsedSeconds() + 1;
      this.elapsedSeconds.set(next);
      if (next >= MAX_SESSION_SECONDS) {
        this.sessionLimitReached.set(true);
        void this.stop();
      }
    }, 1000);
  }

  private stopElapsedTimer(): void {
    if (this.elapsedTimer !== null) {
      clearInterval(this.elapsedTimer);
      this.elapsedTimer = null;
    }
  }

  private async releaseMedia(): Promise<void> {
    this.processor?.port.close();
    this.processor?.disconnect();
    this.source?.disconnect();
    this.muteGain?.disconnect();
    this.stream?.getTracks().forEach((track) => track.stop());
    this.processor = null;
    this.source = null;
    this.muteGain = null;
    this.stream = null;

    if (this.audioContext && this.audioContext.state !== 'closed') {
      await this.audioContext.close();
    }
    this.audioContext = null;
  }

  private closeSocket(): void {
    if (this.socket && this.socket.readyState < WebSocket.CLOSING) {
      this.socket.close(1000, 'Transcription stopped');
    }
    this.socket = null;
  }

  private async handleUnexpectedClose(): Promise<void> {
    this.stopElapsedTimer();
    this.closeSocket();
    await this.releaseMedia();
    if (!this.error()) {
      this.error.set('The Live API connection ended. You can save the finalized words received so far.');
    }
    this.status.set('error');
  }

  private describeStartError(error: unknown): string {
    if (error instanceof DOMException && error.name === 'NotAllowedError') {
      return 'Microphone permission was denied. Allow microphone access in your browser settings, then try again.';
    }
    if (error instanceof DOMException && error.name === 'NotFoundError') {
      return 'No microphone was found. Connect a microphone and try again.';
    }
    if (error instanceof Error && !(error.name === 'HttpErrorResponse')) {
      return error.message;
    }
    return getApiErrorMessage(error);
  }

  private delay(milliseconds: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, milliseconds));
  }
}
